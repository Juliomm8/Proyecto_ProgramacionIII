package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.Nino;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;
import com.jasgames.util.EmojiFonts;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * JuegosPanel (Modo Docente)
 *
 * Versión "wow + intuitiva":
 * - Catálogo y asignación en JTable con zebra y búsqueda.
 * - Multiselección para habilitar/deshabilitar (y aplicar dificultad global) a varios juegos.
 * - Editor claro para dificultad por estudiante:
 *      - "Usar global" vs "Personalizar" (override).
 *      - Aplicar a la selección (multi-fila).
 *
 * Nota: El .form fue eliminado (este panel se construye 100% por código).
 */
public class JuegosPanel extends JPanel {

    // Campos (se mantienen nombres por compatibilidad con el resto del proyecto)
    private JPanel panelJuegos;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JLabel lblTituloJuegos;
    private JButton btnGuardarEstadoJuegos;
    private JLabel lblSeleccionNino;
    private JComboBox cboNinos;
    private JLabel lblTituloAsignacion;
    private JScrollPane panelAsignacionJuegos;
    private JButton btnGuardarAsignacion;

    // Servicios
    private final JuegoService juegoService;
    private final PerfilService perfilService;

    // Cache
    private List<Juego> cacheJuegos = new ArrayList<>();
    private List<Nino> cacheNinos = new ArrayList<>();

    // Pendientes (catálogo) para no guardar hasta el botón
    private final Map<Integer, Boolean> pendingHabilitado = new HashMap<>();
    private final Map<Integer, Integer> pendingDificultadGlobal = new HashMap<>();

    // UI general
    private JLabel lblResumenCatalogo;
    private JLabel lblResumenAsignacion;
    private JButton btnRefrescar;

    // Catálogo (acciones)
    private JTextField txtBuscarCatalogo;
    private JButton btnHabilitarSel;
    private JButton btnDeshabilitarSel;
    private JButton btnMasCatalogo;
    private JButton btnAsignarSelATodos;
    private JSpinner spDifGlobalSel;
    private JButton btnAplicarDifGlobalSel;

    // Asignación (acciones)
    private JTextField txtBuscarAsignacion;

    // Selector de estudiante (más intuitivo para listas grandes)
    private JComboBox<String> cboFiltroAula;
    private JTextField txtFiltroId;
    private JComboBox<String> cboOrdenNino;
    private JButton btnLimpiarFiltrosNino;
    private JLabel lblNinosFiltro;

    private JButton btnListaEstudiantes;
    private JCheckBox chkSoloAsignados;
    private JButton btnAsignarSel;
    private JButton btnQuitarSel;
    private JButton btnMasAsignacion;

    // Debounce para filtros (evita refrescos excesivos cuando se escribe rápido)
    private Timer tDebounceNinos;
    private boolean ignoreNinoEvents = false;

    // Editor intuitivo de dificultad por estudiante
    private JLabel lblEditorJuego;
    private JRadioButton rbUsarGlobal;
    private JRadioButton rbPersonalizar;
    private JSpinner spDifPersonal;
    private JButton btnAplicarEditor;
    private JButton btnResetGlobalSel;

    // Tablas
    private JTable tblCatalogo;
    private JTable tblAsignacion;

    private CatalogoTableModel catalogoModel;
    private AsignacionTableModel asignacionModel;

    private TableRowSorter<CatalogoTableModel> sorterCatalogo;
    private TableRowSorter<AsignacionTableModel> sorterAsignacion;

    private String resumenAsignacionBase = " ";

    // Barra de estado global (DocenteWindow)
    private final Consumer<String> statusSink;

    private void status(String msg) {
        try {
            if (statusSink != null) statusSink.accept(msg);
        } catch (Exception ignored) {
        }
    }

    public JuegosPanel(JuegoService juegoService, PerfilService perfilService, Consumer<String> statusSink) {
        this.juegoService = juegoService;
        this.perfilService = perfilService;
        this.statusSink = (statusSink != null) ? statusSink : (m) -> {};

        initUI();
        cargarJuegos();
        cargarNinos();
        initListeners();
    }

    // Back-compat
    public JuegosPanel(JuegoService juegoService, PerfilService perfilService) {
        this(juegoService, perfilService, null);
    }

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(14, 14, 14, 14));

        add(buildHeader(), BorderLayout.NORTH);

        panelIzquierdo = buildCatalogoPanel();
        panelDerecho = buildAsignacionPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        split.setResizeWeight(0.52);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        add(split, BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Gestión de Juegos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitle = new JLabel("Catálogo (global) y asignación por estudiante (override personal).");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(new Color(90, 90, 90));

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        btnRefrescar = new JButton("Refrescar");
        actions.add(btnRefrescar);

        header.add(titles, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCatalogoPanel() {
        JPanel card = new CardPanel(new BorderLayout(10, 10));

        // TOP
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        lblTituloJuegos = new JLabel("Catálogo del sistema (GLOBAL)");
        lblTituloJuegos.setFont(lblTituloJuegos.getFont().deriveFont(Font.BOLD, 15f));

        lblResumenCatalogo = new JLabel(" ");
        lblResumenCatalogo.setFont(lblResumenCatalogo.getFont().deriveFont(Font.PLAIN, 12f));
        lblResumenCatalogo.setForeground(new Color(90, 90, 90));

        titleBlock.add(lblTituloJuegos);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblResumenCatalogo);

        // Acciones fila 1
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        txtBuscarCatalogo = new JTextField(18);
        txtBuscarCatalogo.setToolTipText("Buscar por nombre, tipo o descripción");

        btnHabilitarSel = new JButton("Habilitar selección");
        btnDeshabilitarSel = new JButton("Deshabilitar selección");
        btnAsignarSelATodos = new JButton("Asignar selección a TODOS");
        btnAsignarSelATodos.setToolTipText("Asigna el/los juego(s) seleccionado(s) a todos los estudiantes");

        btnMasCatalogo = new JButton("Más ▾");
        btnMasCatalogo.setToolTipText("Acciones extra (todo)");

        actions.add(new JLabel("Buscar:"));
        actions.add(txtBuscarCatalogo);
        actions.add(btnHabilitarSel);
        actions.add(btnDeshabilitarSel);
        actions.add(btnAsignarSelATodos);
        actions.add(btnMasCatalogo);

        // Acciones fila 2: dificultad global a selección
        JPanel actions2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions2.setOpaque(false);

        spDifGlobalSel = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        ((JSpinner.DefaultEditor) spDifGlobalSel.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) spDifGlobalSel.getEditor()).getTextField().setHorizontalAlignment(SwingConstants.CENTER);

        btnAplicarDifGlobalSel = new JButton("Aplicar dificultad global a selección");
        btnAplicarDifGlobalSel.setToolTipText("Cambia la dificultad GLOBAL (catálogo) para los juegos seleccionados");

        JLabel hint = new JLabel("Tip: global es el valor por defecto. En la derecha puedes 'personalizar' por estudiante.");
        hint.setForeground(new Color(110, 110, 110));
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11.5f));

        actions2.add(new JLabel("Dificultad global:"));
        actions2.add(spDifGlobalSel);
        actions2.add(btnAplicarDifGlobalSel);
        actions2.add(Box.createHorizontalStrut(10));
        actions2.add(hint);

        top.add(titleBlock);
        top.add(Box.createVerticalStrut(8));
        top.add(actions);
        top.add(Box.createVerticalStrut(6));
        top.add(actions2);

        // Tabla
        catalogoModel = new CatalogoTableModel();
        tblCatalogo = createProTable(catalogoModel);
        sorterCatalogo = new TableRowSorter<>(catalogoModel);
        tblCatalogo.setRowSorter(sorterCatalogo);

        // Multiselección (lo que pediste)
        tblCatalogo.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Editor Spinner para dificultad (edición por celda sigue disponible)
        TableColumn colDif = tblCatalogo.getColumnModel().getColumn(CatalogoTableModel.COL_DIFICULTAD);
        colDif.setCellEditor(new SpinnerEditor(1, 5));

        // Anchos
        setColWidth(tblCatalogo, CatalogoTableModel.COL_ID, 50);
        setColWidth(tblCatalogo, CatalogoTableModel.COL_TIPO, 95);
        setColWidth(tblCatalogo, CatalogoTableModel.COL_DIFICULTAD, 60);
        setColWidth(tblCatalogo, CatalogoTableModel.COL_HABILITADO, 80);

        JScrollPane scroll = new JScrollPane(tblCatalogo);
        styleTableScroll(scroll);

        // BOTTOM
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 18)));

        btnGuardarEstadoJuegos = new JButton("Guardar cambios del catálogo");
        btnGuardarEstadoJuegos.setToolTipText("Guarda habilitado + dificultad global de los juegos");
        bottom.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        bottom.add(btnGuardarEstadoJuegos, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAsignacionPanel() {
        JPanel card = new CardPanel(new BorderLayout(10, 10));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        lblTituloAsignacion = new JLabel("Asignación por estudiante (INDIVIDUAL)");
        lblTituloAsignacion.setFont(lblTituloAsignacion.getFont().deriveFont(Font.BOLD, 15f));

        lblResumenAsignacion = new JLabel(" ");
        lblResumenAsignacion.setFont(lblResumenAsignacion.getFont().deriveFont(Font.PLAIN, 12f));
        lblResumenAsignacion.setForeground(new Color(90, 90, 90));

        titleBlock.add(lblTituloAsignacion);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblResumenAsignacion);


        // Bloque de selección de estudiante (2 filas, sin "wrap" cortado)
        JPanel rowStudent = new JPanel(new GridBagLayout());
        rowStudent.setOpaque(false);

        GridBagConstraints cs = new GridBagConstraints();
        cs.insets = new Insets(2, 0, 2, 8);
        cs.anchor = GridBagConstraints.WEST;
        cs.fill = GridBagConstraints.NONE;
        cs.gridy = 0;

        // Filtros intuitivos (para 500+ estudiantes):
        // 1) Filtra por aula, 2) (opcional) salta por ID, 3) ordena por Nombre/ID.
        cboFiltroAula = new JComboBox<>();
        cboFiltroAula.setPreferredSize(new Dimension(150, cboFiltroAula.getPreferredSize().height));
        cboFiltroAula.setToolTipText("Filtrar estudiantes por aula");

        txtFiltroId = new JTextField();
        txtFiltroId.setColumns(14);
        txtFiltroId.setToolTipText("Buscar por ID o nombre (ej: 23, julio, aula azul)");
        Dimension idSize = new Dimension(180, txtFiltroId.getPreferredSize().height);
        txtFiltroId.setPreferredSize(idSize);
        txtFiltroId.setMinimumSize(idSize);
        txtFiltroId.setEditable(true);
        txtFiltroId.setEnabled(true);
        cboOrdenNino = new JComboBox(new String[]{"Nombre (A-Z)", "ID (asc)", "Aula → Nombre"});
        cboOrdenNino.setPreferredSize(new Dimension(140, cboOrdenNino.getPreferredSize().height));
        cboOrdenNino.setToolTipText("Ordenar lista de estudiantes");

        btnLimpiarFiltrosNino = new JButton("Limpiar");
        btnLimpiarFiltrosNino.setToolTipText("Quitar filtros de aula/ID");

        lblNinosFiltro = new JLabel(" ");
        lblNinosFiltro.setFont(lblNinosFiltro.getFont().deriveFont(Font.PLAIN, 11.5f));
        lblNinosFiltro.setForeground(new Color(110, 110, 110));

        lblSeleccionNino = new JLabel("Estudiante:");
        cboNinos = new JComboBox();
        cboNinos.setPreferredSize(new Dimension(420, cboNinos.getPreferredSize().height));
        cboNinos.setMaximumRowCount(20);
        cboNinos.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof com.jasgames.model.Nino n) {
                    String av = (n.getAvatar() != null && !n.getAvatar().isBlank()) ? n.getAvatar() : "🙂";
                    lbl.setText(av + "  " + n.getNombre() + "  ·  " + n.getAula() + "  ·  id=" + n.getId());
                }
                lbl.setFont(EmojiFonts.emoji(14f));
                return lbl;
            }
        });

        // Fila 1: filtros
        cs.gridx = 0; cs.weightx = 0;
        rowStudent.add(new JLabel("Aula:"), cs);

        cs.gridx = 1;
        rowStudent.add(cboFiltroAula, cs);

        cs.gridx = 2;
        rowStudent.add(new JLabel("Buscar:"), cs);

        cs.gridx = 3;
        cs.fill = GridBagConstraints.HORIZONTAL;
        rowStudent.add(txtFiltroId, cs);
        cs.fill = GridBagConstraints.NONE;

        cs.gridx = 4;
        rowStudent.add(new JLabel("Orden:"), cs);

        cs.gridx = 5;
        rowStudent.add(cboOrdenNino, cs);

        cs.gridx = 6;
        rowStudent.add(btnLimpiarFiltrosNino, cs);

        cs.gridx = 7;
        cs.weightx = 1;
        cs.fill = GridBagConstraints.HORIZONTAL;
        rowStudent.add(lblNinosFiltro, cs);

        // Fila 2: combo estudiante (SIEMPRE visible)
        cs.gridy = 1;
        cs.gridx = 0;
        cs.weightx = 0;
        cs.fill = GridBagConstraints.NONE;
        rowStudent.add(lblSeleccionNino, cs);

        cs.gridx = 1;
        cs.gridwidth = 6;
        cs.weightx = 1;
        cs.fill = GridBagConstraints.HORIZONTAL;
        rowStudent.add(cboNinos, cs);

        btnListaEstudiantes = new JButton("Lista…");
        btnListaEstudiantes.setToolTipText("Abrir lista completa (tabla) para buscar/seleccionar estudiante");
        cs.gridx = 7;
        cs.gridwidth = 1;
        cs.weightx = 0;
        cs.fill = GridBagConstraints.NONE;
        rowStudent.add(btnListaEstudiantes, cs);
        JPanel rowFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowFilters.setOpaque(false);

        txtBuscarAsignacion = new JTextField(16);
        txtBuscarAsignacion.setToolTipText("Filtra por juego");

        chkSoloAsignados = new JCheckBox("Solo asignados");
        chkSoloAsignados.setOpaque(false);

        btnAsignarSel = new JButton("Asignar selección");
        btnQuitarSel = new JButton("Quitar selección");
        btnMasAsignacion = new JButton("Más ▾");

        btnAsignarSel.setToolTipText("Marca como 'Asignado' los juegos seleccionados en la tabla");
        btnQuitarSel.setToolTipText("Desmarca 'Asignado' para la selección");
        btnMasAsignacion.setToolTipText("Asignar todo / Quitar todo / Reset global");

        rowFilters.add(new JLabel("Buscar:"));
        rowFilters.add(txtBuscarAsignacion);
        rowFilters.add(chkSoloAsignados);
        rowFilters.add(btnAsignarSel);
        rowFilters.add(btnQuitarSel);
        rowFilters.add(btnMasAsignacion);

        top.add(titleBlock);
        top.add(Box.createVerticalStrut(8));
        top.add(rowStudent);
        top.add(Box.createVerticalStrut(6));
        top.add(rowFilters);

        // Tabla
        asignacionModel = new AsignacionTableModel();
        tblAsignacion = createProTable(asignacionModel);
        sorterAsignacion = new TableRowSorter<>(asignacionModel);
        tblAsignacion.setRowSorter(sorterAsignacion);

        // Multiselección para aplicar modo/dificultad a varios
        tblAsignacion.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        setColWidth(tblAsignacion, AsignacionTableModel.COL_ID, 50);
        setColWidth(tblAsignacion, AsignacionTableModel.COL_TIPO, 95);
        setColWidth(tblAsignacion, AsignacionTableModel.COL_ASIGNADO, 80);
        setColWidth(tblAsignacion, AsignacionTableModel.COL_MODO, 90);
        setColWidth(tblAsignacion, AsignacionTableModel.COL_DIFICULTAD, 60);

        panelAsignacionJuegos = new JScrollPane(tblAsignacion);
        styleTableScroll(panelAsignacionJuegos);

        // Centro: tabla + editor
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(panelAsignacionJuegos, BorderLayout.CENTER);
        center.add(buildDificultadEditorPanel(), BorderLayout.SOUTH);

        // Bottom: guardar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 18)));

        btnGuardarAsignacion = new JButton("Guardar asignación del estudiante");
        btnGuardarAsignacion.setToolTipText("Guarda qué juegos están asignados y las dificultades personalizadas");
        bottom.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        bottom.add(btnGuardarAsignacion, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDificultadEditorPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(true);
        p.setBackground(new Color(250, 250, 250));
        p.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(0, 0, 0, 18)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel ttl = new JLabel("Dificultad para este estudiante");
        ttl.setFont(ttl.getFont().deriveFont(Font.BOLD, 12.5f));

        lblEditorJuego = new JLabel("Selecciona uno o más juegos en la tabla.");
        lblEditorJuego.setForeground(new Color(90, 90, 90));

        rbUsarGlobal = new JRadioButton("Usar GLOBAL (catálogo)");
        rbPersonalizar = new JRadioButton("PERSONALIZAR (override)");
        rbUsarGlobal.setOpaque(false);
        rbPersonalizar.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbUsarGlobal);
        bg.add(rbPersonalizar);
        rbUsarGlobal.setSelected(true);

        spDifPersonal = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        ((JSpinner.DefaultEditor) spDifPersonal.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) spDifPersonal.getEditor()).getTextField().setHorizontalAlignment(SwingConstants.CENTER);

        JLabel note = new JLabel("<html><span style='color:#666;'>Global = toma la dificultad del catálogo. " +
                "Personal = guarda un valor fijo solo para este estudiante.</span></html>");
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 11.5f));

        btnAplicarEditor = new JButton("Aplicar a selección");
        btnResetGlobalSel = new JButton("Reset a GLOBAL (selección)");
        btnResetGlobalSel.setToolTipText("Quita el override y vuelve a usar la dificultad global del catálogo");

        // Layout
        c.gridx = 0; c.gridy = 0; c.gridwidth = 3; c.weightx = 1;
        p.add(ttl, c);

        c.gridy = 1;
        p.add(lblEditorJuego, c);

        c.gridy = 2; c.gridwidth = 1; c.weightx = 0;
        p.add(rbUsarGlobal, c);

        c.gridx = 1; c.weightx = 0;
        p.add(rbPersonalizar, c);

        c.gridx = 2; c.weightx = 0;
        JPanel difBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        difBox.setOpaque(false);
        difBox.add(new JLabel("Dificultad:"));
        difBox.add(spDifPersonal);
        p.add(difBox, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 3; c.weightx = 1;
        p.add(note, c);

        c.gridy = 4; c.gridwidth = 1; c.weightx = 0;
        p.add(btnAplicarEditor, c);

        c.gridx = 1;
        p.add(btnResetGlobalSel, c);

        // Enable/disable spinner según modo
        rbUsarGlobal.addActionListener(e -> spDifPersonal.setEnabled(false));
        rbPersonalizar.addActionListener(e -> spDifPersonal.setEnabled(true));
        spDifPersonal.setEnabled(false);

        return p;
    }

    private JTable createProTable(AbstractTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!(c instanceof JComponent)) return c;

                boolean selected = isRowSelected(row);
                int modelRow = convertRowIndexToModel(row);

                Color zebra = (modelRow % 2 == 0) ? Color.WHITE : new Color(250, 250, 250);
                Color bg = selected ? new Color(220, 235, 255) : zebra;

                c.setBackground(bg);
                return c;
            }
        };

        t.setRowHeight(46);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setAutoCreateColumnsFromModel(true);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 12f));

        // Renderers
        t.setDefaultRenderer(String.class, new ProTextRenderer());
        t.setDefaultRenderer(Integer.class, new CenterRenderer());
        t.setDefaultRenderer(Boolean.class, new CenterBooleanRenderer());

        return t;
    }

    private void styleTableScroll(JScrollPane sp) {
        sp.setBorder(new MatteBorder(1, 1, 1, 1, new Color(0, 0, 0, 18)));
        sp.getViewport().setOpaque(true);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void setColWidth(JTable t, int col, int px) {
        TableColumn c = t.getColumnModel().getColumn(col);
        c.setMinWidth(px);
        c.setMaxWidth(px);
        c.setPreferredWidth(px);
    }

    // ---------------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------------

    private void cargarJuegos() {
        cacheJuegos = new ArrayList<>(juegoService.obtenerTodos());
        cacheJuegos.sort(Comparator.comparingInt(Juego::getId));
        pendingHabilitado.clear();
        pendingDificultadGlobal.clear();

        catalogoModel.fireAll();
        renderCatalogoResumen();

        actualizarAsignacionesParaSeleccionado();
    }

    private void cargarNinos() {
        cacheNinos = new ArrayList<>(perfilService.obtenerTodosNinos());
        cacheNinos.sort(Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER));
        rebuildAulaFilter();
        aplicarFiltroNinosNow(true);
    }

    private void initListeners() {
        btnRefrescar.addActionListener(e -> {
            stopEditingSafely(tblCatalogo);
            stopEditingSafely(tblAsignacion);
            cargarJuegos();
            cargarNinos();
        });

        // Filtros
        wireSearch(txtBuscarCatalogo, this::aplicarFiltroCatalogo);
        wireSearch(txtBuscarAsignacion, this::aplicarFiltroAsignacion);

        // Estudiantes: filtros por Aula / ID (más intuitivo para listas grandes)
        wireSearchDebounced(txtFiltroId, this::scheduleFiltroNinos);
        cboFiltroAula.addActionListener(e -> aplicarFiltroNinosNow(true));
        cboOrdenNino.addActionListener(e -> aplicarFiltroNinosNow(true));
        btnLimpiarFiltrosNino.addActionListener(e -> {
            if (cboFiltroAula != null && cboFiltroAula.getItemCount() > 0) cboFiltroAula.setSelectedIndex(0); // "Todas"
            if (cboOrdenNino != null && cboOrdenNino.getItemCount() > 0) cboOrdenNino.setSelectedIndex(0);

            if (txtFiltroId != null) {
                txtFiltroId.setText("");
                txtFiltroId.requestFocusInWindow();
                txtFiltroId.selectAll();
            }
            aplicarFiltroNinosNow(true);
        });


        // Estudiantes: selector en tabla (Lista...)
        if (btnListaEstudiantes != null) {
            btnListaEstudiantes.addActionListener(ev -> openStudentListDialog());
        }

        // Catálogo: menú "Más"
        btnMasCatalogo.addActionListener(e -> {
            JPopupMenu m = new JPopupMenu();
            JMenuItem it1 = new JMenuItem("Habilitar TODO");
            JMenuItem it2 = new JMenuItem("Deshabilitar TODO");
            it1.addActionListener(ev -> setHabilitadoCatalogoAll(true));
            it2.addActionListener(ev -> setHabilitadoCatalogoAll(false));
            m.add(it1);
            m.add(it2);
            m.show(btnMasCatalogo, 0, btnMasCatalogo.getHeight());
        });

        // Catálogo: habilitar/deshabilitar selección
        btnHabilitarSel.addActionListener(e -> setHabilitadoCatalogoSelected(true));
        btnDeshabilitarSel.addActionListener(e -> setHabilitadoCatalogoSelected(false));

        // Catálogo: aplicar dificultad global a selección
        btnAplicarDifGlobalSel.addActionListener(e -> onAplicarDificultadGlobalSeleccion());

        // Catálogo: asignar selección a TODOS
        btnAsignarSelATodos.addActionListener(e -> onAsignarSeleccionATodos());

        // Guardar catálogo
        btnGuardarEstadoJuegos.addActionListener(e -> onGuardarEstadosJuegos());

        // Asignación
        cboNinos.addActionListener(e -> {
            if (ignoreNinoEvents) return;
            actualizarAsignacionesParaSeleccionado();
        });
        chkSoloAsignados.addActionListener(e -> aplicarFiltroAsignacion());
        btnGuardarAsignacion.addActionListener(e -> onGuardarAsignacion());

        // Asignación: menú "Más"
        btnMasAsignacion.addActionListener(e -> {
            JPopupMenu m = new JPopupMenu();
            JMenuItem it1 = new JMenuItem("Asignar TODO");
            JMenuItem it2 = new JMenuItem("Quitar TODO");
            JMenuItem it3 = new JMenuItem("Reset GLOBAL (selección)");
            it1.addActionListener(ev -> { asignacionModel.setAsignadoParaTodos(true); renderAsignacionResumen(); });
            it2.addActionListener(ev -> { asignacionModel.setAsignadoParaTodos(false); renderAsignacionResumen(); });
            it3.addActionListener(ev -> resetGlobalEnSeleccionAsignacion());
            m.add(it1);
            m.add(it2);
            m.addSeparator();
            m.add(it3);
            m.show(btnMasAsignacion, 0, btnMasAsignacion.getHeight());
        });

        // Asignación: asignar/quitar selección
        btnAsignarSel.addActionListener(e -> setAsignadoSeleccion(true));
        btnQuitarSel.addActionListener(e -> setAsignadoSeleccion(false));

        // Editor: aplicar y reset
        btnAplicarEditor.addActionListener(e -> onAplicarEditorDificultad());
        btnResetGlobalSel.addActionListener(e -> resetGlobalEnSeleccionAsignacion());

        // Editor: cuando cambia selección en la tabla, actualiza hint
        tblAsignacion.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                updateEditorFromSelection();
            }
        });

        // Atajo: Ctrl+F enfoca el buscador de estudiantes
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ctrl F"), "focusBuscarEstudiante");
        getActionMap().put("focusBuscarEstudiante", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (txtFiltroId != null) {
                    txtFiltroId.requestFocusInWindow();
                    txtFiltroId.selectAll();
                }
            }
        });

        // Atajo: Ctrl+L abre la lista completa de estudiantes
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ctrl L"), "openListaNinos");
        getActionMap().put("openListaNinos", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (btnListaEstudiantes != null) btnListaEstudiantes.doClick();
            }
        });
    }

    private void wireSearch(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    private void wireSearchDebounced(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    // ---------------------------------------------------------------------
    // Filters
    // ---------------------------------------------------------------------

    private void aplicarFiltroCatalogo() {
        final String q = safeLower(txtBuscarCatalogo.getText());

        sorterCatalogo.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends CatalogoTableModel, ? extends Integer> entry) {
                if (q.isBlank()) return true;
                int modelRow = entry.getIdentifier();
                Juego j = catalogoModel.getJuegoAt(modelRow);
                String hay = (j.getId() + " " + safe(j.getNombre()) + " " + safe(j.getDescripcion()) + " " + safe(j.getTipo() != null ? j.getTipo().name() : ""))
                        .toLowerCase(Locale.ROOT);
                return hay.contains(q);
            }
        });
    }

    private void aplicarFiltroAsignacion() {
        final String q = safeLower(txtBuscarAsignacion.getText());
        final boolean solo = chkSoloAsignados.isSelected();

        sorterAsignacion.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends AsignacionTableModel, ? extends Integer> entry) {
                int modelRow = entry.getIdentifier();
                AsignacionRow r = asignacionModel.getRowAt(modelRow);

                if (solo && !r.asignado) return false;
                if (q.isBlank()) return true;

                String hay = (r.idJuego + " " + safe(r.nombre) + " " + safe(r.descripcion) + " " + safe(r.tipo))
                        .toLowerCase(Locale.ROOT);
                return hay.contains(q);
            }
        });

        renderAsignacionResumen();
    }

    // ---------------------------------------------------------------------
    // Filtro de estudiantes (para listas grandes)
    // ---------------------------------------------------------------------


    private void scheduleFiltroNinos() {
        if (tDebounceNinos == null) {
            tDebounceNinos = new Timer(160, e -> {
                tDebounceNinos.stop();
                aplicarFiltroNinosNow(true);
            });
            tDebounceNinos.setRepeats(false);
        }
        tDebounceNinos.restart();
    }

    /** Reconstruye el combo de aulas a partir de los niños cargados. */
    private void rebuildAulaFilter() {
        if (cboFiltroAula == null) return;

        // Guardar selección previa si existe
        Object prev = cboFiltroAula.getSelectedItem();

        Set<String> aulas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Nino n : cacheNinos) {
            String a = safe(n.getAula()).trim();
            if (!a.isEmpty()) aulas.add(a);
        }

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Todas");
        for (String a : aulas) model.addElement(a);

        cboFiltroAula.setModel(model);

        if (prev != null) {
            cboFiltroAula.setSelectedItem(prev);
        }
        if (cboFiltroAula.getSelectedIndex() < 0) {
            cboFiltroAula.setSelectedIndex(0);
        }
    }

    private void aplicarFiltroNinosNow(boolean tryKeepSelection) {
        if (cboNinos == null) return;

        Integer keepId = null;
        if (tryKeepSelection) {
            Object sel = cboNinos.getSelectedItem();
            if (sel instanceof Nino) keepId = ((Nino) sel).getId();
        }

        String aulaSel = (cboFiltroAula != null && cboFiltroAula.getSelectedItem() != null)
                ? String.valueOf(cboFiltroAula.getSelectedItem())
                : "Todas";
        String qNino = (txtFiltroId != null) ? safe(txtFiltroId.getText()).trim().toLowerCase(Locale.ROOT) : "";
        String[] tokens = qNino.isEmpty() ? new String[0] : qNino.split("\s+");

        List<Nino> filtered = new ArrayList<>();
        for (Nino n : cacheNinos) {
            // 1) Filtro por aula
            if (!"Todas".equalsIgnoreCase(aulaSel)) {
                if (!safe(n.getAula()).equalsIgnoreCase(aulaSel)) continue;
            }
            // 2) Filtro por texto: ID (prefijo) o Nombre/Aula (contiene)
            if (tokens.length > 0 && !matchesNinoTokensRecursive(n, tokens, 0)) {
                continue;
            }
            filtered.add(n);
        }

// Orden
        String orden = (cboOrdenNino != null && cboOrdenNino.getSelectedItem() != null)
                ? String.valueOf(cboOrdenNino.getSelectedItem())
                : "Nombre (A-Z)";

        Comparator<Nino> comp;
        if (orden.startsWith("ID")) {
            comp = Comparator.comparingInt(Nino::getId);
        } else if (orden.startsWith("Aula")) {
            comp = Comparator.comparing((Nino n) -> safe(n.getAula()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(n -> safe(n.getNombre()), String.CASE_INSENSITIVE_ORDER);
        } else {
            comp = Comparator.comparing(n -> safe(n.getNombre()), String.CASE_INSENSITIVE_ORDER);
        }
        filtered.sort(comp);

        ignoreNinoEvents = true;
        try {
            DefaultComboBoxModel<Nino> model = new DefaultComboBoxModel<>(filtered.toArray(new Nino[0]));
            cboNinos.setModel(model);

            if (keepId != null) {
                for (int i = 0; i < model.getSize(); i++) {
                    Nino n = model.getElementAt(i);
                    if (n != null && n.getId() == keepId) {
                        cboNinos.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (cboNinos.getSelectedIndex() < 0 && model.getSize() > 0) {
                cboNinos.setSelectedIndex(0);
            }
        } finally {
            ignoreNinoEvents = false;
        }

        if (lblNinosFiltro != null) {
            String extra = "Todas".equalsIgnoreCase(aulaSel) ? "" : (" · " + aulaSel);
            lblNinosFiltro.setText("Mostrando " + filtered.size() + "/" + cacheNinos.size() + extra);
        }

        if (cboNinos.getItemCount() > 0) {
            actualizarAsignacionesParaSeleccionado();
        } else {
            asignacionModel.setRows(Collections.emptyList());
            resumenAsignacionBase = "Habilitados: 0  ·  Asignados: 0";
            lblResumenAsignacion.setText(resumenAsignacionBase);
        }
    }


    private boolean matchesNinoTokensRecursive(Nino n, String[] tokens, int idx) {
        if (tokens == null || idx >= tokens.length) return true;

        String t = tokens[idx];
        if (t == null || t.isBlank()) {
            return matchesNinoTokensRecursive(n, tokens, idx + 1);
        }

        if (!tokenMatchesNino(n, t)) return false;
        return matchesNinoTokensRecursive(n, tokens, idx + 1);
    }

    private boolean tokenMatchesNino(Nino n, String token) {
        String t = token.toLowerCase(Locale.ROOT).trim();
        if (t.isEmpty()) return true;

        // Si es numérico, lo tratamos como prefijo de ID (ej: "2" => 2, 20, 21...)
        boolean numeric = true;
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isDigit(t.charAt(i))) { numeric = false; break; }
        }
        if (numeric) {
            return String.valueOf(n.getId()).startsWith(t);
        }

        // Si es texto, buscar en nombre y aula
        String hay = (safe(n.getNombre()) + " " + safe(n.getAula())).toLowerCase(Locale.ROOT);
        return hay.contains(t);
    }

// ---------------------------------------------------------------------
    // Catálogo: multiselección habilitar/deshabilitar y dif global
    // ---------------------------------------------------------------------

    private void setHabilitadoCatalogoAll(boolean v) {
        stopEditingSafely(tblCatalogo);
        for (Juego j : cacheJuegos) pendingHabilitado.put(j.getId(), v);
        catalogoModel.fireAll();
        renderCatalogoResumen();
        actualizarAsignacionesParaSeleccionado();
    }

    private void setHabilitadoCatalogoSelected(boolean v) {
        stopEditingSafely(tblCatalogo);
        int[] viewRows = tblCatalogo.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en el catálogo.");
            return;
        }
        for (int vr : viewRows) {
            int mr = tblCatalogo.convertRowIndexToModel(vr);
            Juego j = catalogoModel.getJuegoAt(mr);
            pendingHabilitado.put(j.getId(), v);
        }
        catalogoModel.fireAll();
        renderCatalogoResumen();
        actualizarAsignacionesParaSeleccionado();
    }

    private void onAplicarDificultadGlobalSeleccion() {
        stopEditingSafely(tblCatalogo);
        int[] viewRows = tblCatalogo.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en el catálogo para aplicar dificultad.");
            return;
        }
        int nueva = clamp1to5((Integer) spDifGlobalSel.getValue());

        for (int vr : viewRows) {
            int mr = tblCatalogo.convertRowIndexToModel(vr);
            Juego j = catalogoModel.getJuegoAt(mr);
            pendingDificultadGlobal.put(j.getId(), nueva);
        }
        catalogoModel.fireAll();
        actualizarAsignacionesParaSeleccionado();
    }

    private void onAsignarSeleccionATodos() {
        stopEditingSafely(tblCatalogo);

        int[] viewRows = tblCatalogo.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en el catálogo primero.");
            return;
        }

        List<Juego> seleccion = new ArrayList<>();
        for (int vr : viewRows) {
            int mr = tblCatalogo.convertRowIndexToModel(vr);
            seleccion.add(catalogoModel.getJuegoAt(mr));
        }

        // Si alguno está deshabilitado, pregunta si habilitar
        List<Juego> deshab = new ArrayList<>();
        for (Juego j : seleccion) if (!isJuegoHabilitadoActual(j)) deshab.add(j);

        if (!deshab.isEmpty()) {
            int r = JOptionPane.showConfirmDialog(
                    this,
                    "Hay " + deshab.size() + " juego(s) deshabilitado(s) en la selección.\n" +
                            "¿Deseas HABILITARLOS y asignarlos a TODOS?",
                    "Habilitar y asignar",
                    JOptionPane.YES_NO_OPTION
            );
            if (r != JOptionPane.YES_OPTION) return;
            for (Juego j : deshab) pendingHabilitado.put(j.getId(), true);
            catalogoModel.fireAll();
            renderCatalogoResumen();
        }

        if (cacheNinos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay estudiantes registrados.");
            return;
        }

        int ok = JOptionPane.showConfirmDialog(
                this,
                "¿Asignar " + seleccion.size() + " juego(s) a TODOS los estudiantes?\n\n" +
                        "Esto agregará cada juego a la lista de asignaciones de cada niño.",
                "Confirmar asignación masiva",
                JOptionPane.YES_NO_OPTION
        );
        if (ok != JOptionPane.YES_OPTION) return;

        int afectadosTotal = 0;
        for (Juego j : seleccion) {
            int afectados;
            try {
                afectados = perfilService.asignarJuegoATodos(j.getId());
            } catch (NoSuchMethodError err) {
                afectados = asignarJuegoATodosFallback(j.getId());
            }
            // suma "afectados" (niños que no lo tenían)
            afectadosTotal += afectados;
        }

        status("Operación completada. Cambios aplicados: " + afectadosTotal);
        cargarNinos();
        actualizarAsignacionesParaSeleccionado();
    }

    // Fallback: menos eficiente (se usa solo si no existe el método nuevo).
    private int asignarJuegoATodosFallback(int idJuego) {
        List<Nino> ninos = perfilService.obtenerTodosNinos();
        int changed = 0;
        for (Nino n : ninos) {
            Set<Integer> set = n.getJuegosAsignados();
            if (set == null) {
                set = new HashSet<>();
                // Intentamos setearlo si existe setter (evita romper compilación si no existe)
                try {
                    n.getClass().getMethod("setJuegosAsignados", Set.class).invoke(n, set);
                } catch (Exception ignored) { }
            }
            if (set.add(idJuego)) {
                changed++;
                perfilService.asignarJuegosConDificultad(n.getId(), set, n.getDificultadPorJuego());
            }
        }
        return changed;
    }

    private void renderCatalogoResumen() {
        int total = cacheJuegos.size();
        int habilitados = 0;
        for (Juego j : cacheJuegos) if (isJuegoHabilitadoActual(j)) habilitados++;
        lblResumenCatalogo.setText("Total: " + total + "  ·  Habilitados: " + habilitados);
    }

    // ---------------------------------------------------------------------
    // Guardar catálogo
    // ---------------------------------------------------------------------

    private void onGuardarEstadosJuegos() {
        stopEditingSafely(tblCatalogo);

        List<Juego> juegos = juegoService.obtenerTodos();

        for (Juego juego : juegos) {
            Boolean hb = pendingHabilitado.get(juego.getId());
            if (hb != null) juego.setHabilitado(hb);

            Integer nd = pendingDificultadGlobal.get(juego.getId());
            if (nd != null) {
                int nueva = clamp1to5(nd);
                int anterior = clamp1to5(juego.getDificultad());

                if (nueva != anterior) {
                    Object[] opciones = {
                            "Aplicar a TODOS",
                            "Solo sin dificultad personalizada",
                            "Cancelar"
                    };

                    int resp = JOptionPane.showOptionDialog(
                            this,
                            "Vas a cambiar la dificultad GLOBAL de \"" + juego.getNombre() + "\".\n\n" +
                                    "¿Cómo deseas aplicar este cambio?",
                            "Confirmar cambio global",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            opciones,
                            opciones[0]
                    );

                    if (resp == 2 || resp == JOptionPane.CLOSED_OPTION) {
                        pendingDificultadGlobal.put(juego.getId(), anterior);
                        continue;
                    }

                    juego.setDificultad(nueva);

                    if (resp == 0) {
                        // Aplicar a todos, incluso si tenían override
                        perfilService.aplicarDificultadJuegoATodos(juego.getId(), nueva, false);
                    } else if (resp == 1) {
                        // Aplicar solo a quienes NO tienen dificultad personalizada
                        perfilService.aplicarDificultadJuegoATodos(juego.getId(), nueva, true);
                    }
                }
            }
        }

        juegoService.guardar();
        cargarJuegos();
        status("Cambios del catálogo guardados.");
    }

    // ---------------------------------------------------------------------
    // Asignación: cargar, selección, editor
    // ---------------------------------------------------------------------

    private void actualizarAsignacionesParaSeleccionado() {
        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) {
            asignacionModel.setRows(Collections.emptyList());
            updateEditorFromSelection();
            return;
        }

        Set<Integer> asignados = seleccionado.getJuegosAsignados();
        Map<Integer, Integer> overrides = safeMap(seleccionado.getDificultadPorJuego());

        List<AsignacionRow> rows = new ArrayList<>();
        int totalHabilitados = 0;
        int totalAsignados = 0;

        for (Juego juego : cacheJuegos) {
            if (!isJuegoHabilitadoActual(juego)) continue;
            totalHabilitados++;

            int idJuego = juego.getId();
            boolean estaAsignado = asignados != null && asignados.contains(idJuego);
            if (estaAsignado) totalAsignados++;

            int difGlobal = getDificultadGlobalActual(juego);
            boolean personal = overrides.containsKey(idJuego);
            int difPersonal = clamp1to5(personal ? overrides.get(idJuego) : difGlobal);

            rows.add(new AsignacionRow(
                    idJuego,
                    safe(juego.getNombre()),
                    safe(juego.getTipo() != null ? juego.getTipo().name() : ""),
                    safe(juego.getDescripcion()),
                    estaAsignado,
                    difGlobal,
                    difPersonal,
                    personal
            ));
        }

        asignacionModel.setRows(rows);

        resumenAsignacionBase = "Habilitados: " + totalHabilitados + "  ·  Asignados: " + totalAsignados;
        lblResumenAsignacion.setText(resumenAsignacionBase);

        aplicarFiltroAsignacion();
        renderAsignacionResumen();
        updateEditorFromSelection();
    }

    private void updateEditorFromSelection() {
        int[] viewRows = tblAsignacion.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            lblEditorJuego.setText("Selecciona uno o más juegos en la tabla.");
            rbUsarGlobal.setSelected(true);
            spDifPersonal.setValue(1);
            spDifPersonal.setEnabled(false);
            return;
        }

        if (viewRows.length == 1) {
            int mr = tblAsignacion.convertRowIndexToModel(viewRows[0]);
            AsignacionRow r = asignacionModel.getRowAt(mr);

            lblEditorJuego.setText("Seleccionado: " + r.nombre + "  (#" + r.idJuego + ")");

            if (r.personalOverride) {
                rbPersonalizar.setSelected(true);
                spDifPersonal.setEnabled(true);
                spDifPersonal.setValue(r.difPersonal);
            } else {
                rbUsarGlobal.setSelected(true);
                spDifPersonal.setEnabled(false);
                spDifPersonal.setValue(r.difGlobal);
            }
            return;
        }

        // Multi selección: muestra estado “mixto”
        lblEditorJuego.setText("Selección múltiple (" + viewRows.length + " juegos). Elige modo y aplica.");
        rbUsarGlobal.setSelected(true);
        spDifPersonal.setEnabled(false);
        spDifPersonal.setValue(1);
    }

    private void setAsignadoSeleccion(boolean v) {
        stopEditingSafely(tblAsignacion);
        int[] viewRows = tblAsignacion.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en la tabla.");
            return;
        }
        for (int vr : viewRows) {
            int mr = tblAsignacion.convertRowIndexToModel(vr);
            AsignacionRow r = asignacionModel.getRowAt(mr);
            r.asignado = v;
            if (!v) {
                // si quita asignación, también quita override para no dejar basura
                r.personalOverride = false;
                r.difPersonal = r.difGlobal;
            }
        }
        asignacionModel.fireAll();
        renderAsignacionResumen();
        updateEditorFromSelection();
    }

    private void resetGlobalEnSeleccionAsignacion() {
        stopEditingSafely(tblAsignacion);
        int[] viewRows = tblAsignacion.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en la tabla.");
            return;
        }
        for (int vr : viewRows) {
            int mr = tblAsignacion.convertRowIndexToModel(vr);
            AsignacionRow r = asignacionModel.getRowAt(mr);
            if (!r.asignado) continue;
            r.personalOverride = false;
            r.difPersonal = r.difGlobal;
        }
        asignacionModel.fireAll();
        updateEditorFromSelection();
    }

    private void onAplicarEditorDificultad() {
        stopEditingSafely(tblAsignacion);

        int[] viewRows = tblAsignacion.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más juegos en la tabla para aplicar cambios.");
            return;
        }

        boolean personal = rbPersonalizar.isSelected();
        int dif = clamp1to5((Integer) spDifPersonal.getValue());

        // Si hay filas no asignadas, pregunta si las asignamos (para poder aplicar dificultad)
        boolean hayNoAsignados = false;
        for (int vr : viewRows) {
            int mr = tblAsignacion.convertRowIndexToModel(vr);
            if (!asignacionModel.getRowAt(mr).asignado) { hayNoAsignados = true; break; }
        }
        if (hayNoAsignados) {
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Hay juegos en la selección que NO están asignados.\n¿Deseas asignarlos también?",
                    "Asignar y aplicar",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok != JOptionPane.YES_OPTION) return;
            setAsignadoSeleccion(true);
        }

        for (int vr : viewRows) {
            int mr = tblAsignacion.convertRowIndexToModel(vr);
            AsignacionRow r = asignacionModel.getRowAt(mr);
            if (!r.asignado) continue;

            r.personalOverride = personal;
            if (personal) {
                r.difPersonal = dif; // guarda fijo para este estudiante
            } else {
                r.difPersonal = r.difGlobal; // vuelve a global
            }
        }

        asignacionModel.fireAll();
        updateEditorFromSelection();
    }

    private void renderAsignacionResumen() {
        int sel = asignacionModel.countSeleccionados();
        lblResumenAsignacion.setText(resumenAsignacionBase + "  ·  Seleccionados: " + sel);
    }

    private void onGuardarAsignacion() {
        stopEditingSafely(tblAsignacion);

        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un estudiante primero.");
            return;
        }

        Set<Integer> juegosAsignados = new HashSet<>();
        Map<Integer, Integer> dificultadPorJuego = new HashMap<>();

        for (AsignacionRow r : asignacionModel.getRows()) {
            if (!r.asignado) continue;
            juegosAsignados.add(r.idJuego);

            // Guardamos override solo si el modo es PERSONAL
            if (r.personalOverride) {
                dificultadPorJuego.put(r.idJuego, clamp1to5(r.difPersonal));
            }
        }

        perfilService.asignarJuegosConDificultad(seleccionado.getId(), juegosAsignados, dificultadPorJuego);
        JOptionPane.showMessageDialog(this, "Asignación guardada para: " + seleccionado.getNombre());
        cargarNinos();
        actualizarAsignacionesParaSeleccionado();
    }

    public Set<Integer> getJuegosAsignadosParaNino(int idNino) {
        return perfilService.getJuegosAsignados(idNino);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String safeLower(String s) {
        return safe(s).trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp1to5(int v) {
        if (v < 1) return 1;
        if (v > 5) return 5;
        return v;
    }

    private boolean isJuegoHabilitadoActual(Juego juego) {
        Boolean hb = pendingHabilitado.get(juego.getId());
        return (hb != null) ? hb : juego.isHabilitado();
    }

    private int getDificultadGlobalActual(Juego juego) {
        Integer d = pendingDificultadGlobal.get(juego.getId());
        return clamp1to5(d != null ? d : juego.getDificultad());
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }

    private static void stopEditingSafely(JTable t) {
        if (t == null) return;
        if (t.isEditing()) {
            TableCellEditor ed = t.getCellEditor();
            if (ed != null) ed.stopCellEditing();
        }
    }


    // ---------------------------------------------------------------------
    // Selector rápido: lista completa de estudiantes (tabla)
    // ---------------------------------------------------------------------

    private void openStudentListDialog() {
        if (cacheNinos == null || cacheNinos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay estudiantes cargados.", "Lista de estudiantes", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dlg = new JDialog(owner, "Lista de estudiantes", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        dlg.setContentPane(root);

        // Top: filtros
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;

        JLabel lblBuscar = new JLabel("Buscar:");
        JTextField txt = new JTextField(24);
        txt.setToolTipText("ID (prefijo), nombre o aula. Ej: 23, julio, azul 23");

        JLabel lblAula = new JLabel("Aula:");
        JComboBox<String> cboAulaDlg = new JComboBox<>();
        cboAulaDlg.addItem("Todas");
        for (String a : getAulasOrdenadas(cacheNinos)) cboAulaDlg.addItem(a);

        JLabel lblCount = new JLabel(" ");
        lblCount.setForeground(new Color(110, 110, 110));

        c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        top.add(lblBuscar, c);

        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.HORIZONTAL;
        top.add(txt, c);

        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        top.add(lblAula, c);

        c.gridx = 3; c.weightx = 0; c.fill = GridBagConstraints.HORIZONTAL;
        cboAulaDlg.setPreferredSize(new Dimension(160, cboAulaDlg.getPreferredSize().height));
        top.add(cboAulaDlg, c);

        c.gridx = 4; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        top.add(lblCount, c);

        root.add(top, BorderLayout.NORTH);

        // Center: tabla
        NinoPickerTableModel model = new NinoPickerTableModel(cacheNinos);
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Look de header
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);

        TableRowSorter<NinoPickerTableModel> sorter = new TableRowSorter<>(model);
        sorter.setComparator(0, Comparator.comparingInt((Integer a) -> a));
        table.setRowSorter(sorter);

        // Zebra suave
        table.setDefaultRenderer(Object.class, new ZebraPickerRenderer());
        table.setDefaultRenderer(Integer.class, new ZebraPickerRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        root.add(sp, BorderLayout.CENTER);

        // Bottom: acciones
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnSelect = new JButton("Seleccionar");
        JButton btnCancel = new JButton("Cancelar");
        bottom.add(btnCancel);
        bottom.add(btnSelect);
        root.add(bottom, BorderLayout.SOUTH);

        Runnable applyFilter = () -> {
            String aula = (cboAulaDlg.getSelectedItem() != null) ? String.valueOf(cboAulaDlg.getSelectedItem()) : "Todas";
            String q = safe(txt.getText()).trim().toLowerCase(Locale.ROOT);
            String[] tokens = q.isEmpty() ? new String[0] : q.split("\\s+");

            sorter.setRowFilter(new RowFilter<NinoPickerTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends NinoPickerTableModel, ? extends Integer> entry) {
                    int id = (Integer) entry.getValue(0);
                    String nombre = safe(String.valueOf(entry.getValue(1))).toLowerCase(Locale.ROOT);
                    String a = safe(String.valueOf(entry.getValue(2))).toLowerCase(Locale.ROOT);

                    if (!"todas".equalsIgnoreCase(aula)) {
                        if (!a.equalsIgnoreCase(aula)) return false;
                    }

                    if (tokens.length == 0) return true;
                    return matchesStudentTokensRecursive(id, nombre, a, tokens, 0);
                }
            });

            lblCount.setText("Mostrando " + table.getRowCount() + "/" + model.getRowCount());
        };

        // Eventos filtros
        txt.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter.run(); }
        });
        cboAulaDlg.addActionListener(e -> applyFilter.run());

        // Acción seleccionar
        Action doSelect = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                int viewRow = table.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = table.convertRowIndexToModel(viewRow);
                int id = (Integer) model.getValueAt(modelRow, 0);

                Nino chosen = findNinoById(id);
                if (chosen == null) return;

                applyChosenStudent(chosen);
                dlg.dispose();
            }
        };

        btnSelect.addActionListener(doSelect);
        btnCancel.addActionListener(e -> dlg.dispose());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    doSelect.actionPerformed(null);
                }
            }
        });

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pickStudent");
        table.getActionMap().put("pickStudent", doSelect);

        // Selección inicial
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }

        applyFilter.run();

        dlg.setSize(760, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void applyChosenStudent(Nino chosen) {
        if (chosen == null) return;

        // Para que siempre aparezca: quitamos filtro de aula y usamos el ID (prefijo) como búsqueda rápida.
        if (cboFiltroAula != null && cboFiltroAula.getItemCount() > 0) {
            cboFiltroAula.setSelectedItem("Todas");
        }
        if (txtFiltroId != null) {
            txtFiltroId.setText(String.valueOf(chosen.getId()));
        }

        aplicarFiltroNinosNow(false);

        // Seleccionar por ID dentro del combo filtrado
        ComboBoxModel model = cboNinos.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            if (o instanceof Nino && ((Nino) o).getId() == chosen.getId()) {
                cboNinos.setSelectedIndex(i);
                break;
            }
        }

        actualizarAsignacionesParaSeleccionado();
    }

    private Nino findNinoById(int id) {
        for (Nino n : cacheNinos) {
            if (n != null && n.getId() == id) return n;
        }
        return null;
    }

    private List<String> getAulasOrdenadas(List<Nino> ninos) {
        Set<String> set = new HashSet<>();
        for (Nino n : ninos) {
            String a = safe(n != null ? n.getAula() : "");
            if (!a.isBlank()) set.add(a);
        }
        List<String> l = new ArrayList<>(set);
        l.sort(String.CASE_INSENSITIVE_ORDER);
        return l;
    }

    // Recursivo: todas las palabras del buscador deben coincidir
    private boolean matchesStudentTokensRecursive(int id, String nombreLower, String aulaLower, String[] tokens, int idx) {
        if (tokens == null || idx >= tokens.length) return true;

        String t = tokens[idx];
        if (t == null || t.isBlank()) {
            return matchesStudentTokensRecursive(id, nombreLower, aulaLower, tokens, idx + 1);
        }

        if (!tokenMatchesStudent(id, nombreLower, aulaLower, t)) return false;
        return matchesStudentTokensRecursive(id, nombreLower, aulaLower, tokens, idx + 1);
    }

    private boolean tokenMatchesStudent(int id, String nombreLower, String aulaLower, String tokenLower) {
        // Si es número: prefijo de ID
        boolean allDigits = true;
        for (int i = 0; i < tokenLower.length(); i++) {
            char ch = tokenLower.charAt(i);
            if (!Character.isDigit(ch)) { allDigits = false; break; }
        }
        if (allDigits) {
            return String.valueOf(id).startsWith(tokenLower);
        }
        return (nombreLower != null && nombreLower.contains(tokenLower))
                || (aulaLower != null && aulaLower.contains(tokenLower));
    }


    private static final class NinoPickerTableModel extends AbstractTableModel {
        private final List<Nino> all;

        NinoPickerTableModel(List<Nino> all) {
            this.all = (all == null) ? Collections.<Nino>emptyList() : new ArrayList<>(all);
        }

        @Override public int getRowCount() { return all.size(); }
        @Override public int getColumnCount() { return 3; }

        @Override public String getColumnName(int col) {
            switch (col) {
                case 0: return "ID";
                case 1: return "Nombre";
                case 2: return "Aula";
                default: return "";
            }
        }

        @Override public Class<?> getColumnClass(int col) {
            return (col == 0) ? Integer.class : String.class;
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Nino n = all.get(rowIndex);
            if (n == null) return null;

            switch (columnIndex) {
                case 0:
                    return n.getId();
                case 1: {
                    String nombre = safe(n.getNombre());
                    return nombre.isBlank() ? "(Sin nombre)" : nombre;
                }
                case 2:
                    return safe(n.getAula());
                default:
                    return null;
            }
        }
    }


    // ---------------------------------------------------------------------
    // Table models
    // ---------------------------------------------------------------------

    private final class CatalogoTableModel extends AbstractTableModel {
        static final int COL_ID = 0;
        static final int COL_JUEGO = 1;
        static final int COL_TIPO = 2;
        static final int COL_DIFICULTAD = 3;
        static final int COL_HABILITADO = 4;

        private final String[] cols = {"ID", "Juego", "Tipo", "Dif.", "Habilitado"};

        @Override public int getRowCount() { return cacheJuegos.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_ID, COL_DIFICULTAD -> Integer.class;
                case COL_HABILITADO -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_DIFICULTAD || columnIndex == COL_HABILITADO;
        }

        Juego getJuegoAt(int modelRow) {
            return cacheJuegos.get(modelRow);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Juego j = cacheJuegos.get(rowIndex);
            return switch (columnIndex) {
                case COL_ID -> j.getId();
                case COL_JUEGO -> "<html><b>" + esc(j.getNombre()) + "</b><br><span style='color:#666;'>" + esc(shortDesc(j.getDescripcion(), 84)) + "</span></html>";
                case COL_TIPO -> safe(j.getTipo() != null ? j.getTipo().name() : "");
                case COL_DIFICULTAD -> getDificultadGlobalActual(j);
                case COL_HABILITADO -> isJuegoHabilitadoActual(j);
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Juego j = cacheJuegos.get(rowIndex);
            if (columnIndex == COL_HABILITADO) {
                boolean v = Boolean.TRUE.equals(aValue);
                pendingHabilitado.put(j.getId(), v);
                renderCatalogoResumen();
                actualizarAsignacionesParaSeleccionado();
                fireTableRowsUpdated(rowIndex, rowIndex);
                return;
            }
            if (columnIndex == COL_DIFICULTAD) {
                int v = 1;
                if (aValue instanceof Integer) v = (Integer) aValue;
                pendingDificultadGlobal.put(j.getId(), clamp1to5(v));
                actualizarAsignacionesParaSeleccionado();
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }

        void fireAll() {
            fireTableDataChanged();
        }
    }

    private static final class AsignacionRow {
        final int idJuego;
        final String nombre;
        final String tipo;
        final String descripcion;

        boolean asignado;

        final int difGlobal;
        int difPersonal;
        boolean personalOverride;

        AsignacionRow(int idJuego, String nombre, String tipo, String descripcion,
                      boolean asignado, int difGlobal, int difPersonal, boolean personalOverride) {
            this.idJuego = idJuego;
            this.nombre = nombre;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.asignado = asignado;
            this.difGlobal = difGlobal;
            this.difPersonal = difPersonal;
            this.personalOverride = personalOverride;
        }

        String modoTexto() {
            return personalOverride ? "Personal" : "Global";
        }

        int dificultadMostrada() {
            return personalOverride ? difPersonal : difGlobal;
        }
    }

    private final class AsignacionTableModel extends AbstractTableModel {
        static final int COL_ID = 0;
        static final int COL_JUEGO = 1;
        static final int COL_TIPO = 2;
        static final int COL_ASIGNADO = 3;
        static final int COL_MODO = 4;
        static final int COL_DIFICULTAD = 5;

        private final String[] cols = {"ID", "Juego", "Tipo", "Asignado", "Modo", "Dif."};
        private List<AsignacionRow> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_ID, COL_DIFICULTAD -> Integer.class;
                case COL_ASIGNADO -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Para que sea más intuitivo, SOLO editamos "Asignado" en la tabla.
            return columnIndex == COL_ASIGNADO;
        }

        AsignacionRow getRowAt(int modelRow) {
            return rows.get(modelRow);
        }

        List<AsignacionRow> getRows() {
            return rows;
        }

        void setRows(List<AsignacionRow> newRows) {
            this.rows = (newRows == null) ? new ArrayList<>() : new ArrayList<>(newRows);
            fireTableDataChanged();
        }

        void setAsignadoParaTodos(boolean v) {
            for (AsignacionRow r : rows) {
                r.asignado = v;
                if (!v) {
                    r.personalOverride = false;
                    r.difPersonal = r.difGlobal;
                }
            }
            fireTableDataChanged();
        }

        int countSeleccionados() {
            int c = 0;
            for (AsignacionRow r : rows) if (r.asignado) c++;
            return c;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AsignacionRow r = rows.get(rowIndex);
            return switch (columnIndex) {
                case COL_ID -> r.idJuego;
                case COL_JUEGO -> "<html><b>" + esc(r.nombre) + "</b><br><span style='color:#666;'>" + esc(shortDesc(r.descripcion, 84)) + "</span></html>";
                case COL_TIPO -> safe(r.tipo);
                case COL_ASIGNADO -> r.asignado;
                case COL_MODO -> r.modoTexto();
                case COL_DIFICULTAD -> clamp1to5(r.dificultadMostrada());
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            AsignacionRow r = rows.get(rowIndex);
            if (columnIndex == COL_ASIGNADO) {
                r.asignado = Boolean.TRUE.equals(aValue);
                if (!r.asignado) {
                    r.personalOverride = false;
                    r.difPersonal = r.difGlobal;
                }
                fireTableRowsUpdated(rowIndex, rowIndex);
                renderAsignacionResumen();
                updateEditorFromSelection();
            }
        }

        void fireAll() {
            fireTableDataChanged();
        }
    }

    // ---------------------------------------------------------------------
    // Renderers
    // ---------------------------------------------------------------------

    private static final class ProTextRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setVerticalAlignment(SwingConstants.CENTER);
            setForeground(new Color(35, 35, 35));
            return this;
        }
    }

    private static final class CenterRenderer extends DefaultTableCellRenderer {
        CenterRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 6, 0, 6));
            return this;
        }
    }

    private static final class CenterBooleanRenderer extends DefaultTableCellRenderer {
        private final JCheckBox chk = new JCheckBox();

        CenterBooleanRenderer() {
            chk.setHorizontalAlignment(SwingConstants.CENTER);
            chk.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            chk.setSelected(Boolean.TRUE.equals(value));
            chk.setBackground(table.getBackground());
            return chk;
        }
    }

    /**
     * Renderer simple para tabla de selección de estudiantes (zebra + selección).
     */
    private static final class ZebraPickerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBorder(new EmptyBorder(0, 8, 0, 8));

            if (isSelected) {
                setBackground(new Color(220, 235, 255));
            } else {
                setBackground((row % 2 == 0) ? Color.WHITE : new Color(250, 250, 250));
            }
            return this;
        }
    }


    // ---------------------------------------------------------------------
    // Spinner editor (dif. 1-5)
    // ---------------------------------------------------------------------

    private static final class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;

        SpinnerEditor(int min, int max) {
            spinner = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Integer) spinner.setValue(value);
            return spinner;
        }
    }

    // ---------------------------------------------------------------------
    // Card panel
    // ---------------------------------------------------------------------

    private static final class CardPanel extends JPanel {
        private final int arc;

        CardPanel(LayoutManager lm) {
            this(lm, 16);
        }

        CardPanel(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
            setBorder(new EmptyBorder(12, 12, 12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int pad = 1;

                Color base = UIManager.getColor("Panel.background");
                if (base == null) base = new Color(245, 245, 245);

                Color fill = blend(base, Color.WHITE, 0.70f);
                Color stroke = new Color(0, 0, 0, 20);

                g2.setColor(fill);
                g2.fillRoundRect(pad, pad, w - pad * 2, h - pad * 2, arc, arc);
                g2.setColor(stroke);
                g2.drawRoundRect(pad, pad, w - pad * 2, h - pad * 2, arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    private static String shortDesc(String s, int max) {
        s = (s == null) ? "" : s.trim();
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}