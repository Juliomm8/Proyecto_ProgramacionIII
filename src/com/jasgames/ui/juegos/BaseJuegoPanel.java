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

    /**
     * Constructor base.
     * @param actividad La actividad que se va a jugar (contiene el nivel, id, etc.)
     * @param listener Quien escuchará cuando termine el juego (usualmente EstudianteWindow)
     */
    public BaseJuegoPanel(Actividad actividad, JuegoListener listener) {
        this.actividadActual = actividad;
        this.listener = listener;

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
    protected void finalizarJuego(int puntajeObtenido) {
        // 1. Guardamos el puntaje en el objeto de negocio
        if (this.actividadActual != null) {
            this.actividadActual.setPuntos(puntajeObtenido);
        }

        // 2. Avisamos a la ventana principal para que cambie de pantalla
        if (listener != null) {
            listener.onJuegoTerminado(this.actividadActual);
        }
    }
}