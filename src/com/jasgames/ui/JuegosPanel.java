package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.Nino;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel de gestión de juegos para el modo docente.
 *
 * UI (rediseñada):
 * - Izquierda: catálogo del sistema (habilitado + dificultad global)
 * - Derecha: asignación por niño (solo juegos habilitados + dificultad individual)
 *
 * Nota: aunque existe un .form asociado, este panel construye su interfaz por código,
 * reusando nombres de campos para compatibilidad.
 */
public class JuegosPanel extends JPanel {

    // Campos esperados por el .form (algunos no se usan directamente)
    private JPanel panelJuegos;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JLabel lblTituloJuegos;
    private JPanel panelListaJuegos;
    private JButton btnGuardarEstadoJuegos;
    private JLabel lblSeleccionNino;
    private JComboBox cboNinos;
    private JLabel lblTituloAsignacion;
    private JScrollPane panelAsignacionJuegos;
    private JButton btnGuardarAsignacion;

    // Panel interno dentro del scroll de asignaciones
    private JPanel panelAsignacionContenido;

    // UI nueva
    private JTextField txtBuscarCatalogo;
    private JTextField txtBuscarAsignacion;
    private JCheckBox chkSoloAsignados;
    private JLabel lblResumenCatalogo;
    private JLabel lblResumenAsignacion;
    private JButton btnRefrescar;
    private JButton btnHabilitarTodos;
    private JButton btnDeshabilitarTodos;
    private JButton btnAsignarTodos;
    private JButton btnQuitarTodos;

    // Servicios compartidos
    private final JuegoService juegoService;
    private final PerfilService perfilService;

    // Mapas auxiliares: idJuego -> checkbox/spinner
    private final Map<Integer, JCheckBox> checkBoxesJuegos = new LinkedHashMap<>();
    private final Map<Integer, JCheckBox> checkBoxesAsignacion = new LinkedHashMap<>();
    private final Map<Integer, JSpinner> spinnersDificultad = new LinkedHashMap<>();
    private final Map<Integer, JSpinner> spinnersDificultadAsignacion = new LinkedHashMap<>();

    // Cache simple para render
    private List<Juego> cacheJuegos = new ArrayList<>();
    private List<Nino> cacheNinos = new ArrayList<>();

    // Mantener cambios del catálogo incluso si el usuario filtra con el buscador
    private final Map<Integer, Boolean> pendingHabilitado = new HashMap<>();
    private final Map<Integer, Integer> pendingDificultadGlobal = new HashMap<>();

    // Para que el resumen de asignación no “crezca” con cada repaint
    private String resumenAsignacionBase = " ";

    public JuegosPanel(JuegoService juegoService, PerfilService perfilService) {
        this.juegoService = juegoService;
        this.perfilService = perfilService;

        initUI();
        cargarJuegos();
        cargarNinos();
        initListeners();
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

        JLabel subtitle = new JLabel("Habilita juegos del sistema y asigna juegos/dificultad por estudiante.");
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

        // TOP en 2 filas (evita que el título se corte)
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        lblTituloJuegos = new JLabel("Catálogo del sistema");
        lblTituloJuegos.setFont(lblTituloJuegos.getFont().deriveFont(Font.BOLD, 15f));

        lblResumenCatalogo = new JLabel(" ");
        lblResumenCatalogo.setFont(lblResumenCatalogo.getFont().deriveFont(Font.PLAIN, 12f));
        lblResumenCatalogo.setForeground(new Color(90, 90, 90));

        titleBlock.add(lblTituloJuegos);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblResumenCatalogo);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        txtBuscarCatalogo = new JTextField(18);
        txtBuscarCatalogo.setToolTipText("Buscar por nombre o descripción");

        btnHabilitarTodos = new JButton("Habilitar todo");
        btnDeshabilitarTodos = new JButton("Deshabilitar todo");

        actions.add(new JLabel("Buscar:"));
        actions.add(txtBuscarCatalogo);
        actions.add(btnHabilitarTodos);
        actions.add(btnDeshabilitarTodos);

        top.add(titleBlock);
        top.add(Box.createVerticalStrut(8));
        top.add(actions);

        panelListaJuegos = new JPanel();
        panelListaJuegos.setOpaque(false);
        panelListaJuegos.setLayout(new BoxLayout(panelListaJuegos, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(panelListaJuegos);
        styleScroll(scroll);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 18)));

        btnGuardarEstadoJuegos = new JButton("Guardar cambios");
        bottom.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        bottom.add(btnGuardarEstadoJuegos, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    // B) Reemplaza buildAsignacionPanel() por esta versión
    private JPanel buildAsignacionPanel() {
        JPanel card = new CardPanel(new BorderLayout(10, 10));

        // TOP en 3 filas (más limpio y no se corta)
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        lblTituloAsignacion = new JLabel("Asignación por estudiante");
        lblTituloAsignacion.setFont(lblTituloAsignacion.getFont().deriveFont(Font.BOLD, 15f));

        lblResumenAsignacion = new JLabel(" ");
        lblResumenAsignacion.setFont(lblResumenAsignacion.getFont().deriveFont(Font.PLAIN, 12f));
        lblResumenAsignacion.setForeground(new Color(90, 90, 90));

        titleBlock.add(lblTituloAsignacion);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblResumenAsignacion);

        // Fila 1: estudiante
        JPanel rowStudent = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowStudent.setOpaque(false);

        lblSeleccionNino = new JLabel("Estudiante:");
        cboNinos = new JComboBox();
        cboNinos.setPreferredSize(new Dimension(260, cboNinos.getPreferredSize().height));
        cboNinos.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Nino) {
                    Nino n = (Nino) value;
                    setText(n.getNombre() + "  ·  " + n.getAula() + "  ·  id=" + n.getId());
                }
                return this;
            }
        });

        rowStudent.add(lblSeleccionNino);
        rowStudent.add(cboNinos);

        // Fila 2: filtros + acciones
        JPanel rowFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowFilters.setOpaque(false);

        txtBuscarAsignacion = new JTextField(16);
        txtBuscarAsignacion.setToolTipText("Filtra por juego");

        chkSoloAsignados = new JCheckBox("Solo asignados");
        chkSoloAsignados.setOpaque(false);

        btnAsignarTodos = new JButton("Asignar todo");
        btnQuitarTodos = new JButton("Quitar todo");

        rowFilters.add(new JLabel("Buscar:"));
        rowFilters.add(txtBuscarAsignacion);
        rowFilters.add(chkSoloAsignados);
        rowFilters.add(btnAsignarTodos);
        rowFilters.add(btnQuitarTodos);

        top.add(titleBlock);
        top.add(Box.createVerticalStrut(8));
        top.add(rowStudent);
        top.add(Box.createVerticalStrut(6));
        top.add(rowFilters);

        panelAsignacionContenido = new JPanel();
        panelAsignacionContenido.setOpaque(false);
        panelAsignacionContenido.setLayout(new BoxLayout(panelAsignacionContenido, BoxLayout.Y_AXIS));

        panelAsignacionJuegos = new JScrollPane(panelAsignacionContenido);
        styleScroll(panelAsignacionJuegos);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0, 0, 0, 18)));

        btnGuardarAsignacion = new JButton("Guardar asignación");
        bottom.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        bottom.add(btnGuardarAsignacion, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(panelAsignacionJuegos, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void styleScroll(JScrollPane sp) {
        sp.setBorder(new MatteBorder(1, 1, 1, 1, new Color(0, 0, 0, 18)));
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    // ---------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------

    private void cargarJuegos() {
        cacheJuegos = new ArrayList<>(juegoService.obtenerTodos());
        cacheJuegos.sort(Comparator.comparingInt(Juego::getId));

        // El refresco vuelve a estado real del servicio
        pendingHabilitado.clear();
        pendingDificultadGlobal.clear();

        renderCatalogo();
    }

    private void cargarNinos() {
        cboNinos.removeAllItems();

        cacheNinos = new ArrayList<>(perfilService.obtenerTodosNinos());
        cacheNinos.sort(Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER));
        for (Nino n : cacheNinos) cboNinos.addItem(n);

        if (cboNinos.getItemCount() > 0) {
            cboNinos.setSelectedIndex(0);
            actualizarAsignacionesParaSeleccionado();
        } else {
            panelAsignacionContenido.removeAll();
            panelAsignacionContenido.revalidate();
            panelAsignacionContenido.repaint();
        }
    }

    private void initListeners() {
        btnGuardarEstadoJuegos.addActionListener(e -> onGuardarEstadosJuegos());
        cboNinos.addActionListener(e -> actualizarAsignacionesParaSeleccionado());
        btnGuardarAsignacion.addActionListener(e -> onGuardarAsignacion());

        btnRefrescar.addActionListener(e -> {
            cargarJuegos();
            cargarNinos();
        });

        btnHabilitarTodos.addActionListener(e -> {
            for (Map.Entry<Integer, JCheckBox> e2 : checkBoxesJuegos.entrySet()) {
                e2.getValue().setSelected(true);
                pendingHabilitado.put(e2.getKey(), true);
            }
            renderCatalogoResumen();
            actualizarAsignacionesParaSeleccionado();
        });

        btnDeshabilitarTodos.addActionListener(e -> {
            for (Map.Entry<Integer, JCheckBox> e2 : checkBoxesJuegos.entrySet()) {
                e2.getValue().setSelected(false);
                pendingHabilitado.put(e2.getKey(), false);
            }
            renderCatalogoResumen();
            actualizarAsignacionesParaSeleccionado();
        });

        btnAsignarTodos.addActionListener(e -> {
            for (JCheckBox chk : checkBoxesAsignacion.values()) chk.setSelected(true);
            renderAsignacionResumen();
        });

        btnQuitarTodos.addActionListener(e -> {
            for (JCheckBox chk : checkBoxesAsignacion.values()) chk.setSelected(false);
            renderAsignacionResumen();
        });

        chkSoloAsignados.addActionListener(e -> actualizarAsignacionesParaSeleccionado());

        wireSearch(txtBuscarCatalogo, this::renderCatalogo);
        wireSearch(txtBuscarAsignacion, this::actualizarAsignacionesParaSeleccionado);
    }

    private void wireSearch(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    // ---------------------------------------------------------------------
    // Render: catálogo
    // ---------------------------------------------------------------------

    private void renderCatalogo() {
        panelListaJuegos.removeAll();
        checkBoxesJuegos.clear();
        spinnersDificultad.clear();

        String q = safeLower(txtBuscarCatalogo.getText());

        for (Juego juego : cacheJuegos) {
            if (!matchesJuego(juego, q)) continue;

            boolean sel = pendingHabilitado.containsKey(juego.getId())
                    ? pendingHabilitado.get(juego.getId())
                    : juego.isHabilitado();

            int dif = pendingDificultadGlobal.containsKey(juego.getId())
                    ? pendingDificultadGlobal.get(juego.getId())
                    : clamp1to5(juego.getDificultad());

            JComponent row = crearFilaCatalogo(juego, sel, dif);
            panelListaJuegos.add(row);
            panelListaJuegos.add(Box.createVerticalStrut(10));
        }

        renderCatalogoResumen();
        panelListaJuegos.revalidate();
        panelListaJuegos.repaint();

        actualizarAsignacionesParaSeleccionado();
    }

    private void renderCatalogoResumen() {
        int total = cacheJuegos.size();
        int habilitados = 0;
        for (Juego j : cacheJuegos) {
            if (isJuegoHabilitadoActual(j)) habilitados++;
        }
        lblResumenCatalogo.setText("Total: " + total + "  ·  Habilitados: " + habilitados);
    }

    private JComponent crearFilaCatalogo(Juego juego, boolean selected, int dificultad) {
        CardPanel row = new CardPanel(new BorderLayout(12, 8));
        row.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(juego.getNombre());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13.5f));

        JTextArea desc = new JTextArea(safe(juego.getDescripcion()));
        desc.setOpaque(false);
        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        desc.setForeground(new Color(85, 85, 85));

        JLabel badgeId = createBadge("#" + juego.getId());
        JLabel tipo = createBadge(safe(juego.getTipo() != null ? juego.getTipo().name() : "JUEGO"));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line1.setOpaque(false);
        line1.add(name);
        line1.add(badgeId);
        line1.add(tipo);

        left.add(line1);
        left.add(Box.createVerticalStrut(6));
        left.add(desc);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JCheckBox chk = new JCheckBox("Habilitado");
        chk.setOpaque(false);
        chk.setSelected(selected);

        JSpinner sp = new JSpinner(new SpinnerNumberModel(clamp1to5(dificultad), 1, 5, 1));
        sp.setPreferredSize(new Dimension(60, sp.getPreferredSize().height));
        sp.setEnabled(chk.isSelected());

        chk.addItemListener(e -> {
            sp.setEnabled(chk.isSelected());
            pendingHabilitado.put(juego.getId(), chk.isSelected());
            renderCatalogoResumen();
            actualizarAsignacionesParaSeleccionado();
        });

        sp.addChangeListener(e -> pendingDificultadGlobal.put(juego.getId(), (Integer) sp.getValue()));

        right.add(new JLabel("Dificultad:"));
        right.add(sp);
        right.add(chk);

        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        checkBoxesJuegos.put(juego.getId(), chk);
        spinnersDificultad.put(juego.getId(), sp);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JLabel createBadge(String text) {
        JLabel b = new JLabel(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11f));
        b.setOpaque(true);
        b.setBackground(new Color(255, 255, 255, 210));
        b.setForeground(new Color(60, 60, 60));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(0, 0, 0, 28)),
                new EmptyBorder(2, 8, 2, 8)
        ));
        return b;
    }

    // ---------------------------------------------------------------------
    // Guardar catálogo
    // ---------------------------------------------------------------------

    private void onGuardarEstadosJuegos() {
        List<Juego> juegos = juegoService.obtenerTodos();

        for (Juego juego : juegos) {
            JCheckBox chk = checkBoxesJuegos.get(juego.getId());
            if (chk != null) juego.setHabilitado(chk.isSelected());

            JSpinner sp = spinnersDificultad.get(juego.getId());
            if (sp != null) {
                int nueva = (Integer) sp.getValue();
                int anterior = juego.getDificultad();

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
                        sp.setValue(anterior);
                        continue;
                    }

                    juego.setDificultad(nueva);

                    if (resp == 0) {
                        perfilService.aplicarDificultadJuegoATodos(juego.getId(), nueva, false);
                    }
                }
            }
        }

        juegoService.guardar();
        cargarJuegos();
        JOptionPane.showMessageDialog(this, "Cambios guardados.");
    }

    // ---------------------------------------------------------------------
    // Render: asignación
    // ---------------------------------------------------------------------

    private void actualizarAsignacionesParaSeleccionado() {
        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) return;

        Map<Integer, Boolean> prevSel = new HashMap<>();
        Map<Integer, Integer> prevDif = new HashMap<>();
        for (Map.Entry<Integer, JCheckBox> e : checkBoxesAsignacion.entrySet()) prevSel.put(e.getKey(), e.getValue().isSelected());
        for (Map.Entry<Integer, JSpinner> e : spinnersDificultadAsignacion.entrySet()) prevDif.put(e.getKey(), (Integer) e.getValue().getValue());

        panelAsignacionContenido.removeAll();
        checkBoxesAsignacion.clear();
        spinnersDificultadAsignacion.clear();

        Set<Integer> asignados = (seleccionado.getJuegosAsignados() != null)
                ? seleccionado.getJuegosAsignados()
                : Collections.emptySet();

        String q = safeLower(txtBuscarAsignacion.getText());
        boolean soloAsignados = chkSoloAsignados.isSelected();

        int totalHabilitados = 0;
        int totalAsignados = 0;

        for (Juego juego : cacheJuegos) {
            if (!isJuegoHabilitadoActual(juego)) continue;
            totalHabilitados++;

            int idJuego = juego.getId();
            boolean estaAsignado = asignados.contains(idJuego);
            if (estaAsignado) totalAsignados++;

            if (soloAsignados && !estaAsignado) continue;
            if (!matchesJuego(juego, q)) continue;

            boolean sel = prevSel.containsKey(idJuego) ? prevSel.get(idJuego) : estaAsignado;
            int difGlobal = getDificultadGlobalActual(juego);
            int difInicial = prevDif.containsKey(idJuego)
                    ? prevDif.get(idJuego)
                    : seleccionado.getDificultadJuego(idJuego, difGlobal);

            JComponent row = crearFilaAsignacion(juego, sel, clamp1to5(difInicial), difGlobal);
            panelAsignacionContenido.add(row);
            panelAsignacionContenido.add(Box.createVerticalStrut(10));
        }

        resumenAsignacionBase = "Habilitados: " + totalHabilitados + "  ·  Asignados: " + totalAsignados;
        lblResumenAsignacion.setText(resumenAsignacionBase);

        panelAsignacionContenido.revalidate();
        panelAsignacionContenido.repaint();
        renderAsignacionResumen();
    }

    private void renderAsignacionResumen() {
        int asignados = 0;
        for (JCheckBox chk : checkBoxesAsignacion.values()) if (chk.isSelected()) asignados++;
        lblResumenAsignacion.setText(resumenAsignacionBase + "  ·  Seleccionados: " + asignados);
    }

    private JComponent crearFilaAsignacion(Juego juego, boolean selected, int dificultad, int difGlobal) {
        CardPanel row = new CardPanel(new BorderLayout(12, 8));
        row.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(juego.getNombre() + "  " + "#" + juego.getId());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13.5f));

        JTextArea desc = new JTextArea(safe(juego.getDescripcion()));
        desc.setOpaque(false);
        desc.setEditable(false);
        desc.setFocusable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        desc.setForeground(new Color(85, 85, 85));

        left.add(name);
        left.add(Box.createVerticalStrut(6));
        left.add(desc);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JCheckBox chk = new JCheckBox("Asignado");
        chk.setOpaque(false);
        chk.setSelected(selected);

        JSpinner sp = new JSpinner(new SpinnerNumberModel(clamp1to5(dificultad), 1, 5, 1));
        sp.setPreferredSize(new Dimension(60, sp.getPreferredSize().height));
        sp.setEnabled(chk.isSelected());

        JLabel lblModo = createBadge((dificultad == difGlobal) ? "Global" : "Personal");

        chk.addItemListener(e -> {
            sp.setEnabled(chk.isSelected());
            renderAsignacionResumen();
        });

        sp.addChangeListener(e -> {
            int v = (Integer) sp.getValue();
            lblModo.setText(v == difGlobal ? "Global" : "Personal");
        });

        right.add(new JLabel("Dificultad:"));
        right.add(sp);
        right.add(lblModo);
        right.add(chk);

        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        checkBoxesAsignacion.put(juego.getId(), chk);
        spinnersDificultadAsignacion.put(juego.getId(), sp);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // ---------------------------------------------------------------------
    // Guardar asignación
    // ---------------------------------------------------------------------

    private void onGuardarAsignacion() {
        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un niño primero.");
            return;
        }

        Set<Integer> juegosAsignados = new HashSet<>();
        Map<Integer, Integer> dificultadPorJuego = new HashMap<>();

        for (Juego juego : cacheJuegos) {
            if (!isJuegoHabilitadoActual(juego)) continue;
            int idJuego = juego.getId();
            JCheckBox chk = checkBoxesAsignacion.get(idJuego);
            if (chk != null && chk.isSelected()) {
                juegosAsignados.add(idJuego);

                JSpinner sp = spinnersDificultadAsignacion.get(idJuego);
                if (sp != null) {
                    int valor = (Integer) sp.getValue();
                    int difGlobal = getDificultadGlobalActual(juego);

                    if (valor != difGlobal) {
                        dificultadPorJuego.put(idJuego, valor);
                    }
                }
            }
        }

        perfilService.asignarJuegosConDificultad(seleccionado.getId(), juegosAsignados, dificultadPorJuego);
        perfilService.guardarCambios();

        JOptionPane.showMessageDialog(this, "Asignación guardada para: " + seleccionado.getNombre());
        cargarNinos();
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

    private static boolean matchesJuego(Juego j, String qLower) {
        if (qLower == null || qLower.isBlank()) return true;
        String hay = (safe(j.getNombre()) + " " + safe(j.getDescripcion()) + " " + safe(j.getTipo() != null ? j.getTipo().name() : ""))
                .toLowerCase(Locale.ROOT);
        return hay.contains(qLower);
    }

    private static int clamp1to5(int v) {
        if (v < 1) return 1;
        if (v > 5) return 5;
        return v;
    }

    private boolean isJuegoHabilitadoActual(Juego juego) {
        JCheckBox chk = checkBoxesJuegos.get(juego.getId());
        if (chk != null) return chk.isSelected();
        if (pendingHabilitado.containsKey(juego.getId())) return pendingHabilitado.get(juego.getId());
        return juego.isHabilitado();
    }

    private int getDificultadGlobalActual(Juego juego) {
        JSpinner sp = spinnersDificultad.get(juego.getId());
        if (sp != null) return clamp1to5((Integer) sp.getValue());
        if (pendingDificultadGlobal.containsKey(juego.getId())) return clamp1to5(pendingDificultadGlobal.get(juego.getId()));
        return clamp1to5(juego.getDificultad());
    }

    // ---------------------------------------------------------------------
    // UI components
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

                Color fill = blend(base, Color.WHITE, 0.65f);
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
}
