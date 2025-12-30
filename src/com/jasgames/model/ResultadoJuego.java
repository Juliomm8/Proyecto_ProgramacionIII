package com.jasgames.model;

import java.time.LocalDateTime;

public class ResultadoJuego {

    private Integer idEstudiante;
    private String nombreEstudiante;
    private String aula;

    private Juego juego;
    private int puntaje;
    private LocalDateTime fechaHora;
    private int dificultad;

    public ResultadoJuego() {
        // requerido por Gson
    }

    public ResultadoJuego(String nombreEstudiante, Juego juego, int dificultad, int puntaje, LocalDateTime fechaHora) {
        this(null, nombreEstudiante, null, juego, dificultad, puntaje, fechaHora);
    }

    public ResultadoJuego(Integer idEstudiante, String nombreEstudiante, String aula,
                          Juego juego, int dificultad, int puntaje, LocalDateTime fechaHora) {
        this.idEstudiante = idEstudiante;
        this.nombreEstudiante = nombreEstudiante;
        this.aula = aula;
        this.juego = juego;
        this.dificultad = dificultad;
        this.puntaje = puntaje;
        this.fechaHora = fechaHora;
    }

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public Juego getJuego() { return juego; }
    public void setJuego(Juego juego) { this.juego = juego; }

    public int getPuntaje() { return puntaje; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public int getDificultad() { return dificultad; }
    public void setDificultad(int dificultad) { this.dificultad = dificultad; }
}
