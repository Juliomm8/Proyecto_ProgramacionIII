package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.service.AppContext;
import com.jasgames.service.JuegoService;
import com.jasgames.model.Actividad;
import com.jasgames.model.Nino;
import com.jasgames.model.SesionJuego;
import com.jasgames.service.PerfilService;
import com.jasgames.service.PiaService;
import com.jasgames.service.SesionService;
import com.jasgames.service.ScoreService;
import com.jasgames.service.AdaptacionService;
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
    private final PiaService piaService;

    private Nino ninoActual;
    private BaseJuegoPanel juegoEnCurso;

    private DefaultListModel<Juego> juegosListModel;
    private final Map<Integer, JSpinner> spinnersDificultadAsignacion = new LinkedHashMap<>();
    
    private boolean salidaRegistrada = false;
    private volatile boolean guardandoResultado = false;
    
    private JButton btnCambiarNino;
    private JLabel lblAvatarSesion;
    private JLabel lblEstadoUx;
    private javax.swing.Timer uxTimer;

    // Salida protegida (mantener presionado)
    private javax.swing.Timer holdTimer;


    public EstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.juegoService = context.getJuegoService();
        this.perfilService = context.getPerfilService();
        this.sesionService = context.getResultadoService();
        this.piaService = context.getPiaService();

        // Evita IllegalComponentStateException si el panel del diseñador no fue inicializado
        if (panelEstudianteMain == null) {
            // Construimos la UI programáticamente si el .form no inicializó los componentes
            panelEstudianteMain = new JPanel(new BorderLayout(10, 10));

            // Header
            panelHeaderEstudiante = new JPanel(new BorderLayout(8, 8));
            btnBackEstudiante = new JButton("← Volver");
            btnBackEstudiante.setFocusPainted(false);
            lblTituloEstudiante = new JLabel("Modo Estudiante", SwingConstants.CENTER);
            panelHeaderEstudiante.add(btnBackEstudiante, BorderLayout.WEST);
            panelHeaderEstudiante.add(lblTituloEstudiante, BorderLayout.CENTER);

            // Datos estudiante (buscar / sesión visual)
            panelDatosEstudiante = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            lblNombreEstudiante = new JLabel("Estudiante:");
            txtNombreEstudiante = new JTextField(18);
            panelDatosEstudiante.add(lblNombreEstudiante);
            panelDatosEstudiante.add(txtNombreEstudiante);

            JPanel top = new JPanel(new BorderLayout());
            top.add(panelHeaderEstudiante, BorderLayout.NORTH);
            top.add(panelDatosEstudiante, BorderLayout.CENTER);
            panelEstudianteMain.add(top, BorderLayout.NORTH);

            // Panel selección de juegos (izquierda)
            panelSeleccionJuego = new JPanel(new BorderLayout(8, 8));
            lblSeleccionJuego = new JLabel("Selecciona un juego");
            listaJuegosEstudiante = new JList<>();
            scrollJuegosEstudiante = new JScrollPane(listaJuegosEstudiante);

            JPanel acciones = new JPanel(new GridLayout(1, 2, 8, 8));
            btnIniciarJuego = new JButton("▶ Iniciar");
            btnFinalizarJuego = new JButton("⏹ Finalizar");
            acciones.add(btnIniciarJuego);
            acciones.add(btnFinalizarJuego);

            panelSeleccionJuego.add(lblSeleccionJuego, BorderLayout.NORTH);
            panelSeleccionJuego.add(scrollJuegosEstudiante, BorderLayout.CENTER);
            panelSeleccionJuego.add(acciones, BorderLayout.SOUTH);

            Dimension d = new Dimension(320, 10);
            panelSeleccionJuego.setPreferredSize(d);
            panelSeleccionJuego.setMinimumSize(new Dimension(260, 10));

            // Panel de juego (centro)
            panelJuegoEstudiante = new JPanel(new BorderLayout());
            panelJuegoEstudiante.add(new JLabel("Elige un juego de la lista para comenzar", SwingConstants.CENTER), BorderLayout.CENTER);

            JPanel centro = new JPanel(new BorderLayout(10, 10));
            centro.add(panelSeleccionJuego, BorderLayout.WEST);
            centro.add(panelJuegoEstudiante, BorderLayout.CENTER);
            panelEstudianteMain.add(centro, BorderLayout.CENTER);

            // Footer puntaje
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            lblPuntajeActual = new JLabel("Puntaje:");
            lblValorPuntaje = new JLabel("0");
            footer.add(lblPuntajeActual);
            footer.add(lblValorPuntaje);
            panelEstudianteMain.add(footer, BorderLayout.SOUTH);

            setContentPane(panelEstudianteMain);
        } else {
            setContentPane(panelEstudianteMain);
        }

        setTitle("JAS Games - Modo Estudiante");
        // En modo estudiante se protege la salida (evita cerrar con la X sin querer)
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        initModeloJuegos();
        initListeners();

        initUxSesion();
        aplicarEstiloInfantil();
        mostrarModoBusqueda(); // por defecto (si aún no hay sesión visual)

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (guardandoResultado) return;

                // Si hay sesión visual, no cerramos por la X. (se sale con "Salir" manteniendo presionado)
                if (modoSesionVisual()) {
                    mostrarEstadoUx("🖐️ Mantén presionado 'Salir' para salir");
                    return;
                }

                // En modo búsqueda, cerrar = volver / salir normal
                registrarSalidaSiAplica();
                context.setNinoSesion(null);

                dispose();
                if (ventanaAnterior != null) {
                    ventanaAnterior.setVisible(true);
                } else {
                    // Si no hay ventana anterior, cerramos la app
                    System.exit(0);
                }
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

        // Mensaje/estado (tipo "toast") para UX infantil (sin popups)
        lblEstadoUx = new JLabel(" ");
        lblEstadoUx.setFont(lblEstadoUx.getFont().deriveFont(Font.BOLD, 14f));
        lblEstadoUx.setVisible(false);

        // En modo sesión, salir/cambiar niño requiere mantener presionado
        instalarHoldParaAccion(btnCambiarNino,
                () -> salirDeModoEstudiante(),
                "Mantén presionado para cambiar/salir");

        // Se agregan a la derecha (panelDatosEstudiante es FlowLayout)
        panelDatosEstudiante.add(lblAvatarSesion, 0);
        panelDatosEstudiante.add(btnCambiarNino);
        panelDatosEstudiante.add(lblEstadoUx);

        panelDatosEstudiante.revalidate();
        panelDatosEstudiante.repaint();
    }

    /**
     * Estilo infantil SOLO para modo estudiante (sin tocar UI global).
     * Evita cambios agresivos para no romper layouts del UI Designer.
     */
    private void aplicarEstiloInfantil() {
        try {
            Font base = new Font("Dialog", Font.PLAIN, 16);

            if (lblTituloEstudiante != null) {
                lblTituloEstudiante.setFont(lblTituloEstudiante.getFont().deriveFont(Font.BOLD, 20f));
            }

            if (lblSeleccionJuego != null) {
                lblSeleccionJuego.setFont(base.deriveFont(Font.BOLD, 16f));
            }

            if (listaJuegosEstudiante != null) {
                listaJuegosEstudiante.setFont(base);
            }

            if (btnIniciarJuego != null) {
                btnIniciarJuego.setFont(base.deriveFont(Font.BOLD, 16f));
                if (!btnIniciarJuego.getText().contains("▶")) btnIniciarJuego.setText("▶ Iniciar");
            }

            if (btnFinalizarJuego != null) {
                btnFinalizarJuego.setFont(base.deriveFont(Font.BOLD, 16f));
                if (!btnFinalizarJuego.getText().contains("⏹")) btnFinalizarJuego.setText("⏹ Finalizar");
            }

            if (btnBackEstudiante != null) {
                btnBackEstudiante.setFont(base.deriveFont(Font.BOLD, 15f));
            }

            if (lblPuntajeActual != null) {
                lblPuntajeActual.setFont(base.deriveFont(Font.BOLD, 16f));
            }

            if (lblValorPuntaje != null) {
                lblValorPuntaje.setFont(lblValorPuntaje.getFont().deriveFont(Font.BOLD, 18f));
            }

        } catch (Exception ignored) {
            // Si algo falla por L&F, no rompemos la app.
        }
    }

    private void mostrarEstadoUx(String msg) {
        if (lblEstadoUx == null) return;
        if (uxTimer != null) uxTimer.stop();

        lblEstadoUx.setText(msg == null ? "" : msg);
        lblEstadoUx.setVisible(true);

        uxTimer = new javax.swing.Timer(2200, e -> {
            if (lblEstadoUx != null) {
                lblEstadoUx.setText(" ");
                lblEstadoUx.setVisible(false);
            }
        });
        uxTimer.setRepeats(false);
        uxTimer.start();
    }

    private boolean modoSesionVisual() {
        // En sesión visual se entra desde AccesoEstudianteWindow y context.getNinoSesion() queda seteado.
        return (context != null && context.getNinoSesion() != null)
                || (btnCambiarNino != null && btnCambiarNino.isVisible());
    }

    private void salirDeModoEstudiante() {
        if (guardandoResultado) return;

        registrarSalidaSiAplica();
        context.setNinoSesion(null);
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }

    private void instalarHoldParaAccion(JButton boton, Runnable accion, String hint) {
        if (boton == null) return;
        boton.setFocusPainted(false);

        boton.addActionListener(e -> {
            if (!modoSesionVisual()) return; // en búsqueda, el listener normal lo maneja
            if (guardandoResultado) return;
            mostrarEstadoUx("🖐️ " + (hint != null ? hint : "Mantén presionado"));
        });

        boton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!modoSesionVisual()) return;
                if (guardandoResultado) return;
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                // Mantener 1800ms para ejecutar
                if (holdTimer != null) holdTimer.stop();
                holdTimer = new javax.swing.Timer(1800, ev -> {
                    if (holdTimer != null) holdTimer.stop();
                    SwingUtilities.invokeLater(accion);
                });
                holdTimer.setRepeats(false);
                holdTimer.start();

                mostrarEstadoUx("⏳ Mantén... para confirmar");
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (holdTimer != null) holdTimer.stop();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (holdTimer != null) holdTimer.stop();
            }
        });
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
            // En modo búsqueda: vuelve normal.
            // En modo sesión visual (infantil): salida protegida (mantener presionado).
            btnBackEstudiante.addActionListener(e -> {
                if (guardandoResultado) return;
                if (modoSesionVisual()) {
                    mostrarEstadoUx("🖐️ Mantén presionado para salir");
                    return;
                }
                // Volver normal
                registrarSalidaSiAplica();
                context.setNinoSesion(null);
                dispose();
                if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
            });

            instalarHoldParaAccion(btnBackEstudiante, this::salirDeModoEstudiante, "Mantén presionado para salir");
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
            if (modoSesionVisual()) {
                mostrarEstadoUx("🎮 No hay juegos asignados");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Este estudiante no tiene juegos asignados (o están deshabilitados).",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
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
            if (modoSesionVisual()) {
                mostrarEstadoUx("⚠️ Ya estás jugando. Finaliza primero");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Ya hay un juego en curso. Finalízalo antes de iniciar otro.",
                        "Juego en curso",
                        JOptionPane.WARNING_MESSAGE
                );
            }
            return;
        }

        if (ninoActual == null) {
            cargarNinoYJuegos();
            if (ninoActual == null) return;
        }

        Juego juego = listaJuegosEstudiante.getSelectedValue();
        if (juego == null) {
            if (modoSesionVisual()) {
                mostrarEstadoUx("👆 Elige un juego");
            } else {
                JOptionPane.showMessageDialog(this, "Selecciona un juego de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        int nivel = ninoActual.getDificultadEfectiva(juego.getId(), juego.getDificultad());

        // Id estable en int (evita overflow)
        int actividadId = (int) (System.currentTimeMillis() & 0x7fffffff);
        Actividad actividad = new Actividad(actividadId, juego, nivel, 0);

        // Crear panel por ID (escalable)
        BaseJuegoPanel panelJuego = JuegoPanelFactory.crearPanel(actividad, this);
        if (panelJuego == null) {
            if (modoSesionVisual()) {
                mostrarEstadoUx("🚧 Juego no disponible");
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Juego no implementado aún (id=" + juego.getId() + ")",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );
            }
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
                JLabel lbl = new JLabel("💾 Guardando...");
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

        // Mostrar estado mientras se guarda
        lblValorPuntaje.setText("...");
        setGuardandoResultado(true);

        final int[] scoreHolder = {0};

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Integer id = (ninoActual != null) ? ninoActual.getId() : null;
                String nombre = (ninoActual != null) ? ninoActual.getNombre() : "Desconocido";
                String aula = (ninoActual != null) ? ninoActual.getAula() : null;

                LocalDateTime fechaFin = LocalDateTime.now();
                long durMs = actividad.getDuracionMs();
                if (durMs < 0) durMs = 0;

                // Inicio real estimado usando la duración (para reportes correctos)
                LocalDateTime fechaInicio = (durMs > 0)
                        ? fechaFin.minusNanos(durMs * 1_000_000L)
                        : fechaFin;

                SesionJuego sesion = new SesionJuego(
                        id,
                        nombre,
                        aula,
                        actividad.getJuego(),
                        actividad.getNivel(),
                        0,
                        fechaInicio
                );

                // Copiar métricas desde Actividad
                sesion.setFechaFin(fechaFin);
                sesion.setDuracionMs(durMs);
                sesion.setRondasTotales(actividad.getRondasMeta());
                sesion.setRondasCompletadas(actividad.getRondasJugadas());

                sesion.setAciertosTotales(actividad.getRondasCorrectas());
                sesion.setErroresTotales(actividad.getErroresTotales());
                sesion.setIntentosTotales(actividad.getIntentosTotales());
                sesion.setPistasUsadas(actividad.getPistasUsadas());
                sesion.setAciertosPrimerIntento(actividad.getAciertosPrimerIntento());

                sesion.setIntentosMaxPorRonda(actividad.getIntentosMaxPorRonda());
                sesion.setPistasDesdeIntento(actividad.getPistasDesdeIntento());

                // Dificultad inicial/final (adaptación automática puede modificar la final)
                sesion.setDificultadInicial(actividad.getNivel());
                sesion.setDificultadFinal(actividad.getNivel());
                sesion.setDificultadAdaptada(false);

                // Historial (antes de registrar la sesión actual)
                java.util.List<SesionJuego> historial = java.util.Collections.emptyList();
                if (id != null) {
                    historial = sesionService.obtenerUltimasPorNinoYJuego(id, actividad.getJuego().getId(), 3);
                }

                // Score 0..100 (precisión + consistencia + tiempo)
                int score = ScoreService.calcularScore(sesion, historial);
                scoreHolder[0] = score;

                sesion.setPuntaje(score);
                actividad.setPuntos(score);

                // Adaptación automática (usa últimas 3 sesiones, incluyendo la actual)
                if (ninoActual != null) {
                    java.util.List<SesionJuego> ultimas3 = new java.util.ArrayList<>();
                    ultimas3.add(sesion);
                    ultimas3.addAll(historial);
                    if (ultimas3.size() > 3) ultimas3 = ultimas3.subList(0, 3);

                    AdaptacionService.Decision dec = AdaptacionService.evaluarYAplicar(
                            ninoActual,
                            actividad.getJuego().getId(),
                            actividad.getNivel(),
                            ultimas3
                    );

                    sesion.setDificultadFinal(dec.getDificultadSiguiente());
                    sesion.setDificultadAdaptada(dec.isCambio());
                }
                
                // ✅ APLICAR PIA (si existe)
                // Esto actualiza el progreso del PIA y vincula la sesión
                piaService.aplicarSesion(sesion);

                // Persistir
                sesionService.registrarResultado(sesion);

                if (ninoActual != null) {
                    ninoActual.agregarPuntos(score);
                    perfilService.actualizarNino(ninoActual);
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblValorPuntaje.setText(String.valueOf(scoreHolder[0]));

                    // UX infantil: sin popups al terminar (solo un mensaje suave)
                    mostrarEstadoUx("✅ ¡Muy bien! Puntaje guardado");

                    // Limpiar juego y desbloquear UI
                    juegoEnCurso = null;
                    setJuegoActivo(false);

                } catch (Exception ex) {
                    lblValorPuntaje.setText("0");
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