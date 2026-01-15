package com.jasgames.ui;

import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;
import com.jasgames.ui.login.AccesoWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DocenteWindow extends JFrame {

    // Campos (se mantienen por compatibilidad con el resto del proyecto)
    private JPanel mainPanel;
    private JTabbedPane tabbedPanePrincipal;
    private JPanel tabJuegosPanel;
    private JPanel tabPerfilesPanel;
    private JPanel panelHeaderDocente;
    private JLabel lblTituloDocente;
    private JButton btnBackDocente;
    private JButton btnBackups;
    private JButton btnAyuda;
    private JButton btnAcercaDe;
    private JPanel tabDashboardPanel;

    private final AppContext context;
    private final JFrame ventanaAnterior;

    private final JuegoService juegoService;
    private final PerfilService perfilService;

    private PerfilesPanel perfilesPanel;

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

        // IMPORTANTE: construir UI antes de setContentPane(mainPanel)
        initUI();

        setContentPane(mainPanel);
        setTitle("JAS Games - Modo Docente");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 800));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (context.getDocenteSesion() != null) {
                    context.getAuditoriaService().logoutDocente(context.getDocenteSesion().getUsuario());
                }
                context.setDocenteSesion(null);

                // Al cerrar, vuelve a la ventana anterior si existe
                if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
            }
        });

        initTabs();
        initListeners();
    }

    public DocenteWindow() {
        this(new AppContext(), null);
    }

    private void initUI() {
        // Root
        mainPanel = new JPanel(new BorderLayout());

        // Header
        panelHeaderDocente = new JPanel(new BorderLayout(10, 10));
        panelHeaderDocente.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnBackDocente = new JButton("Volver");
        lblTituloDocente = new JLabel("JAS Games - Modo Docente", SwingConstants.CENTER);
        lblTituloDocente.setFont(lblTituloDocente.getFont().deriveFont(Font.BOLD, 18f));

        // Acciones (derecha) - estilo "moderno": botones pequeños, sin saturar
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);

        btnBackups = new JButton("Backups");
        btnAyuda = new JButton("Ayuda");
        btnAcercaDe = new JButton("Acerca de");

        for (JButton b : new JButton[]{btnBackups, btnAyuda, btnAcercaDe}) {
            b.setFocusPainted(false);
        }

        acciones.add(btnBackups);
        acciones.add(btnAyuda);
        acciones.add(btnAcercaDe);

        panelHeaderDocente.add(btnBackDocente, BorderLayout.WEST);
        panelHeaderDocente.add(lblTituloDocente, BorderLayout.CENTER);
        panelHeaderDocente.add(acciones, BorderLayout.EAST);

        // Tabs
        tabbedPanePrincipal = new JTabbedPane();

        // Panels "placeholder" (por compatibilidad; no son estrictamente necesarios)
        tabJuegosPanel = new JPanel(new BorderLayout());
        tabPerfilesPanel = new JPanel(new BorderLayout());
        tabDashboardPanel = new JPanel(new BorderLayout());

        mainPanel.add(panelHeaderDocente, BorderLayout.NORTH);
        mainPanel.add(tabbedPanePrincipal, BorderLayout.CENTER);
    }

    private void initTabs() {
        if (tabbedPanePrincipal == null) return;

        tabbedPanePrincipal.removeAll();

        tabbedPanePrincipal.addTab("Juegos", new JuegosPanel(juegoService, perfilService));

        perfilesPanel = new PerfilesPanel(
                perfilService,
                context.getAulaService(),
                context.getPiaService()
        );
        tabbedPanePrincipal.addTab("Perfiles", perfilesPanel);

        tabbedPanePrincipal.addTab("Aulas", new AulasPanel(context, (int idNino) -> {
            tabbedPanePrincipal.setSelectedComponent(perfilesPanel);
            perfilesPanel.seleccionarNinoPorId(idNino);
        }));

        tabbedPanePrincipal.addTab(
                "Dashboard",
                new DashboardPanel(
                        context.getResultadoService(),
                        context.getPiaService(),
                        context.getAulaService(),
                        (idNino, idObjetivo) -> {
                            tabbedPanePrincipal.setSelectedComponent(perfilesPanel);
                            if (perfilesPanel != null) {
                                perfilesPanel.irAObjetivoPia(idNino, idObjetivo);
                            }
                        }
                )
        );

        tabbedPanePrincipal.addTab("Auditoría", new AuditoriaPanel(context.getAuditoriaService()));
    }

    private void initListeners() {
        if (btnBackDocente != null) {
            btnBackDocente.addActionListener(e -> {
                if (context.getDocenteSesion() != null) {
                    context.getAuditoriaService().logoutDocente(context.getDocenteSesion().getUsuario());
                }

                context.setDocenteSesion(null);
                if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
                dispose();
            });
        }

        if (tabbedPanePrincipal != null) {
            tabbedPanePrincipal.addChangeListener(ev -> {
                if (perfilesPanel != null && tabbedPanePrincipal.getSelectedComponent() == perfilesPanel) {
                    perfilesPanel.refrescarAulas();
                }
            });
        }

        if (btnAyuda != null) {
            btnAyuda.addActionListener(e -> new HelpDialog(this).setVisible(true));
        }

        if (btnAcercaDe != null) {
            btnAcercaDe.addActionListener(e -> new AboutDialog(this).setVisible(true));
        }

        if (btnBackups != null) {
            btnBackups.addActionListener(e -> new BackupRestoreDialog(this, context).setVisible(true));
        }

        installShortcuts();
    }

    private void installShortcuts() {
        JRootPane root = getRootPane();
        if (root == null) return;

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("F1"), "openHelp");
        am.put("openHelp", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnAyuda != null) btnAyuda.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "openBackups");
        am.put("openBackups", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnBackups != null) btnBackups.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK), "openAbout");
        am.put("openAbout", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnAcercaDe != null) btnAcercaDe.doClick();
            }
        });
    }
}