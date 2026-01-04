package com.jasgames.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.UUID;

public final class AtomicFiles {

    private AtomicFiles() {}

    public static void writeStringAtomic(Path target, String content, Charset charset) throws IOException {
        if (target == null) throw new IllegalArgumentException("target null");

        Path dir = target.getParent();
        if (dir != null) Files.createDirectories(dir);

        String base = target.getFileName().toString();
        String prefix = (base.length() >= 3 ? base : "tmp") + ".tmp-";
        Path tmp = Files.createTempFile(dir != null ? dir : Paths.get("."), prefix, "-" + UUID.randomUUID());

        try {
            Files.writeString(tmp, content, charset, StandardOpenOption.TRUNCATE_EXISTING);

            // Intentar movimiento atómico; si el FS no lo soporta, caer a move normal.
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }

        } finally {
            // Si falló antes del move, intenta limpiar el tmp
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }
}
