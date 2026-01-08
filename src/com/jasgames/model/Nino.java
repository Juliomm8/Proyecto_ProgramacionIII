package com.jasgames.model;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class Nino {

    private int id;
    private String nombre;
    private int edad;
    private String diagnostico;
    private int puntosTotales;
    private Set<Integer> juegosAsignados = new HashSet<>();
    private Map<Integer, Integer> dificultadPorJuego = new HashMap<>();
    // Adaptación automática (por juego)
    private Map<Integer, Integer> dificultadAutoPorJuego = new HashMap<>();
    private Map<Integer, Integer> cooldownRestantePorJuego = new HashMap<>();
    private Map<Integer, Boolean> adaptacionAutomaticaPorJuego = new HashMap<>();
    private String aula;
    private String avatar;

    public Nino(int id, String nombre, int edad, String diagnostico) {
        this.id = id;
        this.nombre = nombre;
        this.edad = edad;
        this.diagnostico = diagnostico;
        this.puntosTotales = 0;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdad() {
        return edad;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public Set<Integer> getJuegosAsignados() {
        if (juegosAsignados == null) {
            juegosAsignados = new HashSet<>();
        }
        return juegosAsignados;
    }

    public void setJuegosAsignados(Set<Integer> juegosAsignados) {
        this.juegosAsignados = (juegosAsignados == null) ? new HashSet<>() : new HashSet<>(juegosAsignados);
    }

    public int getPuntosTotales() {
        return puntosTotales;
    }

    public void agregarPuntos(int puntos) {
        this.puntosTotales += puntos;
    }

    public void setPuntosTotales(int puntosTotales) {
        this.puntosTotales = puntosTotales;
    }

    public Map<Integer, Integer> getDificultadPorJuego() {
        if (dificultadPorJuego == null) {
            dificultadPorJuego = new HashMap<>();
        }
        return dificultadPorJuego;
    }

    public void setDificultadPorJuego(Map<Integer, Integer> dificultadPorJuego) {
        this.dificultadPorJuego = (dificultadPorJuego == null) ? new HashMap<>() : dificultadPorJuego;
    }

    public int getDificultadJuego(int idJuego, int difDefault) {
        Integer d = getDificultadPorJuego().get(idJuego);
        return (d != null) ? d : difDefault;
    }

    public void setDificultadJuego(int idJuego, int dificultad) {
        if (dificultad < 1) dificultad = 1;
        if (dificultad > 5) dificultad = 5;
        getDificultadPorJuego().put(idJuego, dificultad);
    }

    // -------------------- Adaptación automática --------------------

    public Map<Integer, Integer> getDificultadAutoPorJuego() {
        if (dificultadAutoPorJuego == null) {
            dificultadAutoPorJuego = new HashMap<>();
        }
        return dificultadAutoPorJuego;
    }

    public void setDificultadAutoPorJuego(Map<Integer, Integer> dificultadAutoPorJuego) {
        this.dificultadAutoPorJuego = (dificultadAutoPorJuego == null) ? new HashMap<>() : dificultadAutoPorJuego;
    }

    public void setDificultadAutoJuego(int idJuego, int dificultad) {
        if (dificultad < 1) dificultad = 1;
        if (dificultad > 5) dificultad = 5;
        getDificultadAutoPorJuego().put(idJuego, dificultad);
    }

    public int getDificultadAutoJuego(int idJuego, int difDefault) {
        Integer d = getDificultadAutoPorJuego().get(idJuego);
        return (d != null) ? d : difDefault;
    }

    public Map<Integer, Integer> getCooldownRestantePorJuego() {
        if (cooldownRestantePorJuego == null) {
            cooldownRestantePorJuego = new HashMap<>();
        }
        return cooldownRestantePorJuego;
    }

    public void setCooldownRestantePorJuego(Map<Integer, Integer> cooldownRestantePorJuego) {
        this.cooldownRestantePorJuego = (cooldownRestantePorJuego == null) ? new HashMap<>() : cooldownRestantePorJuego;
    }

    public int getCooldownRestanteJuego(int idJuego) {
        Integer v = getCooldownRestantePorJuego().get(idJuego);
        return (v != null) ? v : 0;
    }

    public void setCooldownRestanteJuego(int idJuego, int sesionesRestantes) {
        if (sesionesRestantes < 0) sesionesRestantes = 0;
        getCooldownRestantePorJuego().put(idJuego, sesionesRestantes);
    }

    public Map<Integer, Boolean> getAdaptacionAutomaticaPorJuego() {
        if (adaptacionAutomaticaPorJuego == null) {
            adaptacionAutomaticaPorJuego = new HashMap<>();
        }
        return adaptacionAutomaticaPorJuego;
    }

    public void setAdaptacionAutomaticaPorJuego(Map<Integer, Boolean> adaptacionAutomaticaPorJuego) {
        this.adaptacionAutomaticaPorJuego = (adaptacionAutomaticaPorJuego == null) ? new HashMap<>() : adaptacionAutomaticaPorJuego;
    }

    /**
     * Si no hay valor, asumimos TRUE (adaptativo por defecto).
     */
    public boolean isAdaptacionAutomaticaJuego(int idJuego) {
        Boolean v = getAdaptacionAutomaticaPorJuego().get(idJuego);
        return (v == null) ? true : v;
    }

    public void setAdaptacionAutomaticaJuego(int idJuego, boolean enabled) {
        getAdaptacionAutomaticaPorJuego().put(idJuego, enabled);
    }

    public boolean tieneDificultadManual(int idJuego) {
        return getDificultadPorJuego().containsKey(idJuego);
    }

    /**
     * Dificultad efectiva para iniciar: manual > auto > default del juego.
     */
    public int getDificultadEfectiva(int idJuego, int difDefault) {
        if (tieneDificultadManual(idJuego)) {
            return getDificultadJuego(idJuego, difDefault);
        }
        if (isAdaptacionAutomaticaJuego(idJuego)) {
            return getDificultadAutoJuego(idJuego, difDefault);
        }
        return difDefault;
    }

    public String getAula() {
        if (aula == null || aula.isBlank()) return "Aula Azul";
        if ("General".equalsIgnoreCase(aula.trim())) return "Aula Azul";
        return aula.trim();
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public String getAvatar() {
        if (avatar == null || avatar.isBlank()) return "🙂";
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String toString() {
        return nombre + " (id=" + id + ")";
    }
}
