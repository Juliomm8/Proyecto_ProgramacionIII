package com.jasgames.service;

import com.jasgames.model.Juego;
import com.jasgames.model.ResultadoJuego;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ResultadoService {

    private final List<ResultadoJuego> resultados = new ArrayList<>();

    public void registrarResultado(ResultadoJuego resultado) {
        if (resultado != null) {
            resultados.add(resultado);
        }
    }

    public List<ResultadoJuego> obtenerTodos() {
        return new ArrayList<>(resultados); // copia defensiva
    }

    public List<ResultadoJuego> obtenerPorJuego(Juego juego) {
        if (juego == null) {
            return new ArrayList<>();
        }
        return resultados.stream()
                .filter(r -> r.getJuego().equals(juego))
                .collect(Collectors.toList());
    }

    public List<ResultadoJuego> obtenerPorJuegoOrdenadosPorPuntajeDesc(Juego juego) {
        return obtenerPorJuego(juego).stream()
                .sorted(Comparator.comparingInt(ResultadoJuego::getPuntaje).reversed())
                .collect(Collectors.toList());
    }
}
