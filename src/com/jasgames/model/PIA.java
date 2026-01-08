package com.jasgames.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PIA {

    private String idPia;

    // Relación con niño
    private int idNino;
    private String nombreNino;
    private String aula;

    private String objetivoGeneral;

    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;

    private List<ObjetivoPIA> objetivos;

    public PIA() {
        // requerido por Gson
        this.idPia = UUID.randomUUID().toString();
        this.nombreNino = "";
        this.aula = "";
        this.objetivoGeneral = "";
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaCierre = null;
        this.objetivos = new ArrayList<>();
    }

    public PIA(int idNino, String nombreNino, String aula, String objetivoGeneral) {
        this();
        this.idNino = idNino;
        this.nombreNino = (nombreNino == null) ? "" : nombreNino;
        this.aula = (aula == null) ? "" : aula;
        this.objetivoGeneral = (objetivoGeneral == null) ? "" : objetivoGeneral;
    }

    public void agregarObjetivo(ObjetivoPIA obj) {
        if (obj != null) objetivos.add(obj);
    }

    public ObjetivoPIA getObjetivoPorId(String idObjetivo) {
        if (idObjetivo == null) return null;
        for (ObjetivoPIA o : objetivos) {
            if (o != null && idObjetivo.equals(o.getIdObjetivo())) return o;
        }
        return null;
    }

    /**
     * Objetivo “actual”: el primero no completado (si todos completados, null).
     */
    public ObjetivoPIA getObjetivoActivo() {
        for (ObjetivoPIA o : objetivos) {
            if (o != null && !o.isCompletado()) return o;
        }
        return null;
    }

    public void cerrar() {
        this.activo = false;
        if (this.fechaCierre == null) this.fechaCierre = LocalDateTime.now();
    }

    // ---------------- Getters/Setters ----------------

    public String getIdPia() { return idPia; }
    public void setIdPia(String idPia) { this.idPia = idPia; }

    public int getIdNino() { return idNino; }
    public void setIdNino(int idNino) { this.idNino = idNino; }

    public String getNombreNino() { return nombreNino; }
    public void setNombreNino(String nombreNino) { this.nombreNino = nombreNino; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public String getObjetivoGeneral() { return objetivoGeneral; }
    public void setObjetivoGeneral(String objetivoGeneral) { this.objetivoGeneral = objetivoGeneral; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public List<ObjetivoPIA> getObjetivos() { return objetivos; }
    public void setObjetivos(List<ObjetivoPIA> objetivos) { this.objetivos = (objetivos == null) ? new ArrayList<>() : objetivos; }
}
