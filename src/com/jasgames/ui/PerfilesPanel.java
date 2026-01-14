package com.jasgames.ui;

import com.jasgames.model.Nino;
import com.jasgames.model.ObjetivoPIA;
import com.jasgames.model.PIA;
import com.jasgames.service.AulaService;
import com.jasgames.service.PerfilService;
import com.jasgames.service.PiaService;
import com.jasgames.util.EmojiFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PerfilesPanel (Docente)
 * - Lista de estudiantes a la izquierda con filtros (Aula + búsqueda por ID/Nombre + orden).
 * - Formulario claro a la derecha con vista previa de avatar.
 * - Botón único "Guardar" (crea o actualiza según exista el ID).
 *
 * Nota: para evitar romper el proyecto si cambian getters/setters, algunos datos (edad/diagnóstico/avatar)
 * se leen por reflexión cuando hace falta.
 */
public class PerfilesPanel extends JPanel {

    // ---------------------- Servicios ----------------------
    private final PerfilService perfilService;
    private final AulaService aulaService;
    private final PiaService piaService;

    // ---------------------- Datos (cache) ----------------------
    private final List<Nino> cacheNinos = new ArrayList<>();
    private PIA piaActual;

    // ---------------------- UI: filtros/lista ----------------------
    private JTextField txtBuscar;
    private JComboBox<String> cbOrdenarPor;
    private JComboBox<String> cbAulaFiltro;
    private JLabel lblContador;

    private DefaultListModel<Nino> listModel;
    private JList<Nino> listaNinos;

    // ---------------------- UI: formulario ----------------------
    private JPanel formPerfilesPanel;

    private JTextField txtIdNino;
    private JTextField txtNombreNino;
    private JSpinner spEdadNino;
    private JTextField txtDiagnosticoNino;
    private JComboBox<String> cbAula;
    private JComboBox<String> cbAvatar;

    private JLabel lblAvatarPreview;
    private JLabel lblEstado;
    
    // ---------------------- UI: PIA ----------------------
    private JLabel lblPiaEstado;
    private JLabel lblPiaAyuda;
    private JLabel lblPiaObjetivoActivo;

    private JComboBox<ObjetivoPIA> cbPiaObjetivoActivo;
    private JButton btnSetObjetivoActivo;

    private JButton btnEditarObjetivo;
    private JButton btnEliminarObjetivo;

    private JTextArea txtPiaObjetivoGeneral;

    private JTable tblObjetivos;
    private DefaultTableModel modeloObjetivos;

    private JSpinner spObjJuegoId;
    private JTextField txtObjDescripcion;
    private JSpinner spObjMetaRondas;
    private JSpinner spObjMetaSesiones;

    private JButton btnCrearPia;
    private JButton btnGuardarPia;
    private JButton btnCerrarPia;
    private JButton btnAgregarObjetivo;
    
    // Nuevos campos para colapsar PIA
    private JPanel panelPiaBody;
    private JButton btnExpandirPia;

    // ---------------------- UI: botones ----------------------
    private JButton btnEliminarNino;

    // Evitar listeners duplicados (esto causaba que un objetivo PIA se agregue 2 veces)
    private boolean listenersPiaInicializados = false;

    private static final String[] AVATARES = {
            "😃","😄","😁","😊",
            "🙂","😎","🤩","🥳","😺",
            "🐶","🐱","🐼","🐻","🐵",
            "🦊","🐯","🦁","🐸","🐰",
            "🐨","🐙","🐢","🦄","🐞"
    };

    public PerfilesPanel(PerfilService perfilService, AulaService aulaService, PiaService piaService) {
        this.perfilService = perfilService;
        this.aulaService = aulaService;
        this.piaService = piaService;
        initComponents();
        cargarNinosDesdeService();
        aplicarFiltrosYOrden();
        initListenersPia();
    }

    public PerfilesPanel(PerfilService perfilService, AulaService aulaService) {
        this(perfilService, aulaService, new PiaService());
    }

    // Back-compat (por si alguna pantalla antigua lo instancia directo)
    public PerfilesPanel(PerfilService perfilService) {
        this(perfilService, new AulaService(perfilService), new PiaService());
    }

    private static class DefaultDeps {
        final PerfilService perfilService = new PerfilService();
        final AulaService aulaService = new AulaService(perfilService);
        final PiaService piaService = new PiaService();
    }

    public PerfilesPanel() {
        this(new DefaultDeps());
    }

    private PerfilesPanel(DefaultDeps d) {
        this(d.perfilService, d.aulaService, d.piaService);
    }

    // ---------------------- UI ----------------------

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Init tabla PIA
        modeloObjetivos = new DefaultTableModel(
                new Object[]{"ID", "Juego", "Descripción", "MetaRondas", "ProgRondas", "MetaSes", "ProgSes", "Estado"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblObjetivos = new JTable(modeloObjetivos);

        // Ocultar columna interna (ID objetivo)
        try {
            tblObjetivos.removeColumn(tblObjetivos.getColumnModel().getColumn(0));
        } catch (Exception ignored) {}
        tblObjetivos.setRowHeight(22);
        tblObjetivos.setFillsViewportHeight(true);


        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.35);
        split.setBorder(null);
        split.setContinuousLayout(true);

        split.setLeftComponent(buildLeftSelector());
        split.setRightComponent(buildRightForm());

        add(split, BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(4, 4, 4, 4));
        header.setOpaque(false);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Gestión de Perfiles");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitle = new JLabel("Crea, edita y organiza estudiantes. Usa filtros para encontrar rápido a cualquiera.");
        subtitle.setForeground(new Color(90, 90, 90));

        text.add(title);
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> {
            cargarNinosDesdeService();
            aplicarFiltrosYOrden();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(btnRefrescar);

        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JComponent buildLeftSelector() {
        JPanel card = createCard("Estudiantes");
        card.setLayout(new BorderLayout(10, 10));

        // --- filtros (arriba)
        JPanel panelBusquedaOrden = new JPanel(new GridBagLayout());
        panelBusquedaOrden.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 2, 2, 2);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        JLabel lblAulaFiltro = new JLabel("Aula:");
        cbAulaFiltro = new JComboBox<>();
        cbAulaFiltro.setPrototypeDisplayValue("Aula XXXXXXX");
        cbAulaFiltro.addActionListener(e -> aplicarFiltrosYOrden());

        JLabel lblBuscar = new JLabel("Buscar (ID o Nombre):");
        txtBuscar = new JTextField();
        txtBuscar.setToolTipText("Ej: 12, Julio, Mera...");
        JButton btnBuscarNino = new JButton("Buscar");
        btnBuscarNino.addActionListener(e -> aplicarFiltrosYOrden());

        JLabel lblOrdenarPor = new JLabel("Orden:");
        cbOrdenarPor = new JComboBox<>(new String[]{"Nombre (A-Z)", "Nombre (Z-A)", "ID (asc)", "ID (desc)", "Aula (A-Z)"});
        cbOrdenarPor.addActionListener(e -> aplicarFiltrosYOrden());
        JButton btnOrdenar = new JButton("Aplicar");
        btnOrdenar.addActionListener(e -> aplicarFiltrosYOrden());

        JButton btnLimpiarFiltros = new JButton("Limpiar");
        btnLimpiarFiltros.addActionListener(e -> {
            txtBuscar.setText("");
            if (cbAulaFiltro.getItemCount() > 0) cbAulaFiltro.setSelectedIndex(0);
            if (cbOrdenarPor.getItemCount() > 0) cbOrdenarPor.setSelectedIndex(0);
            aplicarFiltrosYOrden();
        });

        // Row 0: Aula + Orden + Limpiar
        gc.gridx = 0; gc.weightx = 0; panelBusquedaOrden.add(lblAulaFiltro, gc);
        gc.gridx = 1; gc.weightx = 1; panelBusquedaOrden.add(cbAulaFiltro, gc);
        gc.gridx = 2; gc.weightx = 0; panelBusquedaOrden.add(lblOrdenarPor, gc);
        gc.gridx = 3; gc.weightx = 1; panelBusquedaOrden.add(cbOrdenarPor, gc);
        gc.gridx = 4; gc.weightx = 0; panelBusquedaOrden.add(btnLimpiarFiltros, gc);

        // Row 1: Buscar
        gc.gridy = 1;
        gc.gridx = 0; gc.weightx = 0; panelBusquedaOrden.add(lblBuscar, gc);
        gc.gridx = 1; gc.weightx = 2; gc.gridwidth = 3; panelBusquedaOrden.add(txtBuscar, gc);
        gc.gridwidth = 1;
        gc.gridx = 4; gc.weightx = 0; panelBusquedaOrden.add(btnBuscarNino, gc);

        // Row 2: contador
        gc.gridy = 2;
        gc.gridx = 0; gc.weightx = 1; gc.gridwidth = 5;
        lblContador = new JLabel("Mostrando 0/0");
        lblContador.setForeground(new Color(110, 110, 110));
        panelBusquedaOrden.add(lblContador, gc);

        card.add(panelBusquedaOrden, BorderLayout.NORTH);

        // --- lista (centro)
        listModel = new DefaultListModel<>();
        listaNinos = new JList<>(listModel);
        listaNinos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaNinos.setVisibleRowCount(12);
        listaNinos.setCellRenderer(new NinoCellRenderer());

        JScrollPane scrollNinos = new JScrollPane(listaNinos);
        scrollNinos.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollNinos, BorderLayout.CENTER);

        // --- listeners
        listaNinos.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Nino n = listaNinos.getSelectedValue();
            if (n != null) {
                mostrarNinoEnFormulario(n);
            } else {
                setEstado("Sin selección");
            }
        });

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltrosYOrden(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltrosYOrden(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltrosYOrden(); }
        });

        return card;
    }

    private JComponent buildRightForm() {
        JPanel card = createCard("Detalle del perfil");
        card.setLayout(new BorderLayout(10, 10));

        // --- cabecera del detalle
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        lblAvatarPreview = new JLabel("🙂", SwingConstants.CENTER);
        lblAvatarPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatarPreview.setVerticalAlignment(SwingConstants.CENTER);

        // Más espacio + padding para que el emoji no se vea “chueco” o recortado
        EmojiFonts.apply(lblAvatarPreview, 32f);
        lblAvatarPreview.setPreferredSize(new Dimension(72, 72));
        lblAvatarPreview.setMinimumSize(new Dimension(72, 72));
        lblAvatarPreview.setOpaque(true);
        lblAvatarPreview.setBackground(new Color(250, 250, 250));
        lblAvatarPreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(5, 5, 5, 5)
        ));

        // Contenedor para centrar perfectamente el preview en el header
        JPanel avatarBox = new JPanel(new GridBagLayout());
        avatarBox.setOpaque(false);
        avatarBox.setPreferredSize(new Dimension(84, 84));
        avatarBox.add(lblAvatarPreview);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Datos del estudiante");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));

        lblEstado = new JLabel("Sin selección");
        lblEstado.setForeground(new Color(110, 110, 110));

        info.add(t);
        info.add(lblEstado);

        top.add(avatarBox, BorderLayout.WEST);
        top.add(info, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);

        // --- formulario
        formPerfilesPanel = new JPanel(new GridBagLayout());
        formPerfilesPanel.setOpaque(false);

        txtIdNino = new JTextField();
        txtNombreNino = new JTextField();

        spEdadNino = new JSpinner(new SpinnerNumberModel(6, 2, 18, 1));
        ((JSpinner.DefaultEditor) spEdadNino.getEditor()).getTextField().setColumns(4);

        txtDiagnosticoNino = new JTextField();

        cbAula = new JComboBox<>();
        cbAula.setPrototypeDisplayValue("Aula XXXXXXX");

        cbAvatar = new JComboBox<>(AVATARES);
        EmojiFonts.apply(cbAvatar, 18f);
        cbAvatar.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(EmojiFonts.emoji(18f));
                return lbl;
            }
        });
        cbAvatar.addActionListener(e -> {
            String av = (String) cbAvatar.getSelectedItem();
            if (av != null && !av.isBlank()) lblAvatarPreview.setText(av);
        });

        // layout gridbag
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridy = 0;

        addField(formPerfilesPanel, gc, 0, "ID:", txtIdNino);
        addField(formPerfilesPanel, gc, 1, "Nombre:", txtNombreNino);
        addField(formPerfilesPanel, gc, 2, "Edad:", spEdadNino);
        addField(formPerfilesPanel, gc, 3, "Aula:", cbAula);
        addField(formPerfilesPanel, gc, 4, "Diagnóstico:", txtDiagnosticoNino);
        addField(formPerfilesPanel, gc, 5, "Avatar:", cbAvatar);
        
        // --- PIA (Plan Individual de Aprendizaje) ---
        gc.gridy = 6;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        
        // Usamos el nuevo panel PIA colapsable
        formPerfilesPanel.add(buildPiaPanel(), gc);
        gc.gridy++;

        // Empuja los campos hacia arriba (evita que queden centrados con mucho espacio vacío)
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 20; // un numero alto
        filler.gridwidth = 2;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.VERTICAL;
        formPerfilesPanel.add(Box.createVerticalGlue(), filler);

        // ✅ A) Convertir el formulario derecho en scroll
        JScrollPane spForm = new JScrollPane(formPerfilesPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spForm.setBorder(BorderFactory.createEmptyBorder());
        spForm.getVerticalScrollBar().setUnitIncrement(16);

        card.add(spForm, BorderLayout.CENTER);

        // --- botones abajo
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnRegistrarNino = new JButton("Guardar");
        JButton btnActualizarNino = new JButton("Actualizar"); // compat (no se muestra)
        btnActualizarNino.setVisible(false);

        btnEliminarNino = new JButton("Eliminar");
        JButton btnLimpiarCampos = new JButton("Nuevo / Limpiar");

        btnEliminarNino.setEnabled(false); // hasta que haya selección válida

        actions.add(btnLimpiarCampos);
        actions.add(btnEliminarNino);
        actions.add(btnRegistrarNino);

        card.add(actions, BorderLayout.SOUTH);

        // listeners
        btnRegistrarNino.addActionListener(e -> guardarCrearOActualizar());
        btnEliminarNino.addActionListener(e -> eliminarNino());
        btnLimpiarCampos.addActionListener(e -> limpiarCampos());

        // UX: si escriben un ID existente, avisa y habilita eliminar
        txtIdNino.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onIdTyping(); }
            @Override public void removeUpdate(DocumentEvent e) { onIdTyping(); }
            @Override public void changedUpdate(DocumentEvent e) { onIdTyping(); }
        });

        return card;
    }
    
    private JPanel buildPiaPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        // Borde superior sutil para separar del formulario
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 0, 4, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;

        // 1) Header Row: Title + Help + Expand/Collapse
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        
        JLabel lblTitle = new JLabel("PIA (Plan Individual de Aprendizaje)");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 14f));
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        headerRight.setOpaque(false);
        
        JButton btnAyudaPia = crearBotonAyudaPia();
        btnAyudaPia.addActionListener(e -> mostrarAyudaPIA());
        
        btnExpandirPia = new JButton("Ver detalles ▼");
        btnExpandirPia.setFocusable(false);
        btnExpandirPia.addActionListener(e -> togglePiaBody());
        
        headerRight.add(btnAyudaPia);
        headerRight.add(btnExpandirPia);
        
        header.add(lblTitle, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        
        root.add(header, gc);
        gc.gridy++;

        // 2) Status Row: State + Buttons
        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setOpaque(false);
        statusPanel.setBorder(new EmptyBorder(0, 8, 0, 8));

        lblPiaEstado = new JLabel("<html><b>PIA:</b> —</html>");
        lblPiaAyuda = new JLabel("...");
        lblPiaAyuda.setFont(lblPiaAyuda.getFont().deriveFont(Font.PLAIN, 11f));
        
        JPanel statusLeft = new JPanel();
        statusLeft.setOpaque(false);
        statusLeft.setLayout(new BoxLayout(statusLeft, BoxLayout.Y_AXIS));
        statusLeft.add(lblPiaEstado);
        statusLeft.add(Box.createVerticalStrut(2));
        statusLeft.add(lblPiaAyuda);
        
        // Buttons
        btnCrearPia = new JButton("Crear PIA");
        btnGuardarPia = new JButton("Guardar PIA");
        btnCerrarPia = new JButton("Cerrar PIA");
        
        JPanel statusButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        statusButtons.setOpaque(false);
        statusButtons.add(btnCrearPia);
        statusButtons.add(btnGuardarPia);
        statusButtons.add(btnCerrarPia);
        
        statusPanel.add(statusLeft, BorderLayout.WEST);
        statusPanel.add(statusButtons, BorderLayout.EAST);
        
        root.add(statusPanel, gc);
        gc.gridy++;

        // 3) Body Panel (Collapsible)
        panelPiaBody = new JPanel(new GridBagLayout());
        panelPiaBody.setOpaque(false);
        panelPiaBody.setVisible(false); // Hidden by default
        panelPiaBody.setBorder(new EmptyBorder(10, 8, 0, 8));

        GridBagConstraints gBody = new GridBagConstraints();
        gBody.insets = new Insets(4, 0, 4, 0);
        gBody.fill = GridBagConstraints.HORIZONTAL;
        gBody.anchor = GridBagConstraints.NORTHWEST;
        gBody.gridx = 0;
        gBody.gridy = 0;
        gBody.weightx = 1.0;

        // Content inside body
        lblPiaObjetivoActivo = new JLabel("Objetivo en progreso: —");
        panelPiaBody.add(lblPiaObjetivoActivo, gBody);
        gBody.gridy++;
        // Selector de objetivo activo (para que el docente decida qué se trabaja primero)
        JPanel filaObjActivo = new JPanel(new BorderLayout(8, 0));
        filaObjActivo.setOpaque(false);

        JLabel lblSel = new JLabel("Cambiar objetivo en progreso:");
        lblSel.setFont(lblSel.getFont().deriveFont(Font.PLAIN, 11f));

        cbPiaObjetivoActivo = new JComboBox<>();
        cbPiaObjetivoActivo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ObjetivoPIA) {
                    ObjetivoPIA o = (ObjetivoPIA) value;
                    String txt = "Juego " + o.getJuegoId() + " — " + o.getDescripcion();
                    setText(txt);
                }
                return this;
            }
        });

        btnSetObjetivoActivo = new JButton("Usar");
        btnSetObjetivoActivo.setFocusable(false);
        btnSetObjetivoActivo.setToolTipText("Establecer el objetivo seleccionado como el objetivo en progreso");

        JPanel rightObj = new JPanel(new BorderLayout(6, 0));
        rightObj.setOpaque(false);
        rightObj.add(cbPiaObjetivoActivo, BorderLayout.CENTER);
        rightObj.add(btnSetObjetivoActivo, BorderLayout.EAST);

        filaObjActivo.add(lblSel, BorderLayout.WEST);
        filaObjActivo.add(rightObj, BorderLayout.CENTER);

        panelPiaBody.add(filaObjActivo, gBody);
        gBody.gridy++;


        // Objetivo general
        JLabel lblObj = new JLabel("<html><b>Objetivo general</b> (resumen para el docente)</html>");
        panelPiaBody.add(lblObj, gBody);
        gBody.gridy++;

        txtPiaObjetivoGeneral = new JTextArea(3, 20);
        txtPiaObjetivoGeneral.setLineWrap(true);
        txtPiaObjetivoGeneral.setWrapStyleWord(true);
        JScrollPane spObjGen = new JScrollPane(txtPiaObjetivoGeneral);
        spObjGen.setPreferredSize(new Dimension(10, 60));
        panelPiaBody.add(spObjGen, gBody);
        gBody.gridy++;

        // Table
        JLabel lblTabla = new JLabel("<html><b>Objetivos medibles</b> (por juego)</html>");
        panelPiaBody.add(lblTabla, gBody);
        gBody.gridy++;

        JLabel lblLeyenda = new JLabel("MetaRondas/ProgRondas: rondas correctas | MetaSes/ProgSes: sesiones completadas");
        lblLeyenda.setFont(lblLeyenda.getFont().deriveFont(10f));
        panelPiaBody.add(lblLeyenda, gBody);
        gBody.gridy++;

        JScrollPane spTabla = new JScrollPane(tblObjetivos);
        spTabla.setPreferredSize(new Dimension(10, 120));
        panelPiaBody.add(spTabla, gBody);
        gBody.gridy++;

        // Botones de acción para objetivos
        JPanel pnlAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pnlAcciones.setOpaque(false);
        btnEditarObjetivo = new JButton("Editar");
        btnEliminarObjetivo = new JButton("Eliminar");
        pnlAcciones.add(btnEditarObjetivo);
        pnlAcciones.add(btnEliminarObjetivo);
        panelPiaBody.add(pnlAcciones, gBody);
        gBody.gridy++;

        // Add Objective Panel
        JPanel nuevo = new JPanel(new GridBagLayout());
        nuevo.setOpaque(false);
        nuevo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Agregar objetivo",
                TitledBorder.LEFT, TitledBorder.TOP
        ));

        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 6, 4, 6);
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.anchor = GridBagConstraints.NORTHWEST;

        spObjJuegoId = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        txtObjDescripcion = new JTextField();
        spObjMetaRondas = new JSpinner(new SpinnerNumberModel(3, 1, 200, 1));
        spObjMetaSesiones = new JSpinner(new SpinnerNumberModel(1, 1, 200, 1));

        JLabel hintJuego = new JLabel("Tip: usa el ID del juego tal como aparece en la pestaña “Juegos”.");
        hintJuego.setFont(hintJuego.getFont().deriveFont(Font.PLAIN, 11f));

        addField(nuevo, g2, 0, "Juego (ID):", spObjJuegoId);
        g2.gridy = 1; g2.gridx = 0; g2.gridwidth = 2; g2.weightx = 1;
        nuevo.add(hintJuego, g2);
        g2.gridwidth = 1;

        addField(nuevo, g2, 2, "Descripción:", txtObjDescripcion);
        addField(nuevo, g2, 3, "Meta (rondas):", spObjMetaRondas);
        addField(nuevo, g2, 4, "Meta (sesiones):", spObjMetaSesiones);

        btnAgregarObjetivo = new JButton("Agregar objetivo");
        JPanel filaBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaBtn.setOpaque(false);
        filaBtn.add(btnAgregarObjetivo);

        g2.gridy = 5; g2.gridx = 0; g2.gridwidth = 2;
        nuevo.add(filaBtn, g2);

        panelPiaBody.add(nuevo, gBody);
        gBody.gridy++;

        root.add(panelPiaBody, gc);
        
        // Init state
        setPiaEdicionEnabled(false);
        // Listeners del PIA
        initListenersPia();

        return root;
    }
    
    private void togglePiaBody() {
        boolean visible = !panelPiaBody.isVisible();
        panelPiaBody.setVisible(visible);
        btnExpandirPia.setText(visible ? "Ocultar detalles ▲" : "Ver detalles ▼");
        // Revalidate to adjust layout
        panelPiaBody.revalidate();
        // We need to revalidate the scroll pane content
        formPerfilesPanel.revalidate();
        formPerfilesPanel.repaint();
    }
    
    private void addField(JPanel panel, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridy = row;
        gc.gridx = 0; gc.weightx = 0;
        panel.add(new JLabel(label), gc);

        gc.gridx = 1; gc.weightx = 1;
        panel.add(field, gc);
    }

    private JPanel createCard(String title) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        p.setBackground(new Color(252, 252, 252));

        if (title != null && !title.isBlank()) {
            TitledBorder tb = BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(),
                    title,
                    TitledBorder.LEFT,
                    TitledBorder.TOP
            );
            tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD, 12f));
            p.setBorder(BorderFactory.createCompoundBorder(tb, p.getBorder()));
        }
        return p;
    }

    // ---------------------- Lógica ----------------------

    private void cargarNinosDesdeService() {
        cacheNinos.clear();
        try {
            List<Nino> todos = perfilService.obtenerTodosNinos();
            if (todos != null) cacheNinos.addAll(todos);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando perfiles: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        refrescarOpcionesAula();
    }

    private void refrescarOpcionesAula() {
        // Carga aulas desde AulaService si existe método; si no, usa las aulas detectadas de los niños.
        Set<String> aulas = new LinkedHashSet<>();
        aulas.add("Todas");

        // intentamos con AulaService (sin romper compilación si cambian nombres)
        if (aulaService != null) {
            List<String> fromSvc = tryGetAulasFromService(aulaService);
            if (fromSvc != null) {
                for (String a : fromSvc) if (a != null && !a.isBlank()) aulas.add(a.trim());
            }
        }

        // fallback: deducir desde niños
        for (Nino n : cacheNinos) {
            String a = safe(getPropString(n, "getAula"));
            if (!a.isBlank()) aulas.add(a.trim());
        }

        // filtro (izquierda)
        if (cbAulaFiltro != null) {
            cbAulaFiltro.removeAllItems();
            for (String a : aulas) cbAulaFiltro.addItem(a);
        }

        // aula del formulario (derecha) sin "Todas"
        if (cbAula != null) {
            cbAula.removeAllItems();
            for (String a : aulas) {
                if (!"Todas".equalsIgnoreCase(a)) cbAula.addItem(a);
            }
            if (cbAula.getItemCount() == 0) cbAula.addItem("Aula Azul");
        }
    }

    /**
     * Refresca únicamente las aulas de los combos (sin depender de que exista un alumno en esa aula).
     * Útil cuando se crean aulas nuevas desde AulasPanel.
     */
    public void refrescarAulas() {
        Object selFiltro = (cbAulaFiltro != null) ? cbAulaFiltro.getSelectedItem() : null;
        Object selForm = (cbAula != null) ? cbAula.getSelectedItem() : null;

        refrescarOpcionesAula();

        // Reintentar selección anterior (si todavía existe)
        if (cbAulaFiltro != null && selFiltro != null) cbAulaFiltro.setSelectedItem(selFiltro);
        if (cbAula != null && selForm != null) cbAula.setSelectedItem(selForm);
    }

    @SuppressWarnings("unchecked")
    private List<String> tryGetAulasFromService(AulaService svc) {
        String[] candidates = {"obtenerNombres", "obtenerNombresAulas", "listarNombresAulas", "getNombresAulas", "obtenerAulas", "listarAulas", "obtenerTodas"};
        for (String mName : candidates) {
            try {
                Method m = svc.getClass().getMethod(mName);
                Object r = m.invoke(svc);
                if (r instanceof List) return (List<String>) r;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void aplicarFiltrosYOrden() {
        // 1) filtrar (recursivo)
        String q = safe(txtBuscar.getText()).trim();
        String aulaSel = (String) cbAulaFiltro.getSelectedItem();
        if (aulaSel == null) aulaSel = "Todas";
        final String qNorm = normalize(q);
        final String aulaNorm = normalize(aulaSel);

        List<Nino> filtrados = new ArrayList<>();
        filtrarRecursivo(cacheNinos, 0, qNorm, aulaNorm, filtrados);

        // 2) ordenar
        Comparator<Nino> comp = buildComparator((String) cbOrdenarPor.getSelectedItem());
        filtrados.sort(comp);

        // 3) pintar modelo
        listModel.clear();
        for (Nino n : filtrados) listModel.addElement(n);

        lblContador.setText("Mostrando " + filtrados.size() + "/" + cacheNinos.size());

        // si el seleccionado ya no existe, limpia selección
        if (listaNinos.getSelectedIndex() >= listModel.size()) {
            listaNinos.clearSelection();
        }
    }

    private void filtrarRecursivo(List<Nino> src, int idx, String qNorm, String aulaNorm, List<Nino> out) {
        if (src == null || idx >= src.size()) return;

        Nino n = src.get(idx);
        boolean aulaOk = true;
        if (!aulaNorm.isBlank() && !"todas".equals(aulaNorm)) {
            String a = normalize(safe(getPropString(n, "getAula")));
            aulaOk = a.equals(aulaNorm);
        }

        boolean queryOk = true;
        if (!qNorm.isBlank()) {
            String id = String.valueOf(getIdSafe(n));
            String nombre = normalize(safe(getPropString(n, "getNombre")));
            queryOk = id.contains(qNorm) || nombre.contains(qNorm);
        }

        if (aulaOk && queryOk) out.add(n);

        filtrarRecursivo(src, idx + 1, qNorm, aulaNorm, out);
    }

    private Comparator<Nino> buildComparator(String orden) {
        if (orden == null) orden = "Nombre (A-Z)";
        switch (orden) {
            case "Nombre (Z-A)":
                return Comparator.comparing((Nino n) -> safe(getPropString(n, "getNombre")), String.CASE_INSENSITIVE_ORDER).reversed();
            case "ID (asc)":
                return Comparator.comparingInt(this::getIdSafe);
            case "ID (desc)":
                return Comparator.comparingInt(this::getIdSafe).reversed();
            case "Aula (A-Z)":
                return Comparator.comparing((Nino n) -> safe(getPropString(n, "getAula")), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(n -> safe(getPropString(n, "getNombre")), String.CASE_INSENSITIVE_ORDER);
            case "Nombre (A-Z)":
            default:
                return Comparator.comparing((Nino n) -> safe(getPropString(n, "getNombre")), String.CASE_INSENSITIVE_ORDER);
        }
    }

    private void mostrarNinoEnFormulario(Nino n) {
        if (n == null) return;

        txtIdNino.setText(String.valueOf(getIdSafe(n)));
        txtIdNino.setEditable(false);
        txtNombreNino.setText(safe(getPropString(n, "getNombre")));

        Integer edad = getPropInt(n, "getEdad");
        if (edad != null) spEdadNino.setValue(clamp(edad, 2, 18));

        txtDiagnosticoNino.setText(safe(getPropString(n, "getDiagnostico")));

        String aula = safe(getPropString(n, "getAula"));
        if (!aula.isBlank()) cbAula.setSelectedItem(aula);

        String avatar = safe(getPropString(n, "getAvatar"));
        if (!avatar.isBlank()) cbAvatar.setSelectedItem(avatar);

        // preview
        String av = (String) cbAvatar.getSelectedItem();
        if (av != null && !av.isBlank()) lblAvatarPreview.setText(av);

        btnEliminarNino.setEnabled(true);
        setEstado("Editando: " + safe(getPropString(n, "getNombre")) + " · id=" + getIdSafe(n));
        
        cargarPiaDelNino();
    }
    
    private void cargarPiaDelNino() {
        modeloObjetivos.setRowCount(0);
        piaActual = null;
        
        Nino ninoSeleccionado = listaNinos.getSelectedValue();

        if (ninoSeleccionado == null) {
            lblPiaEstado.setText("<html><b>PIA:</b> —</html>");
            lblPiaObjetivoActivo.setText("Objetivo en progreso: —");
            if (lblPiaAyuda != null) lblPiaAyuda.setText("Selecciona un estudiante para ver o crear su PIA.");
            txtPiaObjetivoGeneral.setText("");
            btnCrearPia.setEnabled(false);
            setPiaEdicionEnabled(false);
            return;
        }

        piaActual = piaService.obtenerActivo(getIdSafe(ninoSeleccionado));
        if (piaActual != null) piaActual.asegurarObjetivoActivoValido();
        if (piaActual == null) {
            lblPiaEstado.setText("<html><b>PIA:</b> Sin PIA activo</html>");
            lblPiaObjetivoActivo.setText("Objetivo en progreso: —");
            if (lblPiaAyuda != null) lblPiaAyuda.setText("Crea un PIA para definir objetivos. El progreso se llenará automáticamente cuando el estudiante juegue.");
            txtPiaObjetivoGeneral.setText("");
            btnCrearPia.setEnabled(true);
            setPiaEdicionEnabled(false);
            return;
        }

        lblPiaEstado.setText("<html><b>PIA:</b> Activo</html>");
        ObjetivoPIA obj = piaActual.getObjetivoActivo();
        lblPiaObjetivoActivo.setText(
                (obj == null)
                        ? "Objetivo en progreso: (no hay objetivos)"
                        : "Objetivo en progreso: Juego " + obj.getJuegoId() + " — " + obj.getDescripcion() + " (" + obj.getProgresoRondasCorrectas() + "/" + obj.getMetaRondasCorrectas() + ")"
        );
        if (lblPiaAyuda != null) lblPiaAyuda.setText("Edita el objetivo general y agrega objetivos por juego. (El sistema actualizará progreso automáticamente)");
        
        txtPiaObjetivoGeneral.setText(piaActual.getObjetivoGeneral() == null ? "" : piaActual.getObjetivoGeneral());

        // Llenar combo de objetivos en progreso (solo no completados)
        if (cbPiaObjetivoActivo != null) {
            DefaultComboBoxModel<ObjetivoPIA> model = new DefaultComboBoxModel<>();
            for (ObjetivoPIA o : piaActual.getObjetivos()) {
                if (o != null && !o.isCompletado()) model.addElement(o);
            }
            cbPiaObjetivoActivo.setModel(model);

            // Seleccionar el activo si existe
            String idAct = piaActual.getIdObjetivoActivo();
            if (idAct != null) {
                for (int i = 0; i < model.getSize(); i++) {
                    ObjetivoPIA o = model.getElementAt(i);
                    if (o != null && idAct.equals(o.getIdObjetivo())) {
                        cbPiaObjetivoActivo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            cbPiaObjetivoActivo.setEnabled(model.getSize() > 0);
            if (btnSetObjetivoActivo != null) btnSetObjetivoActivo.setEnabled(model.getSize() > 0);
        }

        for (ObjetivoPIA o : piaActual.getObjetivos()) {
            if (o == null) continue;
            modeloObjetivos.addRow(new Object[]{
                    o.getIdObjetivo(),
                    o.getJuegoId(),
                    o.getDescripcion(),
                    o.getMetaRondasCorrectas(),
                    o.getProgresoRondasCorrectas(),
                    o.getMetaSesionesCompletadas(),
                    o.getProgresoSesionesCompletadas(),
                    o.isCompletado() ? "✅" : "⏳"
            });
        }


        btnCrearPia.setEnabled(false);
        setPiaEdicionEnabled(true);
    }

    private void setPiaEdicionEnabled(boolean enabled) {
        if (txtPiaObjetivoGeneral != null) txtPiaObjetivoGeneral.setEnabled(enabled);
        if (tblObjetivos != null) tblObjetivos.setEnabled(enabled);

        if (spObjJuegoId != null) spObjJuegoId.setEnabled(enabled);
        if (txtObjDescripcion != null) txtObjDescripcion.setEnabled(enabled);
        if (spObjMetaRondas != null) spObjMetaRondas.setEnabled(enabled);
        if (spObjMetaSesiones != null) spObjMetaSesiones.setEnabled(enabled);

        if (btnGuardarPia != null) btnGuardarPia.setEnabled(enabled);
        if (btnCerrarPia != null) btnCerrarPia.setEnabled(enabled);
        if (btnAgregarObjetivo != null) btnAgregarObjetivo.setEnabled(enabled);
        if (cbPiaObjetivoActivo != null) cbPiaObjetivoActivo.setEnabled(enabled);
        if (btnSetObjetivoActivo != null) btnSetObjetivoActivo.setEnabled(enabled);

        if (btnEditarObjetivo != null) btnEditarObjetivo.setEnabled(enabled && tblObjetivos.getSelectedRow() >= 0);
        if (btnEliminarObjetivo != null) btnEliminarObjetivo.setEnabled(enabled && tblObjetivos.getSelectedRow() >= 0);
    }
    
    private static void clearActionListeners(AbstractButton b) {
        if (b == null) return;
        for (ActionListener al : b.getActionListeners()) {
            b.removeActionListener(al);
        }
    }

    private void initListenersPia() {
        // Este método puede llamarse varias veces (constructor + builder). Para evitar duplicados
        // (ej. crear objetivo 2 veces), hacemos "rebind": limpiamos listeners anteriores y volvemos a añadirlos.
        if (btnCrearPia == null || btnGuardarPia == null || btnCerrarPia == null || btnAgregarObjetivo == null) {
            return; // aún no se construyó el panel PIA
        }

        clearActionListeners(btnCrearPia);
        clearActionListeners(btnGuardarPia);
        clearActionListeners(btnCerrarPia);
        clearActionListeners(btnAgregarObjetivo);
        clearActionListeners(btnSetObjetivoActivo);
        clearActionListeners(btnEditarObjetivo);
        clearActionListeners(btnEliminarObjetivo);

        // Evitar duplicar listeners de selección en la tabla
        try {
            if (tblObjetivos != null && tblObjetivos.getSelectionModel() instanceof DefaultListSelectionModel) {
                DefaultListSelectionModel m = (DefaultListSelectionModel) tblObjetivos.getSelectionModel();
                for (ListSelectionListener l : m.getListSelectionListeners()) {
                    m.removeListSelectionListener(l);
                }
            }
        } catch (Exception ignored) {
        }

        btnCrearPia.addActionListener(e -> {
            Nino ninoSeleccionado = listaNinos.getSelectedValue();
            if (ninoSeleccionado == null) return;

            PIA pia = new PIA();
            pia.setIdNino(getIdSafe(ninoSeleccionado));
            pia.setNombreNino(safe(getPropString(ninoSeleccionado, "getNombre")));
            pia.setAula((String) cbAula.getSelectedItem());
            pia.setObjetivoGeneral(txtPiaObjetivoGeneral.getText().trim());
            pia.setActivo(true);

            piaService.guardar(pia);
            cargarPiaDelNino();
        });

        btnGuardarPia.addActionListener(e -> {
            if (piaActual == null) return;
            piaActual.setObjetivoGeneral(txtPiaObjetivoGeneral.getText().trim());
            piaService.guardar(piaActual);
            cargarPiaDelNino();
        });

        btnCerrarPia.addActionListener(e -> {
            if (piaActual == null) return;
            piaService.cerrarPIA(piaActual.getIdPia());
            cargarPiaDelNino();
        });

        btnAgregarObjetivo.addActionListener(e -> {
            if (piaActual == null) return;

            // Protege contra doble-disparo (doble click / Enter) mientras se procesa
            btnAgregarObjetivo.setEnabled(false);
            try {

            int juegoId = (int) spObjJuegoId.getValue();
            String desc = txtObjDescripcion.getText().trim();
            int metaRondas = (int) spObjMetaRondas.getValue();
            int metaSes = (int) spObjMetaSesiones.getValue();

            if (desc.isBlank()) desc = "Objetivo juego " + juegoId;

            ObjetivoPIA obj = new ObjetivoPIA(juegoId, desc, metaRondas, metaSes);
            piaActual.getObjetivos().add(obj);

            piaService.guardar(piaActual);

            txtObjDescripcion.setText("");
            cargarPiaDelNino();
            } finally {
                btnAgregarObjetivo.setEnabled(true);
            }
        });
        // Selección / cambio de objetivo activo
        if (btnSetObjetivoActivo != null) {
            btnSetObjetivoActivo.addActionListener(e -> {
                if (piaActual == null) return;
                Object sel = cbPiaObjetivoActivo.getSelectedItem();
                if (!(sel instanceof ObjetivoPIA)) return;

                ObjetivoPIA obj = (ObjetivoPIA) sel;
                piaActual.setIdObjetivoActivo(obj.getIdObjetivo());
                piaActual.asegurarObjetivoActivoValido();
                piaService.guardar(piaActual);
                cargarPiaDelNino();
            });
        }

        // Editar / eliminar objetivo seleccionado
        if (btnEditarObjetivo != null) {
            btnEditarObjetivo.addActionListener(e -> editarObjetivoSeleccionado());
        }
        if (btnEliminarObjetivo != null) {
            btnEliminarObjetivo.addActionListener(e -> eliminarObjetivoSeleccionado());
        }

        // Habilitar/deshabilitar botones según selección
        tblObjetivos.getSelectionModel().addListSelectionListener(e -> actualizarAccionesObjetivo());
        actualizarAccionesObjetivo();
    }

    private void actualizarAccionesObjetivo() {
        boolean enabled = (piaActual != null && piaActual.isActivo());
        boolean selected = (tblObjetivos.getSelectedRow() >= 0);

        if (btnEditarObjetivo != null) btnEditarObjetivo.setEnabled(enabled && selected);
        if (btnEliminarObjetivo != null) btnEliminarObjetivo.setEnabled(enabled && selected);
    }

    private void editarObjetivoSeleccionado() {
        int row = tblObjetivos.getSelectedRow();
        if (row < 0 || piaActual == null) return;

        String idObj = (String) modeloObjetivos.getValueAt(row, 0);
        ObjetivoPIA obj = piaActual.getObjetivoPorId(idObj);
        if (obj == null) return;

        // Permite corregir el juego asociado (si cambia, reiniciamos progreso para evitar mezclas)
        JSpinner spJuegoId = new JSpinner(new SpinnerNumberModel(obj.getJuegoId(), 1, 99, 1));
        JTextField txtDesc = new JTextField(obj.getDescripcion());
        JSpinner spRondas = new JSpinner(new SpinnerNumberModel(obj.getMetaRondasCorrectas(), 1, 999, 1));
        JSpinner spSesiones = new JSpinner(new SpinnerNumberModel(obj.getMetaSesionesCompletadas(), 1, 999, 1));

        JPanel p = new JPanel(new GridLayout(0, 1));
        p.add(new JLabel("Juego (ID):"));
        p.add(spJuegoId);

        JLabel lblNota = new JLabel("<html><i>Nota:</i> si cambias el Juego (ID), el progreso del objetivo se reinicia.</html>");
        p.add(lblNota);

        p.add(new JLabel("Descripción:"));
        p.add(txtDesc);
        p.add(new JLabel("Meta Rondas Correctas:"));
        p.add(spRondas);
        p.add(new JLabel("Meta Sesiones Completadas:"));
        p.add(spSesiones);

        int r = JOptionPane.showConfirmDialog(
                this, p, "Editar Objetivo",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (r == JOptionPane.OK_OPTION) {
            piaService.actualizarObjetivo(piaActual.getIdPia(), idObj, o -> {
                int nuevoJuego = (Integer) spJuegoId.getValue();
                if (nuevoJuego != o.getJuegoId()) {
                    o.setJuegoId(nuevoJuego);
                    o.setProgresoRondasCorrectas(0);
                    o.setProgresoSesionesCompletadas(0);
                    o.setCompletado(false);
                    o.setFechaCompletado(null);
                }

                o.setDescripcion(txtDesc.getText().trim());
                o.setMetaRondasCorrectas((Integer) spRondas.getValue());
                o.setMetaSesionesCompletadas((Integer) spSesiones.getValue());
            });

            cargarPiaDelNino();
        }
    }

    private void eliminarObjetivoSeleccionado() {
        int row = tblObjetivos.getSelectedRow();
        if (row < 0 || piaActual == null) return;

        String idObj = (String) modeloObjetivos.getValueAt(row, 0);
        String desc = (String) modeloObjetivos.getValueAt(row, 2);

        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el objetivo \"" + desc + "\"?\nEl progreso acumulado se perderá.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

        if (r == JOptionPane.YES_OPTION) {
            piaService.eliminarObjetivo(piaActual.getIdPia(), idObj);
            cargarPiaDelNino();
        }
    }

    private void setEstado(String s) {
        lblEstado.setText(s);
    }

    private void onIdTyping() {
        int id = parseIntSafe(txtIdNino.getText().trim(), -1);
        if (id <= 0) {
            btnEliminarNino.setEnabled(listaNinos.getSelectedValue() != null);
            return;
        }

        Nino exist = null;
        try {
            exist = perfilService.buscarNinoPorId(id);
        } catch (Exception ignored) {
        }

        if (exist != null) {
            setEstado("ID existente → al guardar se actualizará · id=" + id);
            btnEliminarNino.setEnabled(true);
        } else {
            setEstado("Nuevo perfil (ID libre) · id=" + id);
            btnEliminarNino.setEnabled(false);
        }
    }

    private void guardarCrearOActualizar() {
        try {
            int id = Integer.parseInt(txtIdNino.getText().trim());
            String nombre = txtNombreNino.getText().trim();
            int edad = (Integer) spEdadNino.getValue();
            String diagnostico = txtDiagnosticoNino.getText().trim();

            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String aula = (String) cbAula.getSelectedItem();
            if (aula == null || aula.isBlank()) aula = "Aula Azul";

            String avatar = (String) cbAvatar.getSelectedItem();
            if (avatar == null || avatar.isBlank()) avatar = "🙂";

            // Creamos un objeto limpio (evita depender de setters que puedan cambiar).
            Nino nino = new Nino(id, nombre, edad, diagnostico);
            setPropString(nino, "setAula", aula);
            setPropString(nino, "setAvatar", avatar);

            boolean existe = perfilService.buscarNinoPorId(id) != null;

            if (!existe) {
                perfilService.registrarNino(nino);
                JOptionPane.showMessageDialog(this, "Perfil registrado: " + nombre, "OK",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                perfilService.actualizarNino(nino);
                JOptionPane.showMessageDialog(this, "Perfil actualizado: " + nombre, "OK",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            cargarNinosDesdeService();
            aplicarFiltrosYOrden();
            seleccionarPorId(id);
            txtIdNino.setEditable(false);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El ID debe ser un número válido", "Validación",
                    JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seleccionarPorId(int id) {
        for (int i = 0; i < listModel.size(); i++) {
            Nino n = listModel.get(i);
            if (getIdSafe(n) == id) {
                listaNinos.setSelectedIndex(i);
                listaNinos.ensureIndexIsVisible(i);
                return;
            }
        }
    }


    /**
     * Permite que otras pantallas (p.ej. AulasPanel) seleccionen un niño por ID.
     * Se usa desde DocenteWindow.
     */
    public void seleccionarNinoPorId(int idNino) {
        if (idNino <= 0) return;

        // Si con filtros actuales no aparece, limpiamos filtros para garantizar que se pueda ver.
        boolean encontrado = false;
        for (Nino n : cacheNinos) {
            if (getIdSafe(n) == idNino) { encontrado = true; break; }
        }
        if (!encontrado) return;

        // mostrar todo para asegurar selección
        if (txtBuscar != null) txtBuscar.setText(String.valueOf(idNino));
        if (cbAulaFiltro != null && cbAulaFiltro.getItemCount() > 0) cbAulaFiltro.setSelectedIndex(0);

        aplicarFiltrosYOrden();
        seleccionarPorId(idNino);
    }

    /**
     * Navegación desde Dashboard: abre el perfil del niño y enfoca el PIA.
     * Si se pasa un idObjetivo, lo selecciona como "objetivo en progreso".
     */
    public void irAObjetivoPia(int idNino, String idObjetivo) {
        if (idNino <= 0) return;
        seleccionarNinoPorId(idNino);

        // Esperar a que el UI termine de cargar el perfil seleccionado
        SwingUtilities.invokeLater(() -> {
            expandirPiaDetalles(true);
            if (idObjetivo != null && !idObjetivo.isBlank()) {
                seleccionarObjetivoEnProgreso(idObjetivo, true);
            }
        });
    }

    private void expandirPiaDetalles(boolean abrir) {
        if (panelPiaBody == null || btnExpandirPia == null) return;
        boolean visible = panelPiaBody.isVisible();
        if (abrir && !visible) {
            togglePiaBody();
        }
        if (!abrir && visible) {
            togglePiaBody();
        }
    }

    private void seleccionarObjetivoEnProgreso(String idObjetivo, boolean aplicar) {
        if (cbPiaObjetivoActivo == null || idObjetivo == null) return;

        for (int i = 0; i < cbPiaObjetivoActivo.getItemCount(); i++) {
            Object it = cbPiaObjetivoActivo.getItemAt(i);
            if (it instanceof ObjetivoPIA) {
                ObjetivoPIA o = (ObjetivoPIA) it;
                if (o != null && idObjetivo.equals(o.getIdObjetivo())) {
                    cbPiaObjetivoActivo.setSelectedIndex(i);
                    if (aplicar && btnSetObjetivoActivo != null && btnSetObjetivoActivo.isEnabled()) {
                        btnSetObjetivoActivo.doClick();
                    }
                    return;
                }
            }
        }
    }

    /**
     * Elimina un niño de forma compatible: intenta varios nombres de método para no romper compilación
     * si el service cambia.
     */
    private boolean invokeEliminarNino(int id) throws Exception {
        String[] candidates = {"eliminarNino", "eliminarNinoPorId", "borrarNino", "removerNino", "deleteNino"};
        Exception last = null;
        for (String mName : candidates) {
            try {
                Method m = perfilService.getClass().getMethod(mName, int.class);
                Object r = m.invoke(perfilService, id);
                if (r instanceof Boolean) return (Boolean) r;
                return true; // si no devuelve boolean, asumimos OK
            } catch (NoSuchMethodException ex) {
                last = ex;
            }
        }
        if (last != null) throw last;
        return false;
    }

    private void eliminarNino() {
        int id = parseIntSafe(txtIdNino.getText().trim(), -1);
        if (id <= 0) {
            JOptionPane.showMessageDialog(this, "Escribe un ID válido para eliminar.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el perfil con ID " + id + "?\nEsta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

        if (r != JOptionPane.YES_OPTION) return;

        try {
            invokeEliminarNino(id);
            JOptionPane.showMessageDialog(this, "Perfil eliminado (id=" + id + ")", "OK",
                    JOptionPane.INFORMATION_MESSAGE);

            limpiarCampos();
            cargarNinosDesdeService();
            aplicarFiltrosYOrden();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarCampos() {
        txtIdNino.setText("");
        txtIdNino.setEditable(true);
        txtIdNino.requestFocusInWindow();
        txtNombreNino.setText("");
        spEdadNino.setValue(6);
        txtDiagnosticoNino.setText("");

        if (cbAula.getItemCount() > 0) cbAula.setSelectedIndex(0);
        cbAvatar.setSelectedIndex(0);
        lblAvatarPreview.setText((String) cbAvatar.getSelectedItem());

        listaNinos.clearSelection();
        btnEliminarNino.setEnabled(false);
        setEstado("Nuevo perfil");
        
        cargarPiaDelNino();
    }

    // ---------------------- Helpers (reflexión y strings) ----------------------

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int getIdSafe(Nino n) {
        try {
            return n.getId();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String normalize(String s) {
        return safe(s).trim().toLowerCase();
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String getPropString(Object obj, String getter) {
        if (obj == null) return "";
        try {
            Method m = obj.getClass().getMethod(getter);
            Object r = m.invoke(obj);
            return r == null ? "" : String.valueOf(r);
        } catch (Exception ignored) {
            return "";
        }
    }

    private Integer getPropInt(Object obj, String getter) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(getter);
            Object r = m.invoke(obj);
            if (r instanceof Integer) return (Integer) r;
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {
        }
        return null;
    }

    private void setPropString(Object obj, String setter, String value) {
        if (obj == null) return;
        try {
            Method m = obj.getClass().getMethod(setter, String.class);
            m.invoke(obj, value);
        } catch (Exception ignored) {
        }
    }
    
    private JButton crearBotonAyudaPia() {
        JButton b = new JButton("?");
        b.setFocusable(false);
        b.setToolTipText("Ayuda sobre PIA (Plan Individual de Aprendizaje)");
        b.setMargin(new Insets(2, 8, 2, 8));
        b.setPreferredSize(new Dimension(42, 26));
        return b;
    }

    private void mostrarAyudaPIA() {
        String texto =
                "PIA (Plan Individual de Aprendizaje)\n\n" +
                "¿Qué es?\n" +
                "Un plan con objetivos medibles para este estudiante.\n\n" +
                "Cómo usarlo (rápido)\n" +
                "1) Crear PIA\n" +
                "2) Escribir el objetivo general (resumen para el docente)\n" +
                "3) Agregar objetivos por juego (metas)\n\n" +
                "Campos principales\n" +
                "- Objetivo general: describe el propósito del plan.\n" +
                "- Objetivos medibles (por juego): cada objetivo define metas.\n" +
                "  • Juego (ID): el ID tal como aparece en la pestaña “Juegos”.\n" +
                "  • Meta rondas correctas: cuántas rondas correctas se buscan.\n" +
                "  • Meta sesiones: cuántas sesiones se requieren.\n\n" +
                "¿Qué significan las columnas?\n" +
                "- MetaRondas / ProgRondas: meta y progreso de rondas correctas.\n" +
                "- MetaSes / ProgSes: meta y progreso de sesiones completadas.\n" +
                "- Estado: ⏳ en progreso / ✅ completado.\n\n" +
                "¿Cómo se actualiza el progreso?\n" +
                "Cuando el estudiante juega, el sistema registra una sesión y suma al objetivo activo.\n\n" +
                "Nota TEA\n" +
                "El sistema mide errores e intentos para métricas y adaptación, sin mostrar castigos al estudiante.";

        JTextArea area = new JTextArea(texto, 16, 52);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(650, 420));

        JOptionPane.showMessageDialog(
                this,
                sp,
                "Ayuda: PIA",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ---------------------- Renderer: lista de niños ----------------------

    private class NinoCellRenderer implements ListCellRenderer<Nino> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Nino> list, Nino value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            JPanel p = new JPanel(new BorderLayout(10, 6));
            p.setBorder(new EmptyBorder(8, 8, 8, 8));

            String avatar = safe(getPropString(value, "getAvatar"));
            if (avatar.isBlank()) avatar = "🙂";

            JLabel av = new JLabel(avatar, SwingConstants.CENTER);
            av.setFont(EmojiFonts.emoji(22f));
            av.setPreferredSize(new Dimension(36, 36));

            JPanel txt = new JPanel();
            txt.setOpaque(false);
            txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));

            String nombre = safe(getPropString(value, "getNombre"));
            if (nombre.isBlank()) nombre = "(Sin nombre)";

            JLabel l1 = new JLabel(nombre);
            l1.setFont(l1.getFont().deriveFont(Font.BOLD, 13f));

            String aula = safe(getPropString(value, "getAula"));
            if (aula.isBlank()) aula = "Sin aula";

            Integer edad = getPropInt(value, "getEdad");
            String meta = aula + " · id=" + getIdSafe(value) + (edad != null ? (" · " + edad + " años") : "");

            JLabel l2 = new JLabel(meta);
            l2.setForeground(new Color(110, 110, 110));
            l2.setFont(l2.getFont().deriveFont(11f));

            txt.add(l1);
            txt.add(l2);

            p.add(av, BorderLayout.WEST);
            p.add(txt, BorderLayout.CENTER);

            if (isSelected) {
                p.setBackground(new Color(220, 235, 252));
            } else {
                // zebra
                p.setBackground((index % 2 == 0) ? new Color(250, 250, 250) : new Color(245, 245, 245));
            }

            return p;
        }
    }
}