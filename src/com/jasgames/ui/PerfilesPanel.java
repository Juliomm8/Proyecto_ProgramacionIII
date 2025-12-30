package com.jasgames.ui;

import com.jasgames.model.CriterioOrdenNino;
import com.jasgames.model.Nino;
import com.jasgames.service.PerfilService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.HashSet;

public class PerfilesPanel extends JPanel {

    // Campos para que el .form quede contento y, de paso, los usamos
    private JPanel panelPerfiles;        // root en el .form
    private JScrollPane scrollNinos;     // scroll de la lista
    private JPanel formPerfilesPanel;    // panel central del formulario
    private JPanel panelBusquedaOrden;

    private JLabel lblOrdenarPor;
    private JLabel lblBuscar;

    private final PerfilService perfilService;

    // Lista
    private DefaultListModel<Nino> listModel;
    private JList<Nino> listaNinos;

    // Formulario
    private JTextField txtIdNino;
    private JTextField txtNombreNino;
    private JSpinner spEdadNino;
    private JTextField txtDiagnosticoNino;

    // Búsqueda y orden
    private JTextField txtBuscar;
    private JComboBox<String> cbOrdenarPor;

    // Botones
    private JButton btnRegistrarNino;
    private JButton btnActualizarNino;
    private JButton btnEliminarNino;
    private JButton btnLimpiarCampos;
    private JButton btnBuscarNino;
    private JButton btnOrdenar;

    private JComboBox<String> cbAula;
    private JComboBox<String> cbAvatar;

    private static final String[] AVATARES = {
            "😀","😃","😄","😁","😊",
            "🙂","😎","🤩","🥳","😺",
            "🐶","🐱","🐼","🐻","🐵",
            "🦊","🐯","🦁","🐸","🐰",
            "🐨","🐙","🐢","🦄","🐞"
    };

    private static final String[] AULAS_PREDEFINIDAS = {
            "Aula Azul",
            "Aula Roja",
            "Aula Verde",
            "Aula Amarilla",
            "Aula Morada"
    };

    public PerfilesPanel(PerfilService perfilService) {
        this.perfilService = perfilService;
        initComponents();
        cargarNinosDesdeService();
    }

    // ---------------------- UI ----------------------

    private void initComponents() {
        // Este panel es el que realmente se muestra (this)
        setLayout(new BorderLayout(10, 10));

        // ---------- PANEL NORTE: BÚSQUEDA + ORDEN ----------
        panelBusquedaOrden = new JPanel(new GridLayout(2, 3, 5, 5));

        lblBuscar = new JLabel("Buscar:");
        txtBuscar = new JTextField();
        btnBuscarNino = new JButton("Buscar");

        lblOrdenarPor = new JLabel("Ordenar por:");
        cbOrdenarPor = new JComboBox<>(new String[]{"ID", "Nombre", "Edad", "Diagnóstico"});
        btnOrdenar = new JButton("Ordenar");

        panelBusquedaOrden.add(lblBuscar);
        panelBusquedaOrden.add(txtBuscar);
        panelBusquedaOrden.add(btnBuscarNino);

        panelBusquedaOrden.add(lblOrdenarPor);
        panelBusquedaOrden.add(cbOrdenarPor);
        panelBusquedaOrden.add(btnOrdenar);

        add(panelBusquedaOrden, BorderLayout.NORTH);

        // ---------- PANEL OESTE: LISTA DE NIÑOS ----------
        listModel = new DefaultListModel<>();
        listaNinos = new JList<>(listModel);
        scrollNinos = new JScrollPane(listaNinos); // usamos el campo que pide el .form
        scrollNinos.setPreferredSize(new Dimension(220, 0));
        add(scrollNinos, BorderLayout.WEST);

        // Cuando selecciono en la lista, se llenan los campos
        listaNinos.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Nino seleccionado = listaNinos.getSelectedValue();
                    if (seleccionado != null) {
                        mostrarNinoEnFormulario(seleccionado);
                    }
                }
            }
        });

        // ---------- PANEL CENTRAL: FORMULARIO ----------
        formPerfilesPanel = new JPanel(new GridLayout(6, 2, 5, 5));

        txtIdNino = new JTextField();
        txtNombreNino = new JTextField();
        spEdadNino = new JSpinner(new SpinnerNumberModel(6, 3, 18, 1));
        txtDiagnosticoNino = new JTextField("TEA");

        cbAula = new JComboBox<>(AULAS_PREDEFINIDAS);
        cbAula.setEditable(false);

        cbAvatar = new JComboBox<>(AVATARES);

        // Hacer el emoji más grande en el combo
        cbAvatar.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 22f));
                return lbl;
            }
        });

        formPerfilesPanel.add(new JLabel("ID:"));
        formPerfilesPanel.add(txtIdNino);
        formPerfilesPanel.add(new JLabel("Nombre:"));
        formPerfilesPanel.add(txtNombreNino);
        formPerfilesPanel.add(new JLabel("Edad:"));
        formPerfilesPanel.add(spEdadNino);
        formPerfilesPanel.add(new JLabel("Diagnóstico:"));
        formPerfilesPanel.add(txtDiagnosticoNino);
        formPerfilesPanel.add(new JLabel("Aula:"));
        formPerfilesPanel.add(cbAula);
        formPerfilesPanel.add(new JLabel("Avatar:"));
        formPerfilesPanel.add(cbAvatar);

        add(formPerfilesPanel, BorderLayout.CENTER);

        // ---------- PANEL SUR: BOTONES ----------
        JPanel panelBotones = new JPanel();
        btnRegistrarNino = new JButton("Registrar");
        btnActualizarNino = new JButton("Actualizar");
        btnEliminarNino = new JButton("Eliminar");
        btnLimpiarCampos = new JButton("Limpiar campos");

        panelBotones.add(btnRegistrarNino);
        panelBotones.add(btnActualizarNino);
        panelBotones.add(btnEliminarNino);
        panelBotones.add(btnLimpiarCampos);

        add(panelBotones, BorderLayout.SOUTH);

        // ---------- EVENTOS ----------
        btnRegistrarNino.addActionListener(e -> registrarNino());
        btnActualizarNino.addActionListener(e -> actualizarNino());
        btnEliminarNino.addActionListener(e -> eliminarNino());
        btnLimpiarCampos.addActionListener(e -> limpiarCampos());
        btnBuscarNino.addActionListener(e -> buscarNino());
        btnOrdenar.addActionListener(e -> ordenarNinos());
    }

    // ---------------------- LÓGICA (usa PerfilService) ----------------------

    private void cargarNinosDesdeService() {
        listModel.clear();
        for (Nino n : perfilService.obtenerTodosNinos()) {
            listModel.addElement(n);
        }
        refrescarOpcionesAula("Aula Azul");
    }

    private void registrarNino() {
        try {
            int id = Integer.parseInt(txtIdNino.getText().trim());
            String nombre = txtNombreNino.getText().trim();
            int edad = (Integer) spEdadNino.getValue();
            String diagnostico = txtDiagnosticoNino.getText().trim();

            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío");
                return;
            }

            if (perfilService.buscarNinoPorId(id) != null) {
                JOptionPane.showMessageDialog(this,
                        "Ya existe un niño con ese ID. Usa \"Actualizar\" para modificarlo.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Nino nino = new Nino(id, nombre, edad, diagnostico);
            
            String aula = (cbAula.getEditor().getItem() != null) ? cbAula.getEditor().getItem().toString().trim() : "Aula Azul";
            if (aula.isBlank()) aula = "Aula Azul";

            String avatar = (String) cbAvatar.getSelectedItem();
            if (avatar == null || avatar.isBlank()) avatar = "🙂";

            nino.setAula(aula);
            nino.setAvatar(avatar);
            
            perfilService.registrarNino(nino);

            eliminarDeListModelPorId(id);
            listModel.addElement(nino);
            
            refrescarOpcionesAula(aula);

            limpiarCampos();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El ID debe ser numérico", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarNino() {
        try {
            int id = Integer.parseInt(txtIdNino.getText().trim());
            String nombre = txtNombreNino.getText().trim();
            int edad = (Integer) spEdadNino.getValue();
            String diagnostico = txtDiagnosticoNino.getText().trim();

            Nino existente = perfilService.buscarNinoPorId(id);
            if (existente == null) {
                JOptionPane.showMessageDialog(this, "No existe un niño con ese ID", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            String aula = (cbAula.getEditor().getItem() != null) ? cbAula.getEditor().getItem().toString().trim() : "Aula Azul";
            if (aula.isBlank()) aula = "Aula Azul";

            String avatar = (String) cbAvatar.getSelectedItem();
            if (avatar == null || avatar.isBlank()) avatar = "🙂";

            Nino actualizado = new Nino(id, nombre, edad, diagnostico);
            actualizado.setPuntosTotales(existente.getPuntosTotales());
            actualizado.setJuegosAsignados(new java.util.HashSet<>(existente.getJuegosAsignados()));
            actualizado.setDificultadPorJuego(new java.util.HashMap<>(existente.getDificultadPorJuego()));
            
            actualizado.setAula(aula);
            actualizado.setAvatar(avatar);
            refrescarOpcionesAula(aula);

            perfilService.actualizarNino(actualizado);

            eliminarDeListModelPorId(id);
            listModel.addElement(actualizado);

            JOptionPane.showMessageDialog(this, "Datos actualizados correctamente");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El ID debe ser numérico", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarNino() {
        Nino seleccionado = listaNinos.getSelectedValue();
        Integer id = null;

        if (seleccionado != null) {
            id = seleccionado.getId();
        } else {
            try {
                id = Integer.parseInt(txtIdNino.getText().trim());
            } catch (NumberFormatException ex) {
                // sin selección ni ID válido
            }
        }

        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un niño o ingresa un ID válido");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar al niño con ID " + id + "?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            boolean eliminado = perfilService.eliminarNinoPorId(id);
            if (eliminado) {
                eliminarDeListModelPorId(id);
                limpiarCampos();
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el niño para eliminar");
            }
        }
    }

    private void limpiarCampos() {
        txtIdNino.setText("");
        txtNombreNino.setText("");
        spEdadNino.setValue(6);
        txtDiagnosticoNino.setText("TEA");
        txtBuscar.setText("");
        listaNinos.clearSelection();

        refrescarOpcionesAula("Aula Azul");
        cbAvatar.setSelectedItem("🙂");
    }

    private void buscarNino() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa un ID o parte del nombre para buscar");
            return;
        }

        Nino encontrado = perfilService.buscarPorIdONombre(texto);
        if (encontrado == null) {
            JOptionPane.showMessageDialog(this, "No se encontró ningún niño con ese ID o nombre");
            return;
        }

        mostrarNinoEnFormulario(encontrado);
        seleccionarEnLista(encontrado);
    }

    private void ordenarNinos() {
        String opcion = (String) cbOrdenarPor.getSelectedItem();
        CriterioOrdenNino criterio;

        if ("Nombre".equalsIgnoreCase(opcion)) {
            criterio = CriterioOrdenNino.NOMBRE;
        } else if ("Edad".equalsIgnoreCase(opcion)) {
            criterio = CriterioOrdenNino.EDAD;
        } else if ("Diagnóstico".equalsIgnoreCase(opcion)) {
            criterio = CriterioOrdenNino.DIAGNOSTICO;
        } else {
            criterio = CriterioOrdenNino.ID;
        }

        List<Nino> ordenados = perfilService.obtenerNinosOrdenados(criterio);
        listModel.clear();
        for (Nino n : ordenados) {
            listModel.addElement(n);
        }
    }

    // ---------------------- MÉTODOS DE APOYO ----------------------

    private void mostrarNinoEnFormulario(Nino nino) {
        txtIdNino.setText(String.valueOf(nino.getId()));
        txtNombreNino.setText(nino.getNombre());
        spEdadNino.setValue(nino.getEdad());
        txtDiagnosticoNino.setText(nino.getDiagnostico());
        
        refrescarOpcionesAula(nino.getAula());

        String av = nino.getAvatar();
        boolean existe = false;
        for (int i = 0; i < cbAvatar.getItemCount(); i++) {
            if (cbAvatar.getItemAt(i).equals(av)) { existe = true; break; }
        }
        cbAvatar.setSelectedItem(existe ? av : "🙂");
    }

    private void refrescarOpcionesAula(String aulaPreferida) {
        java.util.LinkedHashSet<String> aulas = new java.util.LinkedHashSet<>();

        // 1) predefinidas primero
        for (String a : AULAS_PREDEFINIDAS) aulas.add(a);

        // 2) si en el JSON hay aulas extra, también se agregan (por compatibilidad)
        for (Nino n : perfilService.obtenerTodosNinos()) {
            String a = n.getAula();
            if (a != null && !a.isBlank()) aulas.add(a.trim());
        }

        cbAula.removeAllItems();
        for (String a : aulas) cbAula.addItem(a);

        String aulaFinal = (aulaPreferida == null || aulaPreferida.isBlank()) ? "Aula Azul" : aulaPreferida.trim();

        // Si venía "General", lo convertimos
        if ("General".equalsIgnoreCase(aulaFinal)) aulaFinal = "Aula Azul";

        cbAula.setSelectedItem(aulaFinal);
    }


    private void seleccionarEnLista(Nino nino) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getId() == nino.getId()) {
                listaNinos.setSelectedIndex(i);
                listaNinos.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    private void eliminarDeListModelPorId(int id) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getId() == id) {
                listModel.remove(i);
                break;
            }
        }
    }
}
