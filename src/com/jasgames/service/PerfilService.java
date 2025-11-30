package com.jasgames.service;

import com.jasgames.model.CriterioOrdenNino;
import com.jasgames.model.Nino;
import com.jasgames.model.PIA;
import java.util.*;

public class PerfilService {

    private final Map<Integer, Nino> ninosPorId;
    private final Map<Integer, PIA> piaPorNinoId;

    public PerfilService() {
        this.ninosPorId = new HashMap<>();
        this.piaPorNinoId = new HashMap<>();
    }

    // ------------ CRUD BÁSICO ------------

    public void registrarNino(Nino nino) {
        ninosPorId.put(nino.getId(), nino);
    }

    public Nino buscarNinoPorId(int id) {
        return ninosPorId.get(id);
    }

    public boolean eliminarNinoPorId(int id) {
        return ninosPorId.remove(id) != null;
    }

    public void actualizarNino(Nino ninoActualizado) {
        ninosPorId.put(ninoActualizado.getId(), ninoActualizado);
    }

    public Collection<Nino> obtenerTodosNinos() {
        return ninosPorId.values();
    }

    // ------------ BÚSQUEDA ------------

    public List<Nino> buscarPorNombre(String texto) {
        List<Nino> resultado = new ArrayList<>();
        String q = texto.toLowerCase();

        for (Nino nino : ninosPorId.values()) {
            if (nino.getNombre().toLowerCase().contains(q)) {
                resultado.add(nino);
            }
        }
        return resultado;
    }

    public Nino buscarPorIdONombre(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            int id = Integer.parseInt(texto.trim());
            return buscarNinoPorId(id);
        } catch (NumberFormatException e) {
            List<Nino> encontrados = buscarPorNombre(texto);
            return encontrados.isEmpty() ? null : encontrados.get(0);
        }
    }

    // ------------ ORDENAMIENTO ------------

    public List<Nino> obtenerNinosOrdenados(CriterioOrdenNino criterio) {
        List<Nino> lista = new ArrayList<>(ninosPorId.values());

        Comparator<Nino> comparator;

        switch (criterio) {
            case NOMBRE:
                comparator = Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER);
                break;
            case EDAD:
                comparator = Comparator.comparingInt(Nino::getEdad)
                        .thenComparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER);
                break;
            case DIAGNOSTICO:
                comparator = Comparator.comparing(Nino::getDiagnostico, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER);
                break;
            case ID:
            default:
                comparator = Comparator.comparingInt(Nino::getId);
                break;
        }

        lista.sort(comparator);
        return lista;
    }

    // ------------ PIA  ------------

    public void asignarPIA(PIA pia) {
        piaPorNinoId.put(pia.getNino().getId(), pia);
    }

    public PIA obtenerPIADeNino(int idNino) {
        return piaPorNinoId.get(idNino);
    }
}
