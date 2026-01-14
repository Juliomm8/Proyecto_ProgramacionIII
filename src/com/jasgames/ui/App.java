package com.jasgames.ui;

import com.jasgames.service.AppContext;
import com.jasgames.ui.framework.UiTheme;
import com.jasgames.ui.login.AccesoWindow;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class App {
    private JPanel mainWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            AppContext context = new AppContext();
            new AccesoWindow(context).setVisible(true);
        });
    }
}
