package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Minijuego 2: Cuenta y Conecta
 *
 * - Arriba: pizarra con N figuras (1 forma + 1 color por ronda).
 * - Abajo: 3 opciones de números. Si falla, la opción se desvanece (se apaga) y sigue intentando.
 * - Si acierta: pulso (latido) 2 veces y pasa de ronda.
 * - Termina al completar 5 rondas correctas. Puntaje final fijo: 100.
 */
public class JuegoCuentaConectaPanel extends BaseJuegoPanel {

    private static final int RONDAS_META = 5;
    private static final int NUM_OPCIONES = 3;

    private final Random random = new Random();

    private JPanel panelRoot;
    private JLabel lblInstruccion;
    private JLabel lblProgreso;
    private JLabel lblFeedback;

    private JPanel panelPizarra;
    private JPanel panelRespuestas;

    private LienzoCuentaConecta lienzo;
    private OpcionNumeroButton[] botones;

    private int rondasCorrectas;
    private int numeroObjetivo;
    private Forma formaActual;
    private Color colorActual;

    private boolean bloqueado;

    // Animación de pulso
    private double escalaPulso = 1.0;
    private Timer timerPulso;
    private int pasoPulso;
    private Runnable onPulsoTermina;

    // Cache de posiciones (especialmente útil para nivel 5 / nube)
    private List<Point> centrosCache = null;
    private Dimension dimensionCache = null;

    // Paleta sólida, alto contraste
    private static final Color[] PALETA = new Color[]{
            new Color(55, 110, 220),  // azul
            new Color(220, 40, 40),   // rojo
            new Color(60, 170, 90),   // verde
            new Color(240, 140, 50),  // naranja
            new Color(150, 85, 210),  // morado
            new Color(40, 40, 40)     // casi negro
    };

    private enum Forma { CIRCULO, CUADRADO, ESTRELLA }

    public JuegoCuentaConectaPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);
        initUI();
        iniciarJuego();
    }

    private void initUI() {
        panelRoot = new JPanel(new BorderLayout(12, 12));
        panelRoot.setOpaque(false);
        panelRoot.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(panelRoot, BorderLayout.CENTER);

        // Cabecera: instrucción + progreso + feedback
        JPanel panelTop = new JPanel(new GridLayout(3, 1, 0, 6));
        panelTop.setOpaque(false);

        lblInstruccion = new JLabel("Cuenta las figuras y toca el número correcto", SwingConstants.CENTER);
        lblInstruccion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));

        lblProgreso = new JLabel("Ronda 1/" + RONDAS_META, SwingConstants.CENTER);
        lblProgreso.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        lblFeedback = new JLabel(" ", SwingConstants.CENTER);
        lblFeedback.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        panelTop.add(lblInstruccion);
        panelTop.add(lblProgreso);
        panelTop.add(lblFeedback);

        panelRoot.add(panelTop, BorderLayout.NORTH);

        // Pizarra (centro)
        panelPizarra = new JPanel(new BorderLayout());
        panelPizarra.setBackground(Color.WHITE);
        panelPizarra.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        panelPizarra.setOpaque(true);

        lienzo = new LienzoCuentaConecta();
        lienzo.setOpaque(false);

        panelPizarra.add(lienzo, BorderLayout.CENTER);
        panelRoot.add(panelPizarra, BorderLayout.CENTER);

        // Respuestas (abajo)
        panelRespuestas = new JPanel(new GridLayout(1, 3, 18, 0));
        panelRespuestas.setOpaque(false);
        panelRespuestas.setBorder(BorderFactory.createEmptyBorder(10, 40, 0, 40));

        botones = new OpcionNumeroButton[NUM_OPCIONES];
        for (int i = 0; i < NUM_OPCIONES; i++) {
            OpcionNumeroButton btn = new OpcionNumeroButton();
            btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            btn.addActionListener(this::onOpcionClick);
            botones[i] = btn;
            panelRespuestas.add(btn);
        }

        panelRoot.add(panelRespuestas, BorderLayout.SOUTH);
    }

    @Override
    public void iniciarJuego() {
        rondasCorrectas = 0;
        bloqueado = false;

        detenerTimerPulso();
        escalaPulso = 1.0;

        lblFeedback.setText(" ");
        actualizarProgreso();
        nuevaRonda();
    }

    private void nuevaRonda() {
        bloqueado = false;
        lblFeedback.setText(" ");

        int nivel = getNivelSeguro();
        int max = maxNumeroPorNivel(nivel);

        numeroObjetivo = random.nextInt(max) + 1;
        formaActual = Forma.values()[random.nextInt(Forma.values().length)];
        colorActual = PALETA[random.nextInt(PALETA.length)];

        // Opciones
        List<Integer> opciones = generarOpciones(numeroObjetivo, max);
        for (int i = 0; i < NUM_OPCIONES; i++) {
            botones[i].reset(opciones.get(i));
        }

        // Reset cache posiciones
        centrosCache = null;
        dimensionCache = null;
        escalaPulso = 1.0;

        lienzo.repaint();
    }

    private void onOpcionClick(ActionEvent e) {
        if (bloqueado) return;

        OpcionNumeroButton btn = (OpcionNumeroButton) e.getSource();
        int valor = btn.getValor();

        if (valor == numeroObjetivo) {
            bloqueado = true;

            // Bloquea todas las opciones durante la animación
            for (OpcionNumeroButton b : botones) b.setEnabled(false);

            lblFeedback.setText("¡Muy bien!");

            iniciarPulso(() -> {
                rondasCorrectas++;
                actualizarProgreso();

                if (rondasCorrectas >= RONDAS_META) {
                    // Puntaje fijo por completar
                    finalizarJuego(100);
                } else {
                    nuevaRonda();
                }
            });

        } else {
            // Error neutro: apaga el botón incorrecto y sigue intentando
            lblFeedback.setText("Intenta de nuevo");
            btn.fadeOutAndDisable();
        }
    }

    private void iniciarPulso(Runnable alTerminar) {
        detenerTimerPulso();
        onPulsoTermina = alTerminar;
        pasoPulso = 0;

        // 2 latidos rápidos (sube/baja 2 veces)
        final double[] secuencia = new double[]{
                1.00, 1.06, 1.10, 1.06, 1.00,
                1.06, 1.10, 1.06, 1.00
        };

        timerPulso = new Timer(45, ev -> {
            escalaPulso = secuencia[pasoPulso];
            lienzo.repaint();

            pasoPulso++;
            if (pasoPulso >= secuencia.length) {
                detenerTimerPulso();
                escalaPulso = 1.0;
                lienzo.repaint();
                if (onPulsoTermina != null) onPulsoTermina.run();
            }
        });

        timerPulso.start();
    }

    private void detenerTimerPulso() {
        if (timerPulso != null && timerPulso.isRunning()) {
            timerPulso.stop();
        }
        timerPulso = null;
    }

    private void actualizarProgreso() {
        int mostrada = Math.min(rondasCorrectas + 1, RONDAS_META);
        lblProgreso.setText("Ronda " + mostrada + "/" + RONDAS_META);
    }

    private int getNivelSeguro() {
        int nivel = (actividadActual != null) ? actividadActual.getNivel() : 1;
        if (nivel < 1) nivel = 1;
        if (nivel > 5) nivel = 5;
        return nivel;
    }

    private int maxNumeroPorNivel(int nivel) {
        return switch (nivel) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 9;
            case 4 -> 12;
            default -> 15; // nivel 5
        };
    }

    private List<Integer> generarOpciones(int correcto, int max) {
        Set<Integer> set = new HashSet<>();
        set.add(correcto);

        while (set.size() < NUM_OPCIONES) {
            int v = random.nextInt(max) + 1;
            set.add(v);
        }

        List<Integer> lista = new ArrayList<>(set);
        // mezclar para no dejar siempre el correcto en la misma posición
        for (int i = lista.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, tmp);
        }
        return lista;
    }

    // ---------------------------------------------------------------------
    // Lienzo (dibujo de figuras)
    // ---------------------------------------------------------------------
    private class LienzoCuentaConecta extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (numeroObjetivo <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int nivel = getNivelSeguro();

                int size = calcularTamFigura(w, h, numeroObjetivo, nivel);

                List<Point> centros = obtenerCentros(numeroObjetivo, nivel, w, h, size);

                Color borde = new Color(70, 70, 70, 160);
                g2.setStroke(new BasicStroke(3f));

                for (Point c : centros) {
                    int cx = c.x;
                    int cy = c.y;

                    int s = (int) Math.round(size * escalaPulso);
                    int x = cx - (s / 2);
                    int y = cy - (s / 2);

                    g2.setColor(colorActual);

                    switch (formaActual) {
                        case CIRCULO -> {
                            g2.fillOval(x, y, s, s);
                            g2.setColor(borde);
                            g2.drawOval(x, y, s, s);
                        }
                        case CUADRADO -> {
                            int arc = Math.max(10, s / 6);
                            g2.fillRoundRect(x, y, s, s, arc, arc);
                            g2.setColor(borde);
                            g2.drawRoundRect(x, y, s, s, arc, arc);
                        }
                        case ESTRELLA -> {
                            Polygon star = crearEstrella(cx, cy, s / 2, Math.max(8, s / 4), 5);
                            g2.fillPolygon(star);
                            g2.setColor(borde);
                            g2.drawPolygon(star);
                        }
                    }
                }

            } finally {
                g2.dispose();
            }
        }

        private int calcularTamFigura(int w, int h, int n, int nivel) {
            int padding = 40;

            if (nivel == 1) {
                // 1..3 en línea, grande
                int gap = 30;
                int disponibleW = Math.max(1, w - (2 * padding) - ((n - 1) * gap));
                int s = disponibleW / n;
                s = Math.min(s, h - (2 * padding));
                return clamp(s, 90, 170);
            }

            if (nivel >= 2 && nivel <= 4) {
                // grid
                int rows = (nivel == 2) ? 2 : (nivel == 3 ? 3 : 3);
                int cols = (nivel == 2) ? 3 : (nivel == 3 ? 3 : 4);

                int cellW = Math.max(1, (w - 2 * padding) / cols);
                int cellH = Math.max(1, (h - 2 * padding) / rows);

                int s = (int) (Math.min(cellW, cellH) * 0.70);
                return clamp(s, 60, 140);
            }

            // nivel 5: nube
            double area = (double) (Math.max(1, w - 2 * padding)) * (Math.max(1, h - 2 * padding));
            int s = (int) Math.sqrt(area / (n * 8.0));
            return clamp(s, 45, 95);
        }

        private List<Point> obtenerCentros(int n, int nivel, int w, int h, int size) {
            Dimension d = new Dimension(w, h);
            if (centrosCache != null && d.equals(dimensionCache)) {
                return centrosCache;
            }

            List<Point> centros;

            if (nivel == 1) {
                centros = centrosLinea(n, w, h, size);
            } else if (nivel == 2) {
                centros = centrosGrid(n, w, h, 2, 3);
            } else if (nivel == 3) {
                centros = centrosGrid(n, w, h, 3, 3);
            } else if (nivel == 4) {
                // 4x3 = 4 columnas, 3 filas
                centros = centrosGrid(n, w, h, 3, 4);
            } else {
                centros = centrosNube(n, w, h, size);
            }

            centrosCache = centros;
            dimensionCache = d;
            return centrosCache;
        }

        private List<Point> centrosLinea(int n, int w, int h, int size) {
            int padding = 40;
            int gap = 30;

            int totalW = n * size + (n - 1) * gap;
            int startX = (w - totalW) / 2 + size / 2;
            int y = h / 2;

            List<Point> pts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int x = startX + i * (size + gap);
                pts.add(new Point(x, y));
            }
            return pts;
        }

        private List<Point> centrosGrid(int n, int w, int h, int rows, int cols) {
            int padding = 40;

            int gridW = w - 2 * padding;
            int gridH = h - 2 * padding;

            int cellW = Math.max(1, gridW / cols);
            int cellH = Math.max(1, gridH / rows);

            List<Point> pts = new ArrayList<>();
            int count = 0;

            for (int r = 0; r < rows && count < n; r++) {
                for (int c = 0; c < cols && count < n; c++) {
                    int cx = padding + c * cellW + cellW / 2;
                    int cy = padding + r * cellH + cellH / 2;
                    pts.add(new Point(cx, cy));
                    count++;
                }
            }
            return pts;
        }

        private List<Point> centrosNube(int n, int w, int h, int size) {
            int padding = Math.max(40, size / 2 + 20);
            int minDist = (int) Math.max(50, size * 1.20);

            List<Point> pts = new ArrayList<>();
            int tries = 0;
            int maxTries = 5000;

            int minX = padding;
            int maxX = Math.max(minX + 1, w - padding);
            int minY = padding;
            int maxY = Math.max(minY + 1, h - padding);

            while (pts.size() < n && tries < maxTries) {
                int x = minX + random.nextInt(Math.max(1, maxX - minX));
                int y = minY + random.nextInt(Math.max(1, maxY - minY));

                boolean ok = true;
                for (Point q : pts) {
                    int dx = x - q.x;
                    int dy = y - q.y;
                    if (dx * dx + dy * dy < minDist * minDist) {
                        ok = false;
                        break;
                    }
                }

                if (ok) pts.add(new Point(x, y));
                tries++;
            }

            // Fallback si por tamaño/ventana no cupo bien: grid 5x3 (cap 15)
            if (pts.size() < n) {
                return centrosGrid(n, w, h, 3, 5);
            }

            return pts;
        }

        private Polygon crearEstrella(int cx, int cy, int rOuter, int rInner, int puntas) {
            Polygon p = new Polygon();
            double ang = -Math.PI / 2; // arriba
            double step = Math.PI / puntas;

            for (int i = 0; i < puntas * 2; i++) {
                int r = (i % 2 == 0) ? rOuter : rInner;
                int x = cx + (int) Math.round(Math.cos(ang) * r);
                int y = cy + (int) Math.round(Math.sin(ang) * r);
                p.addPoint(x, y);
                ang += step;
            }
            return p;
        }

        private int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
    }

    // ---------------------------------------------------------------------
    // Botón circular con fade-out
    // ---------------------------------------------------------------------
    private static class OpcionNumeroButton extends JButton {

        private int valor;
        private float alpha = 1f;
        private Timer fadeTimer;

        OpcionNumeroButton() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setPreferredSize(new Dimension(180, 180));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        int getValor() {
            return valor;
        }

        void reset(int nuevoValor) {
            this.valor = nuevoValor;
            this.alpha = 1f;
            setEnabled(true);
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            repaint();
        }

        void fadeOutAndDisable() {
            if (!isEnabled()) return;

            setEnabled(false);

            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();

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
        public boolean contains(int x, int y) {
            int d = Math.min(getWidth(), getHeight());
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int r = d / 2;
            int dx = x - cx;
            int dy = y - cy;
            return (dx * dx + dy * dy) <= (r * r);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                int d = Math.min(getWidth(), getHeight());
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;

                Color fill = isEnabled() ? Color.WHITE : new Color(230, 230, 230);
                Color border = isEnabled() ? new Color(80, 80, 80, 160) : new Color(150, 150, 150, 140);
                Color text = isEnabled() ? new Color(30, 30, 30) : new Color(120, 120, 120);

                g2.setColor(fill);
                g2.fillOval(x, y, d, d);

                g2.setStroke(new BasicStroke(4f));
                g2.setColor(border);
                g2.drawOval(x + 2, y + 2, d - 4, d - 4);

                String t = String.valueOf(valor);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();

                int tx = (getWidth() - fm.stringWidth(t)) / 2;
                int ty = (getHeight() + fm.getAscent()) / 2 - 8;

                g2.setColor(text);
                g2.drawString(t, tx, ty);

            } finally {
                g2.dispose();
            }
        }
    }
}
