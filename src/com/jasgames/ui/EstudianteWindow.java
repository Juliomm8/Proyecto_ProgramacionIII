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
import com.jasgames.ui.juegos.JuegoPanelFactory;
import com.jasgames.ui.login.AccesoWindow;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
    
    private boolean salidaRegistrada = false;
    
    private JButton btnCambiarNino;
    private JLabel lblAvatarSesion;

    private static final Map<String, Color> COLOR_AULA = new HashMap<>();
    static {
        COLOR_AULA.put("Aula Azul", new Color(52, 152, 219));
        COLOR_AULA.put("Aula Roja", new Color(231, 76, 60));
        COLOR_AULA.put("Aula Verde", new Color(46, 204, 113));
        COLOR_AULA.put("Aula Amarilla", new Color(241, 196, 15));
        COLOR_AULA.put("Aula Morada", new Color(155, 89, 182));
    }


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
        
        initUxSesion();
        mostrarModoBusqueda(); // por defecto (si aún no hay sesión visual)
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                registrarSalidaSiAplica();
                context.setNinoSesion(null);
            }
        });

        // Si ya hay sesión (por flujo visual), aplicarla automáticamente
        if (context.getNinoSesion() != null) {
            aplicarSesionEstudiante(context.getNinoSesion());
        }
    }

    public EstudianteWindow() {
        this(new AppContext(), null);
    }

    public EstudianteWindow(AppContext context, JFrame ventanaAnterior, Nino ninoSesion) {
        this(context, ventanaAnterior);

        Nino n = (ninoSesion != null) ? ninoSesion : context.getNinoSesion();

        // GUARD: no permitir entrar sin seleccionar estudiante
        if (n == null) {
            JOptionPane.showMessageDialog(
                    ventanaAnterior,
                    "Acceso denegado: selecciona un estudiante primero.",
                    "Seguridad",
                    JOptionPane.WARNING_MESSAGE
            );

            if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
            else new AccesoWindow(context).setVisible(true);

            dispose();
            return;
        }

        aplicarSesionEstudiante(n);
    }
    
    private void initUxSesion() {
        if (panelDatosEstudiante == null) return;

        lblAvatarSesion = new JLabel("🙂");
        lblAvatarSesion.setFont(lblAvatarSesion.getFont().deriveFont(Font.PLAIN, 28f));
        lblAvatarSesion.setVisible(false);

        btnCambiarNino = new JButton("Cambiar niño");
        btnCambiarNino.setFont(btnCambiarNino.getFont().deriveFont(Font.BOLD, 14f));
        btnCambiarNino.setVisible(false);
        btnCambiarNino.addActionListener(e -> {
            if (btnBackEstudiante != null) btnBackEstudiante.doClick();
        });

        // Se agregan a la derecha (panelDatosEstudiante es FlowLayout)
        panelDatosEstudiante.add(lblAvatarSesion, 0);
        panelDatosEstudiante.add(btnCambiarNino);

        panelDatosEstudiante.revalidate();
        panelDatosEstudiante.repaint();
    }

    private void mostrarModoBusqueda() {
        if (lblNombreEstudiante != null) lblNombreEstudiante.setVisible(true);
        if (txtNombreEstudiante != null) {
            txtNombreEstudiante.setVisible(true);
            txtNombreEstudiante.setEnabled(true);
            txtNombreEstudiante.setEditable(true);
        }

        if (lblAvatarSesion != null) lblAvatarSesion.setVisible(false);
        if (btnCambiarNino != null) btnCambiarNino.setVisible(false);

        if (btnBackEstudiante != null) btnBackEstudiante.setText("← Volver");
        if (lblTituloEstudiante != null) lblTituloEstudiante.setText("Modo Estudiante");

        aplicarTemaAula(null);
    }

    private void aplicarTemaAula(String aula) {
        if (panelHeaderEstudiante == null) return;

        if (aula == null || aula.isBlank()) {
            panelHeaderEstudiante.setBorder(null);
            return;
        }

        Color c = COLOR_AULA.getOrDefault(aula, new Color(149, 165, 166));
        panelHeaderEstudiante.setBorder(BorderFactory.createMatteBorder(0, 0, 6, 0, c));
    }
    
    private void registrarSalidaSiAplica() {
        if (salidaRegistrada) return;

        Nino n = (ninoActual != null) ? ninoActual : context.getNinoSesion();
        if (n != null) {
            context.getAuditoriaService().registrar(
                    "SALIDA_ESTUDIANTE",
                    "id=" + n.getId() + " nombre=" + n.getNombre() + " aula=" + n.getAula()
            );
        }

        salidaRegistrada = true;
    }

    private void aplicarSesionEstudiante(Nino nino) {
        if (nino == null) return;

        this.ninoActual = nino;
        context.setNinoSesion(nino);

        if (txtNombreEstudiante != null) {
            txtNombreEstudiante.setText(nino.getNombre());
            txtNombreEstudiante.setEditable(false);
            txtNombreEstudiante.setEnabled(false);
        }

        cargarJuegosAsignadosYHabilitados(nino);

        if (juegosListModel != null && juegosListModel.getSize() > 0) {
            listaJuegosEstudiante.setSelectedIndex(0);
        }
        
        // MODO VISUAL: ocultar búsqueda por teclado y mostrar sesión
        if (lblNombreEstudiante != null) lblNombreEstudiante.setVisible(false);
        if (txtNombreEstudiante != null) txtNombreEstudiante.setVisible(false);

        if (lblAvatarSesion != null) {
            lblAvatarSesion.setText(nino.getAvatar());
            lblAvatarSesion.setVisible(true);
        }

        if (btnCambiarNino != null) btnCambiarNino.setVisible(true);

        if (btnBackEstudiante != null) btnBackEstudiante.setText("Salir");
        if (lblTituloEstudiante != null) lblTituloEstudiante.setText(nino.getAula() + " • " + nino.getNombre());

        aplicarTemaAula(nino.getAula());
        panelHeaderEstudiante.revalidate();
        panelHeaderEstudiante.repaint();
    }

    private void initModeloJuegos() {
        juegosListModel = new DefaultListModel<>();
        listaJuegosEstudiante.setModel(juegosListModel);
    }

    private void initListeners() {
        if (btnBackEstudiante != null) {
            btnBackEstudiante.addActionListener(e -> {
                registrarSalidaSiAplica();
                context.setNinoSesion(null);

                dispose();

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
        // int nivel = perfilService.getDificultadAsignada(ninoActual.getId(), juego.getId(), juego.getDificultad());
        // Usamos la dificultad del juego o la personalizada si existe
        int nivel = ninoActual.getDificultadJuego(juego.getId(), juego.getDificultad());

        Actividad actividad = new Actividad((int) System.currentTimeMillis(), juego, nivel, 0);

        // Crear panel usando factory
        juegoEnCurso = JuegoPanelFactory.crearPanel(actividad, this);
        if (juegoEnCurso == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Juego no implementado aún (id=" + juego.getId() + ")",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
            );
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

        Integer id = (ninoActual != null) ? ninoActual.getId() : null;
        String nombre = (ninoActual != null) ? ninoActual.getNombre() : "Desconocido";
        String aula = (ninoActual != null) ? ninoActual.getAula() : null;

        resultadoService.registrarResultado(new ResultadoJuego(
                id,
                nombre,
                aula,
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
