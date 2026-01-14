package com.jasgames.service;

import com.jasgames.model.SesionJuego;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calcula un puntaje 0..100 usando métricas de precisión, consistencia y tiempo.
 *
 * Nota TEA: el tiempo pesa poco (no se busca “castigar” ir lento),
 * pero sí se registra para seguimiento.
 */
public final class ScoreService {

    private ScoreService() {}

    public static int calcularScore(SesionJuego sesion, List<SesionJuego> historialMismoJuego) {
        if (sesion == null) return 0;

        int rondasTotal = safePos(sesion.getRondasTotales());
        int rondasComp = safePos(sesion.getRondasCompletadas());
        if (rondasTotal == 0) rondasTotal = rondasComp;
        if (rondasComp == 0 && rondasTotal > 0) rondasComp = rondasTotal;

        int intentosTot = safePos(sesion.getIntentosTotales());
        int aciertosTot = safePos(sesion.getAciertosTotales());
        int aciertos1 = safePos(sesion.getAciertosPrimerIntento());

        double precision = (intentosTot > 0) ? clamp01((double) aciertosTot / intentosTot) : 0.0;
        double consistencia = (rondasComp > 0) ? clamp01((double) aciertos1 / rondasComp) : 0.0;

        double tiempoScore = scoreTiempo(sesion.getDuracionMs(), rondasTotal, historialMismoJuego);

        // Completitud: si el juego permite “pasar ronda” sin acierto, esto evita inflar score
        double completitud = (rondasTotal > 0) ? clamp01((double) rondasComp / rondasTotal) : 1.0;
        double factorCompletitud = 0.5 + 0.5 * completitud; // mínimo 0.5

        // Pesos (ajustables)
        double base = (0.60 * precision) + (0.25 * consistencia) + (0.15 * tiempoScore);

        int score = (int) Math.round(100.0 * base * factorCompletitud);
        return clampInt(score, 0, 100);
    }

    private static double scoreTiempo(long duracionMs, int rondasTotal, List<SesionJuego> historial) {
        if (duracionMs <= 0) return 0.6; // neutral

        long baseline = baselineDuracion(historial);
        if (baseline <= 0) {
            // Heurística suave: ~15s por ronda, mínimo 25s
            baseline = Math.max(25_000L, (long) rondasTotal * 15_000L);
        }

        // Más rápido que baseline no da más de 1.0; más lento baja, pero con piso para no castigar.
        double ratio = (double) baseline / (double) duracionMs;
        double score = clamp(ratio, 0.35, 1.0);
        return score;
    }

    private static long baselineDuracion(List<SesionJuego> historial) {
        if (historial == null || historial.isEmpty()) return -1;

        List<Long> durs = new ArrayList<>();
        for (SesionJuego s : historial) {
            if (s == null) continue;
            long d = s.getDuracionMs();
            if (d > 0) durs.add(d);
        }
        if (durs.isEmpty()) return -1;

        Collections.sort(durs);
        int n = durs.size();
        if (n % 2 == 1) return durs.get(n / 2);
        return (durs.get((n / 2) - 1) + durs.get(n / 2)) / 2;
    }

    private static int safePos(int v) {
        return Math.max(0, v);
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
