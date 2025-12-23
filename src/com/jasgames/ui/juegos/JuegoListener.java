package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;

/**
 * Interfaz para comunicar eventos desde el minijuego hacia la ventana principal.
 * Esto permite el desacoplamiento: El juego no necesita saber que existe una "EstudianteWindow".
 */
public interface JuegoListener {

    /**
     * Se debe invocar cuando el niño completa la actividad satisfactoriamente.
     * @param actividad La actividad con el puntaje actualizado.
     */
    void onJuegoTerminado(Actividad actividad);
}