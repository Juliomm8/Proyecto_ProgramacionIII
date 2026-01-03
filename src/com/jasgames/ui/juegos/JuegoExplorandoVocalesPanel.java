package com.jasgames.ui.juegos;

import com.jasgames.audio.AudioPlayer;
import com.jasgames.model.Actividad;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

public class JuegoExplorandoVocalesPanel extends BaseJuegoPanel {

    private static final String AUDIO_JUEGO5 = "/assets/audio/juego5/";
    private static final String AUDIO_VOCALES = "/assets/audio/vocales/";
    private static final int ICON_SIZE = 170;

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

    private void construirUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        lblTitulo = new JLabel("Las Vocales");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 26));
        header.add(lblTitulo, BorderLayout.WEST);

        btnRepetir = new JButton("🔊 Repetir");
        btnRepetir.setFocusPainted(false);
        btnRepetir.addActionListener(e -> repetirAudioPregunta());
        header.add(btnRepetir, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        lblVocal = new JLabel(" ", SwingConstants.CENTER);
        lblVocal.setVisible(false);
        lblVocal.setFont(new Font("Arial", Font.BOLD, 110));
        lblVocal.setForeground(new Color(30, 80, 200));
        add(lblVocal, BorderLayout.CENTER);

        JPanel opciones = new JPanel(new GridLayout(1, 3, 12, 12));
        opciones.setOpaque(false);
        opciones.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        for (int i = 0; i < 3; i++) {
            JButton b = new JButton();
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);

            int idx = i;
            b.addActionListener(e -> onElegirOpcion(idx));

            btnOpciones[i] = b;
            opciones.add(b);
        }

        add(opciones, BorderLayout.SOUTH);
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

        for (JButton b : btnOpciones) {
            b.putClientProperty("elim", false); // reset de eliminadas
            b.setEnabled(true);
            b.setBorder(null);
            b.setIcon(null);
        }

        bloqueado = false;

        if (rondaIdx >= orden.size()) {
            finDelJuego();
            return;
        }

        vocalActual = orden.get(rondaIdx);
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
            btnOpciones[i].setIcon(loadIcon(it.imgPath, ICON_SIZE, ICON_SIZE));
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

            b.setBorder(new LineBorder(new Color(20, 170, 60), 5, true));
            for (JButton other : btnOpciones) other.setEnabled(false);

            AudioPlayer.playSequence(
                    AUDIO_JUEGO5 + "acierto.wav",
                    correctaActual.audioPath,
                    () -> {
                        // el feedback ya terminó, pero igual seguimos bloqueados hasta pasar de ronda
                        audioFeedbackEnCurso = false;

                        timerSiguiente = new Timer(2000, ev -> {
                            ((Timer) ev.getSource()).stop();
                            rondaIdx++;
                            siguienteRonda();
                        });
                        timerSiguiente.setRepeats(false);
                        timerSiguiente.start();
                    }
            );

        } else {
            intentosFallidos++;

            audioFeedbackEnCurso = true;
            btnRepetir.setEnabled(false);

            // Marcar esta opción como eliminada
            b.putClientProperty("elim", true);
            b.setBorder(new LineBorder(new Color(200, 60, 60), 4, true));

            Icon currentIcon = b.getIcon();
            if (currentIcon != null) b.setIcon(makeTransparent(currentIcon, 0.25f));

            // Bloquear TODAS las opciones mientras suena el error (para evitar spam)
            for (JButton x : btnOpciones) x.setEnabled(false);

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
}
