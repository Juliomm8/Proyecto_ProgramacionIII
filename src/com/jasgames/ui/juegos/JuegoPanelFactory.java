package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;

/**
 * Centraliza el mapeo: ID de juego -> Panel.
 * Así EstudianteWindow no se llena de if/else.
 */
public final class JuegoPanelFactory {

    private JuegoPanelFactory() {}

    public static BaseJuegoPanel crearPanel(Actividad actividad, JuegoListener listener) {
        if (actividad == null || actividad.getJuego() == null) return null;

        int idJuego = actividad.getJuego().getId();

        return switch (idJuego) {
            case 1 -> new JuegoColoresPanel(actividad, listener);
            case 2 -> new JuegoCuentaConectaPanel(actividad, listener);
            default -> null;
        };
    }
}
