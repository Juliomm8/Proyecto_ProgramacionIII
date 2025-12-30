package com.jasgames.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Juego;
import com.jasgames.model.ResultadoJuego;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ResultadoService {

    private static final String ARCHIVO_RESULTADOS = "data/resultados.json";

    private final List<ResultadoJuego> resultados = new ArrayList<>();

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ResultadoService() {
        cargarDesdeArchivo();
    }

    public void registrarResultado(ResultadoJuego resultado) {
        if (resultado != null) {
            resultados.add(resultado);
            guardarEnArchivo();
        }
    }

    public List<ResultadoJuego> obtenerTodos() {
        return new ArrayList<>(resultados);
    }

    public List<ResultadoJuego> obtenerPorJuego(Juego juego) {
        if (juego == null) return new ArrayList<>();
        return resultados.stream()
                .filter(r -> r.getJuego() != null && r.getJuego().getId() == juego.getId())
                .collect(Collectors.toList());
    }

    public List<ResultadoJuego> obtenerPorJuegoOrdenadosPorPuntajeDesc(Juego juego) {
        return obtenerPorJuego(juego).stream()
                .sorted(Comparator.comparingInt(ResultadoJuego::getPuntaje).reversed())
                .collect(Collectors.toList());
    }

    // ---------------- PERSISTENCIA ----------------

    private void guardarEnArchivo() {
        try {
            Path path = Paths.get(ARCHIVO_RESULTADOS);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            String json = gson.toJson(resultados);
            Files.writeString(path, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarDesdeArchivo() {
        Path path = Paths.get(ARCHIVO_RESULTADOS);
        if (!Files.exists(path)) return;

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;

            Type tipoLista = new TypeToken<List<ResultadoJuego>>(){}.getType();
            List<ResultadoJuego> cargados = gson.fromJson(json, tipoLista);

            resultados.clear();
            if (cargados != null) resultados.addAll(cargados);

        } catch (Exception e) {
            e.printStackTrace();
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
