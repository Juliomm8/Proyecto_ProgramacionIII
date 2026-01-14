package com.jasgames.service;

import com.google.gson.*;
import com.jasgames.model.Juego;
import com.jasgames.model.SesionJuego;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.FileLocks;
import com.jasgames.util.JsonSafeIO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SesionService {

    private static final String ARCHIVO_RESULTADOS = "data/resultados.json";

    private final List<SesionJuego> resultados = new ArrayList<>();
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_RESULTADOS));

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public SesionService() {
        cargarDesdeArchivo();
    }

    public void registrarResultado(SesionJuego resultado) {
        if (resultado != null) {
            ioLock.lock();
            try {
                resultados.add(resultado);
                guardarEnArchivo();
            } finally {
                ioLock.unlock();
            }
        }
    }

    public List<SesionJuego> obtenerTodos() {
        ioLock.lock();
        try {
            return new ArrayList<>(resultados);
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Elimina una sesión por su id (idSesion). Devuelve true si se eliminó.
     */
    public boolean eliminarSesion(String idSesion) {
        if (idSesion == null || idSesion.isBlank()) return false;

        ioLock.lock();
        try {
            boolean removed = resultados.removeIf(s -> idSesion.equals(s.getIdSesion()));
            if (removed) guardarEnArchivo();
            return removed;
        } finally {
            ioLock.unlock();
        }
    }
    /**
     * Elimina una sesión por su id y devuelve la sesión eliminada (si existía).
     * Útil para implementar "Deshacer" (Undo) desde la UI.
     */
    public Optional<SesionJuego> eliminarSesionYDevolver(String idSesion) {
        if (idSesion == null || idSesion.isBlank()) return Optional.empty();

        ioLock.lock();
        try {
            for (int i = 0; i < resultados.size(); i++) {
                SesionJuego s = resultados.get(i);
                if (s != null && idSesion.equals(s.getIdSesion())) {
                    resultados.remove(i);
                    guardarEnArchivo();
                    return Optional.of(s);
                }
            }
            return Optional.empty();
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Restaura una sesión previamente eliminada. Devuelve false si ya existía una sesión con el mismo id.
     */
    public boolean restaurarSesion(SesionJuego sesion) {
        if (sesion == null || sesion.getIdSesion() == null || sesion.getIdSesion().isBlank()) return false;

        ioLock.lock();
        try {
            String id = sesion.getIdSesion();
            for (SesionJuego s : resultados) {
                if (s != null && id.equals(s.getIdSesion())) return false;
            }
            resultados.add(sesion);
            guardarEnArchivo();
            return true;
        } finally {
            ioLock.unlock();
        }
    }

    public List<SesionJuego> obtenerPorJuego(Juego juego) {
        if (juego == null) return new ArrayList<>();
        ioLock.lock();
        try {
            return resultados.stream()
                    .filter(r -> r.getJuego() != null && r.getJuego().getId() == juego.getId())
                    .collect(Collectors.toList());
        } finally {
            ioLock.unlock();
        }
    }

    public List<SesionJuego> obtenerPorJuegoOrdenadosPorPuntajeDesc(Juego juego) {
        return obtenerPorJuego(juego).stream()
                .sorted(Comparator.comparingInt(SesionJuego::getPuntaje).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Devuelve las últimas N sesiones de un niño para un juego (más recientes primero).
     */
    public java.util.List<com.jasgames.model.SesionJuego> obtenerUltimasPorNinoYJuego(int idNino, int idJuego, int limite) {
        ioLock.lock();
        try {
            if (limite <= 0) limite = 1;

            java.util.List<com.jasgames.model.SesionJuego> filtradas = new java.util.ArrayList<>();
            for (com.jasgames.model.SesionJuego s1 : resultados) {
                if (s1 == null) continue;
                if ((s1.getIdEstudiante() != null && s1.getIdEstudiante() == idNino) && s1.getJuego() != null && s1.getJuego().getId() == idJuego) {
                    filtradas.add(s1);
                }
            }

            filtradas.sort((a, b) -> {
                java.time.LocalDateTime fa = (a.getFechaFin() != null) ? a.getFechaFin() : a.getFechaHora();
                java.time.LocalDateTime fb = (b.getFechaFin() != null) ? b.getFechaFin() : b.getFechaHora();
                if (fa == null && fb == null) return 0;
                if (fa == null) return 1;
                if (fb == null) return -1;
                return fb.compareTo(fa);
            });

            if (filtradas.size() > limite) {
                return new java.util.ArrayList<>(filtradas.subList(0, limite));
            }
            return filtradas;
        } finally {
            ioLock.unlock();
        }
    }

    // ---------------- PERSISTENCIA ----------------

    private void guardarEnArchivo() {
        ioLock.lock();
        try {
            Path path = Paths.get(ARCHIVO_RESULTADOS);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            String json = gson.toJson(resultados);
            AtomicFiles.writeStringAtomic(path, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    private void cargarDesdeArchivo() {
        ioLock.lock();
        try {
            Path path = Paths.get(ARCHIVO_RESULTADOS);
            if (!Files.exists(path)) return;

            SesionJuego[] lista = JsonSafeIO.readOrRecover(
                    path,
                    gson,
                    SesionJuego[].class,
                    new SesionJuego[0]
            );
            resultados.clear();
            if (lista != null) resultados.addAll(Arrays.asList(lista));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return (src == null) ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json == null || json.isJsonNull()) return null;
            return LocalDateTime.parse(json.getAsString());
        }
    }
}