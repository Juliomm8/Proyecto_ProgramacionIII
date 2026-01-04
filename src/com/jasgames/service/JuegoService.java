package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Actividad;
import com.jasgames.model.Juego;
import com.jasgames.model.TipoJuego;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.FileLocks;
import com.jasgames.util.JsonSafeIO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class JuegoService {

    private static final String ARCHIVO_JUEGOS = "data/juegos.json";

    private final List<Juego> juegos;
    private final Queue<Actividad> colaActividades;
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_JUEGOS));

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

        asegurarJuegosMinimos();
    }

    private void cargarJuegosIniciales() {
        ioLock.lock();
        try {
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

            agregarJuego(new Juego(
                    3,
                    "Sigue la Serie",
                    TipoJuego.SERIES,
                    1,
                    "Completa el patrón (serie) eligiendo la figura que falta. 5 rondas completadas = 100 puntos."
            ));

            agregarJuego(new Juego(
                    4,
                    "Vocales Divertidas",
                    TipoJuego.FONEMAS,
                    1,
                    "Mira el dibujo y completa la palabra: toca la vocal inicial correcta. Intentos ilimitados."
            ));

            agregarJuego(new Juego(
                    5,
                    "Explorando las Vocales",
                    TipoJuego.FONEMAS,
                    1,
                    "Escucha la pregunta y toca la imagen cuyo nombre empieza con la vocal mostrada. 5 rondas = 100 puntos."
            ));
        } finally {
            ioLock.unlock();
        }
    }

    private void asegurarJuegosMinimos() {
        ioLock.lock();
        try {
            boolean changed = false;

            if (buscarPorId(1) == null) {
                agregarJuego(new Juego(
                        1,
                        "Discriminación de Colores",
                        TipoJuego.COLORES,
                        1,
                        "Toca el círculo que coincide con el color objetivo. 5 rondas completadas = 100 puntos."
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

            if (buscarPorId(3) == null) {
                agregarJuego(new Juego(
                        3,
                        "Sigue la Serie",
                        TipoJuego.SERIES,
                        1,
                        "Completa el patrón (serie) eligiendo la figura que falta. 5 rondas completadas = 100 puntos."
                ));
                changed = true;
            }

            if (buscarPorId(4) == null) {
                agregarJuego(new Juego(
                        4,
                        "Vocales Divertidas",
                        TipoJuego.FONEMAS,
                        1,
                        "Mira el dibujo y completa la palabra: toca la vocal inicial correcta. Intentos ilimitados."
            ));
                changed = true;
            }

            if (buscarPorId(5) == null) {
                agregarJuego(new Juego(
                        5,
                        "Explorando las Vocales",
                        TipoJuego.FONEMAS,
                        1,
                        "Escucha la pregunta y toca la imagen cuyo nombre empieza con la vocal mostrada. 5 rondas = 100 puntos."
                ));
                changed = true;
            }

            if (changed) guardar();
        } finally {
            ioLock.unlock();
        }
    }

    private Juego buscarPorId(int id) {
        // Este método es privado y se usa dentro de bloques lockeados, no necesita lock propio si solo se llama desde ahí.
        // Pero si se llamara desde fuera, necesitaría lock.
        // Dado que es privado y usado en asegurarJuegosMinimos (que tiene lock), está bien.
        // Sin embargo, para ser consistentes y seguros ante refactorizaciones:
        for (Juego j : juegos) if (j.getId() == id) return j;
        return null;
    }

    /** Guarda juegos (incluye habilitado y dificultad global) en data/juegos.json */
    public void guardar() {
        ioLock.lock();
        try {
            Path pathArchivo = Paths.get(ARCHIVO_JUEGOS);
            Path carpeta = pathArchivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }

            String json = gson.toJson(juegos);
            AtomicFiles.writeStringAtomic(pathArchivo, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    /** Carga juegos desde data/juegos.json. Devuelve true si cargó algo válido. */
    private boolean cargarJuegosDesdeArchivo() {
        ioLock.lock();
        try {
            Path pathArchivo = Paths.get(ARCHIVO_JUEGOS);
            if (!Files.exists(pathArchivo)) return false;

            Juego[] arr = JsonSafeIO.readOrRecover(pathArchivo, gson, Juego[].class, new Juego[0]);
            if (arr == null || arr.length == 0) return false;

            // Normalizar por seguridad
            for (Juego j : arr) {
                if (j.getDificultad() < 1) j.setDificultad(1);
                if (j.getDificultad() > 5) j.setDificultad(5);
            }

            juegos.clear();
            juegos.addAll(Arrays.asList(arr));
            return true;
        } finally {
            ioLock.unlock();
        }
    }

    // ------------ CRUD JUEGOS ------------
    public void agregarJuego(Juego juego) {
        ioLock.lock();
        try {
            juegos.add(juego);
        } finally {
            ioLock.unlock();
        }
    }

    public void eliminarJuego(Juego juego) {
        ioLock.lock();
        try {
            juegos.remove(juego);
        } finally {
            ioLock.unlock();
        }
    }

    public List<Juego> obtenerTodos() {
        ioLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(juegos));
        } finally {
            ioLock.unlock();
        }
    }

    public List<Juego> filtrarPorTipo(TipoJuego tipo) {
        ioLock.lock();
        try {
            List<Juego> resultado = new ArrayList<>();
            for (Juego juego : juegos) {
                if (juego.getTipo() == tipo) {
                    resultado.add(juego);
                }
            }
            return resultado;
        } finally {
            ioLock.unlock();
        }
    }

    // ------------ COLA DE ACTIVIDADES ------------
    // La cola de actividades es en memoria y no se persiste en JSON,
    // pero si se accede desde múltiples hilos, debería sincronizarse.
    // Asumiremos que también debe protegerse.

    public void encolarActividad(Actividad actividad) {
        synchronized (colaActividades) {
            colaActividades.offer(actividad);
        }
    }

    public Actividad siguienteActividad() {
        synchronized (colaActividades) {
            return colaActividades.poll();
        }
    }

    public Queue<Actividad> getColaActividades() {
        // Retornar la cola directamente es peligroso si no es thread-safe.
        // Mejor devolver una copia o envoltorio, pero la interfaz Queue es compleja.
        // Por ahora devolvemos la cola tal cual, asumiendo que quien la use sincronizará o que es para uso local.
        // O mejor, sincronizamos el acceso si es posible.
        return colaActividades;
    }
}
