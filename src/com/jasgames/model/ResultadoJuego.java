package com.jasgames.model;

import java.time.LocalDateTime;

public class ResultadoJuego {

    private final String nombreEstudiante;
    private final Juego juego;
    private final int puntaje;
    private final LocalDateTime fechaHora;
    private final int dificultad;

    public ResultadoJuego(String nombreEstudiante, Juego juego, int dificultad, int puntaje, LocalDateTime fechaHora) {
        this.nombreEstudiante = nombreEstudiante;
        this.juego = juego;
        this.dificultad = dificultad;
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

    public int getDificultad() {
        return dificultad;
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
