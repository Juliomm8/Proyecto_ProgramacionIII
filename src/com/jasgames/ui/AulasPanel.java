package com.jasgames.ui;

import com.jasgames.model.Nino;
import com.jasgames.service.AppContext;
import com.jasgames.service.PerfilService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Consumer;

public class AulasPanel extends JPanel {

    private final PerfilService perfilService;
    private final com.jasgames.service.AulaService aulaService;
    private final IntConsumer onAbrirPerfil;

    // Barra de estado global (DocenteWindow). Si no existe, no hace nada.
    private final Consumer<String> statusSink;

    private final DefaultListModel<String> aulasModel = new DefaultListModel<>();
    private final JList<String> listAulas = new JList<>(aulasModel);

    private final DefaultTableModel tablaModel = new DefaultTableModel(
            new Object[]{"Avatar", "ID", "Nombre", "Aula", "Edad", "Puntos", "Diagnóstico", "Juegos"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tblNinos = new JTable(tablaModel);
    private TableRowSorter<DefaultTableModel> sorterTabla;

    private final JTextField txtBuscar = new JTextField(16);
    private final JComboBox<String> cbOrden = new JComboBox<>(new String[]{
            "Puntos (mayor)",
            "Nombre (A-Z)",
            "ID (menor)",
            "Edad (menor)"
    });
    private final JButton btnRefrescar = new JButton("Refrescar");
    
    // Botones de acción sobre la selección
    private JButton btnMoverSeleccion;
    private JButton btnCopiarIds;

    private final JLabel lblTituloAula = new JLabel("Aulas", SwingConstants.LEFT);
    private final JLabel lblResumen = new JLabel(" ");

    private List<Nino> cacheNinos = new ArrayList<>();
    private final Map<String, Integer> conteoPorAula = new HashMap<>();

    public AulasPanel(AppContext context, IntConsumer onAbrirPerfil, Consumer<String> statusSink) {
        this.perfilService = context.getPerfilService();
        this.aulaService = context.getAulaService();
        this.onAbrirPerfil = onAbrirPerfil;
        this.statusSink = (statusSink != null) ? statusSink : (m) -> {};

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
        instalarMenuContextualTabla();
        instalarMenuContextualListaAulas();

        refrescarDatos();
    }
    
    public AulasPanel(AppContext context, IntConsumer onAbrirPerfil) {
        this(context, onAbrirPerfil, null);
    }

    public AulasPanel(AppContext context) {
        this(context, null, null);
    }

    private void status(String msg) {
        try {
            if (statusSink != null) statusSink.accept(msg);
        } catch (Exception ignored) {
        }
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 10));

        lblTituloAula.setFont(lblTituloAula.getFont().deriveFont(Font.BOLD, 18f));
        header.add(lblTituloAula, BorderLayout.WEST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controles.add(new JLabel("Buscar:"));
        
        txtBuscar.setToolTipText("Buscar por ID, nombre, diagnóstico o aula. Ej: 'aula:azul id:23 tea'");
        controles.add(txtBuscar);
        controles.add(new JLabel("Orden:"));
        controles.add(cbOrden);
        controles.add(btnRefrescar);
        
        JButton btnNuevaAula = new JButton("Nueva aula");
        JButton btnColorAula = new JButton("Color");
        JButton btnEliminarAula = new JButton("Eliminar aula");
        
        // Botones nuevos para selección múltiple
        btnMoverSeleccion = new JButton("Mover selección...");
        btnCopiarIds = new JButton("Copiar IDs");
        
        // Inicialmente deshabilitados hasta que se seleccione algo
        btnMoverSeleccion.setEnabled(false);
        btnCopiarIds.setEnabled(false);
        
        btnNuevaAula.addActionListener(e -> crearAulaDialog());
        btnColorAula.addActionListener(e -> cambiarColorDialog());
        btnEliminarAula.addActionListener(e -> eliminarAulaDialog());
        
        controles.add(btnNuevaAula);
        controles.add(btnColorAula);
        controles.add(btnEliminarAula);
        
        // Agregamos los nuevos botones al panel
        controles.add(btnMoverSeleccion);
        controles.add(btnCopiarIds);

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
                
                int count;
                String label;

                if ("Todas".equalsIgnoreCase(aula)) {
                    count = (cacheNinos == null) ? 0 : cacheNinos.size();
                    label = "Todas las aulas";
                } else {
                    count = conteoPorAula.getOrDefault(aula, 0);
                    label = aula;
                }

                lbl.setText(label + "  (" + count + ")");
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);

                Color base = aulaService.colorDeAula(aula);
                Color suave = fondoSuave(base);

                if (isSelected) {
                    lbl.setBackground(base);
                    lbl.setForeground(textoContraste(base));
                } else {
                    lbl.setBackground(suave);
                    lbl.setForeground(textoContraste(suave));
                }

                return lbl;
            }
        });
    }

    private void configurarTabla() {
        tblNinos.setRowHeight(38);
        tblNinos.setFillsViewportHeight(true);
        
        tblNinos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblNinos.setRowSelectionAllowed(true);

        // Configurar Sorter
        sorterTabla = new TableRowSorter<>(tablaModel);
        sorterTabla.setSortsOnUpdates(true);
        tblNinos.setRowSorter(sorterTabla);
        tblNinos.getTableHeader().setReorderingAllowed(false);

        // Comparadores numéricos para columnas: 1 (ID), 4 (Edad), 5 (Puntos), 7 (Juegos)
        Comparator<Object> intComparator = (a, b) -> Integer.compare(parseIntSafe(a), parseIntSafe(b));
        if (tblNinos.getColumnCount() > 1) sorterTabla.setComparator(1, intComparator);
        if (tblNinos.getColumnCount() > 4) sorterTabla.setComparator(4, intComparator);
        if (tblNinos.getColumnCount() > 5) sorterTabla.setComparator(5, intComparator);
        if (tblNinos.getColumnCount() > 7) sorterTabla.setComparator(7, intComparator);

        // Renderer general para colorear filas por aula (pastel)
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (c instanceof JLabel lbl) {
                    String text = (value == null) ? "" : String.valueOf(value);
                    lbl.setToolTipText(text.isBlank() ? null : text);
                }

                // Zebra + selección (igual que ya tenías)
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(new Color(245, 245, 245));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }

                // Fondo por aula (tu lógica)
                String aulaSeleccionada = listAulas.getSelectedValue();
                Color base = aulaService.colorDeAula(aulaSeleccionada);

                if (base != null) {
                    Color suave = fondoSuave(base);

                    if (!isSelected) {
                        c.setBackground(suave);
                        c.setForeground(textoContraste(suave));
                    } else {
                        c.setBackground(base);
                        c.setForeground(textoContraste(base));
                    }
                }
                
                if (c instanceof JComponent jc) {
                    jc.setBorder(noFocusBorder);
                }
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
        
        // Listener de selección para habilitar/deshabilitar botones
        tblNinos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean has = tblNinos.getSelectedRowCount() > 0;
            if (btnMoverSeleccion != null) btnMoverSeleccion.setEnabled(has);
            if (btnCopiarIds != null) btnCopiarIds.setEnabled(has);
        });
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
        
        // Listeners de los nuevos botones
        btnMoverSeleccion.addActionListener(e -> moverSeleccionadoAOtraAula());
        btnCopiarIds.addActionListener(e -> copiarIdsSeleccionados());
    }
    
    private void activarDobleClickTabla() {
        tblNinos.setToolTipText("Doble click para editar en Perfiles");

        tblNinos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = tblNinos.getSelectedRow();
                    if (viewRow < 0) return;

                    int modelRow = tblNinos.convertRowIndexToModel(viewRow);

                    Object idObj = tblNinos.getModel().getValueAt(modelRow, 1); // Columna ID
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

    private void instalarMenuContextualTabla() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miAbrir = new JMenuItem("Abrir en Perfiles");
        JMenuItem miCopiarId = new JMenuItem("Copiar ID");
        JMenuItem miMover = new JMenuItem("Mover selección a otra aula...");

        miAbrir.addActionListener(ev -> abrirPerfilSeleccionado());
        miCopiarId.addActionListener(ev -> copiarIdSeleccionado());
        miMover.addActionListener(ev -> moverSeleccionadoAOtraAula());

        menu.add(miAbrir);
        menu.add(miCopiarId);
        menu.addSeparator();
        menu.add(miMover);

        tblNinos.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                int r = tblNinos.rowAtPoint(e.getPoint());
                if (r >= 0 && r < tblNinos.getRowCount()) {
                    // Si ya está seleccionada esa fila, mantenemos selección múltiple
                    if (!tblNinos.isRowSelected(r)) {
                        tblNinos.setRowSelectionInterval(r, r);
                    }
                } else {
                    tblNinos.clearSelection();
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });
    }
    
    private void instalarMenuContextualListaAulas() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miRenombrar = new JMenuItem("Renombrar aula...");
        JMenuItem miCopiar = new JMenuItem("Copiar nombre");
        JMenuItem miColor = new JMenuItem("Cambiar color...");

        miRenombrar.addActionListener(e -> renombrarAulaDialog());
        miCopiar.addActionListener(e -> copiarNombreAula());
        miColor.addActionListener(e -> cambiarColorDialog());

        menu.add(miRenombrar);
        menu.add(miCopiar);
        menu.addSeparator();
        menu.add(miColor);

        listAulas.addMouseListener(new MouseAdapter() {
            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                int idx = listAulas.locationToIndex(e.getPoint());
                if (idx >= 0) listAulas.setSelectedIndex(idx);

                String sel = listAulas.getSelectedValue();
                boolean has = sel != null && !sel.isBlank();
                boolean isTodas = has && "Todas".equalsIgnoreCase(sel);
                boolean isAzul = has && "Aula Azul".equalsIgnoreCase(sel);

                miRenombrar.setEnabled(has && !isTodas && !isAzul);
                miCopiar.setEnabled(has && !isTodas);
                miColor.setEnabled(has && !isTodas);

                menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
        });
    }

    private void copiarNombreAula() {
        String aulaSel = listAulas.getSelectedValue();
        if (aulaSel == null || aulaSel.isBlank() || "Todas".equalsIgnoreCase(aulaSel)) return;

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(aulaSel), null);

        status("Copiado: " + aulaSel);
    }

    private void renombrarAulaDialog() {
        String aulaSel = listAulas.getSelectedValue();
        if (aulaSel == null || aulaSel.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Todas".equalsIgnoreCase(aulaSel)) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula específica (no 'Todas').", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Aula Azul".equalsIgnoreCase(aulaSel)) {
            JOptionPane.showMessageDialog(this, "No se puede renombrar 'Aula Azul' (aula por defecto del sistema).", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nuevo = JOptionPane.showInputDialog(this, "Nuevo nombre para \"" + aulaSel + "\":", aulaSel);
        if (nuevo == null) return;
        nuevo = nuevo.trim();
        if (nuevo.isBlank()) return;
        if (nuevo.equalsIgnoreCase(aulaSel)) return;

        // evitar duplicados
        for (String a : aulaService.obtenerNombres()) {
            if (a != null && a.equalsIgnoreCase(nuevo)) {
                JOptionPane.showMessageDialog(this, "Esa aula ya existe.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            Color c = aulaService.colorDeAula(aulaSel);
            String hex = com.jasgames.service.AulaService.toHex(c);

            // 1) crear la nueva con el mismo color
            aulaService.crearAula(nuevo, hex);

            // 2) migrar estudiantes del nombre viejo al nuevo
            perfilService.migrarAula(aulaSel, nuevo);

            // 3) eliminar el aula vieja (ya sin estudiantes)
            aulaService.eliminarAula(aulaSel, nuevo, perfilService);

            refrescarDatos();
            listAulas.setSelectedValue(nuevo, true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Integer> getIdsSeleccionadosTabla() {
        int[] viewRows = tblNinos.getSelectedRows();
        List<Integer> ids = new ArrayList<>();
        if (viewRows == null || viewRows.length == 0) return ids;

        for (int viewRow : viewRows) {
            int modelRow = tblNinos.convertRowIndexToModel(viewRow);
            Object idObj = tblNinos.getModel().getValueAt(modelRow, 1); // Columna ID

            Integer id = null;
            if (idObj instanceof Integer) id = (Integer) idObj;
            else if (idObj != null) {
                try { id = Integer.parseInt(idObj.toString().trim()); } catch (Exception ignored) {}
            }
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private Integer getIdSeleccionadoTabla() {
        int viewRow = tblNinos.getSelectedRow();
        if (viewRow < 0) return null;

        int modelRow = tblNinos.convertRowIndexToModel(viewRow);
        Object idObj = tblNinos.getModel().getValueAt(modelRow, 1); // Columna ID

        if (idObj instanceof Integer) return (Integer) idObj;
        if (idObj == null) return null;

        try { return Integer.parseInt(idObj.toString().trim()); }
        catch (Exception ignored) { return null; }
    }

    private void abrirPerfilSeleccionado() {
        Integer id = getIdSeleccionadoTabla();
        if (id == null) return;

        if (onAbrirPerfil != null) onAbrirPerfil.accept(id);
    }

    private void copiarIdSeleccionado() {
        Integer id = getIdSeleccionadoTabla();
        if (id == null) return;

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(String.valueOf(id)), null);

        status("ID copiado: " + id);
    }
    
    private void copiarIdsSeleccionados() {
        java.util.List<Integer> ids = getIdsSeleccionadosTabla();
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más estudiantes en la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // formato: 12, 15, 18
        String txt = ids.stream()
                .sorted()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(txt), null);

        status("IDs copiados: " + txt);
    }

    private void moverSeleccionadoAOtraAula() {
        List<Integer> ids = getIdsSeleccionadosTabla();
        if (ids.isEmpty()) return;

        List<String> aulas = aulaService.obtenerNombres();
        aulas.removeIf(a -> a == null || a.isBlank());

        if (aulas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay aulas disponibles.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> cb = new JComboBox<>(aulas.toArray(new String[0]));
        cb.setSelectedIndex(0);

        String msg = (ids.size() == 1)
                ? "Mover el estudiante seleccionado a:"
                : "Mover " + ids.size() + " estudiantes seleccionados a:";

        int r = JOptionPane.showConfirmDialog(
                this,
                cb,
                msg,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (r != JOptionPane.OK_OPTION) return;

        String destino = (String) cb.getSelectedItem();
        if (destino == null || destino.isBlank()) return;

        int movidos = 0;
        for (Integer id : ids) {
            Nino n = perfilService.buscarNinoPorId(id);
            if (n == null) continue;

            if (!destino.equalsIgnoreCase(n.getAula())) {
                n.setAula(destino);
                perfilService.actualizarNino(n);
                movidos++;
            }
        }

        refrescarDatos();
        listAulas.setSelectedValue(destino, true);

        JOptionPane.showMessageDialog(this,
                "Movidos: " + movidos + " estudiante(s) a \"" + destino + "\".",
                "OK",
                JOptionPane.INFORMATION_MESSAGE);
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
        aulasModel.addElement("Todas"); // ✅ nueva opción
        
        List<String> nombres = aulaService.obtenerNombres();
        for (String a : nombres) aulasModel.addElement(a);

        // Selección: mantener si se puede; si no, "Todas"
        if (seleccionActual != null) {
            boolean found = false;
            for (int i = 0; i < aulasModel.size(); i++) {
                String v = aulasModel.get(i);
                if (v != null && v.equalsIgnoreCase(seleccionActual)) {
                    listAulas.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found && !aulasModel.isEmpty()) listAulas.setSelectedIndex(0); // "Todas"
        } else {
            if (!aulasModel.isEmpty()) listAulas.setSelectedIndex(0); // "Todas"
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
        
        boolean esTodas = "Todas".equalsIgnoreCase(aulaSel);

        String q = (txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        String[] tokens = q.isBlank() ? new String[0] : q.split("\\s+");
        
        // Filtramos la lista (pero NO ordenamos manualmente, dejamos que el Sorter lo haga)
        List<Nino> lista = new ArrayList<>();
        for (Nino n : cacheNinos) {
            if (!esTodas && !n.getAula().equalsIgnoreCase(aulaSel)) continue;

            if (tokens.length > 0) {
                if (!matchTokensRec(n, tokens, 0)) continue;
            }
            lista.add(n);
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

        // Aplicar orden del combo al Sorter
        aplicarOrdenComboATabla();

        // Resumen + barra de color
        lblTituloAula.setText(esTodas ? "Todas las aulas" : "Aula: " + aulaSel);
        aplicarBarraAula(esTodas ? "Todas" : aulaSel);

        int total = lista.size();
        int suma = 0;
        for (Nino n : lista) suma += n.getPuntosTotales();
        int prom = (total == 0) ? 0 : (suma / total);

        lblResumen.setText("Estudiantes: " + total + " | Puntos total: " + suma + " | Promedio: " + prom);
    }

    private boolean matchTokensRec(Nino n, String[] tokens, int idx) {
        if (idx >= tokens.length) return true;

        String t = tokens[idx];
        if (t == null || t.isBlank()) return matchTokensRec(n, tokens, idx + 1);

        boolean ok = matchOneToken(n, t);
        return ok && matchTokensRec(n, tokens, idx + 1);
    }

    private boolean matchOneToken(Nino n, String t) {
        String id = String.valueOf(n.getId()).toLowerCase(Locale.ROOT);
        String nombre = (n.getNombre() == null) ? "" : n.getNombre().toLowerCase(Locale.ROOT);
        String diag = (n.getDiagnostico() == null) ? "" : n.getDiagnostico().toLowerCase(Locale.ROOT);
        String aula = (n.getAula() == null) ? "" : n.getAula().toLowerCase(Locale.ROOT);

        // Tokens por campo: id:23 aula:azul diag:tea nom:juan
        if (t.startsWith("id:")) {
            String v = t.substring(3).trim();
            return !v.isBlank() && id.contains(v);
        }
        if (t.startsWith("aula:")) {
            String v = t.substring(5).trim();
            return !v.isBlank() && aula.contains(v);
        }
        if (t.startsWith("diag:")) {
            String v = t.substring(5).trim();
            return !v.isBlank() && diag.contains(v);
        }
        if (t.startsWith("nom:") || t.startsWith("nombre:")) {
            String v = t.contains(":") ? t.substring(t.indexOf(':') + 1).trim() : "";
            return !v.isBlank() && nombre.contains(v);
        }

        // Token normal: busca en todo
        return id.contains(t) || nombre.contains(t) || diag.contains(t) || aula.contains(t);
    }

    private void aplicarOrdenComboATabla() {
        if (sorterTabla == null) return;

        String opt = String.valueOf(cbOrden.getSelectedItem());
        List<RowSorter.SortKey> keys = new ArrayList<>();

        // Columnas: 1=ID, 2=Nombre, 4=Edad, 5=Puntos
        switch (opt) {
            case "Puntos (mayor)":
                keys.add(new RowSorter.SortKey(5, SortOrder.DESCENDING));
                break;
            case "Nombre (A-Z)":
                keys.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
                break;
            case "ID (menor)":
                keys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
                break;
            case "Edad (menor)":
                keys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
                break;
        }

        sorterTabla.setSortKeys(keys);
        sorterTabla.sort();
    }

    private void aplicarBarraAula(String aula) {
        Color c;

        if (aula == null || aula.isBlank() || "Todas".equalsIgnoreCase(aula)) {
            c = new Color(180, 180, 180); // ✅ neutral
        } else {
            c = aulaService.colorDeAula(aula);
            if (c == null) c = new Color(180, 180, 180);
        }

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

    private Color textoContraste(Color bg) {
        if (bg == null) return Color.BLACK;

        // Luminancia aproximada (0..255)
        double y = 0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue();

        // Umbral: si es claro -> negro, si es oscuro -> blanco
        return (y >= 150) ? Color.BLACK : Color.WHITE;
    }

    private int parseIntSafe(Object v) {
        try { return Integer.parseInt(String.valueOf(v).trim()); }
        catch (Exception e) { return 0; }
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
        
        if ("Todas".equalsIgnoreCase(aulaSel)) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula específica (no 'Todas').", "Aviso", JOptionPane.WARNING_MESSAGE);
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
        
        if ("Todas".equalsIgnoreCase(aulaSel)) {
            JOptionPane.showMessageDialog(this, "Selecciona un aula específica (no 'Todas').", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cant = perfilService.contarNinosEnAula(aulaSel);

        String destino = null;
        if (cant > 0) {
            List<String> opciones = new ArrayList<>(aulaService.obtenerNombres());
            opciones.removeIf(a -> a.equalsIgnoreCase(aulaSel));

            if (opciones.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No puedes eliminar esta aula porque tiene estudiantes.\n" +
                                "Crea otra aula para migrar estudiantes.",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );

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
