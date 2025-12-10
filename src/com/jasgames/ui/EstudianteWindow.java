package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;

import javax.swing.*;

public class EstudianteWindow extends JFrame {
    private JPanel panelEstudianteMain;
    private JPanel panelHeaderEstudiante;
    private JButton btnBackEstudiante;
    private JLabel lblTituloEstudiante;
    private JPanel panelDatosEstudiante;
    private JLabel lblNombreEstudiante;
    private JTextField txtNombreEstudiante;
    private JPanel panelSeleccionJuego;
    private JLabel lblSeleccionJuego;
    private JList listaJuegosEstudiante;
    private JScrollPane scrollJuegosEstudiante;
    private JPanel panelJuegoEstudiante;
    private JLabel lblPuntajeActual;
    private JLabel lblValorPuntaje;
    private JButton btnFinalizarJuego;
    private JButton btnIniciarJuego;

    private final AppContext context;
    private final SeleccionUsuarioWindow seleccionUsuarioWindow;
    private final JuegoService juegoService;

    private DefaultListModel<Juego> juegosListModel;

    public EstudianteWindow(AppContext context, SeleccionUsuarioWindow seleccionUsuarioWindow) {
        this.context = context;
        this.seleccionUsuarioWindow = seleccionUsuarioWindow;
        this.juegoService = context.getJuegoService();

        setContentPane(panelEstudianteMain);
        setTitle("JAS Games - Modo Estudiante");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        initModeloJuegos();
        initListeners();
    }

    public EstudianteWindow() {
        this(new AppContext(), null);
    }

    private void initModeloJuegos() {
        juegosListModel = new DefaultListModel<>();
        listaJuegosEstudiante.setModel(juegosListModel);


    }

    private void initListeners() {
        if (btnBackEstudiante != null) {
            btnBackEstudiante.addActionListener(e -> {
                this.dispose();
                if (seleccionUsuarioWindow != null) {
                    seleccionUsuarioWindow.setVisible(true);
                }
            });
        }
    }
}
