package com.jasgames.service;

import com.jasgames.model.Nino;
import com.jasgames.model.SesionJuego;

import java.util.List;

/**
 * Adaptación automática (TEA-friendly): ajusta dificultad sin mostrar “castigo”.
 *
 * Reglas:
 * - Solo aplica si el juego está en modo adaptativo para ese niño.
 * - Si el docente fijó dificultad manual para ese juego, no se sobreescribe.
 * - Cooldown: después de un cambio, se espera N sesiones antes de volver a cambiar.
 */
public final class AdaptacionService {

    private AdaptacionService() {}

    public static final class Decision {
        private final int dificultadSiguiente;
        private final boolean cambio;
        private final int cooldownSet;
        private final String motivo;

        public Decision(int dificultadSiguiente, boolean cambio, int cooldownSet, String motivo) {
            this.dificultadSiguiente = dificultadSiguiente;
            this.cambio = cambio;
            this.cooldownSet = cooldownSet;
            this.motivo = (motivo == null) ? "" : motivo;
        }

        public int getDificultadSiguiente() { return dificultadSiguiente; }
        public boolean isCambio() { return cambio; }
        public int getCooldownSet() { return cooldownSet; }
        public String getMotivo() { return motivo; }
    }

    /**
     * Evalúa y aplica (en el perfil del niño) la dificultad automática.
     *
     * @param nino Perfil
     * @param idJuego Id del juego
     * @param dificultadUsada Nivel jugado en esta sesión
     * @param ultimas3 Lista con hasta 3 sesiones más recientes (ideal: incluye la actual)
     */
    public static Decision evaluarYAplicar(Nino nino, int idJuego, int dificultadUsada, List<SesionJuego> ultimas3) {
        if (nino == null) return new Decision(dificultadUsada, false, 0, "sin_nino");

        // Si el docente puso dificultad manual, no tocar.
        if (nino.tieneDificultadManual(idJuego)) {
            return new Decision(dificultadUsada, false, 0, "manual");
        }

        // Si está apagada la adaptación para este juego, no tocar.
        if (!nino.isAdaptacionAutomaticaJuego(idJuego)) {
            return new Decision(dificultadUsada, false, 0, "adaptacion_off");
        }

        int cooldown = nino.getCooldownRestanteJuego(idJuego);
        if (cooldown > 0) {
            nino.setCooldownRestanteJuego(idJuego, cooldown - 1);
            return new Decision(dificultadUsada, false, cooldown - 1, "cooldown");
        }

        // Si no hay suficiente historial, no cambiamos (pero guardamos auto = actual)
        if (ultimas3 == null || ultimas3.isEmpty()) {
            nino.setDificultadAutoJuego(idJuego, dificultadUsada);
            return new Decision(dificultadUsada, false, 0, "sin_historial");
        }

        // Promedios suaves (0..1)
        double pSum = 0.0;
        double cSum = 0.0;
        int n = 0;

        for (SesionJuego s : ultimas3) {
            if (s == null) continue;

            int intentos = Math.max(0, s.getIntentosTotales());
            int aciertos = Math.max(0, s.getAciertosTotales());
            int rondasComp = Math.max(0, s.getRondasCompletadas());
            int aciertos1 = Math.max(0, s.getAciertosPrimerIntento());

            double precision = (intentos > 0) ? clamp01((double) aciertos / intentos) : 0.0;
            double consistencia = (rondasComp > 0) ? clamp01((double) aciertos1 / rondasComp) : 0.0;

            pSum += precision;
            cSum += consistencia;
            n++;
        }

        if (n == 0) {
            nino.setDificultadAutoJuego(idJuego, dificultadUsada);
            return new Decision(dificultadUsada, false, 0, "historial_vacio");
        }

        double pAvg = pSum / n;
        double cAvg = cSum / n;

        int nueva = dificultadUsada;

        // Umbrales (suaves)
        if (pAvg >= 0.88 && cAvg >= 0.60) {
            nueva = dificultadUsada + 1;
        } else if (pAvg <= 0.60 || cAvg <= 0.30) {
            nueva = dificultadUsada - 1;
        }

        nueva = clampInt(nueva, 1, 5);

        if (nueva != dificultadUsada) {
            int setCooldown = 2; // esperar 2 sesiones antes de volver a cambiar
            nino.setDificultadAutoJuego(idJuego, nueva);
            nino.setCooldownRestanteJuego(idJuego, setCooldown);
            return new Decision(nueva, true, setCooldown, "cambio");
        }

        nino.setDificultadAutoJuego(idJuego, dificultadUsada);
        return new Decision(dificultadUsada, false, 0, "sin_cambio");
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
