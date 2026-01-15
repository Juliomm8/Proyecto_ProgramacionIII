package com.jasgames.ui.framework;

import javax.swing.*;
import java.awt.*;

/**
 * Utilidades para aplicar accesibilidad sin cambiar el Look&Feel global.
 *
 * - Letra grande: escala fuentes recordando las fuentes base.
 * - Alto contraste: no se aplica aquí de forma global (se recomienda por pantalla),
 *   para no romper estilos por aula.
 */
public final class UiAccessibility {

    private static final String KEY_BASE_FONT = "__baseFont";

    private UiAccessibility() {}

    public static void applyLargeText(Component root, boolean enabled) {
        if (root == null) return;
        float scale = enabled ? 1.18f : 1.0f;
        applyFontScale(root, scale);
    }

    private static void applyFontScale(Component c, float scale) {
        if (c instanceof JComponent jc) {
            Object baseObj = jc.getClientProperty(KEY_BASE_FONT);
            Font base = (baseObj instanceof Font) ? (Font) baseObj : null;
            if (base == null) {
                base = jc.getFont();
                jc.putClientProperty(KEY_BASE_FONT, base);
            }
            if (base != null) {
                jc.setFont(base.deriveFont(base.getSize2D() * scale));
            }
        }

        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                applyFontScale(child, scale);
            }
        }
    }
}
