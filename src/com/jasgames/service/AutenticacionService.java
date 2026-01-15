package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Docente;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.DataBackups;
import com.jasgames.util.FileLocks;
import com.jasgames.util.JsonSafeIO;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class AutenticacionService {

    private static final String ARCHIVO_DOCENTES = "data/docentes.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Docente> docentes = new ArrayList<>();
    private final ReentrantLock ioLock = FileLocks.of(Paths.get(ARCHIVO_DOCENTES));

    public AutenticacionService() {
        asegurarArchivoExiste();
        cargarDocentes();

        // Si el JSON trae "password" (plano) y no trae hash/salt, lo migra.
        boolean migrado = migrarPasswordsPlanosSiExisten();
        if (migrado) {
            guardarDocentes();
        }
    }

    /** Login clásico: usuario + contraseña -> retorna el Docente logueado */
    public Optional<Docente> login(String usuario, String passwordPlano) {
        if (usuario == null || passwordPlano == null) return Optional.empty();

        ioLock.lock();
        try {
            String u = usuario.trim().toLowerCase(Locale.ROOT);

            for (Docente d : docentes) {
                if (d.getUsuario() != null && d.getUsuario().trim().equalsIgnoreCase(u)) {
                    // Si el docente tiene password plano pero no hash, intentamos migrarlo al vuelo (caso raro si falló el constructor)
                    if ((d.getSalt() == null || d.getPasswordHash() == null) && d.getPassword() != null) {
                         String salt = ContrasenaUtil.generarSaltBase64();
                         String hash = ContrasenaUtil.hashPasswordBase64(d.getPassword(), salt);
                         d.setSalt(salt);
                         d.setPasswordHash(hash);
                         d.setPassword(null);
                         guardarDocentes();
                    }

                    if (d.getSalt() == null || d.getPasswordHash() == null) return Optional.empty();

                    boolean ok = ContrasenaUtil.verificar(passwordPlano, d.getSalt(), d.getPasswordHash());
                    return ok ? Optional.of(d) : Optional.empty();
                }
            }
            return Optional.empty();
        } finally {
            ioLock.unlock();
        }
    }

    

    /**
     * Registro de nuevo docente.
     * Retorna el Docente creado o vacío si no se pudo (usuario repetido o datos inválidos).
     */
    public Optional<Docente> registrarDocente(String usuario, String nombre, String passwordPlano) {
        if (usuario == null || nombre == null || passwordPlano == null) return Optional.empty();

        String u = usuario.trim().toLowerCase(Locale.ROOT);
        String n = nombre.trim();

        if (u.isBlank() || n.isBlank() || passwordPlano.isBlank()) return Optional.empty();
        if (u.length() < 3 || passwordPlano.length() < 4) return Optional.empty();

        ioLock.lock();
        try {
            // usuario único
            for (Docente d : docentes) {
                if (d.getUsuario() != null && d.getUsuario().trim().equalsIgnoreCase(u)) {
                    return Optional.empty();
                }
            }

            int nextId = 1;
            for (Docente d : docentes) {
                if (d != null && d.getId() >= nextId) nextId = d.getId() + 1;
            }

            String salt = ContrasenaUtil.generarSaltBase64();
            String hash = ContrasenaUtil.hashPasswordBase64(passwordPlano, salt);

            Docente nuevo = new Docente();
            nuevo.setId(nextId);
            nuevo.setUsuario(u);
            nuevo.setNombre(n);
            nuevo.setSalt(salt);
            nuevo.setPasswordHash(hash);

            // seguridad: eliminar password plano
            nuevo.setPassword(null);

            docentes.add(nuevo);
            guardarDocentes();
            return Optional.of(nuevo);
        } finally {
            ioLock.unlock();
        }
    }

// ---------------- internos ----------------

    private void asegurarArchivoExiste() {
        ioLock.lock();
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            if (!Files.exists(path)) {
                AtomicFiles.writeStringAtomic(path, "[]", StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    private void cargarDocentes() {
        ioLock.lock();
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);
            Docente[] arr = JsonSafeIO.readOrRecover(path, gson, Docente[].class, new Docente[0]);
            this.docentes.clear();
            this.docentes.addAll(Arrays.asList(arr));
        } finally {
            ioLock.unlock();
        }
    }

    private void guardarDocentes() {
        ioLock.lock();
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);

            // Backup antes de sobrescribir
            DataBackups.backupIfExists(path);

            String json = gson.toJson(docentes);
            AtomicFiles.writeStringAtomic(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLock.unlock();
        }
    }

    /** Convierte password plano -> salt + passwordHash y borra el password */
    private boolean migrarPasswordsPlanosSiExisten() {
        ioLock.lock();
        try {
            boolean migrado = false;

            for (Docente d : docentes) {
                boolean faltaHash = (d.getPasswordHash() == null || d.getPasswordHash().isBlank()
                        || d.getSalt() == null || d.getSalt().isBlank());

                if (faltaHash && d.getPassword() != null && !d.getPassword().isBlank()) {
                    String salt = ContrasenaUtil.generarSaltBase64();
                    String hash = ContrasenaUtil.hashPasswordBase64(d.getPassword(), salt);

                    d.setSalt(salt);
                    d.setPasswordHash(hash);

                    // seguridad: eliminar password plano
                    d.setPassword(null);

                    migrado = true;
                }
            }
            return migrado;
        } finally {
            ioLock.unlock();
        }
    }
}
