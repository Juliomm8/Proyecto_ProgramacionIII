package com.jasgames.ui.framework;

import javax.swing.*;
import java.awt.*;

/**
 * Tema global para que todo el programa se vea consistente.
 */
public final class UiTheme {
    private UiTheme() {}

    public static void install() {
        // Antialiasing de texto
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Look & Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Fuente base consistente
        Font base = new Font("Dialog", Font.PLAIN, 15);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base.deriveFont(Font.BOLD, 15f));
        UIManager.put("TextField.font", base);
        UIManager.put("PasswordField.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD, 14f));
        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("OptionPane.buttonFont", base.deriveFont(Font.BOLD, 14f));
        UIManager.put("ToolTip.font", base);
    }
}
