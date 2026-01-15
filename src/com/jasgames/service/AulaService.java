package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Aula;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.DataBackups;
import com.jasgames.util.FileLocks;
import com.jasgames.util.JsonSafeIO;

import java.awt.Color;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class AulaService {

    private static final String ARCHIVO_AULAS = "data/aulas.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final List<Aula> aulas = new ArrayList<>();
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_AULAS));

    public AulaService(PerfilService perfilService) {
        cargar();
        inicializarSiVacio();
        sincronizarConAulasDeNinos(perfilService); // por compatibilidad si existían aulas en ninos.json
        guardar();
    }

    /**
     * Fuerza una recarga del catálogo desde disco (data/aulas.json).
     * Útil cuando otras pantallas modifican aulas y se quiere refrescar combos.
     */
    public void refrescarDesdeDisco(PerfilService perfilService) {
        ioLock.lock();
        try {
            cargar();
            inicializarSiVacio();
            // mantiene compatibilidad (si existían aulas en ninos.json)
            if (perfilService != null) sincronizarConAulasDeNinos(perfilService);
        } finally {
            ioLock.unlock();
        }
    }

    /** Atajo: recarga desde disco sin sincronizar con perfiles. */
    public void refrescarDesdeDisco() {
        refrescarDesdeDisco(null);
    }

    public List<Aula> obtenerTodas() {
        ioLock.lock();
        try {
            return new ArrayList<>(aulas);
        } finally {
            ioLock.unlock();
        }
    }

    public List<String> obtenerNombres() {
        ioLock.lock();
        try {
            List<String> out = new ArrayList<>();
            for (Aula a : aulas) out.add(a.getNombre());
            out.sort(String.CASE_INSENSITIVE_ORDER);
            return out;
        } finally {
            ioLock.unlock();
        }
    }

    public Aula buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        ioLock.lock();
        try {
            for (Aula a : aulas) {
                if (a.getNombre() != null && a.getNombre().equalsIgnoreCase(nombre.trim())) return a;
            }
            return null;
        } finally {
            ioLock.unlock();
        }
    }

    public Color colorDeAula(String nombreAula) {
        ioLock.lock();
        try {
            Aula a = buscarPorNombre(nombreAula);
            if (a == null) return new Color(149, 165, 166);
            return parseHex(a.getColorHex());
        } finally {
            ioLock.unlock();
        }
    }

    public void crearAula(String nombre, String colorHex) {
        ioLock.lock();
        try {
            String n = normalizarNombre(nombre);
            if (n.isBlank()) throw new IllegalArgumentException("Nombre de aula vacío.");

            if (buscarPorNombre(n) != null) throw new IllegalArgumentException("Esa aula ya existe.");

            aulas.add(new Aula(n, normalizarHex(colorHex)));
            guardar();
        } finally {
            ioLock.unlock();
        }
    }

    public void cambiarColor(String nombreAula, String colorHex) {
        ioLock.lock();
        try {
            Aula a = buscarPorNombre(nombreAula);
            if (a == null) throw new IllegalArgumentException("No existe esa aula.");
            a.setColorHex(normalizarHex(colorHex));
            guardar();
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Elimina aula. Si hay niños en esa aula, migra a aulaDestino (obligatorio).
     * Por seguridad, no permite dejar el sistema sin "Aula Azul".
     */
    public void eliminarAula(String aulaEliminar, String aulaDestino, PerfilService perfilService) {
        ioLock.lock();
        try {
            Aula a = buscarPorNombre(aulaEliminar);
            if (a == null) throw new IllegalArgumentException("No existe esa aula.");

            // Guard rail: mantener Aula Azul siempre (porque Nino.getAula() usa ese default)
            if ("Aula Azul".equalsIgnoreCase(a.getNombre())) {
                throw new IllegalArgumentException("No se puede eliminar Aula Azul (es el aula por defecto del sistema).");
            }

            int cant = perfilService.contarNinosEnAula(a.getNombre());

            if (cant > 0) {
                if (aulaDestino == null || aulaDestino.isBlank())
                    throw new IllegalArgumentException("Debes elegir un aula destino para migrar estudiantes.");

                if (aulaDestino.equalsIgnoreCase(a.getNombre()))
                    throw new IllegalArgumentException("El aula destino no puede ser la misma.");

                if (buscarPorNombre(aulaDestino) == null)
                    throw new IllegalArgumentException("El aula destino no existe.");

                perfilService.migrarAula(a.getNombre(), aulaDestino);
            }

            aulas.removeIf(x -> x.getNombre() != null && x.getNombre().equalsIgnoreCase(a.getNombre()));
            guardar();
        } finally {
            ioLock.unlock();
        }
    }

    // ----------------- Interno -----------------

    private void cargar() {
        ioLock.lock();
        try {
            aulas.clear();
            Path p = Paths.get(ARCHIVO_AULAS);
            Aula[] arr = JsonSafeIO.readOrRecover(p, gson, Aula[].class, new Aula[0]);
            this.aulas.addAll(Arrays.asList(arr));
        } finally {
            ioLock.unlock();
        }
    }

    private void guardar() {
        ioLock.lock();
        try {
            Path p = Paths.get(ARCHIVO_AULAS);
            Path dir = p.getParent();
            if (dir != null) Files.createDirectories(dir);

            // Backup antes de sobrescribir
            DataBackups.backupIfExists(p);

            AtomicFiles.writeStringAtomic(p, gson.toJson(aulas), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    private void inicializarSiVacio() {
        ioLock.lock();
        try {
            // Si no hay archivo o viene vacío, crea base
            if (aulas.isEmpty()) {
                aulas.add(new Aula("Aula Azul", "#3498DB"));
                aulas.add(new Aula("Aula Roja", "#E74C3C"));
                aulas.add(new Aula("Aula Verde", "#2ECC71"));
                aulas.add(new Aula("Aula Amarilla", "#F1C40F"));
                aulas.add(new Aula("Aula Morada", "#9B59B6"));
            }

            // asegurar Aula Azul siempre
            if (buscarPorNombre("Aula Azul") == null) {
                aulas.add(new Aula("Aula Azul", "#3498DB"));
            }
        } finally {
            ioLock.unlock();
        }
    }

    private void sincronizarConAulasDeNinos(PerfilService perfilService) {
        ioLock.lock();
        try {
            // Si en ninos.json existían aulas extra, se agregan con color gris por defecto
            Set<String> aulasEnNinos = perfilService.obtenerAulasEnUso();
            for (String nombre : aulasEnNinos) {
                if (buscarPorNombre(nombre) == null) {
                    aulas.add(new Aula(nombre, "#95A5A6")); // gris
                }
            }
        } finally {
            ioLock.unlock();
        }
    }

    private String normalizarNombre(String n) {
        return (n == null) ? "" : n.trim();
    }

    private String normalizarHex(String hex) {
        if (hex == null || hex.isBlank()) return "#95A5A6";
        String h = hex.trim().toUpperCase(Locale.ROOT);
        if (!h.startsWith("#")) h = "#" + h;
        return h;
    }

    private Color parseHex(String hex) {
        try {
            String h = normalizarHex(hex);
            return Color.decode(h);
        } catch (Exception e) {
            return new Color(149, 165, 166);
        }
    }

    public static String toHex(Color c) {
        if (c == null) return "#95A5A6";
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }


    /**
     * Reemplaza completamente el catálogo de aulas (1 sola escritura).
     * Útil para cargar datos demo / limpiar o restaurar estructuras.
     */
    public void reemplazarAulas(List<Aula> nuevas) {
        ioLock.lock();
        try {
            aulas.clear();
            if (nuevas != null) {
                Set<String> usados = new HashSet<>();
                for (Aula a : nuevas) {
                    if (a == null) continue;

                    String nombre = (a.getNombre() == null) ? "" : a.getNombre().trim();
                    if (nombre.isBlank()) continue;

                    String key = nombre.toLowerCase(Locale.ROOT);
                    if (usados.contains(key)) continue;
                    usados.add(key);

                    String hex = normalizarHex(a.getColorHex());
                    aulas.add(new Aula(nombre, hex));
                }
            }

            // Mantener Aula Azul siempre (default del sistema)
            if (buscarPorNombre("Aula Azul") == null) {
                aulas.add(new Aula("Aula Azul", "#3498DB"));
            }

            guardar();
        } finally {
            ioLock.unlock();
        }
    }

    /**
     * Restaura las aulas por defecto del sistema.
     * No migra niños (úsalo después de limpiar perfiles o migrar manualmente).
     */
    public void resetAulasPorDefecto() {
        ioLock.lock();
        try {
            aulas.clear();
            aulas.add(new Aula("Aula Azul", "#3498DB"));
            aulas.add(new Aula("Aula Roja", "#E74C3C"));
            aulas.add(new Aula("Aula Verde", "#2ECC71"));
            aulas.add(new Aula("Aula Amarilla", "#F1C40F"));
            aulas.add(new Aula("Aula Morada", "#9B59B6"));
            guardar();
        } finally {
            ioLock.unlock();
        }
    }

}
