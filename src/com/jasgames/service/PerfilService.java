package com.jasgames.service;

import com.jasgames.model.Nino;
import com.jasgames.model.PIA;

import java.util.HashMap;
import java.util.Map;

public class PerfilService {

    private final Map<Integer, Nino> ninosPorId;
    private final Map<Integer, PIA> piaPorNinoId;

    public PerfilService() {
        this.ninosPorId = new HashMap<>();
        this.piaPorNinoId = new HashMap<>();
    }

    public void registrarNino(Nino nino) {
        ninosPorId.put(nino.getId(), nino);
    }

    public Nino buscarNinoPorId(int id) {
        return ninosPorId.get(id);
    }

    public Map<Integer, Nino> obtenerTodosNinos() {
        return ninosPorId;
    }

    public void asignarPIA(PIA pia) {
        piaPorNinoId.put(pia.getNino().getId(), pia);
    }

    public PIA obtenerPIADeNino(int idNino) {
        return piaPorNinoId.get(idNino);
    }
}
