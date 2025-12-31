package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Actividad;
import com.jasgames.model.Juego;
import com.jasgames.model.TipoJuego;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JuegoService {

    private static final String ARCHIVO_JUEGOS = "data/juegos.json";

    private final List<Juego> juegos;
    private final Queue<Actividad> colaActividades;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public JuegoService() {
        this.juegos = new ArrayList<>();
        this.colaActividades = new LinkedList<>();

        boolean cargado = cargarJuegosDesdeArchivo();

        if (!cargado) {
            cargarJuegosIniciales();
            guardar();
        }

        // Asegura que existan los juegos mínimos aunque el json venga viejo
        asegurarJuegosMinimos();
    }

    private void cargarJuegosIniciales() {
        agregarJuego(new Juego(
                1,
                "Discriminación de Colores",
                TipoJuego.COLORES,
                1,
                "Toca el círculo del color indicado. Refuerzo positivo y sin castigo por error."
        ));

        agregarJuego(new Juego(
                2,
                "Cuenta y Conecta",
                TipoJuego.NUMEROS,
                1,
                "Cuenta las figuras y toca el número correcto. 5 rondas completadas = 100 puntos."
        ));
    }

    private void asegurarJuegosMinimos() {
        boolean changed = false;

        if (buscarPorId(1) == null) {
            agregarJuego(new Juego(
                    1,
                    "Discriminación de Colores",
                    TipoJuego.COLORES,
                    1,
                    "Toca el círculo del color indicado. Refuerzo positivo y sin castigo por error."
            ));
            changed = true;
        }

        if (buscarPorId(2) == null) {
            agregarJuego(new Juego(
                    2,
                    "Cuenta y Conecta",
                    TipoJuego.NUMEROS,
                    1,
                    "Cuenta las figuras y toca el número correcto. 5 rondas completadas = 100 puntos."
            ));
            changed = true;
        }

        if (changed) guardar();
    }

    private Juego buscarPorId(int id) {
        for (Juego j : juegos) {
            if (j.getId() == id) return j;
        }
        return null;
    }

    /** Guarda juegos (incluye habilitado y dificultad global) en data/juegos.json */
    public void guardar() {
        try {
            Path pathArchivo = Paths.get(ARCHIVO_JUEGOS);
            Path carpeta = pathArchivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta); // crea "data" si no existe
            }

            String json = gson.toJson(juegos);
            Files.writeString(pathArchivo, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Carga juegos desde data/juegos.json. Devuelve true si cargó algo válido. */
    private boolean cargarJuegosDesdeArchivo() {
        Path pathArchivo = Paths.get(ARCHIVO_JUEGOS);
        if (!Files.exists(pathArchivo)) return false;

        try {
            String json = Files.readString(pathArchivo, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return false;

            Type tipoLista = new TypeToken<List<Juego>>() {}.getType();
            List<Juego> cargados = gson.fromJson(json, tipoLista);

            if (cargados == null || cargados.isEmpty()) return false;

            // Normalizar por seguridad
            for (Juego j : cargados) {
                if (j.getDificultad() < 1) j.setDificultad(1);
                if (j.getDificultad() > 5) j.setDificultad(5);
            }

            juegos.clear();
            juegos.addAll(cargados);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
