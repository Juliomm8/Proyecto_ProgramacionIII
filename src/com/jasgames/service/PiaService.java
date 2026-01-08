package com.jasgames.service;

import com.google.gson.*;
import com.jasgames.model.ObjetivoPIA;
import com.jasgames.model.PIA;
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
    
    /**
     * Aplica automáticamente el progreso de una sesión al PIA activo del niño (si existe).
     * Busca un objetivo no completado para el juego jugado y actualiza sus contadores.
     * También vincula la sesión al PIA y al objetivo.
     * 
     * @param sesion La sesión de juego recién terminada.
     * @return true si se aplicó a algún PIA, false si no había PIA activo o no aplicaba.
     */
    public boolean aplicarSesion(SesionJuego sesion) {
        if (sesion == null || sesion.getIdEstudiante() == null || sesion.getJuego() == null) {
            return false;
        }

        ioLock.lock();
        try {
            // 1. Buscar PIA activo del niño
            PIA pia = null;
            for (PIA p : pias) {
                if (p != null && p.getIdNino() == sesion.getIdEstudiante() && p.isActivo()) {
                    pia = p;
                    break;
                }
            }
            if (pia == null) return false;

            // 2. Buscar objetivo para este juego (que no esté completado)
            ObjetivoPIA objetivo = null;
            for (ObjetivoPIA obj : pia.getObjetivos()) {
                if (obj.getJuegoId() == sesion.getJuego().getId() && !obj.isCompletado()) {
                    objetivo = obj;
                    break; // Tomamos el primero que coincida
                }
            }
            
            // Si no hay objetivo específico, no hacemos nada (o podríamos registrar actividad genérica en el futuro)
            if (objetivo == null) return false;

            // 3. Actualizar progreso
            objetivo.setProgresoRondasCorrectas(objetivo.getProgresoRondasCorrectas() + sesion.getAciertosTotales());
            objetivo.setProgresoSesionesCompletadas(objetivo.getProgresoSesionesCompletadas() + 1);
            
            // 4. Evaluar si se completó
            objetivo.evaluarCompletadoSiAplica();

            // 5. Vincular sesión
            sesion.setIdPia(pia.getIdPia());
            sesion.setIdObjetivoPia(objetivo.getIdObjetivo());

            // 6. Guardar cambios en PIA
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
