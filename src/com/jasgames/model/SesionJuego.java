package com.jasgames.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SesionJuego {

    // Identidad
    private String idSesion;

    // Contexto estudiante
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String aula;

    // Contexto juego
    private Juego juego;

    // Scoring (por ahora mantenemos "puntaje" como score final)
    private int puntaje;

    // Tiempos
    private LocalDateTime fechaHora;   // inicio
    private LocalDateTime fechaFin;    // fin
    private long duracionMs;

    // Dificultad
    private int dificultad;           // se mantiene (actual)
    private int dificultadInicial;
    private int dificultadFinal;
    private boolean dificultadAdaptada;

    // Rondas
    private int rondasTotales;
    private int rondasCompletadas;

    // Intentos / pistas
    private int intentosMaxPorRonda;   // default 3
    private int pistasDesdeIntento;    // default 2
    private int intentosTotales;
    private int erroresTotales;
    private int pistasUsadas;
    private int aciertosTotales;
    private int aciertosPrimerIntento;

    // PIA (para cumplir el objetivo del PIA y métricas por objetivo)
    private String idPia;
    private String idObjetivoPia;

    public SesionJuego() {
        // requerido por Gson
        aplicarDefaults();
    }

    public SesionJuego(String nombreEstudiante, Juego juego, int dificultad, int puntaje, LocalDateTime fechaHora) {
        this(null, nombreEstudiante, null, juego, dificultad, puntaje, fechaHora);
    }

    public SesionJuego(Integer idEstudiante, String nombreEstudiante, String aula,
                       Juego juego, int dificultad, int puntaje, LocalDateTime fechaHora) {
        aplicarDefaults();

        this.idEstudiante = idEstudiante;
        this.nombreEstudiante = nombreEstudiante;
        this.aula = aula;

        this.juego = juego;

        this.dificultad = dificultad;
        this.dificultadInicial = dificultad;
        this.dificultadFinal = dificultad;
        this.dificultadAdaptada = false;

        this.puntaje = puntaje;

        this.fechaHora = fechaHora;
    }

    private void aplicarDefaults() {
        this.idSesion = UUID.randomUUID().toString();

        this.fechaHora = LocalDateTime.now();
        this.fechaFin = null;
        this.duracionMs = 0L;

        this.rondasTotales = 1;
        this.rondasCompletadas = 0;

        this.intentosMaxPorRonda = 3;
        this.pistasDesdeIntento = 2;

        this.intentosTotales = 0;
        this.erroresTotales = 0;
        this.pistasUsadas = 0;
        this.aciertosTotales = 0;
        this.aciertosPrimerIntento = 0;

        this.idPia = null;
        this.idObjetivoPia = null;

        this.dificultadInicial = 0;
        this.dificultadFinal = 0;
        this.dificultadAdaptada = false;
    }

    // ===== Getters/Setters (mantengo estilo compacto como tu clase actual) =====

    public String getIdSesion() { return idSesion; }
    public void setIdSesion(String idSesion) { this.idSesion = idSesion; }

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public String getAula() { return aula; }
    public void setAula(String aula) { this.aula = aula; }

    public Juego getJuego() { return juego; }
    public void setJuego(Juego juego) { this.juego = juego; }

    public int getPuntaje() { return puntaje; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public long getDuracionMs() { return duracionMs; }
    public void setDuracionMs(long duracionMs) { this.duracionMs = duracionMs; }

    public int getDificultad() { return dificultad; }
    public void setDificultad(int dificultad) { this.dificultad = dificultad; }

    public int getDificultadInicial() { return dificultadInicial; }
    public void setDificultadInicial(int dificultadInicial) { this.dificultadInicial = dificultadInicial; }

    public int getDificultadFinal() { return dificultadFinal; }
    public void setDificultadFinal(int dificultadFinal) { this.dificultadFinal = dificultadFinal; }

    public boolean isDificultadAdaptada() { return dificultadAdaptada; }
    public void setDificultadAdaptada(boolean dificultadAdaptada) { this.dificultadAdaptada = dificultadAdaptada; }

    public int getRondasTotales() { return rondasTotales; }
    public void setRondasTotales(int rondasTotales) { this.rondasTotales = rondasTotales; }

    public int getRondasCompletadas() { return rondasCompletadas; }
    public void setRondasCompletadas(int rondasCompletadas) { this.rondasCompletadas = rondasCompletadas; }

    public int getIntentosMaxPorRonda() { return intentosMaxPorRonda; }
    public void setIntentosMaxPorRonda(int intentosMaxPorRonda) { this.intentosMaxPorRonda = intentosMaxPorRonda; }

    public int getPistasDesdeIntento() { return pistasDesdeIntento; }
    public void setPistasDesdeIntento(int pistasDesdeIntento) { this.pistasDesdeIntento = pistasDesdeIntento; }

    public int getIntentosTotales() { return intentosTotales; }
    public void setIntentosTotales(int intentosTotales) { this.intentosTotales = intentosTotales; }

    public int getErroresTotales() { return erroresTotales; }
    public void setErroresTotales(int erroresTotales) { this.erroresTotales = erroresTotales; }

    public int getPistasUsadas() { return pistasUsadas; }
    public void setPistasUsadas(int pistasUsadas) { this.pistasUsadas = pistasUsadas; }

    public int getAciertosTotales() { return aciertosTotales; }
    public void setAciertosTotales(int aciertosTotales) { this.aciertosTotales = aciertosTotales; }

    public int getAciertosPrimerIntento() { return aciertosPrimerIntento; }
    public void setAciertosPrimerIntento(int aciertosPrimerIntento) { this.aciertosPrimerIntento = aciertosPrimerIntento; }

    public String getIdPia() { return idPia; }
    public void setIdPia(String idPia) { this.idPia = idPia; }

    public String getIdObjetivoPia() { return idObjetivoPia; }
    public void setIdObjetivoPia(String idObjetivoPia) { this.idObjetivoPia = idObjetivoPia; }
}
