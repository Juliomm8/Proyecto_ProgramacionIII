package com.jasgames.model;

public class Nino {

    private int id;
    private String nombre;
    private int edad;
    private String diagnostico;
    private int puntosTotales;

    public Nino(int id, String nombre, int edad, String diagnostico) {
        this.id = id;
        this.nombre = nombre;
        this.edad = edad;
        this.diagnostico = diagnostico;
        this.puntosTotales = 0;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdad() {
        return edad;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public int getPuntosTotales() {
        return puntosTotales;
    }

    public void agregarPuntos(int puntos) {
        this.puntosTotales += puntos;
    }

    @Override
    public String toString() {
        return nombre + " (id=" + id + ")";
    }
}
