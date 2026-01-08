package com.jasgames.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ObjetivoPIA {

    private String idObjetivo;                 // UUID
    private String titulo;                     // ej: "Discriminación de colores"
    private String descripcion;                // texto corto
    private List<Integer> juegosAsociados;     // ids de juegos

    // metas
    private int metaSesiones;                  // ej: 10
    private double metaPrecision;              // ej: 0.85 (85%)

    // límites de dificultad recomendados para este objetivo
    private int dificultadMin;                 // ej: 1
    private int dificultadMax;                 // ej: 5

    public ObjetivoPIA() {
        // requerido por Gson
        this.idObjetivo = UUID.randomUUID().toString();
        this.juegosAsociados = new ArrayList<>();
        this.metaSesiones = 10;
        this.metaPrecision = 0.85;
        this.dificultadMin = 1;
        this.dificultadMax = 5;
    }

    public ObjetivoPIA(String titulo, String descripcion) {
        this();
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    // ---------------- Getters / Setters ----------------

    public String getIdObjetivo() { return idObjetivo; }
    public void setIdObjetivo(String idObjetivo) { this.idObjetivo = idObjetivo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<Integer> getJuegosAsociados() {
        if (juegosAsociados == null) juegosAsociados = new ArrayList<>();
        return juegosAsociados;
    }
    public void setJuegosAsociados(List<Integer> juegosAsociados) {
        this.juegosAsociados = (juegosAsociados == null) ? new ArrayList<>() : juegosAsociados;
    }

    public int getMetaSesiones() { return metaSesiones; }
    public void setMetaSesiones(int metaSesiones) { this.metaSesiones = metaSesiones; }

    public double getMetaPrecision() { return metaPrecision; }
    public void setMetaPrecision(double metaPrecision) { this.metaPrecision = metaPrecision; }

    public int getDificultadMin() { return dificultadMin; }
    public void setDificultadMin(int dificultadMin) { this.dificultadMin = dificultadMin; }

    public int getDificultadMax() { return dificultadMax; }
    public void setDificultadMax(int dificultadMax) { this.dificultadMax = dificultadMax; }
}
