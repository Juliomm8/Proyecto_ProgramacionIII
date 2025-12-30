package com.jasgames.ui;

import com.jasgames.model.Nino;
import com.jasgames.service.AppContext;
import com.jasgames.service.PerfilService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

public class AulasPanel extends JPanel {

    private final PerfilService perfilService;
    private final com.jasgames.service.AulaService aulaService;
    private final IntConsumer onAbrirPerfil;

    private final DefaultListModel<String> aulasModel = new DefaultListModel<>();
    private final JList<String> listAulas = new JList<>(aulasModel);

    private final DefaultTableModel tablaModel = new DefaultTableModel(
            new Object[]{"Avatar", "ID", "Nombre", "Aula", "Edad", "Puntos", "Diagnóstico", "Juegos"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tblNinos = new JTable(tablaModel);

    private final JTextField txtBuscar = new JTextField(16);
    private final JComboBox<String> cbOrden = new JComboBox<>(new String[]{
            "Puntos (mayor)",
            "Nombre (A-Z)",
            "ID (menor)",
            "Edad (menor)"
    });
    private final JButton btnRefrescar = new JButton("Refrescar");

    private final JLabel lblTituloAula = new JLabel("Aulas", SwingConstants.LEFT);
    private final JLabel lblResumen = new JLabel(" ");

    private List<Nino> cacheNinos = new ArrayList<>();
    private final Map<String, Integer> conteoPorAula = new HashMap<>();

    public AulasPanel(AppContext context, IntConsumer onAbrirPerfil) {
        this.perfilService = context.getPerfilService();
        this.aulaService = context.getAulaService();
        this.onAbrirPerfil = onAbrirPerfil;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(crearHeader(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearFooter(), BorderLayout.SOUTH);

        configurarListaAulas();
        configurarTabla();
        inicializarListeners();
        
        // Doble click en la tabla -> abrir perfiles
        activarDobleClickTabla();

        refrescarDatos();
    }
    
    public AulasPanel(AppContext context) {
        this(context, null);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 10));

        lblTituloAula.setFont(lblTituloAula.getFont().deriveFont(Font.BOLD, 18f));
        header.add(lblTituloAula, BorderLayout.WEST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controles.add(new JLabel("Buscar:"));
        controles.add(txtBuscar);
        controles.add(new JLabel("Orden:"));
        controles.add(cbOrden);
        controles.add(btnRefrescar);
        
        JButton btnNuevaAula = new JButton("Nueva aula");
        JButton btnColorAula = new JButton("Color");
        JButton btnEliminarAula = new JButton("Eliminar aula");
        
        btnNuevaAula.addActionListener(e -> crearAulaDialog());
        btnColorAula.addActionListener(e -> cambiarColorDialog());
        btnEliminarAula.addActionListener(e -> eliminarAulaDialog());
        
        controles.add(btnNuevaAula);
        controles.add(btnColorAula);
        controles.add(btnEliminarAula);

        header.add(controles, BorderLayout.EAST);
        return header;
    }

    private Component crearCentro() {
        // Izquierda: lista de aulas
        JPanel left = new JPanel(new BorderLayout(8, 8));
        JLabel lbl = new JLabel("Aulas", SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        left.add(lbl, BorderLayout.NORTH);

        JScrollPane scrollAulas = new JScrollPane(listAulas);
        scrollAulas.setPreferredSize(new Dimension(220, 0));
        left.add(scrollAulas, BorderLayout.CENTER);

        // Derecha: tabla
        JPanel right = new JPanel(new BorderLayout(8, 8));
        JScrollPane scrollTabla = new JScrollPane(tblNinos);
        right.add(scrollTabla, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.2);
        split.setDividerLocation(240);
        return split;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(lblResumen, BorderLayout.WEST);
        return footer;
    }

    private void configurarListaAulas() {
        listAulas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listAulas.setFont(listAulas.getFont().deriveFont(Font.BOLD, 14f));
        listAulas.setFixedCellHeight(44);

        listAulas.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                String aula = (value == null) ? "" : value.toString();
                int count = conteoPorAula.getOrDefault(aula, 0);

                lbl.setText(aula + "  (" + count + ")");
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);

                Color base = aulaService.colorDeAula(aula);
                Color suave = fondoSuave(base);

                if (isSelected) {
                    lbl.setBackground(base);
                    lbl.setForeground("Aula Amarilla".equalsIgnoreCase(aula) ? Color.BLACK : Color.WHITE);
                } else {
                    lbl.setBackground(suave);
                    lbl.setForeground(Color.BLACK);
                }

                return lbl;
            }
        });
    }

    private void configurarTabla() {
        tblNinos.setRowHeight(38);
        tblNinos.setFillsViewportHeight(true);

        // Renderer general para colorear filas por aula (pastel)
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String aula = String.valueOf(tablaModel.getValueAt(row, 3)); // columna "Aula"
                Color base = aulaService.colorDeAula(aula);

                if (!isSelected) {
                    c.setBackground(fondoSuave(base));
                    c.setForeground(Color.BLACK);
                }
                c.setBorder(noFocusBorder);
                return c;
            }
        };

        for (int i = 0; i < tblNinos.getColumnCount(); i++) {
            tblNinos.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Avatar grande y centrado (columna 0)
        tblNinos.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 22f));
                return lbl;
            }
        });

        // Ajustes de ancho
        tblNinos.getColumnModel().getColumn(0).setPreferredWidth(70);  // Avatar
        tblNinos.getColumnModel().getColumn(1).setPreferredWidth(60);  // ID
        tblNinos.getColumnModel().getColumn(4).setPreferredWidth(60);  // Edad
        tblNinos.getColumnModel().getColumn(5).setPreferredWidth(70);  // Puntos
        tblNinos.getColumnModel().getColumn(7).setPreferredWidth(60);  // Juegos
    }

    private void inicializarListeners() {
        btnRefrescar.addActionListener(e -> refrescarDatos());

        listAulas.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarTabla();
        });

        cbOrden.addActionListener(e -> actualizarTabla());

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarTabla(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarTabla(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarTabla(); }
        });
    }
    
    private void activarDobleClickTabla() {
        tblNinos.setToolTipText("Doble click para editar en Perfiles");

        tblNinos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = tblNinos.getSelectedRow();
                    if (row < 0) return;

                    Object idObj = tablaModel.getValueAt(row, 1); // Columna ID
                    Integer id = null;

                    if (idObj instanceof Integer) id = (Integer) idObj;
                    else if (idObj != null) {
                        try { id = Integer.parseInt(idObj.toString().trim()); } catch (Exception ignored) {}
                    }

                    if (id != null && onAbrirPerfil != null) {
                        onAbrirPerfil.accept(id);
                    }
                }
            }
        });
    }

    private void refrescarDatos() {
        cacheNinos = perfilService.obtenerTodosNinos();
        recargarAulas();
        actualizarTabla();
    }

    private void recargarAulas() {
        String seleccionActual = listAulas.getSelectedValue();

        // Conteos
        conteoPorAula.clear();
        for (Nino n : cacheNinos) {
            String aula = n.getAula();
            conteoPorAula.put(aula, conteoPorAula.getOrDefault(aula, 0) + 1);
        }

        // Modelo
        aulasModel.clear();
        List<String> nombres = aulaService.obtenerNombres();
        for (String a : nombres) aulasModel.addElement(a);

        // Selección: mantener si se puede; si no, la primera
        if (seleccionActual != null && nombres.contains(seleccionActual)) {
            listAulas.setSelectedValue(seleccionActual, true);
        } else if (!nombres.isEmpty()) {
            listAulas.setSelectedIndex(0);
        }
    }

    private void actualizarTabla() {
        tablaModel.setRowCount(0);

        String aulaSel = listAulas.getSelectedValue();
        if (aulaSel == null || aulaSel.isBlank()) {
            lblTituloAula.setText("Aulas");
            lblResumen.setText("Selecciona un aula para ver estudiantes.");
            return;
        }

        String q = (txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String orden = (String) cbOrden.getSelectedItem();
        if (orden == null) orden = "Puntos (mayor)";

        List<Nino> lista = new ArrayList<>();
        for (Nino n : cacheNinos) {
            if (!n.getAula().equalsIgnoreCase(aulaSel)) continue;

            if (!q.isBlank()) {
                String nombre = (n.getNombre() == null) ? "" : n.getNombre().toLowerCase(Locale.ROOT);
                String id = String.valueOf(n.getId());
                String diag = (n.getDiagnostico() == null) ? "" : n.getDiagnostico().toLowerCase(Locale.ROOT);
                if (!(nombre.contains(q) || id.contains(q) || diag.contains(q))) continue;
            }
            lista.add(n);
        }

        // Orden
        switch (orden) {
            case "Nombre (A-Z)" -> lista.sort(Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER));
            case "ID (menor)" -> lista.sort(Comparator.comparingInt(Nino::getId));
            case "Edad (menor)" -> lista.sort(Comparator.comparingInt(Nino::getEdad));
            default -> lista.sort(Comparator.comparingInt(Nino::getPuntosTotales).reversed());
        }

        // Tabla
        for (Nino n : lista) {
            tablaModel.addRow(new Object[]{
                    n.getAvatar(),
                    n.getId(),
                    n.getNombre(),
                    n.getAula(),
                    n.getEdad(),
                    n.getPuntosTotales(),
                    n.getDiagnostico(),
                    (n.getJuegosAsignados() == null) ? 0 : n.getJuegosAsignados().size()
            });
        }

        // Resumen + barra de color
        lblTituloAula.setText("Aula: " + aulaSel);
        aplicarBarraAula(aulaSel);

        int total = lista.size();
        int suma = 0;
        for (Nino n : lista) suma += n.getPuntosTotales();
        int prom = (total == 0) ? 0 : (suma / total);

        lblResumen.setText("Estudiantes: " + total + " | Puntos total: " + suma + " | Promedio: " + prom);
    }

    private void aplicarBarraAula(String aula) {
        Color c = aulaService.colorDeAula(aula);
        Border borde = BorderFactory.createMatteBorder(0, 0, 6, 0, c);
        setBorder(BorderFactory.createCompoundBorder(
                borde,
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        revalidate();
        repaint();
    }

    private Color fondoSuave(Color c) {
        int r = (c.getRed() + 255) / 2;
        int g = (c.getGreen() + 255) / 2;
        int b = (c.getBlue() + 255) / 2;
        return new Color(r, g, b);
    }
    
    private void crearAulaDialog() {
        JTextField txt = new JTextField(16);
        Color elegido = JColorChooser.showDialog(this, "Color del aula", new Color(52,152,219));
        if (elegido == null) return;

        JPanel p = new JPanel(new GridLayout(2,1,6,6));
        p.add(new JLabel("Nombre del aula:"));
        p.add(txt);

        int ok = JOptionPane.showConfirmDialog(this, p, "Nueva aula", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String nombre = txt.getText().trim();
        try {
            aulaService.crearAula(nombre, com.jasgames.service.AulaService.toHex(elegido));
            refrescarDatos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarColorDialog() {
        String aulaSel = listAulas.getSelectedValue();
        if (aulaSel == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Color actual = aulaService.colorDeAula(aulaSel);
        Color nuevo = JColorChooser.showDialog(this, "Nuevo color para " + aulaSel, actual);
        if (nuevo == null) return;

        try {
            aulaService.cambiarColor(aulaSel, com.jasgames.service.AulaService.toHex(nuevo));
            refrescarDatos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarAulaDialog() {
        String aulaSel = listAulas.getSelectedValue();
        if (aulaSel == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cant = perfilService.contarNinosEnAula(aulaSel);

        String destino = null;
        if (cant > 0) {
            List<String> opciones = new ArrayList<>(aulaService.obtenerNombres());
            opciones.removeIf(a -> a.equalsIgnoreCase(aulaSel));

            if (opciones.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Crea otra aula primero para migrar estudiantes.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            destino = (String) JOptionPane.showInputDialog(
                    this,
                    "Este aula tiene " + cant + " estudiantes.\n¿A qué aula los migramos?",
                    "Migrar y eliminar",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones.toArray(),
                    opciones.get(0)
            );
            if (destino == null) return;
        }

        int ok = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el aula \"" + aulaSel + "\"?" + (cant > 0 ? "\nLos estudiantes se moverán a: " + destino : ""),
                "Confirmar",
                JOptionPane.YES_NO_OPTION
        );

        if (ok != JOptionPane.YES_OPTION) return;

        try {
            aulaService.eliminarAula(aulaSel, destino, perfilService);
            refrescarDatos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
