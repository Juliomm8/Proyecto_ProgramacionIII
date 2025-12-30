package com.jasgames.ui.login;

import com.jasgames.model.Nino;
import com.jasgames.service.AppContext;
import com.jasgames.service.DirectorioEscolarService;
import com.jasgames.ui.EstudianteWindow;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AccesoEstudianteWindow extends JFrame {

    private static final String CARD_AULAS = "AULAS";
    private static final String CARD_ESTUDIANTES = "ESTUDIANTES";

    private final AppContext context;
    private final JFrame ventanaAnterior;
    private final DirectorioEscolarService directorio;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JPanel cardAulas = new JPanel(new BorderLayout());
    private final JPanel cardEstudiantes = new JPanel(new BorderLayout());

    private final JLabel lblTitulo = new JLabel("Selecciona tu aula", SwingConstants.CENTER);
    private final JButton btnAtras = new JButton("Atrás");
    private final JButton btnSalir = new JButton("Salir");

    public AccesoEstudianteWindow(AppContext context, JFrame ventanaAnterior) {
        this.context = context;
        this.ventanaAnterior = ventanaAnterior;
        this.directorio = context.getDirectorioEscolarService();

        setTitle("JAS Games - Acceso Estudiante");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        setContentPane(crearContenido());
        cargarAulas();
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(10, 10));
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 22f));
        header.add(lblTitulo, BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnAtras.setEnabled(false);
        izquierda.add(btnAtras);
        header.add(izquierda, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        derecha.add(btnSalir);
        header.add(derecha, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        cards.add(cardAulas, CARD_AULAS);
        cards.add(cardEstudiantes, CARD_ESTUDIANTES);
        root.add(cards, BorderLayout.CENTER);

        btnSalir.addActionListener(e -> salir());
        btnAtras.addActionListener(e -> volverAulas());

        return root;
    }

    private void cargarAulas() {
        List<String> aulas = directorio.obtenerAulas();
        cardAulas.removeAll();

        JPanel panelAulas = new JPanel(new GridLayout(0, 3, 20, 20));
        panelAulas.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        if (aulas.isEmpty()) {
            JLabel msg = new JLabel("No hay aulas registradas en ninos.json", SwingConstants.CENTER);
            msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 18f));
            cardAulas.add(msg, BorderLayout.CENTER);
        } else {
            for (String aula : aulas) {
                JButton btn = new JButton(aula);
                btn.setFont(btn.getFont().deriveFont(Font.BOLD, 22f));
                btn.setFocusPainted(false);
                btn.addActionListener(e -> abrirAula(aula));
                panelAulas.add(btn);
            }

            JScrollPane scroll = new JScrollPane(panelAulas);
            scroll.setBorder(null);
            cardAulas.add(scroll, BorderLayout.CENTER);
        }

        lblTitulo.setText("Selecciona tu aula");
        btnAtras.setEnabled(false);
        cardLayout.show(cards, CARD_AULAS);
        cardAulas.revalidate();
        cardAulas.repaint();
    }

    private void abrirAula(String aula) {
        List<Nino> estudiantes = directorio.obtenerEstudiantesPorAula(aula);
        cardEstudiantes.removeAll();

        JPanel panelEstudiantes = new JPanel(new GridLayout(0, 4, 15, 15));
        panelEstudiantes.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        if (estudiantes.isEmpty()) {
            JLabel msg = new JLabel("No hay estudiantes en el aula: " + aula, SwingConstants.CENTER);
            msg.setFont(msg.getFont().deriveFont(Font.PLAIN, 18f));
            cardEstudiantes.add(msg, BorderLayout.CENTER);
        } else {
            for (Nino n : estudiantes) {
                JButton ficha = new JButton(formatoFicha(n));
                ficha.setFont(ficha.getFont().deriveFont(Font.BOLD, 16f));
                ficha.setFocusPainted(false);
                ficha.addActionListener(e -> seleccionarEstudiante(n));
                panelEstudiantes.add(ficha);
            }

            JScrollPane scroll = new JScrollPane(panelEstudiantes);
            scroll.setBorder(null);
            cardEstudiantes.add(scroll, BorderLayout.CENTER);
        }

        lblTitulo.setText("Aula: " + aula);
        btnAtras.setEnabled(true);
        cardLayout.show(cards, CARD_ESTUDIANTES);
        cardEstudiantes.revalidate();
        cardEstudiantes.repaint();
    }

    private String formatoFicha(Nino n) {
        String avatar = n.getAvatar();
        String nombre = n.getNombre();
        return "<html><div style='text-align:center;'>" +
                "<span style='font-size:30px;'>" + avatar + "</span><br/>" +
                "<span style='font-size:16px;'>" + nombre + "</span>" +
                "</div></html>";
    }

    private void seleccionarEstudiante(Nino nino) {
        context.setNinoSesion(nino);

        EstudianteWindow w = new EstudianteWindow(context, this, nino);
        w.setVisible(true);
        setVisible(false);
    }

    private void volverAulas() {
        lblTitulo.setText("Selecciona tu aula");
        btnAtras.setEnabled(false);
        cardLayout.show(cards, CARD_AULAS);
    }

    private void salir() {
        context.setNinoSesion(null);
        dispose();
        if (ventanaAnterior != null) ventanaAnterior.setVisible(true);
    }
}
