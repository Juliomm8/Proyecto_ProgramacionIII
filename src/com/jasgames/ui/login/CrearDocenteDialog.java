package com.jasgames.ui.login;

import com.jasgames.model.Docente;
import com.jasgames.service.AppContext;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/** Diálogo simple para crear un nuevo usuario Docente. */
public class CrearDocenteDialog extends JDialog {

    private final AppContext context;

    private JTextField txtNombre;
    private JTextField txtUsuario;
    private JPasswordField txtPass1;
    private JPasswordField txtPass2;
    private JLabel lblEstado;

    private String usuarioCreado;

    public CrearDocenteDialog(Window owner, AppContext context) {
        super(owner, "Crear usuario docente", ModalityType.APPLICATION_MODAL);
        this.context = context;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(460, 340);
        setLocationRelativeTo(owner);
        setContentPane(crearContenido());
    }

    public String getUsuarioCreado() {
        return usuarioCreado;
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel titulo = new JLabel("Crear cuenta de docente", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        root.add(titulo, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtNombre = new JTextField(18);
        txtUsuario = new JTextField(18);
        txtPass1 = new JPasswordField(18);
        txtPass2 = new JPasswordField(18);

        int y = 0;
        addRow(form, c, y++, "Nombre:", txtNombre);
        addRow(form, c, y++, "Usuario:", txtUsuario);
        addRow(form, c, y++, "Contraseña:", txtPass1);
        addRow(form, c, y++, "Confirmar:", txtPass2);

        lblEstado = new JLabel(" ", SwingConstants.CENTER);
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        form.add(lblEstado, c);

        root.add(form, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(1, 2, 10, 10));
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnCrear = new JButton("Crear");
        botones.add(btnCancelar);
        botones.add(btnCrear);
        root.add(botones, BorderLayout.SOUTH);

        btnCancelar.addActionListener(e -> dispose());
        btnCrear.addActionListener(e -> crear());
        txtPass2.addActionListener(e -> crear());

        return root;
    }

    private void addRow(JPanel form, GridBagConstraints c, int y, String label, JComponent field) {
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y;
        c.weightx = 0.0;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1.0;
        form.add(field, c);
    }

    private void crear() {
        String nombre = (txtNombre.getText() != null) ? txtNombre.getText().trim() : "";
        String usuario = (txtUsuario.getText() != null) ? txtUsuario.getText().trim() : "";
        String p1 = new String(txtPass1.getPassword());
        String p2 = new String(txtPass2.getPassword());

        if (nombre.isBlank() || usuario.isBlank() || p1.isBlank() || p2.isBlank()) {
            lblEstado.setText("Completa todos los campos.");
            return;
        }
        if (!p1.equals(p2)) {
            lblEstado.setText("Las contraseñas no coinciden.");
            return;
        }

        Optional<Docente> creado = context.getAutenticacionService().registrarDocente(usuario, nombre, p1);
        if (creado.isEmpty()) {
            lblEstado.setText("No se pudo crear (usuario repetido o datos inválidos).");
            return;
        }

        usuarioCreado = creado.get().getUsuario();
        context.getAuditoriaService().registrar("CREAR_DOCENTE", "usuario=" + usuarioCreado);
        dispose();
    }
}
