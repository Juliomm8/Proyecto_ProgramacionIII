package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.ResultadoJuego;
import com.jasgames.service.ResultadoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DashboardPanel extends JPanel {

    // Campos que vienen del .form
    private JPanel panelDashboard;
    private JPanel panelHeaderDashboard;
    private JLabel lblTituloDashboard;
    private JPanel panelFiltrosDashboard;
    private JLabel lblFiltroJuego;
    private JComboBox cbFiltroJuego;
    private JButton btnActualizarDashboard;
    private JButton btnOrdenarPorPuntaje;
    private JScrollPane scrollResultados;
    private JTable tblResultados;

    // --- Nueva lógica ---

    private final ResultadoService resultadoService;
    private DefaultTableModel tablaModelo;

    // Constructor principal: lo usará DocenteWindow
    public DashboardPanel(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;

        setLayout(new BorderLayout());
        add(panelDashboard, BorderLayout.CENTER);

        inicializarTabla();
        cargarJuegosEnCombo();
        inicializarListeners();
    }

    // Constructor sin parámetros SOLO para el diseñador de IntelliJ
    public DashboardPanel() {
        this(new ResultadoService());
    }

    private void inicializarTabla() {
        tablaModelo = new DefaultTableModel(
                new Object[]{"Estudiante", "Juego", "Puntaje", "Fecha y hora"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // tabla solo lectura
            }
        };

        tblResultados.setModel(tablaModelo);
    }

    private void cargarJuegosEnCombo() {
        cbFiltroJuego.removeAllItems();
        cbFiltroJuego.addItem("Todos");

        // Tomamos los juegos únicos de la lista de resultados
        Set<Juego> juegosUnicos = new LinkedHashSet<>();
        for (ResultadoJuego r : resultadoService.obtenerTodos()) {
            juegosUnicos.add(r.getJuego());
        }

        for (Juego juego : juegosUnicos) {
            cbFiltroJuego.addItem(juego); // se mostrará usando toString() de Juego
        }
    }

    private void inicializarListeners() {
        btnActualizarDashboard.addActionListener(e -> {
            cargarJuegosEnCombo();
            actualizarTabla(false);
        });

        btnOrdenarPorPuntaje.addActionListener(e -> {
            cargarJuegosEnCombo();
            actualizarTabla(true);
        });
    }

    private void actualizarTabla(boolean ordenarPorPuntaje) {
        tablaModelo.setRowCount(0); // limpiar tabla

        Object seleccionado = cbFiltroJuego.getSelectedItem();
        List<ResultadoJuego> lista;

        // Si no hay nada o es "Todos"
        if (seleccionado == null || seleccionado instanceof String) {
            lista = new ArrayList<>(resultadoService.obtenerTodos());
            if (ordenarPorPuntaje) {
                lista.sort(Comparator.comparingInt(ResultadoJuego::getPuntaje).reversed());
            }
        } else {
            Juego juego = (Juego) seleccionado;
            if (ordenarPorPuntaje) {
                lista = resultadoService.obtenerPorJuegoOrdenadosPorPuntajeDesc(juego);
            } else {
                lista = resultadoService.obtenerPorJuego(juego);
            }
        }

        for (ResultadoJuego r : lista) {
            String nombreJuego = String.valueOf(r.getJuego());
            tablaModelo.addRow(new Object[]{
                    r.getNombreEstudiante(),
                    nombreJuego,
                    r.getPuntaje(),
                    r.getFechaHora()
            });
        }
    }
}
