package com.jasgames.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditoriaService {

    private static final String ARCHIVO = "data/auditoria.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void registrar(String tipo, String detalle) {
        try {
            Path path = Paths.get(ARCHIVO);
            Path dir = path.getParent();
            if (dir != null) Files.createDirectories(dir);

            String ts = LocalDateTime.now().format(FMT);
            String linea = ts + " | " + tipo + " | " + (detalle == null ? "" : detalle) + System.lineSeparator();

            Files.writeString(
                    path,
                    linea,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helpers opcionales (para que luego sea más fácil llamar)
    public void loginDocente(String usuario, boolean exito) {
        registrar("LOGIN_DOCENTE", "usuario=" + usuario + " exito=" + exito);
    }

    public void logoutDocente(String usuario) {
        registrar("LOGOUT_DOCENTE", "usuario=" + usuario);
    }

    public void seleccionAula(String aula) {
        registrar("SELECCION_AULA", "aula=" + aula);
    }

    public void ingresoEstudiante(int ninoId, String nombre) {
        registrar("INGRESO_ESTUDIANTE", "id=" + ninoId + " nombre=" + nombre);
    }

    public void salidaEstudiante(int ninoId, String nombre) {
        registrar("SALIDA_ESTUDIANTE", "id=" + ninoId + " nombre=" + nombre);
    }

    public String leerUltimasLineas(int maxLineas) {
        try {
            Path path = Paths.get("data/auditoria.log");
            if (!Files.exists(path)) return "Aún no hay registros de auditoría.";

            java.util.List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - maxLineas);

            StringBuilder sb = new StringBuilder();
            for (int i = from; i < lines.size(); i++) {
                sb.append(lines.get(i)).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error leyendo auditoría: " + e.getMessage();
        }
    }

}
