package com.jasgames.service;

public class AppContext {

    private final JuegoService juegoService;
    private final PerfilService perfilService;
    private final ResultadoService resultadoService;

    public AppContext() {
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();
        this.resultadoService = new ResultadoService();
    }

    public JuegoService getJuegoService() {
        return juegoService;
    }

    public PerfilService getPerfilService() {
        return perfilService;
    }

    public ResultadoService getResultadoService() {
        return resultadoService;
    }
}
