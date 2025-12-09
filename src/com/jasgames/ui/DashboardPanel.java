package com.jasgames.ui;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private JPanel panelDashboard;
    private JPanel panelHeaderDashboard;
    private JLabel lblTituloDashboard;
    private JPanel panelFiltrosDashboard;
    private JLabel lblFiltroJuego;
    private JComboBox cbFiltroJuego;
    private JButton btnActualizarDashboard;
    private JButton btnOrdenarPorPuntaje;
    private JTable tblResultados;
    private JScrollPane scrollResultados;

    public DashboardPanel() {

        setLayout(new BorderLayout());
        add(panelDashboard, BorderLayout.CENTER);
    }

}
