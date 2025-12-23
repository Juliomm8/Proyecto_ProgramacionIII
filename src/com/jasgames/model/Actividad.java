package com.jasgames.model;

public class Actividad {

    private int id;
    private Juego juego;
    private int nivel;
    private int puntos;

    public Actividad(int id, Juego juego, int nivel, int puntos) {
        this.id = id;
        this.juego = juego;
        this.nivel = nivel;
        this.puntos = puntos;
    }

    public int getId() {
        return id;
    }

    public Juego getJuego() {
        return juego;
    }

    public int getNivel() {
        return nivel;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    @Override
    public String toString() {
        return juego.getNombre() + " - Nivel " + nivel + " (" + puntos + " pts)";
    }
}
