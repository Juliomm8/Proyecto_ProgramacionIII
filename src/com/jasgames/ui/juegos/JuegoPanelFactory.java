package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;

public final class JuegoPanelFactory {

    private JuegoPanelFactory() {}

    public static BaseJuegoPanel crearPanel(Actividad actividad, JuegoListener listener) {
        if (actividad == null || actividad.getJuego() == null) return null;

        int idJuego = actividad.getJuego().getId();

        switch (idJuego) {
            case 1:
                return new JuegoColoresPanel(actividad, listener);
            case 2:
                return new JuegoCuentaConectaPanel(actividad, listener);
            default:
                return null;
        }
    }
}
