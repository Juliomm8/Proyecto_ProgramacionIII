package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.CriterioOrdenNino;
import com.jasgames.model.Nino;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.DataBackups;
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

public class PerfilService {

    // Archivo donde se guardan los niños
    private static final String ARCHIVO_NINOS = "data/ninos.json";

    // Lista interna de niños
    private final List<Nino> ninos = new ArrayList<>();
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_NINOS));

    // Gson para JSON
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public PerfilService() {
        cargarNinosDesdeArchivo();
    }

    public void guardarCambios() {
        ioLock.lock();
        try {
            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    // =========================================================
    // MÉTODOS QUE LA UI USA
    // =========================================================

    /** Usado por PerfilesPanel para llenar la tabla */
    public List<Nino> obtenerTodosNinos() {
        ioLock.lock();
        try {
            return new ArrayList<>(ninos);
        } finally {
            ioLock.unlock();
        }
    }

    /** Usado por PerfilesPanel al buscar por ID */
    public Nino buscarNinoPorId(int id) {
        ioLock.lock();
        try {
            for (Nino n : ninos) {
                if (n.getId() == id) {
                    return n;
                }
            }
            return null;
        } finally {
            ioLock.unlock();
        }
    }

    /** Devuelve los IDs de juegos asignados a un niño por su ID. */
    public Set<Integer> getJuegosAsignados(int idNino) {
        ioLock.lock();
        try {
            Nino nino = buscarNinoPorId(idNino);
            if (nino == null || nino.getJuegosAsignados() == null) {
                return Collections.emptySet();
            }
            // devolvemos una copia para no exponer la colección interna
            return new HashSet<>(nino.getJuegosAsignados());
        } finally {
            ioLock.unlock();
        }
    }

    /** Reemplaza el conjunto de juegos asignados a un niño y guarda en JSON. */
    public void asignarJuegos(int idNino, Set<Integer> juegosIds) {
        ioLock.lock();
        try {
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
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Buscar un niño por ID (si el texto es numérico) o por nombre
     * (si no es numérico). Este método lo usa PerfilesPanel.
     */
    public Nino buscarPorIdONombre(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        ioLock.lock();
        try {
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
        } finally {
            ioLock.unlock();
        }
    }

    /** Usado por PerfilesPanel para actualizar los datos editados */
    public void actualizarNino(Nino ninoActualizado) {
        if (ninoActualizado == null) return;

        ioLock.lock();
        try {
            for (int i = 0; i < ninos.size(); i++) {
                if (ninos.get(i).getId() == ninoActualizado.getId()) {
                    ninos.set(i, ninoActualizado);
                    guardarNinosEnArchivo();
                    return;
                }
            }
        } finally {
            ioLock.unlock();
        }
    }

    /** Registrar un nuevo niño (o reemplazar si el ID ya existe) */
    public void registrarNino(Nino nino) {
        if (nino == null) return;

        ioLock.lock();
        try {
            // si ya existe ese ID, lo reemplazamos
            eliminarNinoPorId(nino.getId());
            ninos.add(nino);
            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    /** Eliminar un niño por ID */
    public boolean eliminarNinoPorId(int id) {
        ioLock.lock();
        try {
            boolean eliminado = ninos.removeIf(n -> n.getId() == id);
            if (eliminado) {
                guardarNinosEnArchivo();
            }
            return eliminado;
        } finally {
            ioLock.unlock();
        }
    }

    /** Devolver copia de la lista ordenada según el criterio */
    public List<Nino> obtenerNinosOrdenados(CriterioOrdenNino criterio) {
        ioLock.lock();
        try {
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
        } finally {
            ioLock.unlock();
        }
    }

    public int getDificultadAsignada(int idNino, int idJuego, int difDefault) {
        ioLock.lock();
        try {
            Nino nino = buscarNinoPorId(idNino);
            if (nino == null) return difDefault;
            return nino.getDificultadJuego(idJuego, difDefault);
        } finally {
            ioLock.unlock();
        }
    }

    // =========================================================
    // PERSISTENCIA JSON
    // =========================================================
    /** Guarda la lista de niños en data/ninos.json */
    private void guardarNinosEnArchivo() {
        ioLock.lock();
        try {
            Path pathArchivo = Paths.get(ARCHIVO_NINOS);
            Path carpeta = pathArchivo.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta); // crea "data" si no existe
            }

            // Backup antes de sobrescribir
            DataBackups.backupIfExists(pathArchivo);

            String json = gson.toJson(ninos);
            AtomicFiles.writeStringAtomic(pathArchivo, json, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    /** Carga los niños desde data/ninos.json, si existe */
    private void cargarNinosDesdeArchivo() {
        ioLock.lock();
        try {
            Path pathArchivo = Paths.get(ARCHIVO_NINOS);
            Type tipoLista = new TypeToken<List<Nino>>() {}.getType();

            // Lectura robusta con recuperación si el JSON está corrupto
            List<Nino> cargados = JsonSafeIO.readOrRecover(
                    pathArchivo,
                    gson,
                    tipoLista,
                    new ArrayList<>()
            );

            ninos.clear();
            if (cargados != null) {
                ninos.addAll(cargados);
            }

            boolean cambiado = false;
            for (Nino n : ninos) {
                // Normalizaciones seguras (evita NPE por datos viejos/corruptos)
                if (n.getNombre() == null || n.getNombre().isBlank()) { // puede venir null desde JSON
                    n.setNombre("Sin nombre");
                    cambiado = true;
                }
                if (n.getDiagnostico() == null) {
                    n.setDiagnostico("");
                    cambiado = true;
                }

                if (n.getJuegosAsignados() == null) {
                    n.setJuegosAsignados(new HashSet<>());
                    cambiado = true;
                }
                if (n.getDificultadPorJuego() == null) {
                    n.setDificultadPorJuego(new HashMap<>());
                    cambiado = true;
                }

                // Mapas del modo adaptativo (por compatibilidad con versiones previas)
                if (n.getDificultadAutoPorJuego() == null) {
                    n.setDificultadAutoPorJuego(new HashMap<>());
                    cambiado = true;
                }
                if (n.getCooldownRestantePorJuego() == null) {
                    n.setCooldownRestantePorJuego(new HashMap<>());
                    cambiado = true;
                }
                if (n.getAdaptacionAutomaticaPorJuego() == null) {
                    n.setAdaptacionAutomaticaPorJuego(new HashMap<>());
                    cambiado = true;
                }
            }
            if (cambiado) {
                guardarNinosEnArchivo();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    public void asignarJuegosConDificultad(int idNino, Set<Integer> juegosIds, Map<Integer, Integer> dificultades) {
        ioLock.lock();
        try {
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
        } finally {
            ioLock.unlock();
        }
    }

    public void limpiarDificultadJuegoParaTodos(int idJuego) {
        ioLock.lock();
        try {
            for (Nino n : ninos) {
                if (n.getDificultadPorJuego() != null) {
                    n.getDificultadPorJuego().remove(idJuego);
                }
            }
            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    public void aplicarDificultadJuegoATodos(int idJuego, int nuevaDif, boolean soloSinOverride) {
        ioLock.lock();
        try {
            for (Nino n : ninos) {
                if (n.getJuegosAsignados() == null || !n.getJuegosAsignados().contains(idJuego)) continue;

                Map<Integer, Integer> difs = n.getDificultadPorJuego();
                boolean tieneOverride = (difs != null && difs.containsKey(idJuego));

                if (soloSinOverride && tieneOverride) continue;

                n.setDificultadJuego(idJuego, nuevaDif);
            }

            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Asigna (agrega) un juego a TODOS los niños, preservando sus juegos actuales.
     *
     * @return cuántos niños fueron afectados (no lo tenían y se agregó).
     */
    public int asignarJuegoATodos(int idJuego) {
        ioLock.lock();
        try {
            int afectados = 0;
            for (Nino n : ninos) {
                Set<Integer> asg = n.getJuegosAsignados();
                if (asg == null) {
                    asg = new HashSet<>();
                    n.setJuegosAsignados(asg);
                }
                if (asg.add(idJuego)) {
                    afectados++;
                }
            }

            if (afectados > 0) {
                guardarNinosEnArchivo();
            }
            return afectados;
        } finally {
            ioLock.unlock();
        }
    }

    public int contarNinosEnAula(String aula) {
        if (aula == null) return 0;
        ioLock.lock();
        try {
            int c = 0;
            for (Nino n : ninos) {
                if (n.getAula().equalsIgnoreCase(aula.trim())) c++;
            }
            return c;
        } finally {
            ioLock.unlock();
        }
    }

    public void migrarAula(String aulaOrigen, String aulaDestino) {
        if (aulaOrigen == null || aulaDestino == null) return;

        ioLock.lock();
        try {
            String o = aulaOrigen.trim();
            String d = aulaDestino.trim();

            for (Nino n : ninos) {
                if (n.getAula().equalsIgnoreCase(o)) {
                    n.setAula(d);
                }
            }
            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    public Set<String> obtenerAulasEnUso() {
        ioLock.lock();
        try {
            Set<String> set = new LinkedHashSet<>();
            for (Nino n : ninos) {
                String a = n.getAula();
                if (a != null && !a.isBlank()) set.add(a.trim());
            }
            return set;
        } finally {
            ioLock.unlock();
        }
    }



    /**
     * Reemplaza completamente el catálogo de niños (1 sola escritura).
     * Útil para cargar datos demo / limpiar o restaurar estructuras.
     */
    public void reemplazarTodosNinos(List<Nino> nuevos) {
        ioLock.lock();
        try {
            ninos.clear();

            if (nuevos != null) {
                int nextId = 1;
                for (Nino n : nuevos) {
                    if (n != null && n.getId() >= nextId) nextId = n.getId() + 1;
                }

                Set<Integer> usados = new HashSet<>();
                for (Nino n : nuevos) {
                    if (n == null) continue;

                    // asegurar id único y positivo
                    if (n.getId() <= 0 || usados.contains(n.getId())) {
                        n.setId(nextId++);
                    }
                    usados.add(n.getId());

                    // normalizaciones suaves
                    String nombre = (n.getNombre() == null) ? "" : n.getNombre().trim();
                    if (nombre.isBlank()) nombre = "Sin nombre";
                    n.setNombre(nombre);

                    n.setDiagnostico(n.getDiagnostico()); // null -> ""
                    n.setAula(n.getAula());               // null/"General" -> "Aula Azul"

                    // asegurar estructuras no-nulas por compatibilidad
                    n.getJuegosAsignados();
                    n.getDificultadPorJuego();

                    ninos.add(n);
                }
            }

            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

    /** Limpia todos los niños (1 sola escritura). */
    public void limpiarTodosNinos() {
        ioLock.lock();
        try {
            ninos.clear();
            guardarNinosEnArchivo();
        } finally {
            ioLock.unlock();
        }
    }

}
