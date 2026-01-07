package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import javax.swing.*;
import java.awt.*;

/**
 * Clase base abstracta para todos los minijuegos de JAS Games.
 * Centraliza la configuración visual accesible y la lógica de finalización.
 */
public abstract class BaseJuegoPanel extends JPanel {

    protected Actividad actividadActual;
    protected JuegoListener listener;
    private boolean juegoFinalizado = false;
    private final long inicioMs;

    /**
     * Constructor base.
     * @param actividad La actividad que se va a jugar (contiene el nivel, id, etc.)
     * @param listener Quien escuchará cuando termine el juego (usualmente EstudianteWindow)
     */
    public BaseJuegoPanel(Actividad actividad, JuegoListener listener) {
        this.actividadActual = actividad;
        this.listener = listener;

        // Medición de duración de sesión (para métricas/analítica, no para presionar al niño)
        this.inicioMs = System.currentTimeMillis();

        configurarEstiloBase();
    }

    protected void configurarEstiloBase() {
        this.setBackground(new Color(245, 245, 245));

        // Usamos BorderLayout por defecto para facilitar el diseño de los hijos
        this.setLayout(new BorderLayout());
    }

    /**
     * Método abstracto: OBLIGA a todos los juegos a tener una lógica de inicio.
     * Aquí es donde el hijo configurará sus botones, sonidos, etc.
     */
    public abstract void iniciarJuego();

    /**
     * Método helper para que los hijos cierren el juego fácilmente.
     * @param puntajeObtenido Los puntos que ganó el niño.
     */
    protected final void finalizarJuego(int puntajeObtenido) {
        if (juegoFinalizado) return;
        juegoFinalizado = true;

        // 1. Guardamos el puntaje en el objeto de negocio
        if (this.actividadActual != null) {
            this.actividadActual.setPuntos(puntajeObtenido);
        }

        // 2. Guardar duración de la sesión (ms) en Actividad para que luego se persista en SesionJuego
        if (this.actividadActual != null) {
            long dur = System.currentTimeMillis() - inicioMs;
            this.actividadActual.setDuracionMs(Math.max(0L, dur));
        }

        // 3. Notificar a la ventana/controlador
        if (listener != null) {
            listener.onJuegoTerminado(this.actividadActual);
        }
    }

    public Actividad getActividadActual() {
        return actividadActual;
    }

    protected final boolean isJuegoFinalizado() {
        return juegoFinalizado;
    }

    /**
     * Para el botón "Finalizar y guardar puntaje" en EstudianteWindow.
     * Guarda el puntaje que tenga la actividad en ese momento.
     */
    public void finalizarJuegoForzado() {
        int puntos = (actividadActual != null) ? actividadActual.getPuntos() : 0;
        finalizarJuego(puntos);
    }

}