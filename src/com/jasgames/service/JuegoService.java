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

        cargarJuegosIniciales();
    }

    private void cargarJuegosIniciales() {
        agregarJuego(new Juego(
                1,
                "Discriminación de Colores",
                TipoJuego.COLORES,
                1,
                "Toca el círculo del color indicado. Refuerzo positivo y sin castigo por error."
        ));
    }

    public void guardar() {
        // TODO: si luego quieres persistir juegos.json, aquí lo implementas.
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
