package com.jasgames.ui.juegos.framework;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Paletas compartidas para todos los minijuegos.
 *
 * Objetivo:
 * - Evitar duplicación de colores entre juegos
 * - Mantener consistencia visual y alto contraste (TEA)
 */
public final class Paletas {

    private Paletas() {}

    /** Item con nombre + color (útil para el juego de colores). */
    public static final class ColorNombre {
        public final String nombre;
        public final Color color;

        public ColorNombre(String nombre, Color color) {
            this.nombre = nombre;
            this.color = color;
        }
    }

    /**
     * Paleta con nombres (alto contraste) para Discriminación de Colores.
     * Retorna una copia para que cada juego pueda barajar sin afectar a otros.
     */
    public static List<ColorNombre> coloresConNombre() {
        return new ArrayList<>(Arrays.asList(
                new ColorNombre("Rojo", new Color(220, 40, 40)),
                new ColorNombre("Azul", new Color(55, 110, 220)),
                new ColorNombre("Verde", new Color(60, 170, 90)),
                new ColorNombre("Amarillo", new Color(240, 210, 60)),
                new ColorNombre("Naranja", new Color(240, 140, 50)),
                new ColorNombre("Morado", new Color(150, 85, 210)),
                new ColorNombre("Negro", new Color(40, 40, 40)),
                new ColorNombre("Café", new Color(140, 90, 55))
        ));
    }

    /**
     * Paleta sólida (sin nombres) para figuras/animaciones en otros juegos.
     * Devolvemos una copia para evitar modificaciones accidentales.
     */
    public static Color[] coloresSolidos() {
        return new Color[]{
                new Color(55, 110, 220),
                new Color(220, 40, 40),
                new Color(60, 170, 90),
                new Color(240, 140, 50),
                new Color(150, 85, 210),
                new Color(40, 40, 40)
        };
    }

    public static ColorNombre buscarPorNombre(List<ColorNombre> paleta, String nombre) {
        if (paleta == null || paleta.isEmpty()) return null;
        for (ColorNombre c : paleta) {
            if (c != null && c.nombre != null && c.nombre.equalsIgnoreCase(nombre)) return c;
        }
        return paleta.get(0);
    }

    public static Color colorAleatorio(Color[] paleta, Random random) {
        if (paleta == null || paleta.length == 0) return Color.BLACK;
        Random r = (random != null) ? random : new Random();
        return paleta[r.nextInt(paleta.length)];
    }
}
