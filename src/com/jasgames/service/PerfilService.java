package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.CriterioOrdenNino;
import com.jasgames.model.Nino;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PerfilService {

    // Archivo donde se guardan los niños
    private static final String ARCHIVO_NINOS = "data/ninos.json";

    // Lista interna de niños
    private final List<Nino> ninos = new ArrayList<>();

    // Gson para JSON
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public PerfilService() {
        cargarNinosDesdeArchivo();
    }

    // =========================================================
    // MÉTODOS QUE LA UI USA
    // =========================================================

    /** Usado por PerfilesPanel para llenar la tabla */
    public List<Nino> obtenerTodosNinos() {
        return new ArrayList<>(ninos);
    }

    /** Usado por PerfilesPanel al buscar por ID */
    public Nino buscarNinoPorId(int id) {
        for (Nino n : ninos) {
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    /** Devuelve los IDs de juegos asignados a un niño por su ID. */
    public Set<Integer> getJuegosAsignados(int idNino) {
        Nino nino = buscarNinoPorId(idNino);
        if (nino == null || nino.getJuegosAsignados() == null) {
            return Collections.emptySet();
        }
        // devolvemos una copia para no exponer la colección interna
        return new HashSet<>(nino.getJuegosAsignados());
    }

    /** Reemplaza el conjunto de juegos asignados a un niño y guarda en JSON. */
    public void asignarJuegos(int idNino, Set<Integer> juegosIds) {
        Nino nino = buscarNinoPorId(idNino);
        if (nino == null) {
            return;
        }

        if (juegosIds == null) {
            nino.setJuegosAsignados(new HashSet<>());
        } else {
            nino.setJuegosAsignados(new HashSet<>(juegosIds));
        }

        // Persistimos el cambio en data/ninos.json
        guardarNinosEnArchivo();
    }

    /**
     * Buscar un niño por ID (si el texto es numérico) o por nombre
     * (si no es numérico). Este método lo usa PerfilesPanel.
     */
    public Nino buscarPorIdONombre(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String trimmed = texto.trim();

        // 1) Intentar interpretarlo como ID (int)
        try {
            int idBuscado = Integer.parseInt(trimmed);
            for (Nino n : ninos) {
                if (n.getId() == idBuscado) {
                    return n;
                }
            }
        } catch (NumberFormatException e) {
            // No es un número, seguimos con búsqueda por nombre
        }

        // 2) Buscar por nombre que contenga el texto (ignorando mayúsculas/minúsculas)
        String buscadoLower = trimmed.toLowerCase();
        for (Nino n : ninos) {
            if (n.getNombre() != null &&
                    n.getNombre().toLowerCase().contains(buscadoLower)) {
                return n;
            }
        }

        // Si no se encontró nada
        return null;
    }

    /** Usado por PerfilesPanel para actualizar los datos editados */
    public void actualizarNino(Nino ninoActualizado) {
        if (ninoActualizado == null) return;

        for (int i = 0; i < ninos.size(); i++) {
            if (ninos.get(i).getId() == ninoActualizado.getId()) {
                ninos.set(i, ninoActualizado);
                guardarNinosEnArchivo();
                return;
            }
        }
    }

    /** Registrar un nuevo niño (o reemplazar si el ID ya existe) */
    public void registrarNino(Nino nino) {
        if (nino == null) return;

        // si ya existe ese ID, lo reemplazamos
        eliminarNinoPorId(nino.getId());
        ninos.add(nino);
        guardarNinosEnArchivo();
    }

    /** Eliminar un niño por ID */
    public boolean eliminarNinoPorId(int id) {
        boolean eliminado = ninos.removeIf(n -> n.getId() == id);
        if (eliminado) {
            guardarNinosEnArchivo();
        }
        return eliminado;
    }

    /** Devolver copia de la lista ordenada según el criterio */
    public List<Nino> obtenerNinosOrdenados(CriterioOrdenNino criterio) {
        List<Nino> copia = new ArrayList<>(ninos);
        if (criterio == null) return copia;

        switch (criterio) {
            case ID:
                // id es int
                copia.sort(Comparator.comparingInt(Nino::getId));
                break;
            case NOMBRE:
                copia.sort(Comparator.comparing(
                        Nino::getNombre,
                        String.CASE_INSENSITIVE_ORDER
                ));
                break;
            case EDAD:
                copia.sort(Comparator.comparingInt(Nino::getEdad));
                break;
            case DIAGNOSTICO:
                // por si diagnostico no es String, usamos String.valueOf
                copia.sort(Comparator.comparing(
                        n -> String.valueOf(n.getDiagnostico()),
                        String.CASE_INSENSITIVE_ORDER
                ));
                break;
        }
        return copia;
    }

    public int getDificultadAsignada(int idNino, int idJuego, int difDefault) {
        Nino nino = buscarNinoPorId(idNino);
        if (nino == null) return difDefault;
        return nino.getDificultadJuego(idJuego, difDefault);
    }

    // =========================================================
    // PERSISTENCIA JSON
    // =========================================================
    /** Guarda la lista de niños en data/ninos.json */
    private void guardarNinosEnArchivo() {
        try {
            Path pathArchivo = Paths.get(ARCHIVO_NINOS);
            Path carpeta = pathArchivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta); // crea "data" si no existe
            }

            String json = gson.toJson(ninos);
            Files.writeString(pathArchivo, json, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Carga los niños desde data/ninos.json, si existe */
    private void cargarNinosDesdeArchivo() {
        Path pathArchivo = Paths.get(ARCHIVO_NINOS);
        if (!Files.exists(pathArchivo)) {
            return; // primera vez, no hay archivo
        }

        try {
            String json = Files.readString(pathArchivo, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;

            Type tipoLista = new TypeToken<List<Nino>>() {}.getType();
            List<Nino> cargados = gson.fromJson(json, tipoLista);

            ninos.clear();
            if (cargados != null) {
                ninos.addAll(cargados);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void asignarJuegosConDificultad(int idNino, Set<Integer> juegosIds, Map<Integer, Integer> dificultades) {
        Nino nino = buscarNinoPorId(idNino);
        if (nino == null) return;

        Set<Integer> asignados = (juegosIds == null) ? new HashSet<>() : new HashSet<>(juegosIds);
        nino.setJuegosAsignados(asignados);

        // Si no me pasan dificultades, preservo las actuales (pero limpio las que ya no estén asignadas)
        Map<Integer, Integer> difNueva;
        if (dificultades != null) {
            difNueva = new HashMap<>(dificultades);
        } else {
            difNueva = new HashMap<>(nino.getDificultadPorJuego());
        }

        // Quitar dificultades de juegos que ya no están asignados
        difNueva.keySet().removeIf(idJuego -> !asignados.contains(idJuego));

        nino.setDificultadPorJuego(difNueva);

        guardarNinosEnArchivo();
    }
}
