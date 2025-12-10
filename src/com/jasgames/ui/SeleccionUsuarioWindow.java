package com.jasgames.ui;

import com.jasgames.service.AppContext;

import javax.swing.*;

public class SeleccionUsuarioWindow extends JFrame {
    private JPanel panelSeleccionUsuario;
    private JLabel lblTituloSeleccion;
    private JPanel panelBotonesSeleccion;
    private JButton btnModoDocente;
    private JButton btnModoEstudiante;
    private JLabel lblMensajeSeleccion;

    private final AppContext context;

    public SeleccionUsuarioWindow(AppContext context) {
        this.context = context;

        setContentPane(panelSeleccionUsuario);
        setTitle("JAS Games - Selección de usuario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        initListeners();
    }

    public SeleccionUsuarioWindow() {
        this(new AppContext());
    }

    private void initListeners() {
        // Ir a modo Docente
        btnModoDocente.addActionListener(e -> {
            MainWindow mainWindow = new MainWindow(context, this);
            mainWindow.setVisible(true);
            this.setVisible(false);
        });

        // Ir a modo Estudiante
        btnModoEstudiante.addActionListener(e -> {
            EstudianteWindow estudianteWindow = new EstudianteWindow(context, this);
            estudianteWindow.setVisible(true);
            this.setVisible(false);
        });
    }
}
