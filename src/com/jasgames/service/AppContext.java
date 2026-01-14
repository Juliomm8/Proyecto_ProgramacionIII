// AppContext.java
package com.jasgames.service;

import com.jasgames.model.Docente;
import com.jasgames.model.Nino;
import com.jasgames.service.PiaService;

public class AppContext {

    private final JuegoService juegoService;
    private final PerfilService perfilService;
    private final SesionService sesionService;
    private final AuditoriaService auditoriaService;
    private final AulaService aulaService;

    private final PiaService piaService;

    private final AutenticacionService autenticacionService;
    private final DirectorioEscolarService directorioEscolarService;

    private Docente docenteSesion;
    private Nino ninoSesion;

    public AppContext() {
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();
        this.aulaService = new AulaService(perfilService);
        this.sesionService = new SesionService();
        this.auditoriaService = new AuditoriaService();

        this.piaService = new PiaService();

        this.autenticacionService = new AutenticacionService();
        this.directorioEscolarService = new DirectorioEscolarService(perfilService, aulaService);
    }

    public JuegoService getJuegoService() { return juegoService; }
    public PerfilService getPerfilService() { return perfilService; }
    public SesionService getResultadoService() { return sesionService; }
    public SesionService getSesionService() { return sesionService; } // Alias
    public AuditoriaService getAuditoriaService() { return auditoriaService; }
    public AulaService getAulaService() { return aulaService; }

    public PiaService getPiaService() { return piaService; }

    public AutenticacionService getAutenticacionService() { return autenticacionService; }
    public DirectorioEscolarService getDirectorioEscolarService() { return directorioEscolarService; }

    public Docente getDocenteSesion() { return docenteSesion; }
    public void setDocenteSesion(Docente docenteSesion) { this.docenteSesion = docenteSesion; }

    public Nino getNinoSesion() { return ninoSesion; }
    public void setNinoSesion(Nino ninoSesion) { this.ninoSesion = ninoSesion; }
}
