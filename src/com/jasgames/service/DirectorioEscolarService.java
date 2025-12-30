package com.jasgames.service;

import com.jasgames.model.Nino;

import java.util.*;
import java.util.stream.Collectors;

public class DirectorioEscolarService {

    private final PerfilService perfilService;

    public DirectorioEscolarService(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    public List<String> obtenerAulas() {
        return perfilService.obtenerTodosNinos().stream()
                .map(Nino::getAula)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public List<Nino> obtenerEstudiantesPorAula(String aula) {
        String aulaFinal = (aula == null || aula.isBlank()) ? "General" : aula.trim();

        return perfilService.obtenerTodosNinos().stream()
                .filter(n -> n.getAula().equalsIgnoreCase(aulaFinal))
                .sorted(Comparator.comparing(Nino::getNombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(Nino::getId))
                .collect(Collectors.toList());
    }
}
