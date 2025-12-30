package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.model.Actividad;
import com.jasgames.model.Nino;
import com.jasgames.model.ResultadoJuego;
import com.jasgames.model.TipoJuego;
import com.jasgames.service.PerfilService;
import com.jasgames.service.ResultadoService;
import com.jasgames.ui.juegos.BaseJuegoPanel;
import com.jasgames.ui.juegos.JuegoColoresPanel;
import com.jasgames.ui.juegos.JuegoListener;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

public class EstudianteWindow extends JFrame implements JuegoListener {
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
    private final JFrame ventanaAnterior;
    private final JuegoService juegoService;
    private final PerfilService perfilService;
    private final ResultadoService resultadoService;

    private Nino ninoActual;
    private BaseJuegoPanel juegoEnCurso;

    private DefaultListModel<Juego> juegosListModel;
    private final Map<Integer, JSpinner> spinnersDificultadAsignacion = new LinkedHashMap<>();


    public EstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.juegoService = context.getJuegoService();
        this.perfilService = context.getPerfilService();
        this.resultadoService = context.getResultadoService();

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

    public EstudianteWindow(AppContext context, JFrame ventanaAnterior, Nino ninoSesion) {
        this(context, ventanaAnterior);

        if (ninoSesion != null) {
            this.ninoActual = ninoSesion;

            if (txtNombreEstudiante != null) {
                txtNombreEstudiante.setText(ninoSesion.getNombre());
                txtNombreEstudiante.setEditable(false);
                txtNombreEstudiante.setEnabled(false); // evita teclado
            }

            cargarJuegosAsignadosYHabilitados(ninoSesion);

            // opcional: seleccionar el primer juego automáticamente
            if (juegosListModel != null && juegosListModel.getSize() > 0) {
                listaJuegosEstudiante.setSelectedIndex(0);
            }
        }
    }

    private void initModeloJuegos() {
        juegosListModel = new DefaultListModel<>();
        listaJuegosEstudiante.setModel(juegosListModel);
    }

    private void initListeners() {
        if (btnBackEstudiante != null) {
            btnBackEstudiante.addActionListener(e -> {
                this.dispose();
                context.setNinoSesion(null);
                if (ventanaAnterior != null) {
                    ventanaAnterior.setVisible(true);
                }
            });
        }

        if (txtNombreEstudiante != null) {
            txtNombreEstudiante.addActionListener(e -> cargarNinoYJuegos()); // Enter en el campo
        }

        if (btnIniciarJuego != null) {
            btnIniciarJuego.addActionListener(e -> iniciarJuegoSeleccionado());
        }

        if (btnFinalizarJuego != null) {
            btnFinalizarJuego.addActionListener(e -> finalizarYGuardarManual());
        }
    }


    private void cargarNinoYJuegos() {
        String texto = (txtNombreEstudiante != null) ? txtNombreEstudiante.getText().trim() : "";
        if (texto.isBlank()) {
            JOptionPane.showMessageDialog(this, "Escribe el ID o nombre del estudiante.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Nino encontrado = perfilService.buscarPorIdONombre(texto);
        if (encontrado == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el estudiante con: " + texto, "Aviso", JOptionPane.WARNING_MESSAGE);
            juegosListModel.clear();
            ninoActual = null;
            return;
        }

        ninoActual = encontrado;
        cargarJuegosAsignadosYHabilitados(encontrado);
    }

    private void cargarJuegosAsignadosYHabilitados(Nino nino) {
        juegosListModel.clear();

        Set<Integer> asignados = perfilService.getJuegosAsignados(nino.getId());

        for (Juego j : juegoService.obtenerTodos()) {
            if (!j.isHabilitado()) continue;
            if (!asignados.contains(j.getId())) continue;
            juegosListModel.addElement(j);
        }

        if (juegosListModel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Este estudiante no tiene juegos asignados (o están deshabilitados).",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void iniciarJuegoSeleccionado() {
        if (ninoActual == null) {
            cargarNinoYJuegos();
            if (ninoActual == null) return;
        }

        Juego juego = (Juego) listaJuegosEstudiante.getSelectedValue();
        if (juego == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un juego de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Creamos una actividad (nivel puede ser 1 por ahora)
        int nivel = perfilService.getDificultadAsignada(ninoActual.getId(), juego.getId(), juego.getDificultad());
        Actividad actividad = new Actividad((int) System.currentTimeMillis(), juego, nivel, 0);

        // Instanciamos el panel del juego según su tipo
        if (juego.getTipo() == TipoJuego.COLORES) {
            juegoEnCurso = new JuegoColoresPanel(actividad, this);
        } else {
            JOptionPane.showMessageDialog(this, "Tipo de juego no implementado aún: " + juego.getTipo(), "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Montar el juego en el panel central
        panelJuegoEstudiante.removeAll();
        panelJuegoEstudiante.setLayout(new BorderLayout());
        panelJuegoEstudiante.add(juegoEnCurso, BorderLayout.CENTER);
        panelJuegoEstudiante.revalidate();
        panelJuegoEstudiante.repaint();

        // Reset UI puntaje
        lblValorPuntaje.setText("0");

        // Iniciar
        juegoEnCurso.iniciarJuego();
    }

    private void finalizarYGuardarManual() {
        if (juegoEnCurso == null) {
            JOptionPane.showMessageDialog(this, "No hay ningún juego en curso.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        juegoEnCurso.finalizarJuegoForzado();
    }

    @Override
    public void onJuegoTerminado(Actividad actividad) {
        if (actividad == null || actividad.getJuego() == null) return;

        int puntaje = actividad.getPuntos();
        lblValorPuntaje.setText(String.valueOf(puntaje));

        String nombre = (ninoActual != null) ? ninoActual.getNombre() : "Desconocido";

        // Guardar resultado en memoria (Dashboard lo verá)
        resultadoService.registrarResultado(new ResultadoJuego(
                nombre,
                actividad.getJuego(),
                actividad.getNivel(),
                puntaje,
                LocalDateTime.now()
        ));

        // Opcional: sumar puntos al niño y persistir en ninos.json
        if (ninoActual != null) {
            ninoActual.agregarPuntos(puntaje);
            perfilService.actualizarNino(ninoActual);
        }

        JOptionPane.showMessageDialog(
                this,
                "Puntaje guardado: " + puntaje,
                "OK",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
