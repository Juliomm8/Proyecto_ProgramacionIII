package com.jasgames.ui;

import javax.swing.*;
import java.awt.*;

/** Ventana "Acerca de" para darle acabado de producto al sistema. */
public class AboutDialog extends JDialog {

    // Puedes ajustar estos textos sin tocar lógica.
    private static final String APP_NAME = "JAS Games";
    private static final String VERSION = "1.0";
    private static final String COURSE = "Proyecto Programación III e Ingeniería de Requerimientos";
    private static final String AUTHORS = "Equipo JAS Games";

    public AboutDialog(Window owner) {
        super(owner, "Acerca de", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setContentPane(build());
        pack();
        setLocationRelativeTo(owner);
    }

    private JComponent build() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel(APP_NAME, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel ver = new JLabel("Versión " + VERSION, SwingConstants.CENTER);
        ver.setFont(ver.getFont().deriveFont(Font.PLAIN, 14f));

        JPanel north = new JPanel(new GridLayout(2, 1, 0, 4));
        north.add(title);
        north.add(ver);
        root.add(north, BorderLayout.NORTH);

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setOpaque(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setText(
                COURSE + "\n" +
                "Autores: " + AUTHORS + "\n\n" +
                "Sistema educativo con modo docente (moderno) y modo estudiante (infantil), " +
                "con registro de sesiones, PIA, auditoría y backups."
        );

        root.add(info, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(cerrar);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }
}
