package com.jasgames.ui.juegos;

import com.jasgames.model.Actividad;
import com.jasgames.model.Juego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Minijuego 1: Discriminación visual por colores.
 *
 * - UI minimalista.
 * - Círculos dibujados con Graphics2D (no botones).
 * - Feedback positivo inmediato y errores neutrales.
 */
public class JuegoColoresPanel extends BaseJuegoPanel {

    // ===== Campos “esperados” por el .form =====
    private JPanel panelJuegoColores;
    private JPanel panelInstruccion;
    private JLabel lblInstruccion;
    private JLabel lblFeedback;
    private JPanel panelLienzo;

    // ===== Lienzo real donde se dibujan los círculos =====
    private LienzoColores lienzo;

    // ===== Datos del juego (arrays paralelos como definiste) =====
    private Color[] opciones;
    private Rectangle[] zonasClick;
    private String[] nombresOpciones;

    private Color colorObjetivo;
    private String nombreObjetivo;

    private boolean finalizado;
    private int intentos;

    // Para dibujar un borde suave de selección
    private int ultimoIndiceClick = -1;
    private boolean ultimoClickCorrecto = false;

    private final Random random = new Random();

    private int aciertosActuales;
    private int aciertosRequeridos;
    private boolean bloqueadoTemporal;

    private int ultimoObjetivo = -1;

    // Paleta base (puedes ampliar luego)
    private static final Color ROJO = new Color(220, 40, 40);
    private static final Color AZUL = new Color(55, 110, 220);
    private static final Color VERDE = new Color(60, 170, 90);
    private static final Color AMARILLO = new Color(240, 205, 60);
    private static final Color NARANJA = new Color(240, 140, 50);
    private static final Color MORADO = new Color(150, 85, 210);

    private static final OpcionColor[] PALETA = new OpcionColor[]{
            new OpcionColor("ROJO", ROJO),
            new OpcionColor("AZUL", AZUL),
            new OpcionColor("VERDE", VERDE),
            new OpcionColor("AMARILLO", AMARILLO),
            new OpcionColor("NARANJA", NARANJA),
            new OpcionColor("MORADO", MORADO)
    };

    public JuegoColoresPanel(Actividad actividad, JuegoListener listener) {
        super(actividad, listener);
        initUI();
        // Dejar el panel listo de una vez (si el botón “Iniciar” lo reinicia, no pasa nada)
        iniciarJuego();
    }

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

    private void initUI() {
        // “this” ya tiene BorderLayout por BaseJuegoPanel

        panelJuegoColores = new JPanel(new BorderLayout(10, 10));
        panelJuegoColores.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelJuegoColores.setOpaque(false);
        add(panelJuegoColores, BorderLayout.CENTER);

        // ===== NORTH: Instrucción + feedback =====
        panelInstruccion = new JPanel(new GridLayout(2, 1, 0, 6));
        panelInstruccion.setOpaque(false);

        lblInstruccion = new JLabel("Toca el color", SwingConstants.CENTER);
        lblInstruccion.setFont(lblInstruccion.getFont().deriveFont(Font.BOLD, 26f));

        lblFeedback = new JLabel(" ", SwingConstants.CENTER);
        lblFeedback.setFont(lblFeedback.getFont().deriveFont(Font.PLAIN, 16f));

        panelInstruccion.add(lblInstruccion);
        panelInstruccion.add(lblFeedback);
        panelJuegoColores.add(panelInstruccion, BorderLayout.NORTH);

        // ===== CENTER: contenedor del lienzo =====
        panelLienzo = new JPanel(new BorderLayout());
        panelLienzo.setBackground(Color.WHITE);
        panelLienzo.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        panelLienzo.setOpaque(true);

        lienzo = new LienzoColores();
        lienzo.setOpaque(false);
        panelLienzo.add(lienzo, BorderLayout.CENTER);

        panelJuegoColores.add(panelLienzo, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------------
    // Lógica del juego
    // ---------------------------------------------------------------------

    @Override
    public void iniciarJuego() {
        finalizado = false;
        intentos = 0;

        aciertosActuales = 0;
        aciertosRequeridos = calcularAciertosRequeridos();
        bloqueadoTemporal = false;

        ultimoIndiceClick = -1;
        ultimoClickCorrecto = false;
        ultimoObjetivo = -1;

        lblFeedback.setText(" ");

        int numOpciones = calcularCantidadOpciones();
        prepararRonda(numOpciones);       // genera opciones[] y zonasClick[]
        elegirNuevoObjetivo();            // elige color objetivo (sin repetir)
        actualizarInstruccion();

        lienzo.repaint();
    }

    private int calcularCantidadOpciones() {
        int base = 3;

        Juego j = (actividadActual != null) ? actividadActual.getJuego() : null;
        if (j == null) return base;

        int dif = (actividadActual != null) ? actividadActual.getNivel() : 1;
        if (dif <= 2) return 3;
        if (dif <= 4) return 4;
        return 5;
    }

    private void prepararRonda(int n) {
        // Selección aleatoria sin repetición de la paleta
        List<OpcionColor> lista = new ArrayList<>();
        Collections.addAll(lista, PALETA);
        Collections.shuffle(lista, random);

        int cant = Math.min(n, lista.size());

        opciones = new Color[cant];
        zonasClick = new Rectangle[cant];
        nombresOpciones = new String[cant];

        for (int i = 0; i < cant; i++) {
            OpcionColor oc = lista.get(i);
            opciones[i] = oc.color;
            nombresOpciones[i] = oc.nombre;
            zonasClick[i] = new Rectangle(); // se llenará en paintComponent
        }

        int idxObjetivo = random.nextInt(cant);
        colorObjetivo = opciones[idxObjetivo];
        nombreObjetivo = nombresOpciones[idxObjetivo];
    }

    private void actualizarInstruccion() {
        String hex = toHex(colorObjetivo);

        // Swing HTML es simple pero suficiente para colorear la palabra clave.
        lblInstruccion.setText(
                "<html>Toca el color <font color='" + hex + "'><b>" + nombreObjetivo + "</b></font></html>"
        );
    }

    private void onCirculoSeleccionado(int indice) {
        if (finalizado || bloqueadoTemporal) return;
        if (indice < 0 || indice >= opciones.length) return;

        intentos++;
        bloqueadoTemporal = true;

        ultimoIndiceClick = indice;
        ultimoClickCorrecto = opciones[indice].equals(colorObjetivo);

        if (ultimoClickCorrecto) {
            aciertosActuales++;

            // Guardamos progreso en la actividad (por si finaliza forzado)
            if (actividadActual != null) {
                actividadActual.setPuntos(aciertosActuales);
            }

            lblFeedback.setText("¡Muy bien! " + aciertosActuales + "/" + aciertosRequeridos);

            // ¿Ya cumplió meta?
            if (aciertosActuales >= aciertosRequeridos) {
                finalizado = true;

                new Timer(650, e -> {
                    ((Timer) e.getSource()).stop();
                    finalizarJuego(aciertosActuales); // puntaje final = aciertos
                }).start();

                lienzo.repaint();
                return;
            }

            // Si no terminó, pasa a la siguiente ronda
            new Timer(500, e -> {
                ((Timer) e.getSource()).stop();
                lblFeedback.setText(" ");
                avanzarARondaSiguiente();
            }).start();

        } else {
            // Error neutro: no hay castigo, solo avanzamos a otra ronda
            lblFeedback.setText("Intenta de nuevo");

            new Timer(500, e -> {
                ((Timer) e.getSource()).stop();
                lblFeedback.setText(" ");
                avanzarARondaSiguiente();
            }).start();
        }

        lienzo.repaint();
    }


    private void avanzarARondaSiguiente() {
        // Limpia selección visual
        ultimoIndiceClick = -1;
        ultimoClickCorrecto = false;

        // Nuevo objetivo (puedes dejar las mismas opciones para estabilidad TEA)
        elegirNuevoObjetivo();
        actualizarInstruccion();

        bloqueadoTemporal = false;
        lienzo.repaint();
    }

    private void elegirNuevoObjetivo() {
        if (opciones == null || opciones.length == 0) return;

        int idxObjetivo;
        if (opciones.length == 1) {
            idxObjetivo = 0;
        } else {
            do {
                idxObjetivo = random.nextInt(opciones.length);
            } while (idxObjetivo == ultimoObjetivo);
        }

        ultimoObjetivo = idxObjetivo;
        colorObjetivo = opciones[idxObjetivo];
        nombreObjetivo = nombresOpciones[idxObjetivo];
    }

    private int calcularPuntaje() {
        // Simple: 1 punto por acertar. (Luego puedes hacer: más puntos según dificultad)
        return 1;
    }

    private int calcularAciertosRequeridos() {
        Juego j = (actividadActual != null) ? actividadActual.getJuego() : null;
        int dif = (actividadActual != null) ? actividadActual.getNivel() : 1;

        if (dif < 1) dif = 1;
        if (dif > 5) dif = 5;

        return 5 + (dif - 1) * 2;
    }

    // ---------------------------------------------------------------------
    // Lienzo: dibujo + detección de clic
    // ---------------------------------------------------------------------

    private class LienzoColores extends JPanel {

        // Ajustes de accesibilidad (puedes cambiarlos fácil)
        private static final int PADDING = 30;
        private static final int GAP = 30;
        private static final int DIAMETRO_MAX = 170;
        private static final int DIAMETRO_MIN = 110;

        LienzoColores() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    manejarClick(e.getX(), e.getY());
                }
            });
        }

        private void manejarClick(int x, int y) {
            if (zonasClick == null || opciones == null) return;

            Point p = new Point(x, y);
            for (int i = 0; i < zonasClick.length; i++) {
                Rectangle r = zonasClick[i];
                if (r == null) continue;

                // 1) Caja invisible (Rectangle)
                if (!r.contains(p)) continue;

                // 2) Afinamos: que realmente esté dentro del círculo
                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                int radio = r.width / 2;

                int dx = x - cx;
                int dy = y - cy;
                if ((dx * dx + dy * dy) <= (radio * radio)) {
                    onCirculoSeleccionado(i);
                }
                return;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (opciones == null || opciones.length == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                int n = opciones.length;

                // Distribución horizontal centrada
                int disponibleW = w - (2 * PADDING) - ((n - 1) * GAP);
                int diam = disponibleW / n;
                diam = Math.max(DIAMETRO_MIN, Math.min(DIAMETRO_MAX, diam));

                int totalW = (diam * n) + ((n - 1) * GAP);
                int startX = (w - totalW) / 2;
                int y = (h - diam) / 2;

                for (int i = 0; i < n; i++) {
                    int x = startX + i * (diam + GAP);

                    // Guardamos la zona clickeable (Rectangle invisible)
                    if (zonasClick != null && i < zonasClick.length) {
                        zonasClick[i].setBounds(x, y, diam, diam);
                    }

                    // Círculo relleno
                    g2.setColor(opciones[i]);
                    g2.fillOval(x, y, diam, diam);

                    // Borde neutro
                    g2.setStroke(new BasicStroke(3f));
                    g2.setColor(new Color(70, 70, 70, 140));
                    g2.drawOval(x, y, diam, diam);

                    // Borde extra si fue el último click
                    if (i == ultimoIndiceClick) {
                        g2.setStroke(new BasicStroke(6f));
                        g2.setColor(ultimoClickCorrecto
                                ? new Color(60, 180, 90, 180)   // verde suave
                                : new Color(120, 120, 120, 120) // gris suave
                        );
                        g2.drawOval(x - 4, y - 4, diam + 8, diam + 8);
                    }
                }

            } finally {
                g2.dispose();
            }
        }
    }

    private static String toHex(Color c) {
        if (c == null) return "#000000";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static class OpcionColor {
        final String nombre;
        final Color color;

        OpcionColor(String nombre, Color color) {
            this.nombre = nombre;
            this.color = color;
        }
    }
}
