package com.jasgames.ui.login;

import com.jasgames.model.Nino;
import com.jasgames.service.AppContext;
import com.jasgames.service.DirectorioEscolarService;
import com.jasgames.ui.EstudianteWindow;
import com.jasgames.util.EmojiFonts;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

public class AccesoEstudianteWindow extends JFrame {

    private static final String CARD_AULAS = "AULAS";
    private static final String CARD_ESTUDIANTES = "ESTUDIANTES";

    private final AppContext context;
    private final JFrame ventanaAnterior;
    private final DirectorioEscolarService directorio;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JPanel cardAulas = new JPanel(new BorderLayout());
    private final JPanel cardEstudiantes = new JPanel(new BorderLayout());
    
    private JPanel header;

    private final JLabel lblTitulo = new JLabel("Selecciona tu aula", SwingConstants.CENTER);
    private final JButton btnAtras = new JButton("Atrás");
    private final JButton btnSalir = new JButton("Salir");

    // Salida protegida (mantener presionado) para evitar que el niño salga sin querer
    private javax.swing.Timer holdSalir;

    private Color colorAula(String aula) {
        return context.getAulaService().colorDeAula(aula);
    }

    private Color fondoSuave(Color c) {
        // Mezcla con blanco para un fondo pastel (más suave)
        int r = (c.getRed() + 255) / 2;
        int g = (c.getGreen() + 255) / 2;
        int b = (c.getBlue() + 255) / 2;
        return new Color(r, g, b);
    }

    private void aplicarEstiloFicha(JButton ficha, String aula) {
        Color c = colorAula(aula);

        ficha.setOpaque(true);
        ficha.setContentAreaFilled(true);
        ficha.setBorderPainted(true);

        // Fondo pastel + borde grueso del color del aula
        ficha.setBackground(fondoSuave(c));
        Border borde = BorderFactory.createLineBorder(c, 6, true);
        ficha.setBorder(borde);

        ficha.setForeground(Color.BLACK);
    }

    private void aplicarEstiloAula(JButton btn, String aula) {
        Color c = colorAula(aula);

        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setBackground(c);

        if ("Aula Amarilla".equalsIgnoreCase(aula)) btn.setForeground(Color.BLACK);
        else btn.setForeground(Color.WHITE);
    }
    
    private void aplicarBarraAulaHeader(String aula) {
        if (header == null) return;

        if (aula == null || aula.isBlank()) {
            header.setBorder(null);
            return;
        }

        Color c = colorAula(aula);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 8, 0, c));
        header.revalidate();
        header.repaint();
    }

    public AccesoEstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.directorio = context.getDirectorioEscolarService();

        setTitle("JAS Games - Acceso Estudiante");
        // Evita cerrar con la X por accidente (salida protegida)
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                // Hint suave
                btnSalir.doClick();
            }
        });

        setContentPane(crearContenido());
        instalarSalidaProtegida();
        cargarAulas();
    }

    private void instalarSalidaProtegida() {
        // Click normal solo muestra un hint; el cierre real es manteniendo presionado.
        btnSalir.addActionListener(e -> {
            String old = lblTitulo.getText();
            lblTitulo.setText("🖐️ Mantén 2s para salir");
            javax.swing.Timer t = new javax.swing.Timer(1800, ev -> lblTitulo.setText(old));
            t.setRepeats(false);
            t.start();
        });

        btnSalir.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (holdSalir != null) holdSalir.stop();
                holdSalir = new javax.swing.Timer(1800, ev -> {
                    if (holdSalir != null) holdSalir.stop();
                    SwingUtilities.invokeLater(() -> salir());
                });
                holdSalir.setRepeats(false);
                holdSalir.start();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (holdSalir != null) holdSalir.stop();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (holdSalir != null) holdSalir.stop();
            }
        });
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        this.header = new JPanel(new BorderLayout(10, 10));
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 22f));
        this.header.add(lblTitulo, BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnAtras.setEnabled(false);
        izquierda.add(btnAtras);
        this.header.add(izquierda, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        derecha.add(btnSalir);
        this.header.add(derecha, BorderLayout.EAST);

        root.add(this.header, BorderLayout.NORTH);

        cards.add(cardAulas, CARD_AULAS);
        cards.add(cardEstudiantes, CARD_ESTUDIANTES);
        root.add(cards, BorderLayout.CENTER);

        btnAtras.addActionListener(e -> volverAulas());

        return root;
    }

    private void cargarAulas() {
        List<String> aulas = directorio.obtenerAulas();
        cardAulas.removeAll();

        JPanel panelAulas = new JPanel(new GridLayout(0, 3, 20, 20));
        panelAulas.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        if (aulas.isEmpty()) {
            JLabel msg = new JLabel("No hay aulas registradas en ninos.json", SwingConstants.CENTER);
            msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 18f));
            cardAulas.add(msg, BorderLayout.CENTER);
        } else {
            for (String aula : aulas) {
                JButton btn = new JButton(aula);
                btn.setFont(btn.getFont().deriveFont(Font.BOLD, 22f));
                btn.setFocusPainted(false);
                btn.addActionListener(e -> abrirAula(aula));
                panelAulas.add(btn);
                aplicarEstiloAula(btn, aula);
            }

            JScrollPane scroll = new JScrollPane(panelAulas);
            scroll.setBorder(null);
            cardAulas.add(scroll, BorderLayout.CENTER);
        }

        lblTitulo.setText("Selecciona tu aula");
        btnAtras.setEnabled(false);
        cardLayout.show(cards, CARD_AULAS);
        
        aplicarBarraAulaHeader(null);
        
        cardAulas.revalidate();
        cardAulas.repaint();
    }

    private void abrirAula(String aula) {
        // AUDITORÍA: selección de aula
        context.getAuditoriaService().seleccionAula(aula);

        List<Nino> estudiantes = directorio.obtenerEstudiantesPorAula(aula);
        cardEstudiantes.removeAll();

        JPanel panelEstudiantes = new JPanel(new GridLayout(0, 4, 15, 15));
        panelEstudiantes.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        if (estudiantes.isEmpty()) {
            JLabel msg = new JLabel("No hay estudiantes en el aula: " + aula, SwingConstants.CENTER);
            msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 18f));
            cardEstudiantes.add(msg, BorderLayout.CENTER);
        } else {
            for (Nino n : estudiantes) {
                JButton ficha = crearFichaEstudiante(n);
                aplicarEstiloFicha(ficha, n.getAula());
                ficha.addActionListener(e -> seleccionarEstudiante(n));
                panelEstudiantes.add(ficha);
            }

            JScrollPane scroll = new JScrollPane(panelEstudiantes);
            scroll.setBorder(null);
            cardEstudiantes.add(scroll, BorderLayout.CENTER);
        }

        lblTitulo.setText("Aula: " + aula);
        btnAtras.setEnabled(true);
        cardLayout.show(cards, CARD_ESTUDIANTES);
        
        aplicarBarraAulaHeader(aula);
        
        cardEstudiantes.revalidate();
        cardEstudiantes.repaint();
    }

    /**
     * Crea la tarjeta del estudiante SIN HTML para el emoji.
     *
     * Swing (BasicHTML) suele renderizar mal algunos emojis (cuadritos), porque no aplica
     * correctamente el fallback de fuentes dentro del HTML. Para evitarlo, ponemos el emoji
     * en un JLabel con una fuente de emojis (EmojiFonts) y el nombre en otro JLabel.
     */
    private JButton crearFichaEstudiante(Nino n) {
        JButton ficha = new JButton();
        ficha.setFocusPainted(false);
        ficha.setBorderPainted(true);
        ficha.setContentAreaFilled(true);
        ficha.setOpaque(true);
        ficha.setLayout(new BorderLayout());
        ficha.setMargin(new Insets(10, 10, 10, 10));

        String avatar = safeAvatar(n != null ? n.getAvatar() : null);
        JLabel lblAvatar = new JLabel(avatar, SwingConstants.CENTER);
        EmojiFonts.apply(lblAvatar, 40f);
        lblAvatar.setOpaque(false);

        String nombre = (n != null && n.getNombre() != null) ? n.getNombre().trim() : "";
        if (nombre.isBlank()) nombre = "Sin nombre";
        JLabel lblNombre = new JLabel("<html><div style='text-align:center;'>" + escapeHtml(nombre) + "</div></html>",
                SwingConstants.CENTER);
        lblNombre.setFont(lblNombre.getFont().deriveFont(Font.BOLD, 16f));
        lblNombre.setOpaque(false);
        lblNombre.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        ficha.add(lblAvatar, BorderLayout.CENTER);
        ficha.add(lblNombre, BorderLayout.SOUTH);

        return ficha;
    }

    private String safeAvatar(String raw) {
        String a = (raw == null) ? "" : raw.trim();
        if (a.isBlank()) a = "🙂";

        // Si la fuente de emoji no puede renderizar el caracter, usamos un fallback seguro.
        try {
            Font f = EmojiFonts.emoji(40f);
            if (f != null && f.canDisplayUpTo(a) != -1) return "🙂";
        } catch (Exception ignored) {}

        return a;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void seleccionarEstudiante(Nino nino) {
        // AUDITORÍA: ingreso estudiante
        context.getAuditoriaService().registrar(
                "INGRESO_ESTUDIANTE",
                "id=" + nino.getId() + " nombre=" + nino.getNombre() + " aula=" + nino.getAula()
        );

        context.setNinoSesion(nino);

        EstudianteWindow w = new EstudianteWindow(context, this, nino);
        w.setVisible(true);
        setVisible(false);
    }

    private void volverAulas() {
        lblTitulo.setText("Selecciona tu aula");
        btnAtras.setEnabled(false);
        cardLayout.show(cards, CARD_AULAS);
        
        aplicarBarraAulaHeader(null);
    }

    private void salir() {
        context.setNinoSesion(null);
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }
}
