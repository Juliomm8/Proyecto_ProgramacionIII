package com.jasgames.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jasgames.model.Docente;
import com.jasgames.util.AtomicFiles;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AutenticacionService {

    private static final String ARCHIVO_DOCENTES = "data/docentes.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Docente> docentes = new ArrayList<>();

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

        String u = usuario.trim().toLowerCase(Locale.ROOT);

        for (Docente d : docentes) {
            if (d.getUsuario() != null && d.getUsuario().trim().equalsIgnoreCase(u)) {
                if (d.getSalt() == null || d.getPasswordHash() == null) return Optional.empty();

                boolean ok = ContrasenaUtil.verificar(passwordPlano, d.getSalt(), d.getPasswordHash());
                return ok ? Optional.of(d) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    // ---------------- internos ----------------

    private void asegurarArchivoExiste() {
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            if (!Files.exists(path)) {
                AtomicFiles.writeStringAtomic(path, "[]", StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarDocentes() {
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) json = "[]";

            Type tipoLista = new TypeToken<List<Docente>>() {}.getType();
            List<Docente> cargados = gson.fromJson(json, tipoLista);

            docentes.clear();
            if (cargados != null) docentes.addAll(cargados);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guardarDocentes() {
        try {
            Path path = Paths.get(ARCHIVO_DOCENTES);
            String json = gson.toJson(docentes);
            AtomicFiles.writeStringAtomic(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Convierte password plano -> salt + passwordHash y borra el password */
    private boolean migrarPasswordsPlanosSiExisten() {
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
    }
}
