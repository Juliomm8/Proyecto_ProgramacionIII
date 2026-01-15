package com.jasgames.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger simple a archivo para evitar printStackTrace/System.out en producción.
 * Es intencionalmente liviano y sin dependencias externas.
 */
public final class AppLog {

    private static final Object LOCK = new Object();
    private static final String LOG_FILE = "data/app.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLog() {}

    public static void info(String msg) {
        write("INFO", msg, null);
    }

    public static void warn(String msg) {
        write("WARN", msg, null);
    }

    public static void error(String msg) {
        write("ERROR", msg, null);
    }

    public static void error(String msg, Throwable t) {
        write("ERROR", msg, t);
    }

    private static void write(String level, String msg, Throwable t) {
        synchronized (LOCK) {
            try {
                Path path = Paths.get(LOG_FILE);
                Path dir = path.getParent();
                if (dir != null) Files.createDirectories(dir);

                String ts = LocalDateTime.now().format(FMT);
                StringBuilder sb = new StringBuilder();
                sb.append(ts).append(" | ").append(level).append(" | ")
                  .append(msg == null ? "" : msg).append(System.lineSeparator());

                if (t != null) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    sb.append(sw).append(System.lineSeparator());
                }

                Files.writeString(
                        path,
                        sb.toString(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (Exception ignored) {
                // Nunca romper el programa por un fallo de logging.
            }
        }
    }
}
