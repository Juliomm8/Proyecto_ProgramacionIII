package com.jasgames.model;

import java.time.LocalDateTime;

public class ResultadoJuego {

    private final String nombreEstudiante;
    private final Juego juego;
    private final int puntaje;
    private final LocalDateTime fechaHora;

    public ResultadoJuego(String nombreEstudiante, Juego juego, int puntaje, LocalDateTime fechaHora) {
        this.nombreEstudiante = nombreEstudiante;
        this.juego = juego;
        this.puntaje = puntaje;
        this.fechaHora = fechaHora;
    }

    public String getNombreEstudiante() {
        return nombreEstudiante;
    }

    public Juego getJuego() {
        return juego;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public String toString() {
        return "ResultadoJuego{" +
                "nombreEstudiante='" + nombreEstudiante + '\'' +
                ", juego=" + juego +
                ", puntaje=" + puntaje +
                ", fechaHora=" + fechaHora +
                '}';
    }
}
