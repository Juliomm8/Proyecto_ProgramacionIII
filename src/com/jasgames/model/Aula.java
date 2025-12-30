package com.jasgames.model;

public class Aula {
    private String nombre;
    private String colorHex;

    public Aula() {}

    public Aula(String nombre, String colorHex) {
        this.nombre = nombre;
        this.colorHex = colorHex;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
