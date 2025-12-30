package com.jasgames.ui.login;

import com.jasgames.service.AppContext;

import javax.swing.*;
import java.awt.*;

public class AccesoWindow extends JFrame {

    private final AppContext context;

    public AccesoWindow(AppContext context) {
        this.context = context;
        
        // Reset de sesiones al volver al menú principal
        context.setDocenteSesion(null);
        context.setNinoSesion(null);

        setTitle("JAS Games - Acceso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 450);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel titulo = new JLabel("Selecciona tu tipo de acceso", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        root.add(titulo, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 25, 25));

        JButton btnDocente = new JButton("DOCENTE");
        btnDocente.setFont(btnDocente.getFont().deriveFont(Font.BOLD, 20f));
        btnDocente.setFocusPainted(false);

        JButton btnEstudiante = new JButton("ESTUDIANTE");
        btnEstudiante.setFont(btnEstudiante.getFont().deriveFont(Font.BOLD, 20f));
        btnEstudiante.setFocusPainted(false);

        panelBotones.add(btnDocente);
        panelBotones.add(btnEstudiante);
        root.add(panelBotones, BorderLayout.CENTER);

        JLabel nota = new JLabel("Docente: usuario + contraseña | Estudiante: selección visual (sin teclado)", SwingConstants.CENTER);
        root.add(nota, BorderLayout.SOUTH);

        btnDocente.addActionListener(e -> {
            new LoginDocenteWindow(context, this).setVisible(true);
            setVisible(false);
        });

        btnEstudiante.addActionListener(e -> {
            new AccesoEstudianteWindow(context, this).setVisible(true);
            setVisible(false);
        });

        return root;
    }
}
