package com.jasgames.model;

import java.util.ArrayList;
import java.util.List;

public class PIA {

    private int id;
    private Nino nino;
    private String objetivoGeneral;
    private List<Actividad> actividades;

    public PIA(int id, Nino nino, String objetivoGeneral) {
        this.id = id;
        this.nino = nino;
        this.objetivoGeneral = objetivoGeneral;
        this.actividades = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public Nino getNino() {
        return nino;
    }

    public String getObjetivoGeneral() {
        return objetivoGeneral;
    }

    public List<Actividad> getActividades() {
        return actividades;
    }

    public void agregarActividad(Actividad actividad) {
        actividades.add(actividad);
    }
}
