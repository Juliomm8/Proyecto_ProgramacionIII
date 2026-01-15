package com.jasgames.util;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Backups simples para archivos en la carpeta data/.
 *
 * Crea copias en: data/backups/YYYY-MM-DD_HH-mm-ss-SSS/<archivo>
 *
 * - No interrumpe el flujo si algo falla.
 * - Mantiene un máximo de carpetas de backup (global) para no crecer infinito.
 */
public final class DataBackups {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final int MAX_BACKUP_DIRS = 30;

    private DataBackups() {}

    /**
     * Crea un backup del archivo (si existe) antes de sobrescribirlo.
     *
     * @param file archivo dentro de data/ (ej: data/ninos.json)
     */
    public static void backupIfExists(Path file) {
        if (file == null) return;

        try {
            Path normalized = file.toAbsolutePath().normalize();
            String n = normalized.toString();

            // Evitar bucles y basura
            if (n.contains(FileSystems.getDefault().getSeparator() + "backups" + FileSystems.getDefault().getSeparator())) return;
            if (!Files.exists(normalized)) return;
            if (Files.isDirectory(normalized)) return;

            Path dataDir = Paths.get("data").toAbsolutePath().normalize();
            // Si no está dentro de data/, no hacemos backup
            if (!normalized.startsWith(dataDir)) return;

            Path backupsRoot = dataDir.resolve("backups");
            Files.createDirectories(backupsRoot);

            String ts = LocalDateTime.now().format(TS);
            Path backupDir = backupsRoot.resolve(ts);
            Files.createDirectories(backupDir);

            Path target = backupDir.resolve(file.getFileName());
            Files.copy(normalized, target, StandardCopyOption.REPLACE_EXISTING);

            cleanupOldBackups(backupsRoot);

        } catch (Exception ignored) {
            // No romper el programa por un backup
        }
    }

    private static void cleanupOldBackups(Path backupsRoot) {
        try {
            if (backupsRoot == null || !Files.isDirectory(backupsRoot)) return;

            List<Path> dirs = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(backupsRoot)) {
                for (Path p : ds) {
                    if (Files.isDirectory(p)) dirs.add(p);
                }
            }

            // Orden por nombre (timestamp) ascendente
            dirs.sort(Comparator.comparing(p -> p.getFileName().toString()));
            int excess = dirs.size() - MAX_BACKUP_DIRS;
            if (excess <= 0) return;

            for (int i = 0; i < excess; i++) {
                deleteRecursive(dirs.get(i));
            }
        } catch (Exception ignored) {
        }
    }

    private static void deleteRecursive(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        // Eliminar hijos primero
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {}
                    });
        } catch (NoSuchFileException ignored) {
        }
    }
}
