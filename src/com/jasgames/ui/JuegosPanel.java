package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.Nino;
import com.jasgames.service.JuegoService;
import com.jasgames.service.PerfilService;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel de gestión de juegos para el modo docente.
 *
 * - Columna izquierda: habilitar / deshabilitar juegos del sistema.
 * - Columna derecha: asignar juegos habilitados a un niño específico.
 *
 * Aunque existe un .form asociado, este panel construye su interfaz por código,
 * reutilizando los mismos nombres de campos para mantener compatibilidad.
 */
public class JuegosPanel extends JPanel {

    // Campos esperados por el .form (algunos no se usan directamente)
    private JPanel panelJuegos;
    private JPanel panelIzquierdo;
    private JPanel panelDerecho;
    private JLabel lblTituloJuegos;
    private JPanel panelListaJuegos;
    private JButton btnGuardarEstadoJuegos;
    private JLabel lblSeleccionNino;
    private JComboBox cboNinos;               // usaremos JComboBox<Nino>
    private JLabel lblTituloAsignacion;
    private JScrollPane panelAsignacionJuegos;
    private JButton btnGuardarAsignacion;

    // Panel interno dentro del scroll de asignaciones
    private JPanel panelAsignacionContenido;

    // Servicios compartidos
    private final JuegoService juegoService;
    private final PerfilService perfilService;

    // Mapas auxiliares: idJuego -> checkbox
    private final Map<Integer, JCheckBox> checkBoxesJuegos = new LinkedHashMap<>();
    private final Map<Integer, JCheckBox> checkBoxesAsignacion = new LinkedHashMap<>();
    private final Map<Integer, JSpinner> spinnersDificultad = new LinkedHashMap<>();
    private final Map<Integer, JSpinner> spinnersDificultadAsignacion = new LinkedHashMap<>();

    public JuegosPanel(JuegoService juegoService, PerfilService perfilService) {
        this.juegoService = juegoService;
        this.perfilService = perfilService;

        initUI();
        cargarJuegos();
        cargarNinos();
        initListeners();
    }

    // ---------------------------------------------------------------------
    // Inicialización de la interfaz
    // ---------------------------------------------------------------------

    private void initUI() {
        setLayout(new GridLayout(1, 2, 10, 0));

        // ====== Columna izquierda: juegos del sistema ======
        panelIzquierdo = new JPanel(new BorderLayout(5, 5));

        lblTituloJuegos = new JLabel("Juegos disponibles");
        lblTituloJuegos.setHorizontalAlignment(SwingConstants.CENTER);
        panelIzquierdo.add(lblTituloJuegos, BorderLayout.NORTH);

        panelListaJuegos = new JPanel();
        panelListaJuegos.setLayout(new BoxLayout(panelListaJuegos, BoxLayout.Y_AXIS));
        JScrollPane scrollJuegos = new JScrollPane(panelListaJuegos);
        panelIzquierdo.add(scrollJuegos, BorderLayout.CENTER);

        btnGuardarEstadoJuegos = new JButton("Guardar cambios");
        panelIzquierdo.add(btnGuardarEstadoJuegos, BorderLayout.SOUTH);

        // ====== Columna derecha: asignación a niños ======
        panelDerecho = new JPanel(new BorderLayout(5, 5));

        JPanel panelTopDerecho = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblSeleccionNino = new JLabel("Seleccionar niño:");
        cboNinos = new JComboBox();
        panelTopDerecho.add(lblSeleccionNino);
        panelTopDerecho.add(cboNinos);
        panelDerecho.add(panelTopDerecho, BorderLayout.NORTH);

        JPanel panelCentroDerecho = new JPanel(new BorderLayout(5, 5));
        lblTituloAsignacion = new JLabel("Juegos asignados al niño:");
        panelCentroDerecho.add(lblTituloAsignacion, BorderLayout.NORTH);

        panelAsignacionContenido = new JPanel();
        panelAsignacionContenido.setLayout(new BoxLayout(panelAsignacionContenido, BoxLayout.Y_AXIS));
        panelAsignacionJuegos = new JScrollPane(panelAsignacionContenido);
        panelCentroDerecho.add(panelAsignacionJuegos, BorderLayout.CENTER);

        panelDerecho.add(panelCentroDerecho, BorderLayout.CENTER);

        btnGuardarAsignacion = new JButton("Guardar asignación");
        panelDerecho.add(btnGuardarAsignacion, BorderLayout.SOUTH);

        // Agregar columnas al panel principal
        add(panelIzquierdo);
        add(panelDerecho);
    }

    // ---------------------------------------------------------------------
    // Carga de datos inicial
    // ---------------------------------------------------------------------

    /** Crea un checkbox por cada juego registrado en el servicio. */
    private void cargarJuegos() {
        panelListaJuegos.removeAll();
        checkBoxesJuegos.clear();
        spinnersDificultad.clear();

        List<Juego> juegos = juegoService.obtenerTodos();
        for (Juego juego : juegos) {
            JCheckBox chk = new JCheckBox(juego.getNombre());
            chk.setSelected(juego.isHabilitado());

            int difInicial = juego.getDificultad();
            if (difInicial < 1) difInicial = 1;
            if (difInicial > 5) difInicial = 5;

            JSpinner spDif = new JSpinner(new SpinnerNumberModel(difInicial, 1, 5, 1));
            spDif.setPreferredSize(new Dimension(55, spDif.getPreferredSize().height));

            spDif.setEnabled(false);
            spDif.setToolTipText("La dificultad se configura por niño en la columna derecha.");

            JPanel fila = new JPanel(new BorderLayout(8, 0));
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);

            fila.add(chk, BorderLayout.CENTER);

            JPanel panelDif = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            panelDif.add(new JLabel("Dif:"));
            panelDif.add(spDif);
            fila.add(panelDif, BorderLayout.EAST);

            panelListaJuegos.add(fila);

            checkBoxesJuegos.put(juego.getId(), chk);
            spinnersDificultad.put(juego.getId(), spDif);
        }

        panelListaJuegos.revalidate();
        panelListaJuegos.repaint();
    }

    /** Carga los niños en el combo y prepara el mapa de asignaciones. */
    private void cargarNinos() {
        cboNinos.removeAllItems();

        java.util.List<Nino> ninos = perfilService.obtenerTodosNinos();
        for (Nino nino : ninos) {
            cboNinos.addItem(nino);
        }

        if (cboNinos.getItemCount() > 0) {
            cboNinos.setSelectedIndex(0);
            actualizarAsignacionesParaSeleccionado();
        }
    }

    private void initListeners() {
        btnGuardarEstadoJuegos.addActionListener(e -> onGuardarEstadosJuegos());
        cboNinos.addActionListener(e -> actualizarAsignacionesParaSeleccionado());
        btnGuardarAsignacion.addActionListener(e -> onGuardarAsignacion());
    }

    // ---------------------------------------------------------------------
    // IZQUIERDA: habilitar / deshabilitar juegos
    // ---------------------------------------------------------------------

    private void onGuardarEstadosJuegos() {
        List<Juego> juegos = juegoService.obtenerTodos();
        for (Juego juego : juegos) {
            JCheckBox chk = checkBoxesJuegos.get(juego.getId());
            if (chk != null) {
                juego.setHabilitado(chk.isSelected());
            }
        }

        JOptionPane.showMessageDialog(this, "Estados guardados. La dificultad se configura por niño.", "Info", JOptionPane.INFORMATION_MESSAGE);

        actualizarAsignacionesParaSeleccionado();
    }

    // ---------------------------------------------------------------------
    // DERECHA: asignar juegos habilitados a un niño
    // ---------------------------------------------------------------------
    /** Reconstruye la lista de checkboxes de asignación para el niño actual. */
    private void actualizarAsignacionesParaSeleccionado() {
        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) return;

        panelAsignacionContenido.removeAll();
        checkBoxesAsignacion.clear();
        spinnersDificultadAsignacion.clear();

        Set<Integer> asignados = (seleccionado.getJuegosAsignados() != null)
                ? seleccionado.getJuegosAsignados()
                : Collections.emptySet();

        for (Juego juego : juegoService.obtenerTodos()) {
            if (!juego.isHabilitado()) continue;
            int idJuego = juego.getId();

            JCheckBox chk = new JCheckBox(juego.getNombre());
            chk.setSelected(asignados != null && asignados.contains(idJuego));
            checkBoxesAsignacion.put(idJuego, chk);

            int difInicial = seleccionado.getDificultadJuego(idJuego, 1);
            JSpinner sp = new JSpinner(new SpinnerNumberModel(difInicial, 1, 5, 1));
            spinnersDificultadAsignacion.put(idJuego, sp);

            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
            fila.add(chk);
            fila.add(new JLabel("Dificultad:"));
            fila.add(sp);

            panelAsignacionContenido.add(fila);
        }

        panelAsignacionContenido.revalidate();
        panelAsignacionContenido.repaint();
    }

    /** Guarda la selección de juegos para el niño actualmente seleccionado. */
    private void onGuardarAsignacion() {
        Nino seleccionado = (Nino) cboNinos.getSelectedItem();
        if (seleccionado == null) return;

        Set<Integer> juegosAsignados = new HashSet<>();
        Map<Integer, Integer> dificultadMap = new HashMap<>();

        for (Map.Entry<Integer, JCheckBox> entry : checkBoxesAsignacion.entrySet()) {
            int idJuego = entry.getKey();
            JCheckBox chk = entry.getValue();

            if (chk.isSelected()) {
                juegosAsignados.add(idJuego);

                JSpinner sp = spinnersDificultadAsignacion.get(idJuego);
                int dif = (Integer) sp.getValue();
                dificultadMap.put(idJuego, dif);
            }
        }

        perfilService.asignarJuegosConDificultad(seleccionado.getId(), juegosAsignados, dificultadMap);

    }


    /**
     * Devuelve los IDs de juegos asignados a un niño por su ID.
     */
    public Set<Integer> getJuegosAsignadosParaNino(int idNino) {
        return perfilService.getJuegosAsignados(idNino);
    }
}
