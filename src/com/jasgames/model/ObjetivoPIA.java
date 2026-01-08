package com.jasgames.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ObjetivoPIA {

    private String idObjetivo;

    // A qué juego apunta este objetivo
    private int juegoId;

    // Texto entendible (para UI/reportes)
    private String descripcion;

    // Metas simples (puedes crecerlas luego)
    private int metaRondasCorrectas;      // ej: 20 rondas correctas acumuladas
    private int metaSesionesCompletadas;  // ej: 5 sesiones

    // Progreso acumulado
    private int progresoRondasCorrectas;
    private int progresoSesionesCompletadas;

    private boolean completado;
    private LocalDateTime fechaCompletado;

    public ObjetivoPIA() {
        // requerido por Gson
        this.idObjetivo = UUID.randomUUID().toString();
        this.descripcion = "";
        this.metaRondasCorrectas = 0;
        this.metaSesionesCompletadas = 0;
        this.progresoRondasCorrectas = 0;
        this.progresoSesionesCompletadas = 0;
        this.completado = false;
        this.fechaCompletado = null;
    }

    public ObjetivoPIA(int juegoId, String descripcion, int metaRondasCorrectas, int metaSesionesCompletadas) {
        this();
        this.juegoId = juegoId;
        this.descripcion = (descripcion == null) ? "" : descripcion;
        this.metaRondasCorrectas = Math.max(0, metaRondasCorrectas);
        this.metaSesionesCompletadas = Math.max(0, metaSesionesCompletadas);
    }

    public void evaluarCompletadoSiAplica() {
        if (completado) return;

        boolean okRondas = (metaRondasCorrectas <= 0) || (progresoRondasCorrectas >= metaRondasCorrectas);
        boolean okSesiones = (metaSesionesCompletadas <= 0) || (progresoSesionesCompletadas >= metaSesionesCompletadas);

        if (okRondas && okSesiones) {
            completado = true;
            fechaCompletado = LocalDateTime.now();
        }
    }

    // ---------------- Getters/Setters ----------------

    public String getIdObjetivo() { return idObjetivo; }
    public void setIdObjetivo(String idObjetivo) { this.idObjetivo = idObjetivo; }

    public int getJuegoId() { return juegoId; }
    public void setJuegoId(int juegoId) { this.juegoId = juegoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getMetaRondasCorrectas() { return metaRondasCorrectas; }
    public void setMetaRondasCorrectas(int metaRondasCorrectas) { this.metaRondasCorrectas = Math.max(0, metaRondasCorrectas); }

    public int getMetaSesionesCompletadas() { return metaSesionesCompletadas; }
    public void setMetaSesionesCompletadas(int metaSesionesCompletadas) { this.metaSesionesCompletadas = Math.max(0, metaSesionesCompletadas); }

    public int getProgresoRondasCorrectas() { return progresoRondasCorrectas; }
    public void setProgresoRondasCorrectas(int progresoRondasCorrectas) { this.progresoRondasCorrectas = Math.max(0, progresoRondasCorrectas); }

    public int getProgresoSesionesCompletadas() { return progresoSesionesCompletadas; }
    public void setProgresoSesionesCompletadas(int progresoSesionesCompletadas) { this.progresoSesionesCompletadas = Math.max(0, progresoSesionesCompletadas); }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }

    public LocalDateTime getFechaCompletado() { return fechaCompletado; }
    public void setFechaCompletado(LocalDateTime fechaCompletado) { this.fechaCompletado = fechaCompletado; }
}
