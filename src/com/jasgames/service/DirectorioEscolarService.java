package com.jasgames.service;

import com.jasgames.model.Nino;

import java.util.*;
import java.util.stream.Collectors;

public class DirectorioEscolarService {

    private final PerfilService perfilService;
    private final AulaService aulaService;

    /**
     * Canon: AulaService (aulas.json). Compat: si hay aulas en ninos.json que no existan en aulas.json,
     * también se listan.
     */
    public DirectorioEscolarService(PerfilService perfilService, AulaService aulaService) {
        this.perfilService = perfilService;
        this.aulaService = aulaService;
    }

    // Back-compat (por si alguna pantalla antigua lo usa)
    public DirectorioEscolarService(PerfilService perfilService) {
        this(perfilService, null);
    }

    public List<String> obtenerAulas() {
        LinkedHashSet<String> aulas = new LinkedHashSet<>();

        // 1) Canon: aulas.json
        if (aulaService != null) {
            aulas.addAll(aulaService.obtenerNombres());
        }

        // 2) Compatibilidad: aulas que existan en ninos.json
        aulas.addAll(perfilService.obtenerTodosNinos().stream()
                .map(Nino::getAula)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList()));

        if (aulas.isEmpty()) aulas.add("Aula Azul");

        return new ArrayList<>(aulas);
    }

    public List<Nino> obtenerEstudiantesPorAula(String aula) {
        String aulaFinal = (aula == null || aula.isBlank()) ? "Aula Azul" : aula.trim();

        return perfilService.obtenerTodosNinos().stream()
                .filter(n -> n.getAula().equalsIgnoreCase(aulaFinal))
                .sorted(Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(Nino::getId))
                .collect(Collectors.toList());
    }
}
