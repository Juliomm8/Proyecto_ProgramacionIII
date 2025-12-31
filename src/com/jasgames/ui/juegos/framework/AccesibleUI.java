package com.jasgames.ui.juegos.framework;

import javax.swing.*;
import java.awt.*;

/**
 * Constantes y helpers visuales para mantener consistencia y accesibilidad (TEA).
 *
 * Reglas:
 * - Alto contraste
 * - Tipografías Sans Serif
 * - Espaciado generoso
 * - Evitar degradados o detalles finos
 */
public final class AccesibleUI {

    private AccesibleUI() {}

    public static final Color APP_BG = new Color(245, 245, 245);
    public static final Color TABLERO_BG = Color.WHITE;
    public static final Color TABLERO_BORDE = new Color(220, 220, 220);
    public static final Color TEXTO_OSCURO = new Color(25, 25, 25);
    public static final Color TEXTO_NEUTRO = new Color(90, 90, 90);

    public static final Font FONT_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    public static final Font FONT_PROGRESO = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    public static final Font FONT_FEEDBACK = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

    public static JPanel crearContenedorTablero(JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(TABLERO_BG);
        p.setBorder(BorderFactory.createLineBorder(TABLERO_BORDE, 1));
        p.setOpaque(true);
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    public static JLabel crearLabelCentrado(String texto, Font font) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(font);
        lbl.setForeground(TEXTO_OSCURO);
        return lbl;
    }
}
