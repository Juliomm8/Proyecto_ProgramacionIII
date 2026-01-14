package com.jasgames.ui.login;

import com.jasgames.model.Docente;
import com.jasgames.service.AppContext;

import javax.swing.*;
import javax.swing.border.Border;
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

    private JCheckBox chkVer;
    private Border bordeNormal;
    private final Border bordeError = BorderFactory.createLineBorder(new Color(204, 51, 51), 2, true);

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

        // Guardamos borde normal para restaurar cuando se limpia error
        bordeNormal = txtNombre.getBorder();

        int y = 0;
        addRow(form, c, y++, "Nombre:", txtNombre);
        addRow(form, c, y++, "Usuario:", txtUsuario);
        addRow(form, c, y++, "Contraseña:", txtPass1);
        addRow(form, c, y++, "Confirmar:", txtPass2);

        // Reglas rápidas
        JLabel reglas = new JLabel("<html><div style='text-align:center; opacity:0.9;'>" +
                "Usuario: mínimo 3 caracteres &nbsp;|&nbsp; Contraseña: mínimo 4" +
                "</div></html>", SwingConstants.CENTER);
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        form.add(reglas, c);

        chkVer = new JCheckBox("Ver contraseñas");
        chkVer.setFocusPainted(false);
        chkVer.addActionListener(e -> actualizarModoPassword());
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        form.add(chkVer, c);

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

        // Limpia estado visual cuando se escribe
        txtNombre.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));
        txtUsuario.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));
        txtPass1.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));
        txtPass2.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));

        actualizarModoPassword();

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

        limpiarErrores();

        boolean errNombre = nombre.isBlank();
        boolean errUsuario = usuario.isBlank() || usuario.length() < 3;
        boolean errP1 = p1.isBlank() || p1.length() < 4;
        boolean errP2 = p2.isBlank() || p2.length() < 4;

        if (errNombre || errUsuario || errP1 || errP2) {
            marcarError(errNombre, errUsuario, errP1, errP2);
            setEstado("Revisa los campos (usuario ≥ 3, contraseña ≥ 4).", true);
            return;
        }

        if (!p1.equals(p2)) {
            marcarError(false, false, true, true);
            setEstado("Las contraseñas no coinciden.", true);
            return;
        }

        Optional<Docente> creado = context.getAutenticacionService().registrarDocente(usuario, nombre, p1);
        if (creado.isEmpty()) {
            marcarError(false, true, false, false);
            setEstado("No se pudo crear. Es posible que el usuario ya exista.", true);
            return;
        }

        usuarioCreado = creado.get().getUsuario();
        context.getAuditoriaService().registrar("CREAR_DOCENTE", "usuario=" + usuarioCreado);
        dispose();
    }

    private void actualizarModoPassword() {
        boolean ver = chkVer != null && chkVer.isSelected();
        char echo = ver ? (char) 0 : '\u2022';
        txtPass1.setEchoChar(echo);
        txtPass2.setEchoChar(echo);
    }

    private void setEstado(String msg, boolean esError) {
        lblEstado.setText(msg != null ? msg : " ");
        lblEstado.setForeground(esError ? new Color(204, 51, 51) : new Color(0, 128, 0));
    }

    private void limpiarErrores() {
        if (bordeNormal == null) return;
        if (txtNombre != null) txtNombre.setBorder(bordeNormal);
        if (txtUsuario != null) txtUsuario.setBorder(bordeNormal);
        if (txtPass1 != null) txtPass1.setBorder(bordeNormal);
        if (txtPass2 != null) txtPass2.setBorder(bordeNormal);
        if (lblEstado != null) {
            lblEstado.setText(" ");
            lblEstado.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    private void marcarError(boolean nombre, boolean usuario, boolean p1, boolean p2) {
        if (nombre) txtNombre.setBorder(bordeError);
        if (usuario) txtUsuario.setBorder(bordeError);
        if (p1) txtPass1.setBorder(bordeError);
        if (p2) txtPass2.setBorder(bordeError);
    }

    /** Mini helper para no escribir el DocumentListener completo. */
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable r;
        SimpleDocumentListener(Runnable r) { this.r = r; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
    }
}
