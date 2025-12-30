package com.jasgames.ui;

import com.jasgames.service.AuditoriaService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AuditoriaPanel extends JPanel {

    private final AuditoriaService auditoriaService;

    private final JTextArea txtLog = new JTextArea();
    private final JComboBox<Integer> cbCantidad = new JComboBox<>(new Integer[]{50, 100, 200, 500});
    private final JLabel lblEstado = new JLabel(" ");
    
    private final JComboBox<String> cbTipo = new JComboBox<>(new String[]{"TODOS"});
    private final JTextField txtBuscar = new JTextField(18);
    private final JLabel lblConteo = new JLabel(" ");

    private List<String> lineasOriginales = new ArrayList<>();

    public AuditoriaPanel(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(crearHeader(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearFooter(), BorderLayout.SOUTH);

        cbCantidad.setSelectedItem(200);
        
        cbTipo.addActionListener(e -> aplicarFiltrosYMostrar());

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltrosYMostrar(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltrosYMostrar(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltrosYMostrar(); }
        });
        
        cargar();
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("Auditoría de accesos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        header.add(titulo, BorderLayout.WEST);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        controles.add(new JLabel("Ver últimas:"));
        controles.add(cbCantidad);

        controles.add(new JLabel("Tipo:"));
        controles.add(cbTipo);

        controles.add(new JLabel("Buscar:"));
        controles.add(txtBuscar);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargar());
        controles.add(btnRefrescar);

        header.add(controles, BorderLayout.EAST);
        return header;
    }

    private JScrollPane crearCentro() {
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Registro (data/auditoria.log)"));
        return scroll;
    }

    private JPanel crearFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        
        JPanel izq = new JPanel(new GridLayout(2, 1));
        izq.add(lblEstado);
        izq.add(lblConteo);
        footer.add(izq, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCopiar = new JButton("Copiar");
        btnCopiar.addActionListener(e -> copiarAlPortapapeles());

        JButton btnLimpiarVista = new JButton("Limpiar vista");
        btnLimpiarVista.addActionListener(e -> txtLog.setText(""));

        acciones.add(btnLimpiarVista);
        acciones.add(btnCopiar);

        footer.add(acciones, BorderLayout.EAST);
        return footer;
    }

    private void cargar() {
        Integer cant = (Integer) cbCantidad.getSelectedItem();
        if (cant == null) cant = 200;

        String contenido = auditoriaService.leerUltimasLineas(cant);

        // Convertir a lista (línea por línea)
        List<String> lines = new ArrayList<>();
        if (contenido != null && !contenido.isBlank()) {
            String[] arr = contenido.split("\\R"); // cualquier salto de línea
            for (String s : arr) {
                if (!s.trim().isBlank()) lines.add(s);
            }
        }

        lineasOriginales = lines;

        actualizarTiposDesdeLineas(lineasOriginales);
        aplicarFiltrosYMostrar();

        lblEstado.setText("Cargado OK");
    }
    
    private String extraerTipo(String linea) {
        if (linea == null) return "";
        String[] parts = linea.split("\\s\\|\\s", 3); // " | " (3 partes máximo)
        return (parts.length >= 2) ? parts[1].trim() : "";
    }

    private void actualizarTiposDesdeLineas(List<String> lineas) {
        String seleccionado = (String) cbTipo.getSelectedItem();
        if (seleccionado == null) seleccionado = "TODOS";

        Set<String> tipos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String l : lineas) {
            String t = extraerTipo(l);
            if (!t.isBlank()) tipos.add(t);
        }

        cbTipo.removeAllItems();
        cbTipo.addItem("TODOS");
        for (String t : tipos) cbTipo.addItem(t);

        // intenta mantener selección
        for (int i = 0; i < cbTipo.getItemCount(); i++) {
            if (cbTipo.getItemAt(i).equalsIgnoreCase(seleccionado)) {
                cbTipo.setSelectedIndex(i);
                return;
            }
        }
        cbTipo.setSelectedItem("TODOS");
    }

    private void aplicarFiltrosYMostrar() {
        String tipoSel = (String) cbTipo.getSelectedItem();
        if (tipoSel == null) tipoSel = "TODOS";

        String buscar = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);

        List<String> filtradas = new ArrayList<>();
        for (String l : lineasOriginales) {
            String tipo = extraerTipo(l);

            boolean okTipo = tipoSel.equalsIgnoreCase("TODOS") || tipo.equalsIgnoreCase(tipoSel);
            boolean okBuscar = buscar.isBlank() || l.toLowerCase(Locale.ROOT).contains(buscar);

            if (okTipo && okBuscar) filtradas.add(l);
        }

        txtLog.setText(String.join(System.lineSeparator(), filtradas));
        txtLog.setCaretPosition(txtLog.getDocument().getLength());

        lblConteo.setText("Mostrando " + filtradas.size() + " de " + lineasOriginales.size() + " líneas");
    }

    private void copiarAlPortapapeles() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(txtLog.getText()), null);
            lblEstado.setText("Copiado al portapapeles ✅");
        } catch (Exception ex) {
            lblEstado.setText("No se pudo copiar: " + ex.getMessage());
        }
    }
}
