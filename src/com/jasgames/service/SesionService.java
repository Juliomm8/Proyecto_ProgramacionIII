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
