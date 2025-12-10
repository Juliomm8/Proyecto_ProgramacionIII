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

        List<Juego> juegos = juegoService.obtenerTodos();
        for (Juego juego : juegos) {
            JCheckBox chk = new JCheckBox(juego.getNombre());
            chk.setSelected(juego.isHabilitado());
            chk.setAlignmentX(Component.LEFT_ALIGNMENT);

            panelListaJuegos.add(chk);
            checkBoxesJuegos.put(juego.getId(), chk);
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

        JOptionPane.showMessageDialog(
                this,
                "Estados de los juegos actualizados.",
                "Información",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Si el estado cambia, actualizamos también la vista de asignaciones
        actualizarAsignacionesParaSeleccionado();
    }

    // ---------------------------------------------------------------------
    // DERECHA: asignar juegos habilitados a un niño
    // ---------------------------------------------------------------------

    /** Reconstruye la lista de checkboxes de asignación para el niño actual. */
    private void actualizarAsignacionesParaSeleccionado() {
        Object item = cboNinos.getSelectedItem();
        if (!(item instanceof Nino)) {
            panelAsignacionContenido.removeAll();
            panelAsignacionContenido.revalidate();
            panelAsignacionContenido.repaint();
            return;
        }
        Nino seleccionado = (Nino) item;

        panelAsignacionContenido.removeAll();
        checkBoxesAsignacion.clear();

        Set<Integer> juegosAsignados = perfilService.getJuegosAsignados(seleccionado.getId());

        for (Juego juego : juegoService.obtenerTodos()) {
            if (!juego.isHabilitado()) {
                continue; // solo mostramos los juegos habilitados
            }

            JCheckBox chk = new JCheckBox(juego.getNombre());
            chk.setSelected(juegosAsignados.contains(juego.getId()));
            chk.setAlignmentX(Component.LEFT_ALIGNMENT);

            panelAsignacionContenido.add(chk);
            checkBoxesAsignacion.put(juego.getId(), chk);
        }

        panelAsignacionContenido.revalidate();
        panelAsignacionContenido.repaint();
    }

    /** Guarda la selección de juegos para el niño actualmente seleccionado. */
    private void onGuardarAsignacion() {
        Object item = cboNinos.getSelectedItem();
        if (!(item instanceof Nino)) {
            JOptionPane.showMessageDialog(
                    this,
                    "No hay ningún niño seleccionado.",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        Nino seleccionado = (Nino) item;

        Set<Integer> nuevosAsignados = new HashSet<>();
        for (Juego juego : juegoService.obtenerTodos()) {
            JCheckBox chk = checkBoxesAsignacion.get(juego.getId());
            if (chk != null && chk.isSelected()) {
                nuevosAsignados.add(juego.getId());
            }
        }

        perfilService.asignarJuegos(seleccionado.getId(), nuevosAsignados);

        JOptionPane.showMessageDialog(
                this,
                "Asignación de juegos guardada para " + seleccionado.getNombre(),
                "Información",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Devuelve los IDs de juegos asignados a un niño por su ID.
     */
    public Set<Integer> getJuegosAsignadosParaNino(int idNino) {
        return perfilService.getJuegosAsignados(idNino);
    }
}
