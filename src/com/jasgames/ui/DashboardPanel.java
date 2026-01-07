package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.SesionJuego;
import com.jasgames.service.ResultadoService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

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

    // --- Servicios / Tabla ---
    private final ResultadoService resultadoService;
    private DefaultTableModel tablaModelo;

    // --- Filtros nuevos  ---
    private JComboBox<String> cbFiltroAula;
    private JComboBox<Object> cbFiltroDificultad; // "Todas" + Integer
    private JComboBox<String> cbFiltroRango;
    private JComboBox<String> cbOrden;
    private JTextField txtBuscar;
    private JButton btnLimpiar;
    
    // KPIs
    private JPanel panelKpis;
    private JLabel lblKpiPartidas;
    private JLabel lblKpiPromedio;
    private JLabel lblKpiMejor;
    private JLabel lblKpiAulaActiva;
    
    private JPanel cardKpiAulaActiva;
    private String ultimaAulaActiva = null;
    
    private JPanel cardKpiMejor;
    private Integer ultimoIdMejor = null;
    private String ultimoNombreMejor = null;

    private static final String[] AULAS_PREDEFINIDAS = {
            "Aula Azul", "Aula Roja", "Aula Verde", "Aula Amarilla", "Aula Morada"
    };

    // Constructor principal: lo usará DocenteWindow
    public DashboardPanel(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;

        setLayout(new BorderLayout());
        add(panelDashboard, BorderLayout.CENTER);

        inicializarTabla();
        construirFiltrosExtra();
        construirKpis();
        recargarCombosFiltros(true);
        inicializarListeners();

        // Mostrar algo al abrir
        actualizarTabla(false);
    }

    // Constructor sin parámetros SOLO para el diseñador de IntelliJ
    public DashboardPanel() {
        this(new ResultadoService());
    }

    private void inicializarTabla() {
        // Mejor: mostrar ID y Aula (para que el filtro tenga sentido)
        tablaModelo = new DefaultTableModel(
                new Object[]{"ID", "Estudiante", "Aula", "Juego", "Dificultad", "Puntaje", "Fecha", "Hora"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblResultados.setModel(tablaModelo);
    }

    private void construirFiltrosExtra() {
        // Creamos componentes
        cbFiltroAula = new JComboBox<>();
        cbFiltroDificultad = new JComboBox<>();
        cbFiltroRango = new JComboBox<>(new String[]{"Todo", "Hoy", "Últimos 7 días", "Últimos 30 días"});
        cbOrden = new JComboBox<>(new String[]{"Fecha (más reciente)", "Fecha (más antigua)", "Puntaje (mayor)", "Puntaje (menor)"});
        txtBuscar = new JTextField(14);
        btnLimpiar = new JButton("Limpiar");

        // Cambiamos textos de botones existentes para que tengan sentido
        btnActualizarDashboard.setText("Aplicar");
        // El botón antiguo lo dejamos como atajo (puntaje desc), pero ya existe cbOrden.
        btnOrdenarPorPuntaje.setText("Atajo: Puntaje ↓");

        // Reconstruimos el panel de filtros (sin tocar el .form)
        panelFiltrosDashboard.removeAll();

        panelFiltrosDashboard.add(lblFiltroJuego);
        panelFiltrosDashboard.add(cbFiltroJuego);

        panelFiltrosDashboard.add(new JLabel("Aula:"));
        panelFiltrosDashboard.add(cbFiltroAula);

        panelFiltrosDashboard.add(new JLabel("Dificultad:"));
        panelFiltrosDashboard.add(cbFiltroDificultad);

        panelFiltrosDashboard.add(new JLabel("Rango:"));
        panelFiltrosDashboard.add(cbFiltroRango);

        panelFiltrosDashboard.add(new JLabel("Buscar:"));
        panelFiltrosDashboard.add(txtBuscar);

        panelFiltrosDashboard.add(new JLabel("Orden:"));
        panelFiltrosDashboard.add(cbOrden);

        panelFiltrosDashboard.add(btnActualizarDashboard);
        panelFiltrosDashboard.add(btnOrdenarPorPuntaje);
        panelFiltrosDashboard.add(btnLimpiar);

        panelFiltrosDashboard.revalidate();
        panelFiltrosDashboard.repaint();
    }
    
    private void construirKpis() {
        panelKpis = new JPanel(new GridLayout(1, 4, 12, 12));
        panelKpis.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblKpiPartidas = crearKpi("Partidas", "0");
        lblKpiPromedio = crearKpi("Promedio", "0");
        lblKpiMejor = crearKpi("Mejor", "-");
        lblKpiAulaActiva = crearKpi("Aula activa", "-");

        panelKpis.add(wrapKpi(lblKpiPartidas));
        panelKpis.add(wrapKpi(lblKpiPromedio));

        cardKpiMejor = wrapKpi(lblKpiMejor);
        panelKpis.add(cardKpiMejor);

        cardKpiAulaActiva = wrapKpi(lblKpiAulaActiva);
        panelKpis.add(cardKpiAulaActiva);

        habilitarClickMejor();
        habilitarClickAulaActiva();

        // Insertarlo arriba de la tabla: en panelDashboard, justo antes del scrollResultados
        // Como el .form puede variar, lo metemos en el CENTER con un contenedor vertical.
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(panelKpis, BorderLayout.NORTH);
        centro.add(scrollResultados, BorderLayout.CENTER);

        // Reemplazamos la zona del scroll por el nuevo contenedor
        // Quitamos y re-agregamos para no depender del .form
        if (panelDashboard != null) {
            panelDashboard.remove(scrollResultados);
            panelDashboard.add(centro, BorderLayout.CENTER);
            panelDashboard.revalidate();
            panelDashboard.repaint();
        }
    }

    private JLabel crearKpi(String titulo, String valor) {
        JLabel lbl = new JLabel("<html><div style='text-align:center;'>" +
                "<div style='font-size:12px;'>" + titulo + "</div>" +
                "<div style='font-size:20px; font-weight:bold;'>" + valor + "</div>" +
                "</div></html>", SwingConstants.CENTER);
        return lbl;
    }

    private JPanel wrapKpi(JLabel lbl) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.add(lbl, BorderLayout.CENTER);
        return card;
    }
    
    private void habilitarClickMejor() {
        if (cardKpiMejor == null) return;

        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((ultimoIdMejor == null || ultimoIdMejor <= 0) &&
                    (ultimoNombreMejor == null || ultimoNombreMejor.isBlank())) {
                    return;
                }

                // Si hay ID, es el filtro más exacto; si no, usamos nombre
                if (ultimoIdMejor != null && ultimoIdMejor > 0) {
                    txtBuscar.setText(String.valueOf(ultimoIdMejor));
                } else {
                    txtBuscar.setText(ultimoNombreMejor);
                }

                actualizarTabla(false); // por si acaso (aunque el DocumentListener ya refresca)
            }
        };

        // Para que el click funcione tanto en la tarjeta como en el texto
        cardKpiMejor.setToolTipText("Click para filtrar por el estudiante con mejor puntaje");
        cardKpiMejor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardKpiMejor.addMouseListener(click);

        if (lblKpiMejor != null) {
            lblKpiMejor.setToolTipText("Click para filtrar por el estudiante con mejor puntaje");
            lblKpiMejor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblKpiMejor.addMouseListener(click);
        }
    }
    
    private void habilitarClickAulaActiva() {
        if (cardKpiAulaActiva == null) return;

        cardKpiAulaActiva.setToolTipText("Click para filtrar por el aula más activa");
        cardKpiAulaActiva.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cardKpiAulaActiva.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (ultimaAulaActiva == null || ultimaAulaActiva.isBlank()) return;

                seleccionarAulaEnFiltro(ultimaAulaActiva);
                actualizarTabla(false);
            }
        });
    }

    private void seleccionarAulaEnFiltro(String aula) {
        if (cbFiltroAula == null || aula == null || aula.isBlank()) return;

        boolean existe = false;
        for (int i = 0; i < cbFiltroAula.getItemCount(); i++) {
            String item = cbFiltroAula.getItemAt(i);
            if (item != null && item.equalsIgnoreCase(aula)) { existe = true; break; }
        }
        if (!existe) cbFiltroAula.addItem(aula);

        cbFiltroAula.setSelectedItem(aula);
    }

    private void inicializarListeners() {
        btnActualizarDashboard.addActionListener(e -> {
            recargarCombosFiltros(true);
            actualizarTabla(false);
        });

        btnOrdenarPorPuntaje.addActionListener(e -> {
            recargarCombosFiltros(true);
            actualizarTabla(true); // fuerza puntaje desc como atajo
        });

        btnLimpiar.addActionListener(e -> limpiarFiltros());

        // Aplicar en vivo (sin apretar "Aplicar")
        cbFiltroJuego.addActionListener(e -> actualizarTabla(false));
        cbFiltroAula.addActionListener(e -> actualizarTabla(false));
        cbFiltroDificultad.addActionListener(e -> actualizarTabla(false));
        cbFiltroRango.addActionListener(e -> actualizarTabla(false));
        cbOrden.addActionListener(e -> actualizarTabla(false));

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizarTabla(false); }
            @Override public void removeUpdate(DocumentEvent e) { actualizarTabla(false); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarTabla(false); }
        });
    }

    private void limpiarFiltros() {
        cbFiltroJuego.setSelectedItem("Todos");
        cbFiltroAula.setSelectedItem("Todas");
        cbFiltroDificultad.setSelectedItem("Todas");
        cbFiltroRango.setSelectedItem("Todo");
        cbOrden.setSelectedItem("Fecha (más reciente)");
        txtBuscar.setText("");
        actualizarTabla(false);
    }

    private void recargarCombosFiltros(boolean mantenerSelecciones) {
        Object juegoSel = mantenerSelecciones ? cbFiltroJuego.getSelectedItem() : "Todos";
        String aulaSel = mantenerSelecciones ? (String) cbFiltroAula.getSelectedItem() : "Todas";
        Object difSel = mantenerSelecciones ? cbFiltroDificultad.getSelectedItem() : "Todas";

        // ---- Juegos ----
        cbFiltroJuego.removeAllItems();
        cbFiltroJuego.addItem("Todos");

        Set<Juego> juegosUnicos = new LinkedHashSet<>();
        for (SesionJuego r : resultadoService.obtenerTodos()) {
            if (r.getJuego() != null) juegosUnicos.add(r.getJuego());
        }
        for (Juego j : juegosUnicos) cbFiltroJuego.addItem(j);

        // ---- Aulas ----
        cbFiltroAula.removeAllItems();
        cbFiltroAula.addItem("Todas");

        // Primero predefinidas (aunque no haya resultados)
        for (String a : AULAS_PREDEFINIDAS) cbFiltroAula.addItem(a);

        // Luego cualquier aula extra que exista en resultados
        Set<String> aulasExtras = new LinkedHashSet<>();
        for (SesionJuego r : resultadoService.obtenerTodos()) {
            if (r.getAula() != null && !r.getAula().isBlank()) aulasExtras.add(r.getAula().trim());
        }
        for (String a : aulasExtras) {
            boolean ya = false;
            for (int i = 0; i < cbFiltroAula.getItemCount(); i++) {
                if (cbFiltroAula.getItemAt(i).equalsIgnoreCase(a)) { ya = true; break; }
            }
            if (!ya) cbFiltroAula.addItem(a);
        }

        // ---- Dificultad ----
        cbFiltroDificultad.removeAllItems();
        cbFiltroDificultad.addItem("Todas");
        for (int i = 1; i <= 5; i++) cbFiltroDificultad.addItem(i);

        // Restaurar selecciones si se puede
        if (mantenerSelecciones) {
            if (juegoSel != null) cbFiltroJuego.setSelectedItem(juegoSel);
            if (aulaSel != null) cbFiltroAula.setSelectedItem(aulaSel);
            if (difSel != null) cbFiltroDificultad.setSelectedItem(difSel);
        }
    }

    private void actualizarTabla(boolean atajoOrdenarPorPuntajeDesc) {
        tablaModelo.setRowCount(0);

        List<SesionJuego> lista = new ArrayList<>(resultadoService.obtenerTodos());

        // ----- 1) Filtro: Juego -----
        Object juegoSel = cbFiltroJuego.getSelectedItem();
        if (juegoSel instanceof Juego) {
            Juego j = (Juego) juegoSel;
            lista.removeIf(r -> r.getJuego() == null || r.getJuego().getId() != j.getId());
        }

        // ----- 2) Filtro: Aula -----
        String aulaSel = (String) cbFiltroAula.getSelectedItem();
        if (aulaSel != null && !aulaSel.equalsIgnoreCase("Todas")) {
            lista.removeIf(r -> r.getAula() == null || !r.getAula().equalsIgnoreCase(aulaSel));
        }

        // ----- 3) Filtro: Dificultad -----
        Object difSel = cbFiltroDificultad.getSelectedItem();
        if (difSel instanceof Integer) {
            int d = (Integer) difSel;
            lista.removeIf(r -> r.getDificultad() != d);
        }

        // ----- 4) Filtro: Rango de tiempo -----
        String rango = (String) cbFiltroRango.getSelectedItem();
        if (rango == null) rango = "Todo";

        LocalDateTime ahora = LocalDateTime.now();
        switch (rango) {
            case "Hoy" -> lista.removeIf(r -> r.getFechaHora() == null || !r.getFechaHora().toLocalDate().equals(LocalDate.now()));
            case "Últimos 7 días" -> lista.removeIf(r -> r.getFechaHora() == null || r.getFechaHora().isBefore(ahora.minusDays(7)));
            case "Últimos 30 días" -> lista.removeIf(r -> r.getFechaHora() == null || r.getFechaHora().isBefore(ahora.minusDays(30)));
            default -> { /* Todo */ }
        }

        // ----- 5) Filtro: Buscar (nombre / id / aula / juego) -----
        String q = (txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        if (!q.isBlank()) {
            lista.removeIf(r -> {
                String nombre = (r.getNombreEstudiante() == null) ? "" : r.getNombreEstudiante().toLowerCase(Locale.ROOT);
                String aula = (r.getAula() == null) ? "" : r.getAula().toLowerCase(Locale.ROOT);
                String juego = (r.getJuego() == null || r.getJuego().getNombre() == null) ? "" : r.getJuego().getNombre().toLowerCase(Locale.ROOT);
                String id = (r.getIdEstudiante() == null) ? "" : String.valueOf(r.getIdEstudiante());

                return !(nombre.contains(q) || aula.contains(q) || juego.contains(q) || id.contains(q));
            });
        }

        // ----- 6) Orden -----
        String ordenSel = atajoOrdenarPorPuntajeDesc ? "Puntaje (mayor)" : (String) cbOrden.getSelectedItem();
        if (ordenSel == null) ordenSel = "Fecha (más reciente)";

        Comparator<SesionJuego> comp;
        switch (ordenSel) {
            case "Fecha (más antigua)" -> comp = Comparator.comparing(
                    SesionJuego::getFechaHora,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "Puntaje (menor)" -> comp = Comparator.comparingInt(SesionJuego::getPuntaje);
            case "Puntaje (mayor)" -> comp = Comparator.comparingInt(SesionJuego::getPuntaje).reversed();
            default -> comp = Comparator.comparing(
                    SesionJuego::getFechaHora,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
        }
        lista.sort(comp);
        
        actualizarKpis(lista);

        // ----- 7) Pintar tabla -----
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (SesionJuego r : lista) {
            String nombreJuego = (r.getJuego() == null) ? "" : r.getJuego().getNombre();

            LocalDateTime fh = r.getFechaHora();
            String fecha = "";
            String hora = "";
            if (fh != null) {
                fecha = fh.toLocalDate().format(fmtFecha);
                hora = fh.toLocalTime().truncatedTo(ChronoUnit.SECONDS).format(fmtHora);
            }

            tablaModelo.addRow(new Object[]{
                    r.getIdEstudiante(),
                    r.getNombreEstudiante(),
                    r.getAula(),
                    nombreJuego,
                    r.getDificultad(),
                    r.getPuntaje(),
                    fecha,
                    hora
            });
        }
    }
    
    private void actualizarKpis(List<SesionJuego> lista) {
        if (lblKpiPartidas == null) return;

        int total = lista.size();
        int suma = 0;
        SesionJuego mejor = null;

        Map<String, Integer> conteoAula = new HashMap<>();

        for (SesionJuego r : lista) {
            suma += r.getPuntaje();

            if (mejor == null || r.getPuntaje() > mejor.getPuntaje()) {
                mejor = r;
            }

            String aula = (r.getAula() == null || r.getAula().isBlank()) ? "Sin aula" : r.getAula();
            conteoAula.put(aula, conteoAula.getOrDefault(aula, 0) + 1);
        }
        
        // Guardar "mejor" para el click
        if (mejor != null) {
            ultimoIdMejor = mejor.getIdEstudiante();
            ultimoNombreMejor = mejor.getNombreEstudiante();
        } else {
            ultimoIdMejor = null;
            ultimoNombreMejor = null;
        }

        int promedio = (total == 0) ? 0 : (suma / total);

        String mejorTxt = (mejor == null)
                ? "-"
                : mejor.getPuntaje() + " • " + (mejor.getNombreEstudiante() == null ? "" : mejor.getNombreEstudiante());

        String aulaActiva = "-";
        ultimaAulaActiva = null;

        int max = -1;
        for (Map.Entry<String, Integer> e : conteoAula.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                ultimaAulaActiva = e.getKey(); // <-- clave para el click
                aulaActiva = e.getKey() + " (" + e.getValue() + ")";
            }
        }

        lblKpiPartidas.setText(kpiHtml("Partidas", String.valueOf(total)));
        lblKpiPromedio.setText(kpiHtml("Promedio", String.valueOf(promedio)));
        lblKpiMejor.setText(kpiHtml("Mejor", mejorTxt));
        lblKpiAulaActiva.setText(kpiHtml("Aula activa", aulaActiva));
    }

    private String kpiHtml(String titulo, String valor) {
        return "<html><div style='text-align:center;'>" +
                "<div style='font-size:12px;'>" + titulo + "</div>" +
                "<div style='font-size:18px; font-weight:bold;'>" + valor + "</div>" +
                "</div></html>";
    }
}
