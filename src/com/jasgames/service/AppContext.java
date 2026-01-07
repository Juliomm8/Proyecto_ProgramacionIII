package com.jasgames.service;

import com.jasgames.model.Docente;
import com.jasgames.model.Nino;

public class AppContext {

    private final JuegoService juegoService;
    private final PerfilService perfilService;
    private final SesionService sesionService;
    private final AuditoriaService auditoriaService;
    private final AulaService aulaService;

    private final AutenticacionService autenticacionService;
    private final DirectorioEscolarService directorioEscolarService;

    // Sesión (quién está usando el sistema)
    private Docente docenteSesion;
    private Nino ninoSesion;

    public AppContext() {
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();
        this.aulaService = new AulaService(perfilService);
        this.sesionService = new SesionService();
        this.auditoriaService = new AuditoriaService();

        this.autenticacionService = new AutenticacionService();
        this.directorioEscolarService = new DirectorioEscolarService(perfilService, aulaService);
    }

    public JuegoService getJuegoService() { return juegoService; }
    public PerfilService getPerfilService() { return perfilService; }
    public SesionService getResultadoService() { return sesionService; }
    public AuditoriaService getAuditoriaService() { return auditoriaService; }

    public AutenticacionService getAutenticacionService() { return autenticacionService; }
    public DirectorioEscolarService getDirectorioEscolarService() { return directorioEscolarService; }

    public Docente getDocenteSesion() { return docenteSesion; }
    public void setDocenteSesion(Docente docenteSesion) { this.docenteSesion = docenteSesion; }

    public Nino getNinoSesion() { return ninoSesion; }
    public void setNinoSesion(Nino ninoSesion) { this.ninoSesion = ninoSesion; }
    public AulaService getAulaService() { return aulaService; }

}
