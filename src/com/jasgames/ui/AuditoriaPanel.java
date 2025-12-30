package com.jasgames.ui;

import com.jasgames.service.AuditoriaService;

import javax.swing.*;
import java.awt.*;

public class AuditoriaPanel extends JPanel {

    private final AuditoriaService auditoriaService;
    private final JTextArea txtLog = new JTextArea();

    public AuditoriaPanel(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Auditoría de accesos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(txtLog), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefrescar = new JButton("Refrescar");
        JButton btnLimpiarVista = new JButton("Limpiar vista");

        btnRefrescar.addActionListener(e -> cargar());
        btnLimpiarVista.addActionListener(e -> txtLog.setText(""));

        acciones.add(btnLimpiarVista);
        acciones.add(btnRefrescar);
        add(acciones, BorderLayout.SOUTH);

        cargar();
    }

    private void cargar() {
        txtLog.setText(auditoriaService.leerUltimasLineas(200));
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }
}
