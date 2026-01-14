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
        setSize(780, 480);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Encabezado
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel app = new JLabel("JAS Games", SwingConstants.CENTER);
        app.setAlignmentX(Component.CENTER_ALIGNMENT);
        app.setFont(app.getFont().deriveFont(Font.BOLD, 30f));

        JLabel titulo = new JLabel("Acceso al sistema", SwingConstants.CENTER);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subt = new JLabel("Selecciona el tipo de acceso para continuar", SwingConstants.CENTER);
        subt.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(app);
        header.add(Box.createVerticalStrut(4));
        header.add(titulo);
        header.add(Box.createVerticalStrut(2));
        header.add(subt);

        root.add(header, BorderLayout.NORTH);

        // “Tarjetas” de acceso
        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));

        JButton btnDocente = botonTarjeta(
                "Docente",
                "Ingresar con usuario y contraseña",
                "Administración, reportes y seguimiento"
        );

        JButton btnEstudiante = botonTarjeta(
                "Estudiante",
                "Selección visual (sin teclado)",
                "Jugar y guardar puntajes automáticamente"
        );

        center.add(btnDocente);
        center.add(btnEstudiante);
        root.add(center, BorderLayout.CENTER);

        // Pie
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JButton linkCrear = linkButton("¿Primera vez? Crear usuario docente");
        linkCrear.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nota = new JLabel("Consejo: si estás en modo estudiante, usa pantalla completa para mejor experiencia.", SwingConstants.CENTER);
        nota.setAlignmentX(Component.CENTER_ALIGNMENT);

        footer.add(linkCrear);
        footer.add(Box.createVerticalStrut(6));
        footer.add(nota);
        root.add(footer, BorderLayout.SOUTH);

        btnDocente.addActionListener(e -> {
            new LoginDocenteWindow(context, this).setVisible(true);
            setVisible(false);
        });

        btnEstudiante.addActionListener(e -> {
            new AccesoEstudianteWindow(context, this).setVisible(true);
            setVisible(false);
        });

        linkCrear.addActionListener(e -> {
            CrearDocenteDialog dialog = new CrearDocenteDialog(this, context);
            dialog.setVisible(true);
            String u = dialog.getUsuarioCreado();
            if (u != null && !u.isBlank()) {
                // Abre el login y sugiere el usuario recién creado
                LoginDocenteWindow w = new LoginDocenteWindow(context, this);
                w.sugerirUsuario(u);
                w.setVisible(true);
                setVisible(false);
            }
        });

        return root;
    }

    private JButton botonTarjeta(String titulo, String linea1, String linea2) {
        JButton b = new JButton(
                "<html><div style='text-align:center;'>" +
                        "<div style='font-size:22px; font-weight:700; margin-bottom:6px;'>" + titulo + "</div>" +
                        "<div style='font-size:14px; margin-bottom:2px;'>" + linea1 + "</div>" +
                        "<div style='font-size:12px; opacity:0.9;'>" + linea2 + "</div>" +
                        "</div></html>"
        );
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 2, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBackground(new Color(245, 245, 245));
        return b;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0, 102, 204));
        return b;
    }
}
