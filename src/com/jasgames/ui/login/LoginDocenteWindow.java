package com.jasgames.ui.login;

import com.jasgames.model.Docente;
import com.jasgames.service.AppContext;
import com.jasgames.ui.DocenteWindow;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class LoginDocenteWindow extends JFrame {

    private final AppContext context;
    private final JFrame ventanaAnterior;

    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JLabel lblEstado;

    public LoginDocenteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;

        setTitle("JAS Games - Login Docente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 330);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Ingreso Docente", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        root.add(titulo, BorderLayout.NORTH);

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
        txtUsuario.setPreferredSize(new Dimension(220, 28));
        form.add(txtUsuario, c);

        c.gridx = 0; c.gridy = 1;
        c.weightx = 0.0;
        form.add(new JLabel("Contraseña:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        txtContrasena = new JPasswordField(18);
        txtContrasena.setPreferredSize(new Dimension(220, 28));
        form.add(txtContrasena, c);

        lblEstado = new JLabel(" ");
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        form.add(lblEstado, c);

        root.add(form, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(1, 2, 10, 10));
        JButton btnVolver = new JButton("Volver");
        JButton btnIngresar = new JButton("Ingresar");
        botones.add(btnVolver);
        botones.add(btnIngresar);
        root.add(botones, BorderLayout.SOUTH);

        btnVolver.addActionListener(e -> volver());
        btnIngresar.addActionListener(e -> intentarLogin());
        txtContrasena.addActionListener(e -> intentarLogin());

        return root;
    }

    private void intentarLogin() {
        String usuario = (txtUsuario.getText() != null) ? txtUsuario.getText().trim() : "";
        String contrasena = new String(txtContrasena.getPassword());

        if (usuario.isBlank() || contrasena.isBlank()) {
            lblEstado.setText("Completa usuario y contraseña.");
            return;
        }

        Optional<Docente> docenteOpt = context.getAutenticacionService().login(usuario, contrasena);
        if (docenteOpt.isEmpty()) {
            lblEstado.setText("Credenciales incorrectas.");
            return;
        }

        context.setDocenteSesion(docenteOpt.get());

        DocenteWindow docenteWindow = new DocenteWindow(context, ventanaAnterior);
        docenteWindow.setVisible(true);

        dispose();
    }

    private void volver() {
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }
}
