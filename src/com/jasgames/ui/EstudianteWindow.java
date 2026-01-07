package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.model.Actividad;
import com.jasgames.model.Nino;
import com.jasgames.model.SesionJuego;
import com.jasgames.service.PerfilService;
import com.jasgames.service.SesionService;
import com.jasgames.ui.juegos.BaseJuegoPanel;
import com.jasgames.ui.juegos.JuegoListener;
import com.jasgames.ui.juegos.JuegoPanelFactory;
import com.jasgames.ui.login.AccesoWindow;

import java.awt.*;
import java.time.LocalDateTime;
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
    private JList<Juego> listaJuegosEstudiante;
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
    private final SesionService sesionService;

    private Nino ninoActual;
    private BaseJuegoPanel juegoEnCurso;

    private DefaultListModel<Juego> juegosListModel;
    private final Map<Integer, JSpinner> spinnersDificultadAsignacion = new LinkedHashMap<>();
    
    private boolean salidaRegistrada = false;
    private volatile boolean guardandoResultado = false;
    
    private JButton btnCambiarNino;
    private JLabel lblAvatarSesion;


    public EstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.juegoService = context.getJuegoService();
        this.perfilService = context.getPerfilService();
        this.sesionService = context.getResultadoService();

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

        if (btnFinalizarJuego != null) btnFinalizarJuego.setEnabled(false);
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

        Color c = context.getAulaService().colorDeAula(aula);
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
                if (guardandoResultado) return;
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

    // 1) Helper para bloquear/desbloquear selección e inicio
    private void setJuegoActivo(boolean activo) {
        // activo = hay juego en curso
        if (btnIniciarJuego != null) btnIniciarJuego.setEnabled(!activo && !guardandoResultado);
        if (listaJuegosEstudiante != null) listaJuegosEstudiante.setEnabled(!activo && !guardandoResultado);

        if (btnFinalizarJuego != null) btnFinalizarJuego.setEnabled(activo && !guardandoResultado);
    }

    private void iniciarJuegoSeleccionado() {
        if (guardandoResultado) return;
        
        // 2) Evita iniciar si ya hay uno
        if (juegoEnCurso != null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ya hay un juego en curso. Finalízalo antes de iniciar otro.",
                    "Juego en curso",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (ninoActual == null) {
            cargarNinoYJuegos();
            if (ninoActual == null) return;
        }

        Juego juego = listaJuegosEstudiante.getSelectedValue();
        if (juego == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un juego de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int nivel = ninoActual.getDificultadJuego(juego.getId(), juego.getDificultad());

        // Id estable en int (evita overflow)
        int actividadId = (int) (System.currentTimeMillis() & 0x7fffffff);
        Actividad actividad = new Actividad(actividadId, juego, nivel, 0);

        // Crear panel por ID (escalable)
        BaseJuegoPanel panelJuego = JuegoPanelFactory.crearPanel(actividad, this);
        if (panelJuego == null) {
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
        panelJuegoEstudiante.add(panelJuego, BorderLayout.CENTER);
        panelJuegoEstudiante.revalidate();
        panelJuegoEstudiante.repaint();

        // Reset UI puntaje
        lblValorPuntaje.setText("0");

        // Guardar referencia y bloquear UI
        juegoEnCurso = panelJuego;
        setJuegoActivo(true);

        // Iniciar una sola vez, pero después del layout para evitar tamaños 0 en el lienzo
        SwingUtilities.invokeLater(() -> {
            if (juegoEnCurso != null) juegoEnCurso.iniciarJuego();
        });
    }

    private void finalizarYGuardarManual() {
        if (guardandoResultado) return;
        // 4) Guarda: si no hay juego, no hacer nada
        if (juegoEnCurso == null) {
            // JOptionPane.showMessageDialog(this, "No hay ningún juego en curso.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        juegoEnCurso.finalizarJuegoForzado();
    }
    
    private void setGuardandoResultado(boolean v) {
        guardandoResultado = v;

        // Botones clave
        if (btnFinalizarJuego != null) btnFinalizarJuego.setEnabled(!v && juegoEnCurso != null);

        // Si tienes botón de "Iniciar" o lista de juegos:
        if (btnIniciarJuego != null) btnIniciarJuego.setEnabled(!v && juegoEnCurso == null);
        if (listaJuegosEstudiante != null) listaJuegosEstudiante.setEnabled(!v && juegoEnCurso == null);

        // Si tienes navegación (volver/cerrar sesión)
        if (btnBackEstudiante != null) btnBackEstudiante.setEnabled(!v);
        if (btnCambiarNino != null) btnCambiarNino.setEnabled(!v);

        // Cursor
        setCursor(v ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());

        // (Opcional PRO) bloquear clicks en toda la ventana con GlassPane
        if (getRootPane() != null) {
            if (v) {
                JPanel glass = new JPanel(new GridBagLayout());
                glass.setOpaque(true);
                glass.setBackground(new Color(0, 0, 0, 80));
                JLabel lbl = new JLabel("Guardando resultado...");
                lbl.setForeground(Color.WHITE);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
                glass.add(lbl);
                getRootPane().setGlassPane(glass);
                glass.setVisible(true);
            } else {
                Component gp = getRootPane().getGlassPane();
                if (gp != null) gp.setVisible(false);
            }
        }
    }

    @Override
    public void onJuegoTerminado(Actividad actividad) {
        if (actividad == null || actividad.getJuego() == null) return;

        int puntaje = actividad.getPuntos();
        lblValorPuntaje.setText(String.valueOf(puntaje));

        setGuardandoResultado(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Integer id = (ninoActual != null) ? ninoActual.getId() : null;
                String nombre = (ninoActual != null) ? ninoActual.getNombre() : "Desconocido";
                String aula = (ninoActual != null) ? ninoActual.getAula() : null;

                SesionJuego sesion = new SesionJuego(
        id,
        nombre,
        aula,
        actividad.getJuego(),
        actividad.getNivel(),
        puntaje,
        LocalDateTime.now()
);

// Copiar métricas desde Actividad (Paso 2: plumbing)
if (actividad != null) {
    sesion.setDuracionMs(actividad.getDuracionMs());
    sesion.setFechaFin(LocalDateTime.now());

    sesion.setRondasTotales(actividad.getRondasMeta());
    sesion.setRondasCompletadas(actividad.getRondasJugadas());

    sesion.setAciertosTotales(actividad.getRondasCorrectas());
    sesion.setErroresTotales(actividad.getErroresTotales());
    sesion.setIntentosTotales(actividad.getIntentosTotales());
    sesion.setPistasUsadas(actividad.getPistasUsadas());
    sesion.setAciertosPrimerIntento(actividad.getAciertosPrimerIntento());

    sesion.setIntentosMaxPorRonda(actividad.getIntentosMaxPorRonda());
    sesion.setPistasDesdeIntento(actividad.getPistasDesdeIntento());
}

sesionService.registrarResultado(sesion);


                if (ninoActual != null) {
                    ninoActual.agregarPuntos(puntaje);
                    perfilService.actualizarNino(ninoActual);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            EstudianteWindow.this,
                            "Puntaje guardado correctamente.",
                            "Listo",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    // 3) Limpiar juego y desbloquear UI
                    juegoEnCurso = null;
                    setJuegoActivo(false);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            EstudianteWindow.this,
                            "Error guardando el resultado:\n" + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    setGuardandoResultado(false);
                }
            }
        }.execute();
    }
}
