package com.jasgames.ui.login;

import com.jasgames.service.AppContext;
import com.jasgames.util.EmojiFonts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AccesoWindow extends JFrame {

    private final AppContext context;

    public AccesoWindow(AppContext context) {
        this.context = context;
        
        // Reset de sesiones al volver al menú principal
        context.setDocenteSesion(null);
        context.setNinoSesion(null);

        setTitle("JAS Games - Acceso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    private JPanel crearContenido() {
        GradientPanel root = new GradientPanel(new Color(231, 246, 255), new Color(255, 242, 230));
        root.setLayout(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Encabezado
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titleRow.setOpaque(false);

        JLabel icon = new JLabel(safeEmoji("\uD83C\uDFAE"), SwingConstants.CENTER); // 🎮
        icon.setFont(EmojiFonts.emoji(44f));

        JLabel app = new JLabel("JAS Games", SwingConstants.CENTER);
        app.setFont(app.getFont().deriveFont(Font.BOLD, 36f));

        titleRow.add(icon);
        titleRow.add(app);

        JLabel titulo = new JLabel("¡Elige cómo quieres entrar!", SwingConstants.CENTER);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subt = new JLabel("Docentes administran • Estudiantes juegan", SwingConstants.CENTER);
        subt.setAlignmentX(Component.CENTER_ALIGNMENT);
        subt.setFont(subt.getFont().deriveFont(Font.PLAIN, 14f));

        header.add(titleRow);
        header.add(Box.createVerticalStrut(6));
        header.add(titulo);
        header.add(Box.createVerticalStrut(2));
        header.add(subt);

        root.add(header, BorderLayout.NORTH);

        // “Tarjetas” de acceso
        JPanel center = new JPanel(new GridLayout(1, 2, 18, 18));
        center.setOpaque(false);

        JButton btnDocente = tarjetaAcceso(
                "Docente",
                safeEmoji("\uD83D\uDC69\u200D\uD83C\uDFEB"), // 👩‍🏫
                "Ingresar con usuario y contraseña",
                "Administración • Reportes • Seguimiento",
                new Color(243, 246, 255),
                new Color(178, 192, 255)
        );

        JButton btnEstudiante = tarjetaAcceso(
                "Estudiante",
                safeEmoji("\uD83E\uDDD2"), // 🧒
                "Selección visual (sin teclado)",
                "Jugar • Guardar puntajes automáticamente",
                new Color(255, 250, 228),
                new Color(255, 212, 92)
        );

        center.add(btnDocente);
        center.add(btnEstudiante);
        root.add(center, BorderLayout.CENTER);

        // Pie
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JButton linkCrear = linkButton("¿Primera vez? Crear usuario docente");
        linkCrear.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nota = new JLabel("Tip: en modo estudiante, la pantalla completa ayuda a concentrarse.", SwingConstants.CENTER);
        nota.setAlignmentX(Component.CENTER_ALIGNMENT);
        nota.setFont(nota.getFont().deriveFont(Font.PLAIN, 13f));

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

    private JButton tarjetaAcceso(
            String titulo,
            String emoji,
            String linea1,
            String linea2,
            Color baseBg,
            Color baseBorder
    ) {
        JButton b = new JButton();
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBackground(baseBg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.setLayout(new BorderLayout());
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBorder, 3, true),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)
        ));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(emoji, SwingConstants.CENTER);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setFont(EmojiFonts.emoji(58f));

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 30f));

        JLabel l1 = new JLabel(linea1, SwingConstants.CENTER);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);
        l1.setFont(l1.getFont().deriveFont(Font.BOLD, 16f));

        JLabel l2 = new JLabel(linea2, SwingConstants.CENTER);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);
        l2.setFont(l2.getFont().deriveFont(Font.PLAIN, 14f));

        inner.add(Box.createVerticalGlue());
        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(lblTitulo);
        inner.add(Box.createVerticalStrut(10));
        inner.add(l1);
        inner.add(Box.createVerticalStrut(6));
        inner.add(l2);
        inner.add(Box.createVerticalGlue());

        b.add(inner, BorderLayout.CENTER);

        // Hover suave (se siente "botón grande")
        Color hoverBg = tint(baseBg, 0.06);
        Color hoverBorder = tint(baseBorder, 0.10);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(hoverBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBorder, 3, true),
                        BorderFactory.createEmptyBorder(22, 22, 22, 22)
                ));
            }

            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(baseBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(baseBorder, 3, true),
                        BorderFactory.createEmptyBorder(22, 22, 22, 22)
                ));
            }
        });

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
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        return b;
    }

    private static Color tint(Color c, double amount) {
        // amount: 0..1 (mezcla con blanco)
        int r = (int) Math.round(c.getRed() + (255 - c.getRed()) * amount);
        int g = (int) Math.round(c.getGreen() + (255 - c.getGreen()) * amount);
        int b = (int) Math.round(c.getBlue() + (255 - c.getBlue()) * amount);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String safeEmoji(String emoji) {
        try {
            Font f = EmojiFonts.emoji(44f);
            return (f.canDisplayUpTo(emoji) == -1) ? emoji : "\uD83D\uDE42"; // 🙂
        } catch (Exception e) {
            return "\uD83D\uDE42"; // 🙂
        }
    }

    private static final class GradientPanel extends JPanel {
        private final Color top;
        private final Color bottom;

        private GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                g2.fillRect(0, 0, w, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
