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
        return juegosAsignados;
    }

    public void setJuegosAsignados(Set<Integer> juegosAsignados) {
        this.juegosAsignados = juegosAsignados;
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
