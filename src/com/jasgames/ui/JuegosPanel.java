package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.TipoJuego;
import com.jasgames.service.JuegoService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class JuegosPanel extends JPanel {

    // Campo para que el .form quede contento (no lo usamos directamente)
    private JPanel panelJuegos;
    private JScrollPane scrollJuegos;
    private JPanel formJuegosPanel;
    private JPanel panelBotonesJuegos;

    private final JuegoService juegoService;

    // Componentes de la UI
    private DefaultListModel<Juego> listModel;
    private JList<Juego> listaJuegos;
    private JTextField txtNombreJuego;
    private JComboBox<TipoJuego> cbTipoJuego;
    private JSpinner spDificultad;
    private JTextArea txtDescripcionJuego;
    private JButton btnAgregarJuego;
    private JButton btnEliminarJuego;
    private JButton btnLimpiarJuego;

    // Para generar IDs
    private int nextId = 1;

    public JuegosPanel(JuegoService juegoService) {
        this.juegoService = juegoService;
        initComponents();
        cargarJuegosDesdeService();
    }

    // ------------------- UI -------------------

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ----- Lista de juegos a la izquierda -----
        listModel = new DefaultListModel<>();
        listaJuegos = new JList<>(listModel);
        JScrollPane scroll = new JScrollPane(listaJuegos);
        scroll.setPreferredSize(new Dimension(220, 0));
        add(scroll, BorderLayout.WEST);

        // ----- Formulario al centro -----
        formJuegosPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        txtNombreJuego = new JTextField();
        cbTipoJuego = new JComboBox<>(TipoJuego.values());
        spDificultad = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        txtDescripcionJuego = new JTextArea(4, 20);

        formJuegosPanel.add(new JLabel("Nombre:"));
        formJuegosPanel.add(txtNombreJuego);
        formJuegosPanel.add(new JLabel("Tipo:"));
        formJuegosPanel.add(cbTipoJuego);
        formJuegosPanel.add(new JLabel("Dificultad:"));
        formJuegosPanel.add(spDificultad);
        formJuegosPanel.add(new JLabel("Descripción:"));
        formJuegosPanel.add(new JScrollPane(txtDescripcionJuego));

        add(formJuegosPanel, BorderLayout.CENTER);

        // ----- Botones abajo -----
        panelBotonesJuegos = new JPanel();
        btnAgregarJuego = new JButton("Agregar");
        btnEliminarJuego = new JButton("Eliminar");
        btnLimpiarJuego = new JButton("Limpiar");

        panelBotonesJuegos.add(btnAgregarJuego);
        panelBotonesJuegos.add(btnEliminarJuego);
        panelBotonesJuegos.add(btnLimpiarJuego);

        add(panelBotonesJuegos, BorderLayout.SOUTH);

        // ----- Eventos -----
        btnAgregarJuego.addActionListener(e -> agregarJuego());
        btnEliminarJuego.addActionListener(e -> eliminarJuegoSeleccionado());
        btnLimpiarJuego.addActionListener(e -> limpiarFormulario());
    }

    // ------------------- LÓGICA (usa JuegoService) -------------------

    private void cargarJuegosDesdeService() {
        List<Juego> existentes = juegoService.obtenerTodos();
        for (Juego j : existentes) {
            listModel.addElement(j);
            if (j.getId() >= nextId) {
                nextId = j.getId() + 1;
            }
        }
    }

    private void agregarJuego() {
        String nombre = txtNombreJuego.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío");
            return;
        }

        TipoJuego tipo = (TipoJuego) cbTipoJuego.getSelectedItem();
        int dificultad = (Integer) spDificultad.getValue();
        String descripcion = txtDescripcionJuego.getText().trim();

        Juego juego = new Juego(nextId++, nombre, tipo, dificultad, descripcion);

        juegoService.agregarJuego(juego);
        listModel.addElement(juego);
        limpiarFormulario();
    }

    private void eliminarJuegoSeleccionado() {
        Juego seleccionado = listaJuegos.getSelectedValue();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un juego primero");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el juego \"" + seleccionado.getNombre() + "\"?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            juegoService.eliminarJuego(seleccionado);
            listModel.removeElement(seleccionado);
            limpiarFormulario();
        }
    }

    private void limpiarFormulario() {
        txtNombreJuego.setText("");
        cbTipoJuego.setSelectedIndex(0);
        spDificultad.setValue(1);
        txtDescripcionJuego.setText("");
        listaJuegos.clearSelection();
    }
}
