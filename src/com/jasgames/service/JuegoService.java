package com.jasgames.service;

import com.jasgames.model.Actividad;
import com.jasgames.model.Juego;

import java.util.*;

public class JuegoService {

    private final List<Juego> juegos;
    private final Queue<Actividad> colaActividades;

    public JuegoService() {
        this.juegos = new ArrayList<>();
        this.colaActividades = new LinkedList<>();
    }

    // CRUD de juegos
    public void agregarJuego(Juego juego) {
        juegos.add(juego);
    }

    public List<Juego> obtenerTodos() {
        return Collections.unmodifiableList(juegos);
    }

    public void eliminarJuego(Juego juego) {
        juegos.remove(juego);
    }

    // Cola de actividades
    public void encolarActividad(Actividad actividad) {
        colaActividades.offer(actividad);
    }

    public Actividad siguienteActividad() {
        return colaActividades.poll();
    }

    public Queue<Actividad> getColaActividades() {
        return colaActividades;
    }
}
