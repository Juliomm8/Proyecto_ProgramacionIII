package com.jasgames.ui;

import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;

import javax.swing.*;

public class MainWindow extends JFrame {

    private final JuegoService juegoService;
    private final PerfilService perfilService;
    private JPanel mainPanel;
    private JTabbedPane tabbedPanePrincipal;
    private JPanel tabJuegosPanel;
    private JPanel tabPerfilesPanel;

    public MainWindow() {
        // Servicios de cada módulo
        this.juegoService = new JuegoService();
        this.perfilService = new PerfilService();

        // Config general de la ventana
        setTitle("JAS Games - Avance Proyecto");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null); // centrar

        // Pestañas (módulos)
        JTabbedPane tabbedPane = new JTabbedPane();
        //tabbedPane.addTab("Juegos", new JuegosPanel(juegoService));
        //tabbedPane.addTab("Perfiles", new PerfilesPanel(perfilService));

        // Agregamos el tabbed al frame
        add(tabbedPane);
    }
}
