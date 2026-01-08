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
 * - Control de rondas (meta, jugadas, aciertos, errores)
 * - 3 intentos por ronda (configurable)
 * - Pista suave desde el 2do intento (configurable)
 * - Bloqueo de entrada (cuando hay animación)
 * - Animación estándar de pulso (latido) reutilizable
 */
public abstract class JuegoRondasPanel extends BaseJuegoPanel {

    private JPanel root;
    private JLabel lblTitulo;
    private JLabel lblProgreso;
    private JLabel lblFeedback;
    private JPanel hostTablero;
    private JPanel hostRespuestas;

    // Métricas principales
    protected int rondasCorrectas;
    protected int erroresTotales;
    protected boolean bloqueado;

    // Nuevo: rondas jugadas (se incrementa tanto por acierto como por agotar intentos)
    protected int rondasJugadas;

    // Control por ronda
    private int intentosEnRonda;
    private boolean pistaAplicadaEnRonda;

    // Métricas reales (para SesionJuego)
    private int intentosTotalesReal;
    private int pistasUsadasReal;
    private int aciertosPrimerIntentoReal;

    /** Factor de escala para animación de pulso (úsalo en paint). */
    private double pulsoScale = 1.0;
    private Timer timerPulso;
    private Timer timerAvanceRonda;

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

    /** Configurable por juego si quieres. Default 3. */
    protected int getIntentosMaxPorRonda() {
        int v = (actividadActual != null) ? actividadActual.getIntentosMaxPorRonda() : 3;
        return (v <= 0) ? 3 : v;
    }

    /** Configurable por juego si quieres. Default 2 (pista desde el 2do intento). */
    protected int getPistasDesdeIntento() {
        int v = (actividadActual != null) ? actividadActual.getPistasDesdeIntento() : 2;
        return (v <= 0) ? 2 : v;
    }

    /** Índice 0-based de la ronda actual (cuántas rondas ya se completaron/jugaron). */
    protected final int getIndiceRondaActual() {
        return Math.max(0, rondasJugadas);
    }

    protected final double getPulsoScale() {
        return pulsoScale;
    }

    // -------------------- Ciclo de vida --------------------

    @Override
    public final void iniciarJuego() {
        detenerPulso();
        detenerAvanceRonda();
        pulsoScale = 1.0;

        rondasCorrectas = 0;
        rondasJugadas = 0;
        erroresTotales = 0;
        bloqueado = false;

        intentosEnRonda = 0;
        pistaAplicadaEnRonda = false;

        intentosTotalesReal = 0;
        pistasUsadasReal = 0;
        aciertosPrimerIntentoReal = 0;

        // Defaults en Actividad (por si luego quieres verlos en UI/reportes)
        if (actividadActual != null) {
            actividadActual.setIntentosMaxPorRonda(getIntentosMaxPorRonda());
            actividadActual.setPistasDesdeIntento(getPistasDesdeIntento());
        }

        setFeedback(" ");
        actualizarProgreso();

        onAntesDeIniciar();
        prepararNuevaRonda();
    }

    /** Hook opcional para resetear estados internos en el hijo. */
    protected void onAntesDeIniciar() {}

    /** El hijo define cómo se prepara cada ronda. */
    protected abstract void prepararNuevaRonda();

    /** Hook opcional: se llama cuando el niño llega al intento donde corresponde una pista. */
    protected void aplicarPistaSuave() {
        // por defecto no hace nada; cada juego puede apagar 1 distractor o dar una pista visual
    }

    /** Hook opcional: se llama cuando se agotan los intentos de la ronda (antes de pasar a la siguiente). */
    protected void onIntentosAgotados() {
        // por defecto no hace nada
    }

    /** Default: rondasCorrectas (si quieres fijo, sobrescribe). */
    protected int calcularPuntosFinales() {
        return rondasCorrectas;
    }

    // -------------------- Helpers (acierto/error) --------------------

    private void registrarIntento(boolean correcto) {
        // Registra intento real
        boolean primerIntentoEnRonda = (intentosEnRonda == 0);
        intentosEnRonda++;
        intentosTotalesReal++;

        if (!correcto) {
            erroresTotales++;
        } else {
            if (primerIntentoEnRonda) {
                aciertosPrimerIntentoReal++;
            }
        }

        // Pista desde el 2do intento (o lo que defina el juego)
        if (!correcto && !pistaAplicadaEnRonda && intentosEnRonda >= getPistasDesdeIntento()) {
            pistaAplicadaEnRonda = true;
            pistasUsadasReal++;
            try {
                aplicarPistaSuave();
            } catch (Exception ignored) {
                // Evitar que una pista mal implementada rompa el juego
            }
        }
    }

    private void resetEstadoRonda() {
        intentosEnRonda = 0;
        pistaAplicadaEnRonda = false;
    }

    protected final void marcarAciertoConPulso(JComponent repaintTarget, Runnable antesDeSiguienteRonda) {
        if (bloqueado) return;
        bloqueado = true;

        registrarIntento(true);

        setFeedback("¡Muy bien!");

        reproducirPulso(repaintTarget, () -> {
            rondasCorrectas++;
            rondasJugadas++;
            actualizarProgreso();

            if (rondasJugadas >= getRondasMeta()) {
                volcarMetricasFinDeSesion();
                finalizarJuego(calcularPuntosFinales());
                return;
            }

            resetEstadoRonda();
            if (antesDeSiguienteRonda != null) antesDeSiguienteRonda.run();
            bloqueado = false;
            prepararNuevaRonda();
        });
    }

    protected final void marcarErrorNeutro(String feedback) {
        if (bloqueado) return;

        registrarIntento(false);

        setFeedback((feedback == null || feedback.isBlank()) ? "Intenta de nuevo" : feedback);

        // Si se agotaron los intentos, avanzamos a la siguiente ronda con una pausa suave
        if (intentosEnRonda >= getIntentosMaxPorRonda()) {
            bloqueado = true;
            try {
                onIntentosAgotados();
            } catch (Exception ignored) {}

            setFeedback("Vamos con la siguiente");

            detenerAvanceRonda();
            timerAvanceRonda = new Timer(700, ev -> {
                ((Timer) ev.getSource()).stop();
                timerAvanceRonda = null;

                rondasJugadas++;
                actualizarProgreso();

                if (rondasJugadas >= getRondasMeta()) {
                    volcarMetricasFinDeSesion();
                    finalizarJuego(calcularPuntosFinales());
                    return;
                }

                resetEstadoRonda();
                bloqueado = false;
                prepararNuevaRonda();
            });
            timerAvanceRonda.setRepeats(false);
            timerAvanceRonda.start();
        }
    }

    /**
     * Vuelca métricas reales de la sesión hacia {@link Actividad} para que EstudianteWindow
     * pueda persistirlas en {@link com.jasgames.model.SesionJuego}.
     */
    protected void volcarMetricasFinDeSesion() {
        if (actividadActual == null) return;

        int meta = getRondasMeta();

        actividadActual.setRondasMeta(meta);
        actividadActual.setRondasJugadas(rondasJugadas);
        actividadActual.setRondasCorrectas(rondasCorrectas);

        actividadActual.setErroresTotales(erroresTotales);
        actividadActual.setIntentosTotales(intentosTotalesReal);

        actividadActual.setIntentosMaxPorRonda(getIntentosMaxPorRonda());
        actividadActual.setPistasDesdeIntento(getPistasDesdeIntento());

        actividadActual.setPistasUsadas(pistasUsadasReal);
        actividadActual.setAciertosPrimerIntento(aciertosPrimerIntentoReal);

        // Compatibilidad con campo antiguo
        actividadActual.setIntentosFallidos(erroresTotales);
    }

    protected final void actualizarProgreso() {
        int meta = getRondasMeta();
        int mostrada = Math.min(rondasJugadas + 1, meta);
        if (rondasJugadas >= meta) mostrada = meta;
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

    private void detenerAvanceRonda() {
        if (timerAvanceRonda != null && timerAvanceRonda.isRunning()) {
            timerAvanceRonda.stop();
        }
        timerAvanceRonda = null;
    }

    /**
     * Limpieza automática cuando el panel se remueve del árbol de UI.
     * Útil para evitar Timers “fantasma” si el usuario sale de un juego.
     */
    @Override
    public void removeNotify() {
        detenerPulso();
        detenerAvanceRonda();
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
