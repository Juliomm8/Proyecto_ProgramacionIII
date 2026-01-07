package com.jasgames.model;

public class Actividad {

    private int id;
    private Juego juego;
    private int nivel;
    private int puntos;
    private int intentosFallidos = 0;

    // Métricas de sesión (para analítica y adaptación; TEA-friendly: no implica castigo en UI)
    private long duracionMs = 0L;
    private int rondasMeta = 0;
    private int rondasJugadas = 0;
    private int rondasCorrectas = 0;
    private int erroresTotales = 0;
    private int intentosTotales = 0;
    private int pistasUsadas = 0;
    private int aciertosPrimerIntento = 0;
    private int intentosMaxPorRonda = 3;
    private int pistasDesdeIntento = 2;

    public Actividad(int id, Juego juego, int nivel, int puntos) {
        this.id = id;
        this.juego = juego;
        this.nivel = nivel;
        this.puntos = puntos;
    }

    public int getId() {
        return id;
    }

    public Juego getJuego() {
        return juego;
    }

    public int getNivel() {
        return nivel;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }


    public long getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(long duracionMs) {
        this.duracionMs = duracionMs;
    }

    public int getRondasMeta() {
        return rondasMeta;
    }

    public void setRondasMeta(int rondasMeta) {
        this.rondasMeta = rondasMeta;
    }

    public int getRondasJugadas() {
        return rondasJugadas;
    }

    public void setRondasJugadas(int rondasJugadas) {
        this.rondasJugadas = rondasJugadas;
    }

    public int getRondasCorrectas() {
        return rondasCorrectas;
    }

    public void setRondasCorrectas(int rondasCorrectas) {
        this.rondasCorrectas = rondasCorrectas;
    }

    public int getErroresTotales() {
        return erroresTotales;
    }

    public void setErroresTotales(int erroresTotales) {
        this.erroresTotales = erroresTotales;
    }

    public int getIntentosTotales() {
        return intentosTotales;
    }

    public void setIntentosTotales(int intentosTotales) {
        this.intentosTotales = intentosTotales;
    }

    public int getPistasUsadas() {
        return pistasUsadas;
    }

    public void setPistasUsadas(int pistasUsadas) {
        this.pistasUsadas = pistasUsadas;
    }

    public int getAciertosPrimerIntento() {
        return aciertosPrimerIntento;
    }

    public void setAciertosPrimerIntento(int aciertosPrimerIntento) {
        this.aciertosPrimerIntento = aciertosPrimerIntento;
    }

    public int getIntentosMaxPorRonda() {
        return intentosMaxPorRonda;
    }

    public void setIntentosMaxPorRonda(int intentosMaxPorRonda) {
        this.intentosMaxPorRonda = intentosMaxPorRonda;
    }

    public int getPistasDesdeIntento() {
        return pistasDesdeIntento;
    }

    public void setPistasDesdeIntento(int pistasDesdeIntento) {
        this.pistasDesdeIntento = pistasDesdeIntento;
    }

    @Override
    public String toString() {
        return juego.getNombre() + " - Nivel " + nivel + " (" + puntos + " pts)";
    }
}