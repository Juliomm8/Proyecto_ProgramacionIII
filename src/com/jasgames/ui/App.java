package com.jasgames.ui;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class App {

    private JPanel mainWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}
