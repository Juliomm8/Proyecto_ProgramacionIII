package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import com.jasgames.ui.juegos.framework.JuegoRondasPanel;
import com.jasgames.ui.juegos.framework.AccesibleUI;
import com.jasgames.ui.juegos.framework.Paletas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Minijuego 1: Discriminación de Colores (clickeable en lienzo)
 *
 * - Objetivo visual: se muestra un círculo grande del color objetivo (arriba).
 * - Opciones: círculos clickeables en el lienzo (abajo).
 * - Error: NO cambia la ronda; el círculo incorrecto se apaga (fade + disable).
 * - Acierto: pulso/latido 2 veces y pasa a la siguiente ronda.
 * - Termina al completar 5 rondas correctas. Puntaje final fijo: 100.
 */
public class JuegoColoresPanel extends JuegoRondasPanel {

    private static final int RONDAS_META = 5;

    private final Random random = new Random();

    private LienzoColores lienzo;

    private List<Paletas.ColorNombre> paletaCompleta;

    private List<OpcionColor> opciones;
    private OpcionColor objetivo;

    private Dimension dimensionCache;

    // Para pulso: queremos resaltar solo el objetivo + el correcto
    private OpcionColor ultimoCorrecto;

    public JuegoColoresPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);

        setInstruccion("Toca el círculo que coincide con el color objetivo");

        paletaCompleta = Paletas.coloresConNombre();

        lienzo = new LienzoColores();
        lienzo.setOpaque(false);
        setTablero(lienzo);

        // No ponemos panel de respuestas (para que no parezca el juego 2)
        JPanel vacio = new JPanel();
        vacio.setOpaque(false);
        vacio.setPreferredSize(new Dimension(10, 10));
        setPanelRespuestas(vacio);

        iniciarJuego();
    }

    @Override
    protected void onAntesDeIniciar() {
        setFeedback(" ");
        opciones = new ArrayList<>();
        objetivo = null;
        ultimoCorrecto = null;
        dimensionCache = null;
    }

    @Override
    protected void onAntesDeSalir() {
        if (opciones != null) {
            for (OpcionColor op : opciones) {
                if (op != null && op.fadeTimer != null && op.fadeTimer.isRunning()) op.fadeTimer.stop();
            }
        }
    }

    @Override
    protected int getRondasMetaPorNivel(int nivel) {
        return RONDAS_META; // siempre 5 rondas
    }

    @Override
    protected int calcularPuntosFinales() {
        return 100; // fijo: completó = 100
    }

    @Override
    protected void prepararNuevaRonda() {
        setBloqueado(false);
        setFeedback(" ");

        int nivel = getNivelSeguro();
        int cantidadOpciones = cantidadOpcionesPorNivel(nivel);

        // Elegir subconjunto de paleta por nivel (evita saturación en niveles bajos)
        List<Paletas.ColorNombre> base = paletaPorNivel(nivel);

        // Seleccionar opciones únicas
        Collections.shuffle(base, random);
        List<Paletas.ColorNombre> seleccion = base.subList(0, Math.min(cantidadOpciones, base.size()));

        for (OpcionColor op : opciones) {
            if (op.fadeTimer != null && op.fadeTimer.isRunning()) op.fadeTimer.stop();
        }

        opciones.clear();
        for (Paletas.ColorNombre item : seleccion) {
            opciones.add(new OpcionColor(item));
        }

        // Elegir objetivo entre las opciones
        objetivo = opciones.get(random.nextInt(opciones.size()));
        ultimoCorrecto = null;

        // Forzar recálculo de layout
        dimensionCache = null;
        lienzo.repaint();
    }

    private int getNivelSeguro() {
        int nivel = (actividadActual != null) ? actividadActual.getNivel() : 1;
        if (nivel < 1) nivel = 1;
        if (nivel > 5) nivel = 5;
        return nivel;
    }

    private int cantidadOpcionesPorNivel(int nivel) {
        // Mantenerlo simple y escalable
        switch (nivel) {
            case 1: return 3;
            case 2: return 4;
            case 3: return 5;
            case 4: return 6;
            default: return 8; // nivel 5
        }
    }

    private List<Paletas.ColorNombre> paletaPorNivel(int nivel) {
        // Define una curva suave: pocos colores al inicio, más después.
        // Puedes ajustar si quieres.
        List<Paletas.ColorNombre> p = new ArrayList<>(paletaCompleta);

        if (nivel == 1) {
            return Arrays.asList(
                    encontrar("Rojo"), encontrar("Azul"), encontrar("Verde")
            );
        }
        if (nivel == 2) {
            return Arrays.asList(
                    encontrar("Rojo"), encontrar("Azul"), encontrar("Verde"), encontrar("Amarillo")
            );
        }
        if (nivel == 3) {
            return Arrays.asList(
                    encontrar("Rojo"), encontrar("Azul"), encontrar("Verde"),
                    encontrar("Amarillo"), encontrar("Naranja")
            );
        }
        if (nivel == 4) {
            return Arrays.asList(
                    encontrar("Rojo"), encontrar("Azul"), encontrar("Verde"),
                    encontrar("Amarillo"), encontrar("Naranja"), encontrar("Morado")
            );
        }
        // nivel 5: toda la paleta
        return p;
    }

    private Paletas.ColorNombre encontrar(String nombre) {
        for (Paletas.ColorNombre c : paletaCompleta) {
            if (c.nombre.equalsIgnoreCase(nombre)) return c;
        }
        return paletaCompleta.get(0);
    }

    // ---------------------------------------------------------------------
    // Modelo interno
    // ---------------------------------------------------------------------
    private static class OpcionColor {
        final Paletas.ColorNombre item;
        boolean enabled = true;
        float alpha = 1f;

        // Layout
        int cx, cy, r;

        // Fade timer
        Timer fadeTimer;

        OpcionColor(Paletas.ColorNombre item) {
            this.item = item;
        }

        boolean contains(int x, int y) {
            int dx = x - cx;
            int dy = y - cy;
            return dx * dx + dy * dy <= r * r;
        }

        void fadeOutAndDisable(JComponent repaintTarget) {
            if (!enabled) return;
            enabled = false;

            if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
            alpha = 1f;

            fadeTimer = new Timer(35, e -> {
                alpha -= 0.08f;
                if (alpha <= 0.25f) {
                    alpha = 0.25f;
                    ((Timer) e.getSource()).stop();
                }
                if (repaintTarget != null) repaintTarget.repaint();
            });
            fadeTimer.start();
        }
    }

    // ---------------------------------------------------------------------
    // Lienzo: muestra objetivo + opciones clickeables
    // ---------------------------------------------------------------------
    private class LienzoColores extends JPanel {

        LienzoColores() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isBloqueado()) return;

                    Dimension d = new Dimension(LienzoColores.this.getWidth(), LienzoColores.this.getHeight());
                    if (dimensionCache == null || !dimensionCache.equals(d)) {
                        layoutOpciones(d.width, d.height);
                        dimensionCache = d;
                    }

                    if (objetivo == null || opciones == null || opciones.isEmpty()) return;

                    Point p = e.getPoint();

                    OpcionColor click = null;
                    for (OpcionColor op : opciones) {
                        if (op.enabled && op.contains(p.x, p.y)) {
                            click = op;
                            break;
                        }
                    }
                    if (click == null) return;

                    if (click == objetivo) {
                        // NO hacer setBloqueado(true) aquí (el framework lo hace)
                        // NO deshabilitar opciones aquí (el bloqueo del framework evita multi-click)
                        ultimoCorrecto = click;

                        marcarAciertoConPulso(LienzoColores.this, null);
                    } else {
                        marcarErrorNeutro("Intenta de nuevo");
                        click.fadeOutAndDisable(LienzoColores.this);
                    }
                }
            });

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (objetivo == null || opciones == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                // Recalcular layout si cambia tamaño
                Dimension d = new Dimension(w, h);
                if (dimensionCache == null || !dimensionCache.equals(d)) {
                    layoutOpciones(w, h);
                    dimensionCache = d;
                }

                // Fondo claro
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, w, h);

                // Zonas: objetivo arriba, opciones abajo
                int topH = (int) (h * 0.34);
                int bottomY = topH;

                // Texto corto (no depender de lectura)
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                g2.setColor(new Color(90, 90, 90));
                String t = "Busca este color:";
                int tw = g2.getFontMetrics().stringWidth(t);
                g2.drawString(t, (w - tw) / 2, 26);

                // Dibujo del objetivo: círculo grande centrado
                int targetR = Math.min(w, topH) / 4;
                targetR = clamp(targetR, 55, 95);

                int targetCx = w / 2;
                int targetCy = topH / 2 + 10;

                boolean pulsoActivo = (getPulsoScale() != 1.0);
                boolean resaltarTarget = pulsoActivo; // pulso al acertar

                int tr = resaltarTarget ? (int) Math.round(targetR * getPulsoScale()) : targetR;

                dibujarCirculo(g2, objetivo.item.color, targetCx, targetCy, tr, 1f, true);

                // Opciones: círculos clickeables
                for (OpcionColor op : opciones) {
                    boolean resaltar = pulsoActivo && (op == ultimoCorrecto);
                    int r = resaltar ? (int) Math.round(op.r * getPulsoScale()) : op.r;

                    dibujarCirculo(g2, op.item.color, op.cx, op.cy, r, op.alpha, op.enabled);
                }

                // Línea sutil separando zonas
                g2.setColor(new Color(235, 235, 235));
                g2.fillRect(0, bottomY, w, 2);

            } finally {
                g2.dispose();
            }
        }

        private void layoutOpciones(int w, int h) {
            int nivel = getNivelSeguro();
            int n = opciones.size();

            int topH = (int) (h * 0.34);
            int availableH = h - topH;

            // Decidir grid según nivel para que siempre sea limpio
            int rows, cols;
            switch (nivel) {
                case 1:
                    rows = 1; cols = 3;
                    break;
                case 2:
                    rows = 2; cols = 2;
                    break;
                case 3:
                    rows = 2; cols = 3;
                    break;
                case 4:
                    rows = 2; cols = 3;
                    break;
                default:
                    rows = 2; cols = 4; // 8 opciones
            }

            // Padding y tamaño de celda
            int paddingX = 50;
            int paddingY = 35;

            int gridW = Math.max(1, w - 2 * paddingX);
            int gridH = Math.max(1, availableH - 2 * paddingY);

            int cellW = gridW / cols;
            int cellH = gridH / rows;

            int r = (int) (Math.min(cellW, cellH) * 0.33);
            r = clamp(r, 45, 85);

            // Generar posiciones en orden, centrando si sobran celdas
            List<Point> puntos = new ArrayList<>();
            for (int rr = 0; rr < rows; rr++) {
                for (int cc = 0; cc < cols; cc++) {
                    int cx = paddingX + cc * cellW + cellW / 2;
                    int cy = topH + paddingY + rr * cellH + cellH / 2;
                    puntos.add(new Point(cx, cy));
                }
            }

            // Si hay más celdas que opciones, escoger posiciones centradas (las del medio)
            // Esto evita que 5 opciones queden “esquinadas”.
            List<Point> usados = seleccionarPuntosCentrados(puntos, n);

            for (int i = 0; i < opciones.size(); i++) {
                OpcionColor op = opciones.get(i);
                Point p = usados.get(i);
                op.cx = p.x;
                op.cy = p.y;
                op.r = r;
            }
        }

        private List<Point> seleccionarPuntosCentrados(List<Point> all, int n) {
            if (all.size() == n) return all;

            // Ordenar por cercanía al centro
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            List<Point> sorted = new ArrayList<>(all);
            sorted.sort(Comparator.comparingInt(p -> dist2(p, center)));

            List<Point> picked = sorted.subList(0, n);
            // Mantener orden estable para que no se sienta “random” el layout
            return new ArrayList<>(picked);
        }

        private int dist2(Point a, Point b) {
            int dx = a.x - b.x;
            int dy = a.y - b.y;
            return dx * dx + dy * dy;
        }

        private void dibujarCirculo(Graphics2D g2, Color fill, int cx, int cy, int r, float alpha, boolean enabled) {
            int x = cx - r;
            int y = cy - r;
            int d = r * 2;

            // alpha
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // relleno sólido
            g2.setColor(fill);
            g2.fillOval(x, y, d, d);

            // borde
            g2.setStroke(new BasicStroke(AccesibleUI.STROKE_GRUESO));
            g2.setColor(enabled ? AccesibleUI.BORDE_ACTIVO : AccesibleUI.BORDE_INACTIVO);
            g2.drawOval(x + 2, y + 2, d - 4, d - 4);

            g2.setComposite(old);
        }

        private int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
    }
}
