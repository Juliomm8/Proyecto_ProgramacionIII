package com.jasgames.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.Type;
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
            AppLog.error("Error leyendo " + path + ": " + ex.getMessage(), ex);
            return defaultValue;
        }
    }

    /**
     * Variante que soporta tipos genéricos (por ejemplo: List<Nino>) usando TypeToken.
     */
    public static <T> T readOrRecover(Path path, Gson gson, Type type, T defaultValue) {
        try {
            if (!Files.exists(path)) {
                AtomicFiles.writeStringAtomic(path, gson.toJson(defaultValue), StandardCharsets.UTF_8);
                return defaultValue;
            }

            String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                backupAndReset(path, gson.toJson(defaultValue));
                return defaultValue;
            }

            return gson.fromJson(raw, type);

        } catch (JsonSyntaxException ex) {
            backupAndReset(path, gson.toJson(defaultValue));
            return defaultValue;

        } catch (Exception ex) {
            AppLog.error("Error leyendo " + path + ": " + ex.getMessage(), ex);
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

                // Backup adicional en data/backups/ (no crítico)
                try { DataBackups.backupIfExists(path); } catch (Exception ignored) {}
            }
            AtomicFiles.writeStringAtomic(path, cleanJson, StandardCharsets.UTF_8);
        } catch (Exception e) {
            AppLog.error("No se pudo recuperar " + path + ": " + e.getMessage(), e);
        }
    }
}
