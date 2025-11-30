package com.jasgames.service;

import com.jasgames.model.Actividad;
import com.jasgames.model.Juego;
import com.jasgames.model.TipoJuego;

import java.util.*;

public class JuegoService {

    private final List<Juego> juegos;
    private final Queue<Actividad> colaActividades;

    public JuegoService() {
        this.juegos = new ArrayList<>();
        this.colaActividades = new LinkedList<>();
    }

    // ------------ CRUD JUEGOS ------------
    public void agregarJuego(Juego juego) {
        juegos.add(juego);
    }

    public void eliminarJuego(Juego juego) {
        juegos.remove(juego);
    }

    public List<Juego> obtenerTodos() {
        return Collections.unmodifiableList(juegos);
    }

    public List<Juego> filtrarPorTipo(TipoJuego tipo) {
        List<Juego> resultado = new ArrayList<>();
        for (Juego juego : juegos) {
            if (juego.getTipo() == tipo) {
                resultado.add(juego);
            }
        }
        return resultado;
    }

    // ------------ COLA DE ACTIVIDADES ------------
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
