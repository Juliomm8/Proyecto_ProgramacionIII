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

    // Objetivo activo elegido por el docente (opcional)
    private String idObjetivoActivo;

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
        this.idObjetivoActivo = null;
    }

    public PIA(int idNino, String nombreNino, String aula, String objetivoGeneral) {
        this();
        this.idNino = idNino;
        this.nombreNino = (nombreNino == null) ? "" : nombreNino;
        this.aula = (aula == null) ? "" : aula;
        this.objetivoGeneral = (objetivoGeneral == null) ? "" : objetivoGeneral;
    }

    public void agregarObjetivo(ObjetivoPIA obj) {
    if (obj == null) return;
    getObjetivos().add(obj);
    if (idObjetivoActivo == null) {
        idObjetivoActivo = obj.getIdObjetivo();
    }
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
    // 1) Si el docente eligió un objetivo activo, priorizarlo si sigue vigente
    if (idObjetivoActivo != null) {
        ObjetivoPIA elegido = getObjetivoPorId(idObjetivoActivo);
        if (elegido != null && !elegido.isCompletado()) return elegido;
    }

    // 2) Fallback: primero no completado
    for (ObjetivoPIA o : getObjetivos()) {
        if (o != null && !o.isCompletado()) return o;
    }
    return null;
}

    
/**
 * Asegura que el objetivo activo apunte a un objetivo existente y no completado.
 * Si el objetivo activo ya no es válido, selecciona el primer objetivo no completado.
 * Si no hay objetivos en progreso, deja el activo en null.
 */
public void asegurarObjetivoActivoValido() {
    if (idObjetivoActivo != null) {
        ObjetivoPIA o = getObjetivoPorId(idObjetivoActivo);
        if (o == null || o.isCompletado()) {
            idObjetivoActivo = null;
        }
    }

    if (idObjetivoActivo == null) {
        for (ObjetivoPIA o : getObjetivos()) {
            if (o != null && !o.isCompletado()) {
                idObjetivoActivo = o.getIdObjetivo();
                return;
            }
        }
    }
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

    public List<ObjetivoPIA> getObjetivos() { if (objetivos == null) objetivos = new ArrayList<>(); return objetivos; }
    public void setObjetivos(List<ObjetivoPIA> objetivos) { this.objetivos = (objetivos == null) ? new ArrayList<>() : objetivos; }

public String getIdObjetivoActivo() { return idObjetivoActivo; }
public void setIdObjetivoActivo(String idObjetivoActivo) { this.idObjetivoActivo = idObjetivoActivo; }

}
