package com.jasgames.ui.juegos.framework;

import com.jasgames.model.Actividad;
import com.jasgames.ui.juegos.BaseJuegoPanel;
import com.jasgames.ui.juegos.JuegoListener;

import javax.swing.*;
import java.awt.*;

/**
 * Plantilla base "pro" para minijuegos por rondas.
 *
 * Qué resuelve:
 * - UI consistente (instrucción, progreso, feedback)
 * - Control de rondas (meta, aciertos, errores)
 * - Bloqueo de entrada (cuando hay animación)
 * - Animación estándar de pulso (latido) reutilizable
 *
 * Cómo se usa:
 * 1) En el constructor del hijo:
 *    - setInstruccion(...)
 *    - setTablero(...)
 *    - setPanelRespuestas(...)
 * 2) Implementa:
 *    - prepararNuevaRonda()
 *    - calcularPuntosFinales() (opcional)
 * 3) En acierto:
 *    - marcarAciertoConPulso(repaintTarget, antesDeSiguienteRonda)
 * 4) En error:
 *    - marcarErrorNeutro("Intenta de nuevo")
 */
public abstract class JuegoRondasPanel extends BaseJuegoPanel {

    private JPanel root;
    private JLabel lblTitulo;
    private JLabel lblProgreso;
    private JLabel lblFeedback;
    private JPanel hostTablero;
    private JPanel hostRespuestas;

    protected int rondasCorrectas;
    protected int erroresTotales;
    protected boolean bloqueado;

    /** Factor de escala para animación de pulso (úsalo en paint). */
    private double pulsoScale = 1.0;
    private Timer timerPulso;

    protected JuegoRondasPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);
        initTemplateUI();
    }

    private void initTemplateUI() {
        root = new JPanel(new BorderLayout(12, 12));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(root, BorderLayout.CENTER);

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        top.setOpaque(false);

        lblTitulo = AccesibleUI.crearLabelCentrado("", AccesibleUI.FONT_TITULO);
        lblProgreso = AccesibleUI.crearLabelCentrado("", AccesibleUI.FONT_PROGRESO);
        lblFeedback = AccesibleUI.crearLabelCentrado(" ", AccesibleUI.FONT_FEEDBACK);

        lblProgreso.setForeground(AccesibleUI.TEXTO_NEUTRO);
        lblFeedback.setForeground(AccesibleUI.TEXTO_NEUTRO);

        top.add(lblTitulo);
        top.add(lblProgreso);
        top.add(lblFeedback);

        root.add(top, BorderLayout.NORTH);

        hostTablero = new JPanel(new BorderLayout());
        hostTablero.setOpaque(false);
        root.add(hostTablero, BorderLayout.CENTER);

        hostRespuestas = new JPanel(new BorderLayout());
        hostRespuestas.setOpaque(false);
        root.add(hostRespuestas, BorderLayout.SOUTH);
    }

    // -------------------- API del template --------------------

    protected final void setInstruccion(String texto) {
        lblTitulo.setText(texto == null ? "" : texto);
    }

    protected final void setFeedback(String texto) {
        lblFeedback.setText(texto == null || texto.isBlank() ? " " : texto);
    }

    protected final void setTablero(JComponent componente) {
        hostTablero.removeAll();
        if (componente != null) {
            hostTablero.add(AccesibleUI.crearContenedorTablero(componente), BorderLayout.CENTER);
        }
        hostTablero.revalidate();
        hostTablero.repaint();
    }

    protected final void setPanelRespuestas(JComponent componente) {
        hostRespuestas.removeAll();
        if (componente != null) {
            hostRespuestas.add(componente, BorderLayout.CENTER);
        }
        hostRespuestas.revalidate();
        hostRespuestas.repaint();
    }

    protected final void setBloqueado(boolean value) {
        this.bloqueado = value;
    }

    protected final boolean isBloqueado() {
        return bloqueado;
    }

    /** Default: 5 rondas para todos los niveles (puedes sobrescribir getRondasMetaPorNivel). */
    protected final int getRondasMeta() {
        int nivel = (actividadActual != null) ? actividadActual.getNivel() : 1;
        return getRondasMetaPorNivel(clamp(nivel, 1, 5));
    }

    protected int getRondasMetaPorNivel(int nivel) {
        return 5;
    }

    protected final double getPulsoScale() {
        return pulsoScale;
    }

    // -------------------- Ciclo de vida --------------------

    @Override
    public final void iniciarJuego() {
        detenerPulso();
        pulsoScale = 1.0;

        rondasCorrectas = 0;
        erroresTotales = 0;
        bloqueado = false;

        setFeedback(" ");
        actualizarProgreso();

        onAntesDeIniciar();
        prepararNuevaRonda();
    }

    /** Hook opcional para resetear estados internos en el hijo. */
    protected void onAntesDeIniciar() {}

    /** El hijo define cómo se prepara cada ronda. */
    protected abstract void prepararNuevaRonda();

    /** Default: rondasCorrectas (si quieres fijo, sobrescribe). */
    protected int calcularPuntosFinales() {
        return rondasCorrectas;
    }

    // -------------------- Helpers (acierto/error) --------------------

    protected final void marcarAciertoConPulso(JComponent repaintTarget, Runnable antesDeSiguienteRonda) {
        if (bloqueado) return;
        bloqueado = true;

        setFeedback("¡Muy bien!");

        reproducirPulso(repaintTarget, () -> {
            rondasCorrectas++;
            actualizarProgreso();

            if (rondasCorrectas >= getRondasMeta()) {
                finalizarJuego(calcularPuntosFinales());
                return;
            }

            if (antesDeSiguienteRonda != null) antesDeSiguienteRonda.run();
            bloqueado = false;
            prepararNuevaRonda();
        });
    }

    protected final void marcarErrorNeutro(String feedback) {
        erroresTotales++;
        setFeedback((feedback == null || feedback.isBlank()) ? "Intenta de nuevo" : feedback);
    }

    protected final void actualizarProgreso() {
        int meta = getRondasMeta();
        int mostrada = Math.min(rondasCorrectas + 1, meta);
        lblProgreso.setText("Ronda " + mostrada + "/" + meta);
    }

    // -------------------- Animación pulso --------------------

    protected final void reproducirPulso(JComponent repaintTarget, Runnable onFinish) {
        detenerPulso();

        final double[] seq = new double[]{
                1.00, 1.06, 1.10, 1.06, 1.00,
                1.06, 1.10, 1.06, 1.00
        };

        final int[] step = {0};
        timerPulso = new Timer(45, e -> {
            pulsoScale = seq[step[0]];
            if (repaintTarget != null) repaintTarget.repaint();
            step[0]++;

            if (step[0] >= seq.length) {
                detenerPulso();
                pulsoScale = 1.0;
                if (repaintTarget != null) repaintTarget.repaint();
                if (onFinish != null) onFinish.run();
            }
        });

        timerPulso.start();
    }

    protected final void detenerPulso() {
        if (timerPulso != null && timerPulso.isRunning()) {
            timerPulso.stop();
        }
        timerPulso = null;
    }

    /**
     * Limpieza automática cuando el panel se remueve del árbol de UI.
     * Útil para evitar Timers “fantasma” si el usuario sale de un juego.
     */
    @Override
    public void removeNotify() {
        detenerPulso();
        onAntesDeSalir();
        super.removeNotify();
    }

    /** Hook opcional para que cada juego detenga sus Timers internos (fade, etc.). */
    protected void onAntesDeSalir() {
        // opcional
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
