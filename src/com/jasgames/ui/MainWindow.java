package com.jasgames.ui;

import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;

import javax.swing.*;

public class MainWindow extends JFrame {

    private JPanel mainPanel;
    private JTabbedPane tabbedPanePrincipal;
    private JPanel tabJuegosPanel;
    private JPanel tabPerfilesPanel;
    private JPanel panelHeaderDocente;
    private JLabel lblTituloDocente;
    private JButton btnBackDocente;
    private JPanel tabDashboardPanel;

    private final AppContext context;
    private final SeleccionUsuarioWindow seleccionUsuarioWindow;

    private final JuegoService juegoService;
    private final PerfilService perfilService;

    public MainWindow(AppContext context, SeleccionUsuarioWindow seleccionUsuarioWindow) {
        this.context = context;
        this.seleccionUsuarioWindow = seleccionUsuarioWindow;
        this.juegoService = context.getJuegoService();
        this.perfilService = context.getPerfilService();

        setContentPane(mainPanel);
        setTitle("JAS Games - Modo Docente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        initTabs();
        initListeners();
    }

    public MainWindow() {
        this(new AppContext(), null);
    }

    private void initTabs() {
        tabbedPanePrincipal.removeAll();

        tabbedPanePrincipal.addTab("Juegos", new JuegosPanel(juegoService));
        tabbedPanePrincipal.addTab("Perfiles", new PerfilesPanel(perfilService));
        tabbedPanePrincipal.addTab("Dashboard", new DashboardPanel(context.getResultadoService()));
    }


    private void initListeners() {
        if (btnBackDocente != null) {
            btnBackDocente.addActionListener(e -> {
                this.dispose();

                if (seleccionUsuarioWindow != null) {
                    seleccionUsuarioWindow.setVisible(true);
                }
            });
        }
    }
}