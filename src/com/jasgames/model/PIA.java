package com.jasgames.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PIA {

    private String idPia;                 // UUID
    private int idNino;                   // referencia al niño (no objeto completo)
    private boolean activo;               // PIA vigente
    private String objetivoGeneral;

    private List<ObjetivoPIA> objetivos;  // metas del PIA
    private String idObjetivoActivo;      // objetivo seleccionado (nullable)

    public PIA() {
        // requerido por Gson
        this.idPia = UUID.randomUUID().toString();
        this.objetivos = new ArrayList<>();
        this.activo = true;
        this.idObjetivoActivo = null;
    }

    public PIA(int idNino, String objetivoGeneral) {
        this();
        this.idNino = idNino;
        this.objetivoGeneral = objetivoGeneral;
    }

    // ---------------- Getters / Setters ----------------

    public String getIdPia() { return idPia; }
    public void setIdPia(String idPia) { this.idPia = idPia; }

    public int getIdNino() { return idNino; }
    public void setIdNino(int idNino) { this.idNino = idNino; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getObjetivoGeneral() { return objetivoGeneral; }
    public void setObjetivoGeneral(String objetivoGeneral) { this.objetivoGeneral = objetivoGeneral; }

    public List<ObjetivoPIA> getObjetivos() {
        if (objetivos == null) objetivos = new ArrayList<>();
        return objetivos;
    }
    public void setObjetivos(List<ObjetivoPIA> objetivos) {
        this.objetivos = (objetivos == null) ? new ArrayList<>() : objetivos;
    }

    public String getIdObjetivoActivo() { return idObjetivoActivo; }
    public void setIdObjetivoActivo(String idObjetivoActivo) { this.idObjetivoActivo = idObjetivoActivo; }

    // Helpers
    public void agregarObjetivo(ObjetivoPIA o) {
        if (o == null) return;
        getObjetivos().add(o);
        if (idObjetivoActivo == null) {
            idObjetivoActivo = o.getIdObjetivo();
        }
    }
}
