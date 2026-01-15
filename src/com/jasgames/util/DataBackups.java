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

    
    /** Ruta absoluta a data/backups (puede no existir). */
    public static Path getBackupsRoot() {
        return Paths.get("data").toAbsolutePath().normalize().resolve("backups");
    }

    /** Lista carpetas de backup (más reciente primero). */
    public static List<Path> listBackupDirs() {
        List<Path> dirs = new ArrayList<>();
        try {
            Path root = getBackupsRoot();
            if (!Files.exists(root) || !Files.isDirectory(root)) return dirs;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path p : stream) {
                    if (Files.isDirectory(p)) dirs.add(p);
                }
            }

            dirs.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        } catch (Exception ignored) {}
        return dirs;
    }

    /** Resultado de una restauración desde backup. */
    public static final class RestoreResult {
        public final boolean ok;
        public final String message;
        public final int restoredFiles;
        public final List<String> files;

        public RestoreResult(boolean ok, String message, int restoredFiles, List<String> files) {
            this.ok = ok;
            this.message = message;
            this.restoredFiles = restoredFiles;
            this.files = files;
        }
    }

    /**
     * Restaura los archivos contenidos en una carpeta de backup hacia data/.
     * Antes de sobrescribir cada archivo, crea un backup del archivo actual.
     */
    public static RestoreResult restoreBackupDir(Path backupDir) {
        if (backupDir == null) {
            return new RestoreResult(false, "No se seleccionó ningún backup.", 0, new ArrayList<>());
        }

        try {
            Path dataDir = Paths.get("data").toAbsolutePath().normalize();
            Path root = getBackupsRoot();
            Path b = backupDir.toAbsolutePath().normalize();

            if (!b.startsWith(root)) {
                return new RestoreResult(false, "Backup inválido (fuera de data/backups).", 0, new ArrayList<>());
            }
            if (!Files.exists(b) || !Files.isDirectory(b)) {
                return new RestoreResult(false, "La carpeta de backup no existe.", 0, new ArrayList<>());
            }

            int restored = 0;
            List<String> restoredNames = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(b)) {
                for (Path src : stream) {
                    if (!Files.isRegularFile(src)) continue;

                    Path target = dataDir.resolve(src.getFileName().toString());

                    // Backup de seguridad del archivo actual (si existía)
                    backupIfExists(target);

                    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                    restored++;
                    restoredNames.add(src.getFileName().toString());
                }
            }

            if (restored == 0) {
                return new RestoreResult(false, "El backup seleccionado no contiene archivos para restaurar.", 0, restoredNames);
            }

            return new RestoreResult(true, "Datos restaurados correctamente.", restored, restoredNames);

        } catch (Exception ex) {
            return new RestoreResult(false, "Error al restaurar: " + ex.getMessage(), 0, new ArrayList<>());
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
