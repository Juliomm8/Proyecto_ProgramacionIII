package com.jasgames.ui.login;

import com.jasgames.model.Docente;
import com.jasgames.service.AppContext;
import com.jasgames.ui.DocenteWindow;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Optional;

public class LoginDocenteWindow extends JFrame {

    private final AppContext context;
    private final JFrame ventanaAnterior;

    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JLabel lblEstado;

    private JCheckBox chkVer;

    private Border bordeNormal;
    private final Border bordeError = BorderFactory.createLineBorder(new Color(204, 51, 51), 2, true);

    public LoginDocenteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;

        setTitle("JAS Games - Login Docente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 330);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    /**
     * Permite prellenar el usuario (por ejemplo, después de crear una cuenta).
     */
    public void sugerirUsuario(String usuario) {
        if (txtUsuario != null) {
            txtUsuario.setText(usuario != null ? usuario : "");
        }
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel app = new JLabel("JAS Games", SwingConstants.CENTER);
        app.setAlignmentX(Component.CENTER_ALIGNMENT);
        app.setFont(app.getFont().deriveFont(Font.BOLD, 24f));

        JLabel titulo = new JLabel("Ingreso de docente", SwingConstants.CENTER);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JLabel subt = new JLabel("Accede para administrar aulas, perfiles y reportes", SwingConstants.CENTER);
        subt.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(app);
        header.add(Box.createVerticalStrut(3));
        header.add(titulo);
        header.add(Box.createVerticalStrut(2));
        header.add(subt);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.CENTER;

        c.gridx = 0; c.gridy = 0;
        c.weightx = 0.0; // Label no necesita estirarse
        form.add(new JLabel("Usuario:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        txtUsuario = new JTextField(18);
        txtUsuario.setPreferredSize(new Dimension(240, 30));
        form.add(txtUsuario, c);

        c.gridx = 0; c.gridy = 1;
        c.weightx = 0.0;
        form.add(new JLabel("Contraseña:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        JPanel passRow = new JPanel(new BorderLayout(8, 0));
        passRow.setOpaque(false);

        txtContrasena = new JPasswordField(18);
        txtContrasena.setPreferredSize(new Dimension(240, 30));
        passRow.add(txtContrasena, BorderLayout.CENTER);

        chkVer = new JCheckBox("Ver");
        chkVer.setFocusPainted(false);
        chkVer.addActionListener(e -> actualizarModoPassword());
        passRow.add(chkVer, BorderLayout.EAST);

        form.add(passRow, c);

        // Guardamos el borde normal para poder “pintar” errores sin arruinar el look
        bordeNormal = txtUsuario.getBorder();

        lblEstado = new JLabel(" ");
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        form.add(lblEstado, c);

        root.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JPanel botones = new JPanel(new GridLayout(1, 2, 10, 10));
        JButton btnVolver = new JButton("Volver");
        JButton btnIngresar = new JButton("Ingresar");
        botones.add(btnVolver);
        botones.add(btnIngresar);

        JButton btnCrearUsuario = linkButton("¿No tienes cuenta? Crear usuario docente");
        btnCrearUsuario.setAlignmentX(Component.CENTER_ALIGNMENT);

        south.add(botones);
        south.add(Box.createVerticalStrut(8));
        south.add(btnCrearUsuario);
        root.add(south, BorderLayout.SOUTH);

        btnVolver.addActionListener(e -> volver());
        btnIngresar.addActionListener(e -> intentarLogin());
        txtUsuario.addActionListener(e -> txtContrasena.requestFocusInWindow());
        txtContrasena.addActionListener(e -> intentarLogin());
        btnCrearUsuario.addActionListener(e -> abrirCrearUsuario());

        // Limpia estado visual cuando el usuario escribe
        txtUsuario.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));
        txtContrasena.getDocument().addDocumentListener(new SimpleDocumentListener(this::limpiarErrores));

        // Estado inicial
        actualizarModoPassword();
        return root;
    }

    private void intentarLogin() {
        String usuario = (txtUsuario.getText() != null) ? txtUsuario.getText().trim() : "";
        String contrasena = new String(txtContrasena.getPassword());

        limpiarErrores();

        if (usuario.isBlank() || contrasena.isBlank()) {
            marcarError(usuario.isBlank(), contrasena.isBlank());
            setEstado("Completa usuario y contraseña.", true);
            if (usuario.isBlank()) txtUsuario.requestFocusInWindow();
            else txtContrasena.requestFocusInWindow();
            return;
        }

        Optional<Docente> docenteOpt = context.getAutenticacionService().login(usuario, contrasena);
        context.getAuditoriaService().loginDocente(usuario, docenteOpt.isPresent());

        if (docenteOpt.isEmpty()) {
            marcarError(true, true);
            setEstado("Usuario o contraseña incorrectos.", true);
            txtContrasena.setText("");
            txtContrasena.requestFocusInWindow();
            return;
        }

        context.setDocenteSesion(docenteOpt.get());

        DocenteWindow docenteWindow = new DocenteWindow(context, ventanaAnterior);
        docenteWindow.setVisible(true);

        dispose();
    }

    private void abrirCrearUsuario() {
        CrearDocenteDialog dialog = new CrearDocenteDialog(this, context);
        dialog.setVisible(true);
        String nuevoUsuario = dialog.getUsuarioCreado();
        if (nuevoUsuario != null && !nuevoUsuario.isBlank()) {
            txtUsuario.setText(nuevoUsuario);
            txtContrasena.setText("");
            txtContrasena.requestFocusInWindow();
            setEstado("Usuario creado. Ahora inicia sesión.", false);
        }
    }

    private void volver() {
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }

    private void actualizarModoPassword() {
        if (txtContrasena == null) return;
        if (chkVer != null && chkVer.isSelected()) {
            txtContrasena.setEchoChar((char) 0);
        } else {
            // '•' = bullet, se ve bien en la mayoría de Look&Feels
            txtContrasena.setEchoChar('\u2022');
        }
    }

    private void setEstado(String msg, boolean esError) {
        lblEstado.setText(msg != null ? msg : " ");
        lblEstado.setForeground(esError ? new Color(204, 51, 51) : new Color(0, 128, 0));
    }

    private void limpiarErrores() {
        if (txtUsuario != null && bordeNormal != null) txtUsuario.setBorder(bordeNormal);
        if (txtContrasena != null && bordeNormal != null) txtContrasena.setBorder(bordeNormal);
        if (lblEstado != null) {
            lblEstado.setText(" ");
            lblEstado.setForeground(UIManager.getColor("Label.foreground"));
        }
    }

    private void marcarError(boolean usuarioError, boolean passError) {
        if (usuarioError && txtUsuario != null) txtUsuario.setBorder(bordeError);
        if (passError && txtContrasena != null) txtContrasena.setBorder(bordeError);
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0, 102, 204));
        return b;
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
