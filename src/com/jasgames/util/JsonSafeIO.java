package com.jasgames.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class JsonSafeIO {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private JsonSafeIO() {}

    public static <T> T readOrRecover(Path path, Gson gson, Class<T> clazz, T defaultValue) {
        try {
            if (!Files.exists(path)) {
                // crea si no existe
                AtomicFiles.writeStringAtomic(path, gson.toJson(defaultValue), StandardCharsets.UTF_8);
                return defaultValue;
            }

            String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                backupAndReset(path, gson.toJson(defaultValue));
                return defaultValue;
            }

            return gson.fromJson(raw, clazz);

        } catch (JsonSyntaxException ex) {
            // JSON inválido
            backupAndReset(path, gson.toJson(defaultValue));
            return defaultValue;

        } catch (Exception ex) {
            // IO u otro error inesperado -> no caigas
            System.err.println("Error leyendo " + path + ": " + ex.getMessage());
            return defaultValue;
        }
    }

    private static void backupAndReset(Path path, String cleanJson) {
        try {
            if (Files.exists(path)) {
                String ts = LocalDateTime.now().format(TS);
                Path bak = path.resolveSibling(path.getFileName().toString() + ".bak-" + ts);
                try {
                    Files.copy(path, bak, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
            AtomicFiles.writeStringAtomic(path, cleanJson, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("No se pudo recuperar " + path + ": " + e.getMessage());
        }
    }
}
