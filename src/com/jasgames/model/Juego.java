package com.jasgames.model;

public class Juego {

    private int id;
    private String nombre;
    private TipoJuego tipo;
    private int dificultad; // 1-5 por ejemplo
    private String descripcion;

    public Juego(int id, String nombre, TipoJuego tipo, int dificultad, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.dificultad = dificultad;
        this.descripcion = descripcion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoJuego getTipo() {
        return tipo;
    }

    public void setTipo(TipoJuego tipo) {
        this.tipo = tipo;
    }

    public int getDificultad() {
        return dificultad;
    }

    public void setDificultad(int dificultad) {
        this.dificultad = dificultad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipo + ", dif. " + dificultad + ")";
    }
}
