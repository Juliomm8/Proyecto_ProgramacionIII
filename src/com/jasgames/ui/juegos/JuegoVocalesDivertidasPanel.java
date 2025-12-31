package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import com.jasgames.ui.juegos.framework.AccesibleUI;
import com.jasgames.ui.juegos.framework.JuegoRondasPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Juego 4: "Vocales Divertidas"
 *
 * Reglas (MVP):
 * - Tablero: pictograma + palabra incompleta ("_ S O")
 * - Opciones: 3 vocales (1 correcta + 2 distractores aleatorios)
 * - Error: el botón se apaga (fade) y se deshabilita. Intentos ilimitados.
 * - Acierto: la vocal "vuela" al hueco y luego pulso (framework).
 * - 5 rondas por partida. Puntaje final fijo: 100.
 */
public class JuegoVocalesDivertidasPanel extends JuegoRondasPanel {

    private static final int RONDAS_META = 5;
    private static final int NUM_OPCIONES = 3;
    private static final String[] VOCALES = new String[]{"A", "E", "I", "O", "U"};

    private final Random random = new Random();

    // UI
    private LienzoVocales lienzo;
    private JPanel panelRespuestas;
    private VocalButton[] botones;

    // Banco + rondas
    private List<ItemVocal> banco;
    private List<ItemVocal> planRondas; // tamaño 5
    private ItemVocal itemActual;

    // Opciones ronda
    private String vocalCorrecta;

    // Animación (letra volando)
    private Timer animTimer;
    private boolean animando;
    private boolean mostrarCompleta;
    private String animLetra;
    private Point animStart;
    private Point animEnd;
    private double animT; // 0..1

    private static final class ItemVocal {
        final String vocalCorrecta;   // lógica: "A" | "E" | "I" | "O" | "U"
        final String palabraCompleta; // presentación: puede tener tildes (ej. "ÁRBOL")
        final String recursoImagen;   // opcional: futuro /assets/...png

        ItemVocal(String vocalCorrecta, String palabraCompleta, String recursoImagen) {
            this.vocalCorrecta = vocalCorrecta;
            this.palabraCompleta = palabraCompleta;
            this.recursoImagen = recursoImagen;
        }
    }

    public JuegoVocalesDivertidasPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);

        setInstruccion("¿Con qué vocal empieza?");

        banco = construirBancoInicial();

        lienzo = new LienzoVocales();
        lienzo.setOpaque(false);
        setTablero(lienzo);

        panelRespuestas = construirPanelRespuestas();
        setPanelRespuestas(panelRespuestas);
        // NO llamar iniciarJuego() aquí (se inicia desde EstudianteWindow)
    }

    private JPanel construirPanelRespuestas() {
        JPanel p = new JPanel(new GridLayout(1, NUM_OPCIONES, 18, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 60, 0, 60));

        botones = new VocalButton[NUM_OPCIONES];
        for (int i = 0; i < NUM_OPCIONES; i++) {
            VocalButton btn = new VocalButton();
            btn.addActionListener(this::onOpcionClick);
            botones[i] = btn;
            p.add(btn);
        }
        return p;
    }

    @Override
    protected void onAntesDeIniciar() {
        setBloqueado(false);
        setFeedback(" ");

        detenerAnimacion();
        animando = false;
        mostrarCompleta = false;
        animLetra = null;
        animStart = null;
        animEnd = null;
        animT = 0;

        planRondas = generarPlanRondasBalanceado();
        itemActual = null;
        vocalCorrecta = null;
    }

    @Override
    protected void onAntesDeSalir() {
        detenerAnimacion();
        if (botones != null) {
            for (VocalButton b : botones) {
                if (b != null) b.stopAnimations();
            }
        }
    }

    @Override
    protected int getRondasMetaPorNivel(int nivel) {
        return RONDAS_META;
    }

    @Override
    protected int calcularPuntosFinales() {
        return 100;
    }

    @Override
    protected void prepararNuevaRonda() {
        setBloqueado(false);
        setFeedback(" ");

        detenerAnimacion();
        animando = false;
        mostrarCompleta = false;
        animLetra = null;
        animStart = null;
        animEnd = null;
        animT = 0;

        for (VocalButton b : botones) {
            b.setEnabled(true);
            b.resetAlpha();
            b.setCorrecto(false);
        }

        if (planRondas == null || planRondas.size() < RONDAS_META) {
            planRondas = generarPlanRondasBalanceado();
        }

        int idx = Math.min(Math.max(rondasCorrectas, 0), planRondas.size() - 1);
        itemActual = planRondas.get(idx);
        vocalCorrecta = itemActual.vocalCorrecta;

        lienzo.setItem(itemActual);

        asignarOpciones();
        lienzo.repaint();
    }

    private void onOpcionClick(ActionEvent e) {
        if (isBloqueado() || animando) return;

        VocalButton btn = (VocalButton) e.getSource();
        String valor = btn.getLetra();
        if (valor == null) return;

        if (valor.equals(vocalCorrecta)) {
            // Refuerzo sonoro MVP
            Toolkit.getDefaultToolkit().beep();

            for (VocalButton b : botones) b.setEnabled(false);
            btn.setCorrecto(true);
            setFeedback("¡Muy bien!");
            iniciarAnimacionViaje(btn);

        } else {
            marcarErrorNeutro("Intenta de nuevo");
            btn.fadeOutAndDisable();
        }
    }

    private void asignarOpciones() {
        List<String> opciones = new ArrayList<>();
        opciones.add(vocalCorrecta);

        List<String> pool = new ArrayList<>();
        for (String v : VOCALES) {
            if (!v.equals(vocalCorrecta)) pool.add(v);
        }
        Collections.shuffle(pool, random);
        opciones.add(pool.get(0));
        opciones.add(pool.get(1));

        Collections.shuffle(opciones, random);

        for (int i = 0; i < NUM_OPCIONES; i++) {
            botones[i].reset(opciones.get(i));
        }
    }

    // -------------------- Animación --------------------

    private void iniciarAnimacionViaje(VocalButton origen) {
        Point destino = lienzo.getCentroHueco();
        if (destino == null) {
            // fallback
            mostrarCompleta = true;
            lienzo.repaint();
            marcarAciertoConPulso(lienzo, this::resetPostAcierto);
            return;
        }

        Point startLocal = SwingUtilities.convertPoint(
                origen,
                new Point(origen.getWidth() / 2, origen.getHeight() / 2),
                lienzo
        );

        animando = true;
        animLetra = vocalCorrecta;
        animStart = startLocal;
        animEnd = destino;
        animT = 0;

        detenerAnimacion();
        animTimer = new Timer(16, ev -> {
            animT += 0.045; // ~350-400ms
            if (animT >= 1.0) {
                animT = 1.0;
                ((Timer) ev.getSource()).stop();

                animando = false;
                mostrarCompleta = true;
                lienzo.repaint();

                marcarAciertoConPulso(lienzo, this::resetPostAcierto);
                return;
            }
            lienzo.repaint();
        });
        animTimer.start();
    }

    private void resetPostAcierto() {
        mostrarCompleta = false;
        animLetra = null;
        animStart = null;
        animEnd = null;
        animT = 0;
        detenerAnimacion();
    }

    private void detenerAnimacion() {
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = null;
    }

    // -------------------- Banco / rondas --------------------

    private List<ItemVocal> construirBancoInicial() {
        // Lista curada (nivel 1 seguro). Recurso de imagen queda preparado para futuro.
        List<ItemVocal> l = new ArrayList<>();

        // A
        l.add(new ItemVocal("A", "AVIÓN", null));
        l.add(new ItemVocal("A", "ÁRBOL", null));
        l.add(new ItemVocal("A", "ABEJA", null));
        l.add(new ItemVocal("A", "ARAÑA", null));
        l.add(new ItemVocal("A", "AZUL", null));

        // E
        l.add(new ItemVocal("E", "ELEFANTE", null));
        l.add(new ItemVocal("E", "ESTRELLA", null));
        l.add(new ItemVocal("E", "ESCALERA", null));
        l.add(new ItemVocal("E", "ELOTE", null));
        l.add(new ItemVocal("E", "ESPADA", null));

        // I
        l.add(new ItemVocal("I", "IGLESIA", null));
        l.add(new ItemVocal("I", "ISLA", null));
        l.add(new ItemVocal("I", "IGUANA", null));
        l.add(new ItemVocal("I", "IMÁN", null));
        l.add(new ItemVocal("I", "IGLÚ", null));

        // O
        l.add(new ItemVocal("O", "OSO", null));
        l.add(new ItemVocal("O", "OJO", null));
        l.add(new ItemVocal("O", "OLLA", null));
        l.add(new ItemVocal("O", "OREJA", null));
        l.add(new ItemVocal("O", "OCHO", null));

        // U
        l.add(new ItemVocal("U", "UVA", null));
        l.add(new ItemVocal("U", "UÑA", null));
        l.add(new ItemVocal("U", "UNO", null));
        l.add(new ItemVocal("U", "UNICORNIO", null));
        l.add(new ItemVocal("U", "UNIVERSO", null));

        return l;
    }

    /**
     * Plan de 5 rondas balanceado: 1 palabra por vocal (A,E,I,O,U) y luego barajado.
     */
    private List<ItemVocal> generarPlanRondasBalanceado() {
        Map<String, List<ItemVocal>> porVocal = new HashMap<>();
        for (String v : VOCALES) porVocal.put(v, new ArrayList<>());
        for (ItemVocal it : banco) {
            if (it != null && it.vocalCorrecta != null && porVocal.containsKey(it.vocalCorrecta)) {
                porVocal.get(it.vocalCorrecta).add(it);
            }
        }

        List<ItemVocal> plan = new ArrayList<>();
        for (String v : VOCALES) {
            List<ItemVocal> lista = porVocal.get(v);
            if (lista == null || lista.isEmpty()) {
                // fallback improbable
                plan.add(banco.get(random.nextInt(banco.size())));
            } else {
                plan.add(lista.get(random.nextInt(lista.size())));
            }
        }
        Collections.shuffle(plan, random);
        return plan;
    }

    // -------------------- Lienzo --------------------

    private class LienzoVocales extends JPanel {

        private ItemVocal item;
        private Point centroHueco; // cache para animación

        void setItem(ItemVocal item) {
            this.item = item;
            this.centroHueco = null;
        }

        Point getCentroHueco() {
            return centroHueco;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (item == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                // Zona de pictograma (arriba)
                int imgBox = (int) Math.round(Math.min(w, h) * 0.48);
                imgBox = clamp(imgBox, 140, 260);

                int imgX = (w - imgBox) / 2;
                int imgY = (int) Math.round(h * 0.12);

                dibujarPictogramaPlaceholder(g2, imgX, imgY, imgBox, item);

                // Zona de palabra (abajo)
                int wordY = imgY + imgBox + (int) Math.round(h * 0.10);
                wordY = Math.min(wordY, h - 70);

                String palabra = item.palabraCompleta;
                if (palabra == null) palabra = "";

                // Restante sin la inicial
                String resto = palabra.length() <= 1 ? "" : palabra.substring(1);

                int baseSize = (int) Math.round(Math.min(w, h) * 0.12);
                baseSize = clamp(baseSize, 34, 54);
                Font font = new Font(Font.SANS_SERIF, Font.BOLD, baseSize);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics();

                // Tokens: [hueco] + letras del resto
                List<String> tokens = new ArrayList<>();
                tokens.add("_");
                for (int i = 0; i < resto.length(); i++) {
                    tokens.add(String.valueOf(resto.charAt(i)));
                }

                int gap = Math.max(12, baseSize / 3);
                int totalW = 0;
                for (String t : tokens) totalW += fm.stringWidth(t);
                totalW += gap * (tokens.size() - 1);

                int startX = (w - totalW) / 2;
                int baseline = wordY;

                // Pinta tokens y calcula centro del hueco
                int x = startX;
                centroHueco = null;

                for (int i = 0; i < tokens.size(); i++) {
                    String t = tokens.get(i);

                    if (i == 0) {
                        // Hueco más visible: línea gruesa
                        int uw = Math.max(fm.stringWidth("_") + 10, baseSize);
                        int uy = baseline + 8;
                        int ux1 = x;
                        int ux2 = x + uw;

                        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(AccesibleUI.TEXTO_NEUTRO);
                        g2.drawLine(ux1, uy, ux2, uy);

                        // Centro del hueco = destino de animación
                        centroHueco = new Point((ux1 + ux2) / 2, baseline - fm.getAscent() / 2);

                        // Si ya se acertó, mostrar la vocal con pulso
                        if (mostrarCompleta) {
                            double s = getPulsoScale();
                            int fs = (int) Math.round(baseSize * s);
                            Font f2 = new Font(Font.SANS_SERIF, Font.BOLD, fs);
                            g2.setFont(f2);
                            FontMetrics fm2 = g2.getFontMetrics();

                            String letra = item.vocalCorrecta;
                            int lx = centroHueco.x - fm2.stringWidth(letra) / 2;
                            int ly = baseline;

                            g2.setColor(AccesibleUI.TEXTO_OSCURO);
                            g2.drawString(letra, lx, ly);

                            // volver a font base
                            g2.setFont(font);
                            fm = g2.getFontMetrics();
                        }

                        x += uw + gap;
                        continue;
                    }

                    g2.setColor(AccesibleUI.TEXTO_OSCURO);
                    g2.drawString(t, x, baseline);
                    x += fm.stringWidth(t) + gap;
                }

                // Animación: letra volando hacia el hueco
                if (animando && animLetra != null && animStart != null && animEnd != null) {
                    double t = easeOut(animT);
                    int cx = (int) Math.round(animStart.x + (animEnd.x - animStart.x) * t);
                    int cy = (int) Math.round(animStart.y + (animEnd.y - animStart.y) * t);

                    int fs = clamp((int) Math.round(baseSize * 1.05), 32, 64);
                    Font f3 = new Font(Font.SANS_SERIF, Font.BOLD, fs);
                    g2.setFont(f3);
                    FontMetrics fma = g2.getFontMetrics();

                    g2.setColor(AccesibleUI.TEXTO_OSCURO);
                    int tx = cx - fma.stringWidth(animLetra) / 2;
                    int ty = cy + fma.getAscent() / 2;
                    g2.drawString(animLetra, tx, ty);
                }

                // Texto suave con la palabra completa (opcional, ayuda visual)
                if (mostrarCompleta) {
                    g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                    g2.setColor(AccesibleUI.TEXTO_NEUTRO);
                    String completa = item.palabraCompleta;
                    int tw2 = g2.getFontMetrics().stringWidth(completa);
                    int y2 = Math.min(h - 18, baseline + 34);
                    g2.drawString(completa, (w - tw2) / 2, y2);
                }

            } finally {
                g2.dispose();
            }
        }

        private void dibujarPictogramaPlaceholder(Graphics2D g2, int x, int y, int size, ItemVocal item) {
            int arc = Math.max(18, size / 6);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, size, size, arc, arc);
            g2.setColor(AccesibleUI.TABLERO_BORDE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, size, size, arc, arc);

            // MVP: placeholder neutral (evita problemas de emoji/fuentes).
            g2.setColor(AccesibleUI.TEXTO_NEUTRO);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            String t = "[ IMAGEN ]";
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth(t)) / 2;
            int ty = y + size / 2 + fm.getAscent() / 2;
            g2.drawString(t, tx, ty);
        }

        private double easeOut(double t) {
            double u = 1 - t;
            return 1 - u * u * u;
        }
    }

    // -------------------- Botón circular (vocal) --------------------

    private static class VocalButton extends JButton {
        private String letra;
        private float alpha = 1f;
        private Timer fadeTimer;
        private boolean correcto;

        VocalButton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setPreferredSize(new Dimension(150, 130));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        String getLetra() { return letra; }

        void reset(String letra) {
            this.letra = letra;
            this.correcto = false;
            setEnabled(true);
            resetAlpha();
            repaint();
        }

        void setCorrecto(boolean value) {
            this.correcto = value;
            repaint();
        }

        void resetAlpha() {
            stopAnimations();
            alpha = 1f;
            repaint();
        }

        void stopAnimations() {
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            fadeTimer = null;
        }

        void fadeOutAndDisable() {
            if (!isEnabled()) return;
            setEnabled(false);

            stopAnimations();
            alpha = 1f;

            fadeTimer = new Timer(35, e -> {
                alpha -= 0.08f;
                if (alpha <= 0.25f) {
                    alpha = 0.25f;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            fadeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                int w = getWidth();
                int h = getHeight();
                int d = Math.min(w, h) - 6;
                int cx = w / 2;
                int cy = h / 2;
                int x = cx - d / 2;
                int y = cy - d / 2;

                Color bg = Color.WHITE;
                if (correcto) {
                    bg = new Color(230, 255, 235); // verde suave
                }

                g2.setColor(bg);
                g2.fillOval(x, y, d, d);

                g2.setStroke(new BasicStroke(3f));
                g2.setColor(isEnabled() ? AccesibleUI.BORDE_ACTIVO : AccesibleUI.BORDE_INACTIVO);
                g2.drawOval(x, y, d, d);

                if (letra != null) {
                    int fs = clamp(d / 2, 36, 60);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fs));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = cx - fm.stringWidth(letra) / 2;
                    int ty = cy + fm.getAscent() / 2 - 6;
                    g2.setColor(AccesibleUI.TEXTO_OSCURO);
                    g2.drawString(letra, tx, ty);
                }

            } finally {
                g2.dispose();
            }
        }

        private int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
