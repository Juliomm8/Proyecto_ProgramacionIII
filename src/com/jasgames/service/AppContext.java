package com.jasgames.service;

public class AppContext {

    private final JuegoService juegoService;
    private final PerfilService perfilService;

    public AppContext() {
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();
    }

    public JuegoService getJuegoService() {
        return juegoService;
    }

    public PerfilService getPerfilService() {
        return perfilService;
    }
}
