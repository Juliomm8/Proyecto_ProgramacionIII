package com.jasgames.ui.framework;

/**
 * Tema global
 */
public final class UiTheme {
    private UiTheme() {}

    public static void install() {
        // Antialiasing de texto (suave y sin afectar layouts)
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }
}
