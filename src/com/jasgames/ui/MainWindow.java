package com.jasgames.ui;

import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;

import javax.swing.*;

public class MainWindow extends JFrame {

    private JPanel mainPanel; // fantasma para el .form
    private JTabbedPane tabbedPanePrincipal;
    private JPanel tabJuegosPanel;
    private JPanel tabPerfilesPanel;

    private final JuegoService juegoService;
    private final PerfilService perfilService;

    public MainWindow() {
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();

        setTitle("JAS Games - Avance Proyecto");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Juegos", new JuegosPanel(juegoService));
        tabbedPane.addTab("Perfiles", new PerfilesPanel(perfilService));

        add(tabbedPane);
    }
}
