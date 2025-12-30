package com.jasgames.ui.login;

import com.jasgames.service.AppContext;

import javax.swing.*;
import java.awt.*;

public class AccesoEstudianteWindow extends JFrame {

    private final AppContext context;
    private final JFrame ventanaAnterior;

    public AccesoEstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;

        setTitle("JAS Games - Acceso Estudiante");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Acceso Estudiante (Visual)", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        root.add(titulo, BorderLayout.NORTH);

        JLabel msg = new JLabel("Siguiente paso: cascada Aula → Estudiante", SwingConstants.CENTER);
        msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 18f));
        root.add(msg, BorderLayout.CENTER);

        JButton btnVolver = new JButton("Volver");
        btnVolver.addActionListener(e -> volver());
        root.add(btnVolver, BorderLayout.SOUTH);

        return root;
    }

    private void volver() {
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }
}
