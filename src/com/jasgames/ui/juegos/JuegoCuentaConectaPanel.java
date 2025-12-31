package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import com.jasgames.ui.juegos.framework.JuegoRondasPanel;
import com.jasgames.ui.juegos.framework.AccesibleUI;
import com.jasgames.ui.juegos.framework.Paletas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class JuegoCuentaConectaPanel extends JuegoRondasPanel {

    private static final int RONDAS_META = 5;
    private static final int NUM_OPCIONES = 3;

    private final Random random = new Random();

    private JPanel panelRespuestas;
    private LienzoCuentaConecta lienzo;
    private OpcionNumeroButton[] botones;

    private int numeroObjetivo;
    private Forma formaActual;
    private Color colorActual;

    private List<Point> centrosCache;
    private Dimension dimensionCache;

    private enum Forma { CIRCULO, CUADRADO, ESTRELLA }

    private final Color[] paletaSolida = Paletas.coloresSolidos();

    public JuegoCuentaConectaPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);

        setInstruccion("Cuenta las figuras y toca el número correcto");

        lienzo = new LienzoCuentaConecta();
        lienzo.setOpaque(false);
        setTablero(lienzo);

        panelRespuestas = construirPanelRespuestas();
        setPanelRespuestas(panelRespuestas);
    }

    private JPanel construirPanelRespuestas() {
        JPanel p = new JPanel(new GridLayout(1, 3, 18, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 40, 0, 40));

        botones = new OpcionNumeroButton[NUM_OPCIONES];
        for (int i = 0; i < NUM_OPCIONES; i++) {
            OpcionNumeroButton btn = new OpcionNumeroButton();
            btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            btn.addActionListener(this::onOpcionClick);
            botones[i] = btn;
            p.add(btn);
        }
        return p;
    }

    @Override
    protected void onAntesDeIniciar() {
        setBloqueado(false);
        numeroObjetivo = 0;
        centrosCache = null;
        dimensionCache = null;
        setFeedback(" ");
    }

    @Override
    protected void onAntesDeSalir() {
        if (botones != null) {
            for (OpcionNumeroButton b : botones) {
                if (b != null) b.stopAnimations();
            }
        }
    }

    @Override
    protected int getRondasMetaPorNivel(int nivel) {
        return RONDAS_META; // siempre 5 rondas
    }

    @Override
    protected int calcularPuntosFinales() {
        return 100; // fijo: completó la actividad = 100
    }

    @Override
    protected void prepararNuevaRonda() {
        setBloqueado(false);
        setFeedback(" ");

        int nivel = getNivelSeguro();
        int max = maxNumeroPorNivel(nivel);

        numeroObjetivo = random.nextInt(max) + 1;
        formaActual = Forma.values()[random.nextInt(Forma.values().length)];
        colorActual = Paletas.colorAleatorio(paletaSolida, random);

        List<Integer> opciones = generarOpciones(numeroObjetivo, max);
        for (int i = 0; i < NUM_OPCIONES; i++) {
            botones[i].reset(opciones.get(i));
        }

        centrosCache = null;
        dimensionCache = null;

        lienzo.repaint();
    }

    private void onOpcionClick(ActionEvent e) {
        if (isBloqueado()) return;

        OpcionNumeroButton btn = (OpcionNumeroButton) e.getSource();
        int valor = btn.getValor();

        if (valor == numeroObjetivo) {
            // deshabilita inputs ya mismo (y el template maneja el bloqueado)
            for (OpcionNumeroButton b : botones) b.setEnabled(false);

            // acierto: pulso + next round / finalizar
            marcarAciertoConPulso(lienzo, null);
        } else {
            marcarErrorNeutro("Intenta de nuevo");
            btn.fadeOutAndDisable();
        }
    }

    private int getNivelSeguro() {
        int nivel = (actividadActual != null) ? actividadActual.getNivel() : 1;
        if (nivel < 1) nivel = 1;
        if (nivel > 5) nivel = 5;
        return nivel;
    }

    private int maxNumeroPorNivel(int nivel) {
        switch (nivel) {
            case 1: return 3;
            case 2: return 5;
            case 3: return 9;
            case 4: return 12;
            default: return 15;
        }
    }

    private List<Integer> generarOpciones(int correcto, int max) {
        Set<Integer> set = new HashSet<>();
        set.add(correcto);

        while (set.size() < NUM_OPCIONES) {
            set.add(random.nextInt(max) + 1);
        }

        List<Integer> lista = new ArrayList<>(set);
        for (int i = lista.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, tmp);
        }
        return lista;
    }

    // -------------------- Lienzo --------------------
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

                Color borde = AccesibleUI.BORDE_ACTIVO;
                g2.setStroke(new BasicStroke(AccesibleUI.STROKE_GRUESO));

                for (Point c : centros) {
                    int cx = c.x;
                    int cy = c.y;

                    int s = (int) Math.round(size * getPulsoScale());
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
                int gap = 30;
                int disponibleW = Math.max(1, w - (2 * padding) - ((n - 1) * gap));
                int s = disponibleW / n;
                s = Math.min(s, h - (2 * padding));
                return clamp(s, 90, 170);
            }

            if (nivel >= 2 && nivel <= 4) {
                int rows = (nivel == 2) ? 2 : 3;
                int cols = (nivel == 2) ? 3 : (nivel == 3 ? 3 : 4);

                int cellW = Math.max(1, (w - 2 * padding) / cols);
                int cellH = Math.max(1, (h - 2 * padding) / rows);

                int s = (int) (Math.min(cellW, cellH) * 0.70);
                return clamp(s, 60, 140);
            }

            double area = (double) (Math.max(1, w - 2 * padding)) * (Math.max(1, h - 2 * padding));
            int s = (int) Math.sqrt(area / (n * 8.0));
            return clamp(s, 45, 95);
        }

        private List<Point> obtenerCentros(int n, int nivel, int w, int h, int size) {
            Dimension d = new Dimension(w, h);
            if (centrosCache != null && d.equals(dimensionCache)) return centrosCache;

            List<Point> centros;
            if (nivel == 1) centros = centrosLinea(n, w, h, size);
            else if (nivel == 2) centros = centrosGrid(n, w, h, 2, 3);
            else if (nivel == 3) centros = centrosGrid(n, w, h, 3, 3);
            else if (nivel == 4) centros = centrosGrid(n, w, h, 3, 4);
            else centros = centrosNube(n, w, h, size);

            centrosCache = centros;
            dimensionCache = d;
            return centrosCache;
        }

        private List<Point> centrosLinea(int n, int w, int h, int size) {
            int gap = 30;
            int totalW = n * size + (n - 1) * gap;
            int startX = (w - totalW) / 2 + size / 2;
            int y = h / 2;

            List<Point> pts = new ArrayList<>();
            for (int i = 0; i < n; i++) pts.add(new Point(startX + i * (size + gap), y));
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

            int minX = padding, maxX = Math.max(minX + 1, w - padding);
            int minY = padding, maxY = Math.max(minY + 1, h - padding);

            while (pts.size() < n && tries < 5000) {
                int x = minX + random.nextInt(Math.max(1, maxX - minX));
                int y = minY + random.nextInt(Math.max(1, maxY - minY));

                boolean ok = true;
                for (Point q : pts) {
                    int dx = x - q.x;
                    int dy = y - q.y;
                    if (dx * dx + dy * dy < minDist * minDist) { ok = false; break; }
                }
                if (ok) pts.add(new Point(x, y));
                tries++;
            }

            if (pts.size() < n) return centrosGrid(n, w, h, 3, 5); // fallback (hasta 15)
            return pts;
        }

        private Polygon crearEstrella(int cx, int cy, int rOuter, int rInner, int puntas) {
            Polygon p = new Polygon();
            double ang = -Math.PI / 2;
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

    // -------------------- Botón circular con fade --------------------
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

        int getValor() { return valor; }

        void reset(int nuevoValor) {
            valor = nuevoValor;
            alpha = 1f;
            setEnabled(true);
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            repaint();
        }

        void stopAnimations() {
            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            fadeTimer = null;
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
                Color border = isEnabled() ? AccesibleUI.BORDE_ACTIVO : AccesibleUI.BORDE_INACTIVO;
                Color text = isEnabled() ? new Color(30, 30, 30) : new Color(120, 120, 120);

                g2.setColor(fill);
                g2.fillOval(x, y, d, d);

                g2.setStroke(new BasicStroke(AccesibleUI.STROKE_GRUESO));
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
