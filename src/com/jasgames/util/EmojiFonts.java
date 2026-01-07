package com.jasgames.util;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class EmojiFonts {

    // Windows: Segoe UI Emoji (mejor opción)
    // Mac: Apple Color Emoji
    // Linux: Noto Color Emoji (si está instalada)
    private static final String[] CANDIDATES = {
            "Segoe UI Emoji",
            "Apple Color Emoji",
            "Noto Color Emoji",
            "Segoe UI Symbol"
    };

    private static String cachedFamily;

    private EmojiFonts() {}

    private static String pickFamily() {
        if (cachedFamily != null) return cachedFamily;

        try {
            String[] fams = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            Set<String> set = new HashSet<>(Arrays.asList(fams));
            for (String c : CANDIDATES) {
                if (set.contains(c)) {
                    cachedFamily = c;
                    return cachedFamily;
                }
            }
        } catch (Exception ignored) {}

        // fallback a la fuente UI actual
        Font f = UIManager.getFont("Label.font");
        cachedFamily = (f != null) ? f.getFamily() : "Dialog";
        return cachedFamily;
    }

    public static Font emoji(float size) {
        return new Font(pickFamily(), Font.PLAIN, Math.round(size));
    }

    public static void apply(JComponent c, float size) {
        if (c != null) c.setFont(emoji(size));
    }
}
