package com.jasgames.ui;

import com.jasgames.service.AppContext;

import javax.swing.*;

public class App {

    private JPanel mainWindow;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppContext context = new AppContext();
            SeleccionUsuarioWindow seleccion = new SeleccionUsuarioWindow(context);
            seleccion.setVisible(true);
        });
    }
}
