package com.jasgames.ui;

import javax.swing.*;

public class App {

    private JPanel mainWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);   // aquí abrimos la ventana principal
        });
    }
}
