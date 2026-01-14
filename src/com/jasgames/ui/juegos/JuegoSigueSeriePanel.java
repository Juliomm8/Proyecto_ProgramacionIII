package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import com.jasgames.ui.juegos.framework.AccesibleUI;
import com.jasgames.ui.juegos.framework.JuegoRondasPanel;
import com.jasgames.ui.juegos.framework.Paletas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Juego 3: "Sigue la Serie"
 *
 * Reglas:
 * - Hueco SIEMPRE al final.
 * - Ficha = Forma + Color (unidad indivisible).
 * - Nivel 1-2: 4 casillas / patrón ABAB -> falta B.
 * - Nivel 3-4: 5 casillas / patrón A A B A -> falta A.
 * - Nivel 5:   5 casillas / patrón A B C A -> falta B.
 * - 3 opciones abajo, errores se apagan (fade) y se deshabilitan.
 * - Acierto: la ficha viaja al hueco (siempre animado) y luego pulso.
 * - 5 rondas completadas = 100 puntos.
 */
public class JuegoSigueSeriePanel extends JuegoRondasPanel {

    private static final int RONDAS_META = 5;
    private static final int NUM_OPCIONES = 3;

    private final Random random = new Random();
    private final Color[] paletaSolida = Paletas.coloresSolidos();

    private LienzoSerie lienzo;
    private JPanel panelRespuestas;
    private OpcionFichaButton[] botones;

    // Ronda
    private Ficha[] secuenciaVisible;     // contiene null en la casilla faltante
    private int idxFaltante;              // siempre última
    private Ficha respuestaCorrecta;

    // Layout cache
    private Dimension dimensionCache;
    private Point[] centrosSlots;
    private int slotSize;

    // Animación de viaje
    private Timer animTimer;
    private boolean animando;
    private boolean mostrarRespuesta;
    private Ficha animFicha;
    private Point animStart;
    private Point animEnd;
    private double animT; // 0..1

    private enum Forma { CIRCULO, CUADRADO, TRIANGULO, ESTRELLA }

    private static final class Ficha {
        final Forma forma;
        final Color color;

        Ficha(Forma forma, Color color) {
            this.forma = forma;
            this.color = color;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ficha)) return false;
            Ficha ficha = (Ficha) o;
            return forma == ficha.forma && Objects.equals(color, ficha.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(forma, color);
        }
    }

    public JuegoSigueSeriePanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);

        setInstruccion("Completa la serie: toca la figura que falta");

        lienzo = new LienzoSerie();
        lienzo.setOpaque(false);
        setTablero(lienzo);

        panelRespuestas = construirPanelRespuestas();
        setPanelRespuestas(panelRespuestas);
        // NO llamar iniciarJuego() aquí (se inicia desde EstudianteWindow)
    }

    private JPanel construirPanelRespuestas() {
        JPanel p = new JPanel(new GridLayout(1, NUM_OPCIONES, 18, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 40, 0, 40));

        botones = new OpcionFichaButton[NUM_OPCIONES];
        for (int i = 0; i < NUM_OPCIONES; i++) {
            OpcionFichaButton btn = new OpcionFichaButton();
            btn.addActionListener(this::onOpcionClick);
            botones[i] = btn;
            p.add(btn);
        }
        return p;
    }

    @Override
    protected void onAntesDeIniciar() {
        setBloqueado(false);
        dimensionCache = null;
        centrosSlots = null;
        slotSize = 0;

        detenerAnimacion();
        animando = false;
        mostrarRespuesta = false;
        animFicha = null;
        animStart = null;
        animEnd = null;
        animT = 0;

        setFeedback(" ");
    }

    @Override
    protected void onAntesDeSalir() {
        detenerAnimacion();
        if (botones != null) {
            for (OpcionFichaButton b : botones) {
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
    protected void aplicarPistaSuave() {
        // Pista suave: apaga 1 distractor adicional
        if (botones == null || respuestaCorrecta == null) return;

        java.util.List<OpcionFichaButton> candidatos = new java.util.ArrayList<>();
        for (OpcionFichaButton b : botones) {
            if (b != null && b.isEnabled()) {
                Ficha f = b.getFicha();
                if (f != null && !f.equals(respuestaCorrecta)) candidatos.add(b);
            }
        }
        if (candidatos.isEmpty()) return;

        OpcionFichaButton elegido = candidatos.get(random.nextInt(candidatos.size()));
        elegido.fadeOutAndDisable();
    }


    @Override
    protected void prepararNuevaRonda() {
        setBloqueado(false);
        setFeedback(" ");

        detenerAnimacion();
        animando = false;
        mostrarRespuesta = false;
        animFicha = null;
        animStart = null;
        animEnd = null;
        animT = 0;

        for (OpcionFichaButton b : botones) {
            b.setEnabled(true);
            b.resetAlpha();
        }

        int nivel = getNivelSeguro();
        int tipo = tipoSeriePorNivel(nivel);

        generarSerie(tipo);
        asignarOpciones();

        dimensionCache = null;
        lienzo.repaint();
    }

    private void onOpcionClick(ActionEvent e) {
        if (isBloqueado() || animando) return;

        OpcionFichaButton btn = (OpcionFichaButton) e.getSource();
        Ficha valor = btn.getFicha();
        if (valor == null) return;

        if (valor.equals(respuestaCorrecta)) {
            for (OpcionFichaButton b : botones) b.setEnabled(false);
            setFeedback("¡Muy bien!");
            iniciarAnimacionViaje(btn);
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

    private int tipoSeriePorNivel(int nivel) {
        if (nivel <= 2) return 1; // ABAB (4)
        if (nivel <= 4) return 2; // A A B A (5)
        return 3;                 // A B C A (5)
    }

    private void generarSerie(int tipo) {
        // Contraste fuerte: formas distintas + colores distintos
        List<Forma> formas = new ArrayList<>();
        Collections.addAll(formas, Forma.values());
        Collections.shuffle(formas, random);

        Color c1 = paletaSolida[random.nextInt(paletaSolida.length)];
        Color c2 = colorDistinto(c1);
        Color c3 = colorDistinto(c1, c2);

        Ficha A = new Ficha(formas.get(0), c1);
        Ficha B = new Ficha(formas.get(1), c2);
        Ficha C = (tipo == 3) ? new Ficha(formas.get(2), c3) : null;

        Ficha[] completa;
        if (tipo == 1) {
            completa = new Ficha[]{A, B, A, B};
        } else if (tipo == 2) {
            completa = new Ficha[]{A, A, B, A, A};
        } else {
            completa = new Ficha[]{A, B, C, A, B};
        }

        idxFaltante = completa.length - 1; // SIEMPRE al final
        respuestaCorrecta = completa[idxFaltante];

        secuenciaVisible = new Ficha[completa.length];
        for (int i = 0; i < completa.length; i++) {
            secuenciaVisible[i] = (i == idxFaltante) ? null : completa[i];
        }
    }

    private void asignarOpciones() {
        LinkedHashSet<Ficha> set = new LinkedHashSet<>();
        set.add(respuestaCorrecta);

        for (Ficha f : secuenciaVisible) {
            if (f != null && set.size() < NUM_OPCIONES) set.add(f);
        }

        while (set.size() < NUM_OPCIONES) {
            set.add(fichaAleatoriaNoEn(set));
        }

        List<Ficha> lista = new ArrayList<>(set);
        Collections.shuffle(lista, random);

        for (int i = 0; i < NUM_OPCIONES; i++) {
            botones[i].reset(lista.get(i));
        }
    }

    private Ficha fichaAleatoriaNoEn(Set<Ficha> existentes) {
        for (int tries = 0; tries < 500; tries++) {
            Forma forma = Forma.values()[random.nextInt(Forma.values().length)];
            Color color = paletaSolida[random.nextInt(paletaSolida.length)];
            Ficha f = new Ficha(forma, color);
            if (!existentes.contains(f)) return f;
        }
        return new Ficha(Forma.CIRCULO, Color.BLACK);
    }

    private Color colorDistinto(Color... usados) {
        for (int tries = 0; tries < 100; tries++) {
            Color c = paletaSolida[random.nextInt(paletaSolida.length)];
            boolean ok = true;
            for (Color u : usados) {
                if (Objects.equals(u, c)) { ok = false; break; }
            }
            if (ok) return c;
        }
        return paletaSolida[0];
    }

    // -------------------- Animación --------------------

    private void iniciarAnimacionViaje(OpcionFichaButton origen) {
        asegurarLayoutSlots();
        if (centrosSlots == null || centrosSlots.length == 0) {
            mostrarRespuesta = true;
            lienzo.repaint();
            marcarAciertoConPulso(lienzo, this::resetPostAcierto);
            return;
        }

        Point destino = centrosSlots[idxFaltante];
        Point startLocal = SwingUtilities.convertPoint(
                origen,
                new Point(origen.getWidth() / 2, origen.getHeight() / 2),
                lienzo
        );

        animando = true;
        animFicha = respuestaCorrecta;
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
                mostrarRespuesta = true;
                lienzo.repaint();

                marcarAciertoConPulso(lienzo, this::resetPostAcierto);
                return;
            }
            lienzo.repaint();
        });
        animTimer.start();
    }

    private void resetPostAcierto() {
        mostrarRespuesta = false;
        animFicha = null;
        animStart = null;
        animEnd = null;
        animT = 0;
        detenerAnimacion();
    }

    private void detenerAnimacion() {
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = null;
    }

    // -------------------- Layout --------------------

    private void asegurarLayoutSlots() {
        Dimension d = new Dimension(lienzo.getWidth(), lienzo.getHeight());
        if (dimensionCache == null || !dimensionCache.equals(d)) {
            calcularLayoutSlots(d.width, d.height);
            dimensionCache = d;
        }
    }

    private void calcularLayoutSlots(int w, int h) {
        if (secuenciaVisible == null || secuenciaVisible.length == 0) {
            centrosSlots = null;
            slotSize = 0;
            return;
        }

        int n = secuenciaVisible.length;
        int margin = 60;
        int gap = 30;

        int disponibleW = Math.max(1, w - 2 * margin - (n - 1) * gap);
        int s = disponibleW / n;
        s = Math.min(s, Math.max(120, h / 3));
        slotSize = clamp(s, 90, 150);

        int totalW = n * slotSize + (n - 1) * gap;
        int startX = (w - totalW) / 2;
        int cy = h / 2;

        centrosSlots = new Point[n];
        for (int i = 0; i < n; i++) {
            int cx = startX + i * (slotSize + gap) + slotSize / 2;
            centrosSlots[i] = new Point(cx, cy);
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // -------------------- Lienzo --------------------

    private class LienzoSerie extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (secuenciaVisible == null || secuenciaVisible.length == 0) return;

            asegurarLayoutSlots();
            if (centrosSlots == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(210, 210, 210));

                for (int i = 0; i < centrosSlots.length - 1; i++) {
                    Point a = centrosSlots[i];
                    Point b = centrosSlots[i + 1];
                    g2.drawLine(a.x, a.y, b.x, b.y);
                    dibujarFlecha(g2, a, b);
                }

                for (int i = 0; i < centrosSlots.length; i++) {
                    Point c = centrosSlots[i];
                    int x = c.x - slotSize / 2;
                    int y = c.y - slotSize / 2;

                    int arc = Math.max(16, slotSize / 5);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(x, y, slotSize, slotSize, arc, arc);

                    g2.setColor(AccesibleUI.TABLERO_BORDE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(x, y, slotSize, slotSize, arc, arc);

                    if (i == idxFaltante) {
                        if (mostrarRespuesta) {
                            double s = getPulsoScale();
                            dibujarFicha(g2, respuestaCorrecta, c.x, c.y, (int) Math.round(slotSize * 0.72 * s));
                        } else {
                            g2.setColor(AccesibleUI.TEXTO_NEUTRO);
                            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(34, slotSize / 3)));
                            FontMetrics fm = g2.getFontMetrics();
                            String t = "?";
                            int tx = c.x - fm.stringWidth(t) / 2;
                            int ty = c.y + fm.getAscent() / 2 - 6;
                            g2.drawString(t, tx, ty);
                        }
                    } else {
                        Ficha f = secuenciaVisible[i];
                        if (f != null) dibujarFicha(g2, f, c.x, c.y, (int) Math.round(slotSize * 0.72));
                    }
                }

                if (animando && animFicha != null && animStart != null && animEnd != null) {
                    double t = easeOut(animT);
                    int cx = (int) Math.round(animStart.x + (animEnd.x - animStart.x) * t);
                    int cy = (int) Math.round(animStart.y + (animEnd.y - animStart.y) * t);
                    int s = (int) Math.round(slotSize * 0.64);
                    dibujarFicha(g2, animFicha, cx, cy, s);
                }

            } finally {
                g2.dispose();
            }
        }

        private double easeOut(double t) {
            double u = 1 - t;
            return 1 - u * u * u;
        }

        private void dibujarFlecha(Graphics2D g2, Point a, Point b) {
            double ang = Math.atan2(b.y - a.y, b.x - a.x);
            int len = 12;
            int back = 18;

            int px = b.x - (int) Math.round(Math.cos(ang) * back);
            int py = b.y - (int) Math.round(Math.sin(ang) * back);

            int x1 = px + (int) Math.round(Math.cos(ang + Math.PI * 0.75) * len);
            int y1 = py + (int) Math.round(Math.sin(ang + Math.PI * 0.75) * len);
            int x2 = px + (int) Math.round(Math.cos(ang - Math.PI * 0.75) * len);
            int y2 = py + (int) Math.round(Math.sin(ang - Math.PI * 0.75) * len);

            Polygon p = new Polygon();
            p.addPoint(px, py);
            p.addPoint(x1, y1);
            p.addPoint(x2, y2);
            g2.fillPolygon(p);
        }

        private void dibujarFicha(Graphics2D g2, Ficha ficha, int cx, int cy, int size) {
            if (ficha == null) return;
            int s = Math.max(40, size);
            int x = cx - s / 2;
            int y = cy - s / 2;

            g2.setStroke(new BasicStroke(AccesibleUI.STROKE_GRUESO));
            g2.setColor(ficha.color);
            Color borde = AccesibleUI.BORDE_ACTIVO;

            switch (ficha.forma) {
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
                case TRIANGULO -> {
                    Polygon tri = new Polygon();
                    tri.addPoint(cx, cy - s / 2);
                    tri.addPoint(cx - s / 2, cy + s / 2);
                    tri.addPoint(cx + s / 2, cy + s / 2);
                    g2.fillPolygon(tri);
                    g2.setColor(borde);
                    g2.drawPolygon(tri);
                }
                case ESTRELLA -> {
                    Polygon star = crearEstrella(cx, cy, s / 2, Math.max(10, s / 4), 5);
                    g2.fillPolygon(star);
                    g2.setColor(borde);
                    g2.drawPolygon(star);
                }
            }
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
    }

    // -------------------- Botón opción (ficha) --------------------

    private static class OpcionFichaButton extends JButton {
        private Ficha ficha;
        private float alpha = 1f;
        private Timer fadeTimer;

        OpcionFichaButton() {
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setPreferredSize(new Dimension(180, 150));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        Ficha getFicha() { return ficha; }

        void reset(Ficha f) {
            ficha = f;
            setEnabled(true);
            resetAlpha();
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
                int arc = 26;

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setStroke(new BasicStroke(2f));
                g2.setColor(isEnabled() ? AccesibleUI.BORDE_ACTIVO : AccesibleUI.BORDE_INACTIVO);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                if (ficha != null) {
                    int size = (int) (Math.min(w, h) * 0.55);
                    int cx = w / 2;
                    int cy = h / 2;

                    g2.setStroke(new BasicStroke(AccesibleUI.STROKE_GRUESO));
                    g2.setColor(ficha.color);
                    Color borde = AccesibleUI.BORDE_ACTIVO;

                    int x = cx - size / 2;
                    int y = cy - size / 2;

                    switch (ficha.forma) {
                        case CIRCULO -> {
                            g2.fillOval(x, y, size, size);
                            g2.setColor(borde);
                            g2.drawOval(x, y, size, size);
                        }
                        case CUADRADO -> {
                            int a = Math.max(10, size / 6);
                            g2.fillRoundRect(x, y, size, size, a, a);
                            g2.setColor(borde);
                            g2.drawRoundRect(x, y, size, size, a, a);
                        }
                        case TRIANGULO -> {
                            Polygon tri = new Polygon();
                            tri.addPoint(cx, cy - size / 2);
                            tri.addPoint(cx - size / 2, cy + size / 2);
                            tri.addPoint(cx + size / 2, cy + size / 2);
                            g2.fillPolygon(tri);
                            g2.setColor(borde);
                            g2.drawPolygon(tri);
                        }
                        case ESTRELLA -> {
                            Polygon star = crearEstrella(cx, cy, size / 2, Math.max(10, size / 4), 5);
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
    }
}