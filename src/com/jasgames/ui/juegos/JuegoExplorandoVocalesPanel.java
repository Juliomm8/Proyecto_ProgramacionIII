package com.jasgames.ui.juegos;

import com.jasgames.audio.AudioPlayer;
import com.jasgames.model.Actividad;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer; // Mantener para el fade
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

public class JuegoExplorandoVocalesPanel extends BaseJuegoPanel {

    private static final String AUDIO_JUEGO5 = "/assets/audio/juego5/";
    private static final String AUDIO_VOCALES = "/assets/audio/vocales/";
    private static final int ICON_SIZE = 220;

    // UI
    private JLabel lblTitulo;
    private JButton btnRepetir;
    private JLabel lblVocal;
    private final JButton[] btnOpciones = new JButton[3];

    // Estado juego
    private final Map<Character, List<VocalItem>> banco = new HashMap<>();
    private final List<Character> orden = new ArrayList<>(Arrays.asList('A', 'E', 'I', 'O', 'U'));

    private int rondaIdx = 0;
    private int intentosFallidos = 0;

    private Character vocalActual = null;
    private VocalItem correctaActual = null;
    private String audioPreguntaActual = null;

    private boolean bloqueado = false;
    private Timer timerSiguiente = null;

    private boolean audioFeedbackEnCurso = false; // error / acierto+palabra
    private boolean audioPreguntaEnCurso = false; // repetir pregunta

    // Campos nuevos para efectos visuales
    private float bodyAlpha = 1f;
    private JPanel bodyPanel;
    private JLabel lblProgreso;

    private JLabel lblPregunta;
    private ProgressDots dots;

    // ====== Modelo interno ======
    private static class VocalItem {
        final char vocal;
        final String palabra;
        final String imgPath;
        final String audioPath;

        VocalItem(char vocal, String palabra, String imgPath, String audioPath) {
            this.vocal = vocal;
            this.palabra = palabra;
            this.imgPath = imgPath;
            this.audioPath = audioPath;
        }
    }

    // ✅ Constructor correcto para tu Factory
    public JuegoExplorandoVocalesPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);

        construirUI();
        construirBanco();

        // NO llamar iniciarJuego() aquí (EstudianteWindow lo llama con invokeLater)
    }

    // ✅ EstudianteWindow llama esto automáticamente
    @Override
    public void iniciarJuego() {
        iniciarPartida();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        GradientPaint gp = new GradientPaint(
                0, 0, new Color(245, 248, 255),
                0, getHeight(), new Color(255, 255, 255)
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private void construirUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setOpaque(true);

        // ===== HEADER =====
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        lblTitulo = new JLabel("Las Vocales");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblTitulo.setForeground(new Color(30, 80, 200));

        lblProgreso = new JLabel("Ronda 1/5");
        lblProgreso.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblProgreso.setForeground(new Color(110, 110, 110));

        dots = new ProgressDots(5);
        dots.setCurrent(0);

        JPanel progresoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        progresoRow.setOpaque(false);
        progresoRow.add(dots);
        progresoRow.add(lblProgreso);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(lblTitulo);
        left.add(Box.createVerticalStrut(6));
        left.add(progresoRow);

        header.add(left, BorderLayout.WEST);

        btnRepetir = new JButton("Repetir");
        btnRepetir.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRepetir.setForeground(Color.WHITE);
        btnRepetir.setBackground(new Color(30, 80, 200));
        btnRepetir.setOpaque(true);
        btnRepetir.setContentAreaFilled(true);
        btnRepetir.setFocusPainted(false);
        btnRepetir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRepetir.setBorder(new CompoundBorder(
                new LineBorder(new Color(30, 80, 200), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        btnRepetir.addActionListener(e -> repetirAudioPregunta());

        header.add(btnRepetir, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ===== BODY con FADE =====
        bodyPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bodyAlpha));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        bodyPanel.setOpaque(false);

        // Card central (blanco con sombra)
        RoundedCardPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(26, 26, 26, 26));

        lblPregunta = new JLabel(" ", SwingConstants.CENTER);
        lblPregunta.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        lblPregunta.setForeground(new Color(90, 90, 90));
        card.add(lblPregunta, BorderLayout.NORTH);

        lblVocal = new JLabel(" ", SwingConstants.CENTER);
        lblVocal.setFont(new Font("Segoe UI", Font.BOLD, 150));
        lblVocal.setForeground(new Color(30, 80, 200));
        card.add(lblVocal, BorderLayout.CENTER);

        JPanel opciones = new JPanel(new GridLayout(1, 3, 26, 0));
        opciones.setOpaque(false);
        opciones.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));

        for (int i = 0; i < 3; i++) {
            CardButton b = new CardButton();
            b.setPreferredSize(new Dimension(320, 320));

            int idx = i;
            b.addActionListener(e -> onElegirOpcion(idx));

            btnOpciones[i] = b;
            opciones.add(b);
        }

        card.add(opciones, BorderLayout.SOUTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        bodyPanel.add(card, gbc);

        add(bodyPanel, BorderLayout.CENTER);
    }

    private void construirBanco() {
        // A
        addItem('A', "AVIÓN", "/assets/vocales/avion.png", AUDIO_VOCALES + "avion.wav");
        addItem('A', "ÁRBOL", "/assets/vocales/arbol.png", AUDIO_VOCALES + "arbol.wav");
        addItem('A', "ABEJA", "/assets/vocales/abeja.png", AUDIO_VOCALES + "abeja.wav");

        // E
        addItem('E', "ELEFANTE", "/assets/vocales/elefante.png", AUDIO_VOCALES + "elefante.wav");
        addItem('E', "ESTRELLA", "/assets/vocales/estrella.png", AUDIO_VOCALES + "estrella.wav");
        addItem('E', "ESPEJO", "/assets/vocales/espejo.png", AUDIO_VOCALES + "espejo.wav");

        // I
        addItem('I', "ISLA", "/assets/vocales/isla.png", AUDIO_VOCALES + "isla.wav");
        addItem('I', "IGLÚ", "/assets/vocales/iglu.png", AUDIO_VOCALES + "iglu.wav");
        addItem('I', "IMÁN", "/assets/vocales/iman.png", AUDIO_VOCALES + "iman.wav");

        // O
        addItem('O', "OSO", "/assets/vocales/oso.png", AUDIO_VOCALES + "oso.wav");
        addItem('O', "OJO", "/assets/vocales/ojo.png", AUDIO_VOCALES + "ojo.wav");
        addItem('O', "OCHO", "/assets/vocales/ocho.png", AUDIO_VOCALES + "ocho.wav");

        // U
        addItem('U', "UVA", "/assets/vocales/uva.png", AUDIO_VOCALES + "uva.wav");
        addItem('U', "UNICORNIO", "/assets/vocales/unicornio.png", AUDIO_VOCALES + "unicornio.wav");
        addItem('U', "UNO", "/assets/vocales/uno.png", AUDIO_VOCALES + "uno.wav");
    }

    private void addItem(char vocal, String palabra, String imgPath, String audioPath) {
        banco.computeIfAbsent(vocal, k -> new ArrayList<>())
                .add(new VocalItem(vocal, palabra, imgPath, audioPath));
    }

    private void iniciarPartida() {
        if (timerSiguiente != null && timerSiguiente.isRunning()) timerSiguiente.stop();
        AudioPlayer.stop();

        Collections.shuffle(orden);
        rondaIdx = 0;
        intentosFallidos = 0;

        siguienteRonda();
    }

    private void siguienteRonda() {
        audioFeedbackEnCurso = false;
        audioPreguntaEnCurso = false;
        btnRepetir.setEnabled(true);

        for (JButton b0 : btnOpciones) {
            b0.putClientProperty("elim", false);
            b0.setEnabled(true);
            b0.setIcon(null);
            b0.setDisabledIcon(null);
            if (b0 instanceof CardButton) ((CardButton) b0).resetVisual();
        }

        bloqueado = false;

        if (rondaIdx >= orden.size()) {
            finDelJuego();
            return;
        }

        vocalActual = orden.get(rondaIdx);
        lblProgreso.setText("Ronda " + (rondaIdx + 1) + "/5");
        dots.setCurrent(rondaIdx);
        lblPregunta.setText("¿Qué objeto empieza con la letra " + vocalActual + "?");
        lblVocal.setVisible(true);
        lblVocal.setText(String.valueOf(vocalActual));

        audioPreguntaActual = AUDIO_JUEGO5 + "pregunta_" + Character.toLowerCase(vocalActual) + ".wav";

        List<VocalItem> posiblesCorrectas = banco.get(vocalActual);
        correctaActual = posiblesCorrectas.get(new Random().nextInt(posiblesCorrectas.size()));

        List<VocalItem> poolIncorrectas = new ArrayList<>();
        for (Map.Entry<Character, List<VocalItem>> entry : banco.entrySet()) {
            if (entry.getKey() != vocalActual) poolIncorrectas.addAll(entry.getValue());
        }
        Collections.shuffle(poolIncorrectas);

        VocalItem d1 = poolIncorrectas.get(0);
        VocalItem d2 = poolIncorrectas.get(1);

        List<VocalItem> opciones = new ArrayList<>(Arrays.asList(correctaActual, d1, d2));
        Collections.shuffle(opciones);

        for (int i = 0; i < 3; i++) {
            VocalItem it = opciones.get(i);
            btnOpciones[i].putClientProperty("item", it);
            Icon ic = loadIcon(it.imgPath, ICON_SIZE, ICON_SIZE);
            btnOpciones[i].setIcon(ic);
            btnOpciones[i].setDisabledIcon(makeTransparent(ic, 0.22f)); // suave
        }

        AudioPlayer.play(audioPreguntaActual, null);
    }

    private void repetirAudioPregunta() {
        if (audioPreguntaActual == null) return;

        // Si ya hay un audio de feedback (error/acierto) o ya está sonando la pregunta, ignorar.
        if (audioFeedbackEnCurso || audioPreguntaEnCurso) return;

        audioPreguntaEnCurso = true;
        btnRepetir.setEnabled(false);

        AudioPlayer.play(audioPreguntaActual, () -> {
            audioPreguntaEnCurso = false;
            // Solo re-habilitar si no estamos bloqueados por otra cosa
            if (!audioFeedbackEnCurso) btnRepetir.setEnabled(true);
        });
    }

    private void onElegirOpcion(int idx) {
        if (bloqueado || audioFeedbackEnCurso) return;

        JButton b = btnOpciones[idx];
        VocalItem elegido = (VocalItem) b.getClientProperty("item");
        if (elegido == null) return;

        if (elegido == correctaActual) {
            bloqueado = true;
            audioFeedbackEnCurso = true;
            btnRepetir.setEnabled(false);

            if (b instanceof CardButton) ((CardButton) b).setCorrect(true);
            for (JButton other : btnOpciones) other.setEnabled(false);

            AudioPlayer.playSequence(
                    AUDIO_JUEGO5 + "acierto.wav",
                    correctaActual.audioPath,
                    () -> {
                        // Mantener feedback visible un ratito, luego transición suave
                        Timer hold = new Timer(1200, ev -> {
                            ((Timer) ev.getSource()).stop();

                            // Fade OUT
                            fadeBody(1f, 0f, 220, () -> {
                                rondaIdx++;
                                siguienteRonda(); // prepara siguiente ronda (setea letra + iconos + pregunta)
                                // Fade IN
                                fadeBody(0f, 1f, 220, null);
                            });
                        });
                        hold.setRepeats(false);
                        hold.start();
                    }
            );

        } else {
            intentosFallidos++;

            // Bloquear para que no spameen clicks y no se acumulen audios
            audioFeedbackEnCurso = true;
            btnRepetir.setEnabled(false);

            // Marcar este botón como eliminado (queda apagado para siempre en esta ronda)
            b.putClientProperty("elim", true);

            // Feedback visual suave: apagar opción
            b.setEnabled(false);
            b.setBorder(new LineBorder(new Color(210, 210, 210), 4, true)); // neutro (o b.setBorder(null))

            // Mientras suena el audio, bloquea temporalmente las otras opciones
            for (JButton x : btnOpciones) {
                if (x.isEnabled()) x.setEnabled(false);
            }

            // Audio de error suave
            AudioPlayer.play(AUDIO_JUEGO5 + "error.wav", () -> {
                audioFeedbackEnCurso = false;
                btnRepetir.setEnabled(true);

                // Rehabilitar solo las que NO están eliminadas
                setOpcionesEnabledRespetandoEliminadas(true);
            });
        }
    }

    private void finDelJuego() {
        bloqueado = true;
        for (JButton b : btnOpciones) b.setEnabled(false);

        // ✅ Guardar intentos fallidos en la actividad (ver paso 2)
        if (actividadActual != null) {
            actividadActual.setIntentosFallidos(intentosFallidos);
        }

        // ✅ Reproducir fin y luego finalizar (puntaje fijo 100)
        AudioPlayer.play(AUDIO_JUEGO5 + "fin.wav", () -> finalizarJuego(100));
    }

    private void fadeBody(float from, float to, int durationMs, Runnable onDone) {
        final int fps = 25;
        final int delay = 1000 / fps;
        final int steps = Math.max(1, durationMs / delay);
        final int[] i = {0};

        bodyAlpha = from;
        bodyPanel.repaint();

        Timer t = new Timer(delay, null);
        t.addActionListener(e -> {
            i[0]++;
            float p = i[0] / (float) steps;
            bodyAlpha = from + (to - from) * p;
            bodyPanel.repaint();

            if (i[0] >= steps) {
                t.stop();
                bodyAlpha = to;
                bodyPanel.repaint();
                if (onDone != null) onDone.run();
            }
        });
        t.start();
    }

    private Icon loadIcon(String path, int w, int h) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("Imagen no encontrada: " + path);
                return null;
            }
            BufferedImage img = ImageIO.read(url);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Icon makeTransparent(Icon icon, float alpha) {
        if (!(icon instanceof ImageIcon)) return icon;

        Image img = ((ImageIcon) icon).getImage();
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        return new ImageIcon(out);
    }

    private void setOpcionesEnabledRespetandoEliminadas(boolean enabled) {
        for (JButton b : btnOpciones) {
            boolean elim = Boolean.TRUE.equals(b.getClientProperty("elim"));
            b.setEnabled(enabled && !elim);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (timerSiguiente != null && timerSiguiente.isRunning()) timerSiguiente.stop();
        audioFeedbackEnCurso = false;
        audioPreguntaEnCurso = false;
        AudioPlayer.stop();
    }

    private static class RoundedCardPanel extends JPanel {
        RoundedCardPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 34;

            // sombra suave
            for (int i = 10; i >= 1; i--) {
                float a = 0.015f * i;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(i, i + 3, w - i * 2, h - i * 2, arc, arc);
            }

            // card blanco
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(255, 255, 255, 245));
            g2.fillRoundRect(0, 0, w, h - 6, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class CardButton extends JButton {
        private boolean hover = false;
        private boolean correct = false;

        CardButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(20, 20, 20, 20));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (isEnabled() && !correct) { hover = true; repaint(); }
                }
                @Override public void mouseExited(MouseEvent e) {
                    hover = false; repaint();
                }
            });
        }

        void resetVisual() {
            hover = false;
            correct = false;
            repaint();
        }

        void setCorrect(boolean v) {
            correct = v;
            hover = false;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 10;
            int arc = 30;

            // sombra (solo si está habilitado)
            if (isEnabled()) {
                for (int i = 6; i >= 1; i--) {
                    float a = 0.02f * i;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                    g2.setColor(Color.BLACK);
                    g2.fillRoundRect(pad + i, pad + i + 2, w - 2 * (pad + i), h - 2 * (pad + i), arc, arc);
                }
            }

            // fondo
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(isEnabled() ? Color.WHITE : new Color(252, 252, 252));
            g2.fillRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, arc, arc);

            // borde
            Color border = new Color(215, 221, 235);
            if (correct) border = new Color(70, 200, 120);
            else if (hover) border = new Color(110, 160, 255);

            g2.setStroke(new BasicStroke(correct ? 5f : 4f));
            g2.setColor(border);
            g2.drawRoundRect(pad, pad, w - 2 * pad, h - 2 * pad, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ProgressDots extends JComponent {
        private final int total;
        private int current = 0;

        ProgressDots(int total) {
            this.total = total;
            setPreferredSize(new Dimension(total * 18, 14));
            setMinimumSize(getPreferredSize());
            setOpaque(false);
        }

        void setCurrent(int current) {
            this.current = Math.max(0, Math.min(current, total - 1));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 0;
            for (int i = 0; i < total; i++) {
                boolean doneOrCurrent = (i <= current);
                g2.setColor(doneOrCurrent ? new Color(30, 80, 200) : new Color(210, 210, 210));
                g2.fillOval(x, 2, 10, 10);
                x += 18;
            }

            g2.dispose();
        }
    }
}
