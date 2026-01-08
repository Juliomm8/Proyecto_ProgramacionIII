package com.jasgames.service;

import com.google.gson.*;
import com.jasgames.model.ObjetivoPIA;
import com.jasgames.model.PIA;
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
import java.util.function.Consumer;

public class PiaService {

    private static final String ARCHIVO_PIAS = "data/pias.json";

    private final List<PIA> pias = new ArrayList<>();
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_PIAS));

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public PiaService() {
        cargarDesdeArchivo();
    }

    public List<PIA> obtenerTodos() {
        ioLock.lock();
        try {
            return new ArrayList<>(pias);
        } finally {
            ioLock.unlock();
        }
    }

    public List<PIA> obtenerPorNino(int idNino) {
        ioLock.lock();
        try {
            List<PIA> out = new ArrayList<>();
            for (PIA p : pias) {
                if (p != null && p.getIdNino() == idNino) out.add(p);
            }
            return out;
        } finally {
            ioLock.unlock();
        }
    }

    public PIA obtenerActivo(int idNino) {
        ioLock.lock();
        try {
            for (PIA p : pias) {
                if (p != null && p.getIdNino() == idNino && p.isActivo()) return p;
            }
            return null;
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Upsert por idPia. Si el PIA viene activo, desactiva otros PIAs del mismo niño.
     */
    public PIA guardar(PIA pia) {
        if (pia == null) return null;

        ioLock.lock();
        try {
            int idx = indexOf(pia.getIdPia());
            if (idx >= 0) pias.set(idx, pia);
            else pias.add(pia);

            if (pia.isActivo()) {
                for (PIA other : pias) {
                    if (other == null) continue;
                    if (Objects.equals(other.getIdPia(), pia.getIdPia())) continue;
                    if (other.getIdNino() == pia.getIdNino() && other.isActivo()) {
                        other.setActivo(false);
                        if (other.getFechaCierre() == null) other.setFechaCierre(LocalDateTime.now());
                    }
                }
            }

            guardarEnArchivo();
            return pia;
        } finally {
            ioLock.unlock();
        }
    }

    public boolean cerrarPIA(String idPia) {
        if (idPia == null) return false;

        ioLock.lock();
        try {
            PIA p = obtenerPorIdInterno(idPia);
            if (p == null) return false;
            p.cerrar();
            guardarEnArchivo();
            return true;
        } finally {
            ioLock.unlock();
        }
    }

    public boolean actualizarObjetivo(String idPia, String idObjetivo, Consumer<ObjetivoPIA> mutator) {
        if (idPia == null || idObjetivo == null || mutator == null) return false;

        ioLock.lock();
        try {
            PIA p = obtenerPorIdInterno(idPia);
            if (p == null) return false;

            ObjetivoPIA obj = p.getObjetivoPorId(idObjetivo);
            if (obj == null) return false;

            mutator.accept(obj);
            obj.evaluarCompletadoSiAplica();

            guardarEnArchivo();
            return true;
        } finally {
            ioLock.unlock();
        }
    }

    // ----------------- helpers internos -----------------

    private int indexOf(String idPia) {
        if (idPia == null) return -1;
        for (int i = 0; i < pias.size(); i++) {
            PIA p = pias.get(i);
            if (p != null && idPia.equals(p.getIdPia())) return i;
        }
        return -1;
    }

    private PIA obtenerPorIdInterno(String idPia) {
        int idx = indexOf(idPia);
        if (idx < 0) return null;
        return pias.get(idx);
    }

    // ---------------- PERSISTENCIA ----------------

    private void guardarEnArchivo() {
        try {
            Path path = Paths.get(ARCHIVO_PIAS);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            String json = gson.toJson(pias);
            AtomicFiles.writeStringAtomic(path, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarDesdeArchivo() {
        try {
            Path path = Paths.get(ARCHIVO_PIAS);
            if (!Files.exists(path)) return;

            PIA[] lista = JsonSafeIO.readOrRecover(
                    path,
                    gson,
                    PIA[].class,
                    new PIA[0]
            );

            pias.clear();
            if (lista != null) pias.addAll(Arrays.asList(lista));

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
