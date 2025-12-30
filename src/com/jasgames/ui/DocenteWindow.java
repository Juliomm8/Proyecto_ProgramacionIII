package com.jasgames.ui;

import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;
import com.jasgames.ui.login.AccesoWindow;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

public class DocenteWindow extends JFrame {

    private JPanel mainPanel;
    private JTabbedPane tabbedPanePrincipal;
    private JPanel tabJuegosPanel;
    private JPanel tabPerfilesPanel;
    private JPanel panelHeaderDocente;
    private JLabel lblTituloDocente;
    private JButton btnBackDocente;
    private JPanel tabDashboardPanel;

    private final AppContext context;
    private final JFrame ventanaAnterior;

    private final JuegoService juegoService;
    private final PerfilService perfilService;

    public DocenteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.juegoService = context.getJuegoService();
        this.perfilService = context.getPerfilService();

        // GUARD: no permitir entrar sin sesión docente
        if (context.getDocenteSesion() == null) {
            JOptionPane.showMessageDialog(
                    ventanaAnterior,
                    "Acceso denegado: primero inicia sesión como Docente.",
                    "Seguridad",
                    JOptionPane.WARNING_MESSAGE
            );

            if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
            else new AccesoWindow(context).setVisible(true);

            dispose();
            return;
        }

        setContentPane(mainPanel);
        setTitle("JAS Games - Modo Docente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (context.getDocenteSesion() != null) {
                    context.getAuditoriaService().logoutDocente(context.getDocenteSesion().getUsuario());
                }
                context.setDocenteSesion(null);
            }
        });

        initTabs();
        initListeners();
    }

    public DocenteWindow() {
        this(new AppContext(), null);
    }

    private void initTabs() {
        tabbedPanePrincipal.removeAll();

        tabbedPanePrincipal.addTab("Juegos", new JuegosPanel(juegoService, perfilService));
        tabbedPanePrincipal.addTab("Perfiles", new PerfilesPanel(perfilService));
        tabbedPanePrincipal.addTab("Dashboard", new DashboardPanel(context.getResultadoService()));
        tabbedPanePrincipal.addTab("Auditoría", new AuditoriaPanel(context.getAuditoriaService()));
    }


    private void initListeners() {
        if (btnBackDocente != null) {
            btnBackDocente.addActionListener(e -> {
                // AUDITORÍA: logout docente
                if (context.getDocenteSesion() != null) {
                    context.getAuditoriaService().logoutDocente(context.getDocenteSesion().getUsuario());
                }

                context.setDocenteSesion(null);
                if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
                dispose();
            });
        }
    }
}
