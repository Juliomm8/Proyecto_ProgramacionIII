package com.jasgames.ui;

import com.jasgames.model.Juego;
import com.jasgames.model.ObjetivoPIA;
import com.jasgames.model.PIA;
import com.jasgames.model.SesionJuego;
import com.jasgames.service.AulaService;
import com.jasgames.service.PerfilService;
import com.jasgames.service.PiaService;
import com.jasgames.service.SesionService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class DashboardPanel extends JPanel {

    /**
     * Permite que el Dashboard navegue hacia la pestaña "Perfiles" para revisar/editar el objetivo del PIA.
     */
    public interface ObjectiveNavigator {
        void goToObjective(int idNino, String idObjetivo);
    }

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
    private final SesionService sesionService;
    private final PiaService piaService;
    private final AulaService aulaServiceField;

    private final ObjectiveNavigator navigator;

    private final java.util.List<SesionJuego> filasTabla = new java.util.ArrayList<>();
    private DefaultTableModel tablaModelo;

    // --- Filtros nuevos  ---
    private JComboBox<String> cbFiltroAula;
    private JComboBox<Object> cbFiltroDificultad; // "Todas" + Integer
    private JComboBox<String> cbFiltroRango;
    private JComboBox<String> cbOrden;
    private JCheckBox chkSoloPia;
    private JTextField txtBuscar;
    private JButton btnLimpiar;
    private JButton btnEliminarSesion;

    // Menú contextual (click derecho) sobre la tabla
    private JPopupMenu menuTabla;
    
    // KPIs
    private JPanel panelKpis;
    private JLabel lblKpiPartidas;
    private JLabel lblKpiPromedio;
    private JLabel lblKpiMejor;
    private JLabel lblKpiAulaActiva;
    private JLabel lblKpiPia;
    
    private JPanel cardKpiAulaActiva;
    private String ultimaAulaActiva = null;
    
    private JPanel cardKpiMejor;
    private Integer ultimoIdMejor = null;
    private String ultimoNombreMejor = null;
    
    private JPanel cardKpiPia;

    // --- Reporte PIA (Paso 4.9) ---
    private JTabbedPane tabsCentro;
    private JPanel panelReportePia;
    private JComboBox<PIA> cbReportePia;
    private JCheckBox chkIncluirPiasCerrados;
    private JSpinner spUltimasNSesiones;
    private JTable tblReporteObjetivos;
    private JTable tblReporteSesiones;
    private DefaultTableModel modeloReporteObjetivos;
    private DefaultTableModel modeloReporteSesiones;
    private JLabel lblReporteResumen;
    private JButton btnReporteMarcarActivo;
    private JButton btnReporteIrAObjetivo;
    private JButton btnReporteRefrescar;
    private JButton btnReporteRecalcular;

    private static final String[] AULAS_PREDEFINIDAS = {
            "Aula Azul", "Aula Roja", "Aula Verde", "Aula Amarilla", "Aula Morada"
    };

    // Constructor principal: lo usará DocenteWindow
    public DashboardPanel(SesionService sesionService, PiaService piaService, AulaService aulaService, ObjectiveNavigator navigator) {
        this.sesionService = sesionService;
        this.piaService = piaService;
        this.aulaServiceField = aulaService;
        this.navigator = navigator;

        setLayout(new BorderLayout());
        add(panelDashboard, BorderLayout.CENTER);

        inicializarTabla();
        construirFiltrosExtra();
        construirReportePia();
        construirKpis();
        recargarCombosFiltros(true);
        inicializarListeners();

        // Mostrar algo al abrir
        actualizarTabla(false);

        // Cargar datos del reporte PIA al abrir
        refrescarReportePia();
    }

    // Back-compat
    public DashboardPanel(SesionService sesionService, PiaService piaService, AulaService aulaService) {
        this(sesionService, piaService, aulaService, null);
    }

    public DashboardPanel(SesionService sesionService, PiaService piaService) {
        this(sesionService, piaService, null, null);
    }

    public DashboardPanel() {
        this(new SesionService(), new PiaService(), new AulaService(new PerfilService()), null);
    }

    private void inicializarTabla() {
        tablaModelo = new DefaultTableModel(
                new Object[]{"ID", "Estudiante", "Aula", "Juego", "Dificultad", "Puntaje", "Intentos", "Errores", "Pistas", "Duración(s)", "Precisión", "PIA", "Objetivo", "Fecha", "Hora"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblResultados.setModel(tablaModelo);
        tblResultados.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblResultados.setFillsViewportHeight(true);
        tblResultados.setRowHeight(22);
    }

    private void construirFiltrosExtra() {
        // Creamos componentes
        cbFiltroAula = new JComboBox<>();
        cbFiltroDificultad = new JComboBox<>();
        cbFiltroRango = new JComboBox<>(new String[]{"Todo", "Hoy", "Últimos 7 días", "Últimos 30 días"});
        cbOrden = new JComboBox<>(new String[]{"Fecha (más reciente)", "Fecha (más antigua)", "Puntaje (mayor)", "Puntaje (menor)"});
        txtBuscar = new JTextField(14);
        btnLimpiar = new JButton("Limpiar");
        btnEliminarSesion = new JButton("Eliminar sesión");
        btnEliminarSesion.setEnabled(false);
        chkSoloPia = new JCheckBox("Solo PIA");

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
        panelFiltrosDashboard.add(chkSoloPia);

        panelFiltrosDashboard.add(new JLabel("Buscar:"));
        panelFiltrosDashboard.add(txtBuscar);

        panelFiltrosDashboard.add(new JLabel("Orden:"));
        panelFiltrosDashboard.add(cbOrden);

        panelFiltrosDashboard.add(btnActualizarDashboard);
        panelFiltrosDashboard.add(btnOrdenarPorPuntaje);
        panelFiltrosDashboard.add(btnLimpiar);
        panelFiltrosDashboard.add(btnEliminarSesion);

        panelFiltrosDashboard.revalidate();
        panelFiltrosDashboard.repaint();
    }
    
    private void construirKpis() {
        panelKpis = new JPanel(new GridLayout(1, 5, 12, 12));
        panelKpis.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblKpiPartidas = crearKpi("Partidas", "0");
        lblKpiPromedio = crearKpi("Promedio", "0");
        lblKpiMejor = crearKpi("Mejor", "-");
        lblKpiAulaActiva = crearKpi("Aula activa", "-");
        lblKpiPia = crearKpi("PIA", "—");

        panelKpis.add(wrapKpi(lblKpiPartidas));
        panelKpis.add(wrapKpi(lblKpiPromedio));

        cardKpiMejor = wrapKpi(lblKpiMejor);
        panelKpis.add(cardKpiMejor);

        cardKpiAulaActiva = wrapKpi(lblKpiAulaActiva);
        panelKpis.add(cardKpiAulaActiva);
        
        cardKpiPia = wrapKpi(lblKpiPia);
        panelKpis.add(cardKpiPia);

        habilitarClickMejor();
        habilitarClickAulaActiva();

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(panelKpis, BorderLayout.NORTH);

        // Tabs: Sesiones (tabla actual) + Reporte PIA
        tabsCentro = new JTabbedPane();
        tabsCentro.addTab("Sesiones", scrollResultados);
        if (panelReportePia != null) {
            tabsCentro.addTab("Reporte PIA", panelReportePia);
        }
        centro.add(tabsCentro, BorderLayout.CENTER);

        if (panelDashboard != null) {
            panelDashboard.remove(scrollResultados);
            panelDashboard.add(centro, BorderLayout.CENTER);
            panelDashboard.revalidate();
            panelDashboard.repaint();
        }
    }

    // ----------------------------------------------------------------------
    // Paso 4.9 — Reporte por Objetivo PIA (para docentes) — dentro del Dashboard
    // ----------------------------------------------------------------------

    private void construirReportePia() {
        panelReportePia = new JPanel(new BorderLayout(10, 10));
        panelReportePia.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Header / controles ---
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        cbReportePia = new JComboBox<>();
        cbReportePia.setPreferredSize(new Dimension(320, 26));
        cbReportePia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Seleccione un PIA…");
                    return this;
                }
                if (value instanceof PIA) {
                    PIA p = (PIA) value;
                    String estado = p.isActivo() ? "Activo" : "Cerrado";
                    setText("" + p.getIdNino() + " — " + safe(p.getNombreNino()) + " (" + safe(p.getAula()) + ") · " + estado);
                }
                return this;
            }
        });

        chkIncluirPiasCerrados = new JCheckBox("Incluir PIAs cerrados");
        chkIncluirPiasCerrados.setFocusable(false);

        spUltimasNSesiones = new JSpinner(new SpinnerNumberModel(10, 3, 50, 1));
        JLabel lblN = new JLabel("Últimas sesiones:");

        btnReporteRefrescar = new JButton("Refrescar");
        btnReporteRecalcular = new JButton("Recalcular progreso");
        btnReporteMarcarActivo = new JButton("Marcar como activo");
        btnReporteIrAObjetivo = new JButton("Ir al objetivo");

        btnReporteMarcarActivo.setEnabled(false);
        btnReporteIrAObjetivo.setEnabled(false);

        top.add(new JLabel("PIA:"));
        top.add(cbReportePia);
        top.add(chkIncluirPiasCerrados);
        top.add(lblN);
        top.add(spUltimasNSesiones);
        top.add(btnReporteRefrescar);
        top.add(btnReporteRecalcular);
        top.add(btnReporteMarcarActivo);
        top.add(btnReporteIrAObjetivo);

        panelReportePia.add(top, BorderLayout.NORTH);

        // --- Tabla de objetivos ---
        modeloReporteObjetivos = new DefaultTableModel(
                new Object[]{"ID_OBJ", "Juego", "Descripción", "ProgRondas", "MetaRondas", "ProgSes", "MetaSes", "Estado", "Precisión", "Errores prom.", "Duración prom.(s)", "Última"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblReporteObjetivos = new JTable(modeloReporteObjetivos);
        tblReporteObjetivos.setFillsViewportHeight(true);
        tblReporteObjetivos.setRowHeight(22);
        tblReporteObjetivos.setAutoCreateRowSorter(true);

        // ocultar columna ID_OBJ
        tblReporteObjetivos.getColumnModel().getColumn(0).setMinWidth(0);
        tblReporteObjetivos.getColumnModel().getColumn(0).setMaxWidth(0);
        tblReporteObjetivos.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane spObj = new JScrollPane(tblReporteObjetivos);

        // --- Tabla de últimas sesiones por objetivo ---
        modeloReporteSesiones = new DefaultTableModel(
                new Object[]{"Fecha", "Puntaje", "Dificultad", "Intentos", "Errores", "Pistas", "Dur(s)", "Precisión", "ID_SES"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblReporteSesiones = new JTable(modeloReporteSesiones);
        tblReporteSesiones.setFillsViewportHeight(true);
        tblReporteSesiones.setRowHeight(22);
        tblReporteSesiones.setAutoCreateRowSorter(true);

        // ocultar columna ID_SES
        tblReporteSesiones.getColumnModel().getColumn(8).setMinWidth(0);
        tblReporteSesiones.getColumnModel().getColumn(8).setMaxWidth(0);
        tblReporteSesiones.getColumnModel().getColumn(8).setWidth(0);

        JScrollPane spSes = new JScrollPane(tblReporteSesiones);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spObj, spSes);
        split.setResizeWeight(0.55);
        panelReportePia.add(split, BorderLayout.CENTER);

        // --- Resumen inferior ---
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        lblReporteResumen = new JLabel("Seleccione un PIA para ver objetivos y progreso.");
        bottom.add(lblReporteResumen, BorderLayout.CENTER);
        panelReportePia.add(bottom, BorderLayout.SOUTH);

        // --- listeners ---
        btnReporteRefrescar.addActionListener(e -> refrescarReportePia());
        btnReporteRecalcular.addActionListener(e -> recalcularPiaSeleccionado());
        chkIncluirPiasCerrados.addActionListener(e -> refrescarReportePia());

        cbReportePia.addActionListener(e -> {
            cargarObjetivosDelPiaSeleccionado();
            actualizarDetalleObjetivoSeleccionado();
        });

        spUltimasNSesiones.addChangeListener(e -> actualizarDetalleObjetivoSeleccionado());

        tblReporteObjetivos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            actualizarDetalleObjetivoSeleccionado();
        });

        btnReporteMarcarActivo.addActionListener(e -> marcarObjetivoComoActivo());
        btnReporteIrAObjetivo.addActionListener(e -> irAObjetivoEnPerfiles());
    }

    private void refrescarReportePia() {
        if (cbReportePia == null) return;

        PIA sel = (PIA) cbReportePia.getSelectedItem();

        cbReportePia.removeAllItems();
        cbReportePia.addItem(null);

        boolean incluirCerrados = chkIncluirPiasCerrados != null && chkIncluirPiasCerrados.isSelected();
        List<PIA> pias = piaService.obtenerTodos();

        // Orden estable (por nombre, luego id)
        pias.sort(Comparator
                .comparing((PIA p) -> safe(p.getNombreNino()).toLowerCase(Locale.ROOT))
                .thenComparingInt(PIA::getIdNino)
                .thenComparing(PIA::getIdPia, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        for (PIA p : pias) {
            if (p == null) continue;
            if (!incluirCerrados && !p.isActivo()) continue;
            cbReportePia.addItem(p);
        }

        // restaurar selección si aún existe
        if (sel != null) {
            for (int i = 0; i < cbReportePia.getItemCount(); i++) {
                PIA it = (PIA) cbReportePia.getItemAt(i);
                if (it != null && Objects.equals(it.getIdPia(), sel.getIdPia())) {
                    cbReportePia.setSelectedIndex(i);
                    break;
                }
            }
        }

        cargarObjetivosDelPiaSeleccionado();
        actualizarDetalleObjetivoSeleccionado();
    }

    private void cargarObjetivosDelPiaSeleccionado() {
        if (modeloReporteObjetivos == null) return;
        modeloReporteObjetivos.setRowCount(0);
        modeloReporteSesiones.setRowCount(0);

        PIA pia = (PIA) cbReportePia.getSelectedItem();
        if (pia == null) {
            lblReporteResumen.setText("Seleccione un PIA para ver objetivos y progreso.");
            btnReporteMarcarActivo.setEnabled(false);
            btnReporteIrAObjetivo.setEnabled(false);
            return;
        }

        // Tomamos todas las sesiones una sola vez
        List<SesionJuego> sesiones = sesionService.obtenerTodos();

        for (ObjetivoPIA o : pia.getObjetivos()) {
            if (o == null) continue;

            StatsObjetivo st = calcularStatsObjetivo(pia, o, sesiones);

            modeloReporteObjetivos.addRow(new Object[]{
                    o.getIdObjetivo(),
                    o.getJuegoId(),
                    safe(o.getDescripcion()),
                    o.getProgresoRondasCorrectas(),
                    o.getMetaRondasCorrectas(),
                    o.getProgresoSesionesCompletadas(),
                    o.getMetaSesionesCompletadas(),
                    o.isCompletado() ? "✅" : "⏳",
                    st.precisionTxt,
                    st.erroresPromTxt,
                    st.duracionPromTxt,
                    st.ultimaTxt
            });
        }

        if (modeloReporteObjetivos.getRowCount() == 0) {
            lblReporteResumen.setText("Este PIA no tiene objetivos todavía.");
        } else {
            lblReporteResumen.setText("Seleccione un objetivo para ver detalle y últimas sesiones.");
        }

        // Auto seleccionar la primera fila para que el docente vea algo
        if (tblReporteObjetivos.getRowCount() > 0) {
            tblReporteObjetivos.setRowSelectionInterval(0, 0);
        }
    }

    private void actualizarDetalleObjetivoSeleccionado() {
        if (modeloReporteSesiones == null) return;
        modeloReporteSesiones.setRowCount(0);

        PIA pia = (PIA) cbReportePia.getSelectedItem();
        if (pia == null) {
            btnReporteMarcarActivo.setEnabled(false);
            btnReporteIrAObjetivo.setEnabled(false);
            return;
        }

        int viewRow = tblReporteObjetivos.getSelectedRow();
        if (viewRow < 0) {
            btnReporteMarcarActivo.setEnabled(false);
            btnReporteIrAObjetivo.setEnabled(false);
            return;
        }
        int row = tblReporteObjetivos.convertRowIndexToModel(viewRow);
        String idObj = (String) modeloReporteObjetivos.getValueAt(row, 0);
        ObjetivoPIA obj = pia.getObjetivoPorId(idObj);
        if (obj == null) {
            btnReporteMarcarActivo.setEnabled(false);
            btnReporteIrAObjetivo.setEnabled(false);
            return;
        }

        int limite = 10;
        try {
            Object v = spUltimasNSesiones.getValue();
            if (v instanceof Integer) limite = (Integer) v;
        } catch (Exception ignored) {}
        if (limite <= 0) limite = 10;

        List<SesionJuego> sesiones = sesionesParaObjetivo(pia, obj, sesionService.obtenerTodos());
        sesiones.sort((a, b) -> {
            LocalDateTime fa = (a.getFechaFin() != null) ? a.getFechaFin() : a.getFechaHora();
            LocalDateTime fb = (b.getFechaFin() != null) ? b.getFechaFin() : b.getFechaHora();
            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int count = 0;
        for (SesionJuego s : sesiones) {
            if (count >= limite) break;
            count++;

            LocalDateTime fh = (s.getFechaFin() != null) ? s.getFechaFin() : s.getFechaHora();
            String fecha = (fh == null) ? "—" : fh.format(fmt);

            int intentos = s.getIntentosTotales();
            String prec = "—";
            if (intentos > 0) {
                double p = (double) s.getAciertosTotales() / (double) intentos;
                prec = String.format(Locale.US, "%.0f%%", p * 100.0);
            }

            String dur = "—";
            if (s.getDuracionMs() > 0) dur = String.format(Locale.US, "%.1f", s.getDuracionMs() / 1000.0);

            modeloReporteSesiones.addRow(new Object[]{
                    fecha,
                    s.getPuntaje(),
                    s.getDificultad(),
                    s.getIntentosTotales(),
                    s.getErroresTotales(),
                    s.getPistasUsadas(),
                    dur,
                    prec,
                    safe(s.getIdSesion())
            });
        }

        // Botones
        boolean puedeMarcar = pia.isActivo() && !obj.isCompletado();
        btnReporteMarcarActivo.setEnabled(puedeMarcar);
        btnReporteIrAObjetivo.setEnabled(navigator != null);

        // Resumen
        StatsObjetivo st = calcularStatsObjetivo(pia, obj, sesionService.obtenerTodos());
        String activoTxt = (Objects.equals(pia.getIdObjetivoActivo(), obj.getIdObjetivo())) ? " (activo)" : "";
        lblReporteResumen.setText(
                "Objetivo: Juego " + obj.getJuegoId() + " — " + safe(obj.getDescripcion()) + activoTxt +
                        " | Progreso: " + obj.getProgresoRondasCorrectas() + "/" + obj.getMetaRondasCorrectas() + " rondas, " +
                        obj.getProgresoSesionesCompletadas() + "/" + obj.getMetaSesionesCompletadas() + " sesiones" +
                        " | Precisión: " + st.precisionTxt + " | Errores prom.: " + st.erroresPromTxt + " | Duración prom.: " + st.duracionPromTxt
        );
    }

    private void marcarObjetivoComoActivo() {
        PIA pia = (PIA) cbReportePia.getSelectedItem();
        if (pia == null) return;

        int viewRow = tblReporteObjetivos.getSelectedRow();
        if (viewRow < 0) return;
        int row = tblReporteObjetivos.convertRowIndexToModel(viewRow);
        String idObj = (String) modeloReporteObjetivos.getValueAt(row, 0);
        if (idObj == null) return;

        boolean ok = piaService.setObjetivoActivo(pia.getIdPia(), idObj);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo marcar como activo (puede estar completado o no existir).", "PIA", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refrescarReportePia();
    }

    private void irAObjetivoEnPerfiles() {
        if (navigator == null) {
            JOptionPane.showMessageDialog(this, "Esta acción no está disponible en esta pantalla.", "PIA", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PIA pia = (PIA) cbReportePia.getSelectedItem();
        if (pia == null) return;
        int viewRow = tblReporteObjetivos.getSelectedRow();
        if (viewRow < 0) return;
        int row = tblReporteObjetivos.convertRowIndexToModel(viewRow);
        String idObj = (String) modeloReporteObjetivos.getValueAt(row, 0);
        if (idObj == null) return;

        navigator.goToObjective(pia.getIdNino(), idObj);
    }


    private void recalcularPiaSeleccionado() {
    PIA pia = (PIA) cbReportePia.getSelectedItem();
    if (pia == null) {
        JOptionPane.showMessageDialog(this, "Seleccione un PIA primero.", "PIA", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    int ok = JOptionPane.showConfirmDialog(
            this,
            "Recalculará el progreso del PIA desde las sesiones guardadas.\n" +
                    "Útil si se eliminaron sesiones o se corrigieron datos.\n\n" +
                    "¿Desea continuar?",
            "Recalcular progreso PIA",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
    );
    if (ok != JOptionPane.YES_OPTION) return;

    boolean done = piaService.recalcularProgresoPIA(pia.getIdPia(), sesionService.obtenerTodos());
    if (!done) {
        JOptionPane.showMessageDialog(this, "No se pudo recalcular (PIA no encontrado).", "PIA", JOptionPane.ERROR_MESSAGE);
        return;
    }

    refrescarReportePia();
    JOptionPane.showMessageDialog(this, "Progreso recalculado correctamente.", "PIA", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class StatsObjetivo {
        String precisionTxt = "—";
        String erroresPromTxt = "—";
        String duracionPromTxt = "—";
        String ultimaTxt = "—";
    }

    private StatsObjetivo calcularStatsObjetivo(PIA pia, ObjetivoPIA obj, List<SesionJuego> todas) {
        StatsObjetivo st = new StatsObjetivo();
        if (pia == null || obj == null || todas == null) return st;

        List<SesionJuego> sesiones = sesionesParaObjetivo(pia, obj, todas);
        if (sesiones.isEmpty()) return st;

        int n = 0;
        int sumErrores = 0;
        long sumDur = 0L;
        int cntDur = 0;
        int sumAciertos = 0;
        int sumIntentos = 0;
        LocalDateTime ultima = null;

        for (SesionJuego s : sesiones) {
            if (s == null) continue;
            n++;
            sumErrores += s.getErroresTotales();
            if (s.getDuracionMs() > 0) {
                sumDur += s.getDuracionMs();
                cntDur++;
            }
            sumAciertos += s.getAciertosTotales();
            sumIntentos += s.getIntentosTotales();

            LocalDateTime fh = (s.getFechaFin() != null) ? s.getFechaFin() : s.getFechaHora();
            if (fh != null && (ultima == null || fh.isAfter(ultima))) ultima = fh;
        }

        if (sumIntentos > 0) {
            double p = (double) sumAciertos / (double) sumIntentos;
            st.precisionTxt = String.format(Locale.US, "%.0f%%", p * 100.0);
        }

        if (n > 0) {
            double e = (double) sumErrores / (double) n;
            st.erroresPromTxt = String.format(Locale.US, "%.1f", e);
        }

        if (cntDur > 0) {
            double d = (sumDur / 1000.0) / (double) cntDur;
            st.duracionPromTxt = String.format(Locale.US, "%.1f s", d);
        }

        if (ultima != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            st.ultimaTxt = ultima.toLocalDate().format(fmt);
        }

        return st;
    }

    private List<SesionJuego> sesionesParaObjetivo(PIA pia, ObjetivoPIA obj, List<SesionJuego> todas) {
        if (pia == null || obj == null || todas == null) return new ArrayList<>();
        String idPia = pia.getIdPia();
        String idObj = obj.getIdObjetivo();
        List<SesionJuego> out = new ArrayList<>();
        for (SesionJuego s : todas) {
            if (s == null) continue;
            if (idPia == null || idObj == null) continue;
            if (!Objects.equals(idPia, s.getIdPia())) continue;
            if (!Objects.equals(idObj, s.getIdObjetivoPia())) continue;
            out.add(s);
        }
        return out;
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

                actualizarTabla(false);
            }
        };

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

    private void seleccionarJuegoEnFiltro(int idJuego) {
        if (cbFiltroJuego == null || idJuego <= 0) return;

        for (int i = 0; i < cbFiltroJuego.getItemCount(); i++) {
            Object it = cbFiltroJuego.getItemAt(i);
            if (it instanceof Juego && ((Juego) it).getId() == idJuego) {
                cbFiltroJuego.setSelectedIndex(i);
                return;
            }
        }
    }

    private void inicializarListeners() {
        btnActualizarDashboard.addActionListener(e -> {
            recargarCombosFiltros(true);
            actualizarTabla(false);
        });

        btnOrdenarPorPuntaje.addActionListener(e -> {
            recargarCombosFiltros(true);
            actualizarTabla(true);
        });

        btnLimpiar.addActionListener(e -> limpiarFiltros());
        if (btnEliminarSesion != null) {
            btnEliminarSesion.addActionListener(e -> eliminarSesionSeleccionada());
        }

        // Aplicar en vivo (sin apretar "Aplicar")
        cbFiltroJuego.addActionListener(e -> actualizarTabla(false));
        cbFiltroAula.addActionListener(e -> actualizarTabla(false));
        cbFiltroDificultad.addActionListener(e -> actualizarTabla(false));
        cbFiltroRango.addActionListener(e -> actualizarTabla(false));
        chkSoloPia.addActionListener(e -> actualizarTabla(false));
        cbOrden.addActionListener(e -> actualizarTabla(false));

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { actualizarTabla(false); }
            @Override public void removeUpdate(DocumentEvent e) { actualizarTabla(false); }
            @Override public void changedUpdate(DocumentEvent e) { actualizarTabla(false); }
        });

        instalarMenuContextualTabla();

        // Habilitar/deshabilitar botón eliminar según selección
        tblResultados.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            if (btnEliminarSesion != null) {
                btnEliminarSesion.setEnabled(tblResultados.getSelectedRow() >= 0);
            }
        });
    }

    private void instalarMenuContextualTabla() {
        if (tblResultados == null) return;
        if (menuTabla != null) return; // evitar duplicados

        menuTabla = new JPopupMenu();

        JMenuItem miVer = new JMenuItem("Ver detalle…");
        JMenuItem miFiltrarEst = new JMenuItem("Filtrar por este estudiante");
        JMenuItem miFiltrarJuego = new JMenuItem("Filtrar por este juego");
        JMenuItem miFiltrarAula = new JMenuItem("Filtrar por esta aula");
        JMenuItem miSoloPia = new JMenuItem("Ver solo sesiones con PIA");
        JMenuItem miCopiar = new JMenuItem("Copiar resumen");
        JMenuItem miCopiarId = new JMenuItem("Copiar ID de sesión");
        JMenuItem miEliminar = new JMenuItem("Eliminar sesión…");

        miVer.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s != null) mostrarDetalleSesion(s);
        });

        miFiltrarEst.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s == null) return;
            if (txtBuscar != null) {
                if (s.getIdEstudiante() != null) txtBuscar.setText(String.valueOf(s.getIdEstudiante()));
                else txtBuscar.setText(safe(s.getNombreEstudiante()));
            }
            actualizarTabla(false);
        });

        miFiltrarJuego.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s == null || s.getJuego() == null) return;
            seleccionarJuegoEnFiltro(s.getJuego().getId());
            actualizarTabla(false);
        });

        miFiltrarAula.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s == null) return;
            seleccionarAulaEnFiltro(s.getAula());
            actualizarTabla(false);
        });

        miSoloPia.addActionListener(e -> {
            if (chkSoloPia != null) chkSoloPia.setSelected(true);
            actualizarTabla(false);
        });

        miCopiar.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s == null) return;
            copiarAlPortapapeles(resumenSesion(s));
        });

        miCopiarId.addActionListener(e -> {
            SesionJuego s = getSesionSeleccionada();
            if (s == null) return;
            copiarAlPortapapeles(s.getIdSesion() == null ? "" : s.getIdSesion());
        });

        miEliminar.addActionListener(e -> eliminarSesionSeleccionada());

        menuTabla.add(miVer);
        menuTabla.addSeparator();
        menuTabla.add(miFiltrarEst);
        menuTabla.add(miFiltrarJuego);
        menuTabla.add(miFiltrarAula);
        menuTabla.add(miSoloPia);
        menuTabla.addSeparator();
        menuTabla.add(miCopiar);
        menuTabla.add(miCopiarId);
        menuTabla.addSeparator();
        menuTabla.add(miEliminar);

        tblResultados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = tblResultados.rowAtPoint(e.getPoint());
                    if (viewRow < 0) return;
                    int modelRow = tblResultados.convertRowIndexToModel(viewRow);
                    if (modelRow >= 0 && modelRow < filasTabla.size()) {
                        mostrarDetalleSesion(filasTabla.get(modelRow));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }

            @Override
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                int viewRow = tblResultados.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    tblResultados.setRowSelectionInterval(viewRow, viewRow);
                } else {
                    tblResultados.clearSelection();
                }

                SesionJuego s = getSesionSeleccionada();
                boolean hay = s != null;
                miVer.setEnabled(hay);
                miFiltrarEst.setEnabled(hay);
                miFiltrarJuego.setEnabled(hay && s.getJuego() != null);
                miFiltrarAula.setEnabled(hay && s.getAula() != null && !s.getAula().isBlank());
                miCopiar.setEnabled(hay);
                miCopiarId.setEnabled(hay && s.getIdSesion() != null && !s.getIdSesion().isBlank());
                miEliminar.setEnabled(hay);
                miSoloPia.setEnabled(chkSoloPia != null && !chkSoloPia.isSelected());

                menuTabla.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private SesionJuego getSesionSeleccionada() {
        int viewRow = tblResultados.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = tblResultados.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filasTabla.size()) return null;
        return filasTabla.get(modelRow);
    }

    private void copiarAlPortapapeles(String texto) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto == null ? "" : texto), null);
        } catch (Exception ignored) {
            // En algunos entornos el clipboard puede fallar; no es crítico.
        }
    }

    private String resumenSesion(SesionJuego s) {
        if (s == null) return "";
        String juego = (s.getJuego() != null) ? safe(s.getJuego().getNombre()) : "—";
        String fecha = (s.getFechaFin() != null) ? s.getFechaFin().toLocalDate().toString() : (s.getFechaHora() != null ? s.getFechaHora().toLocalDate().toString() : "—");
        String durTxt = (s.getDuracionMs() > 0) ? String.format(Locale.US, "%.1fs", s.getDuracionMs() / 1000.0) : "—";
        String piaTxt = (s.getIdPia() != null && !s.getIdPia().isBlank()) ? "Sí" : "—";

        return "Sesión: " + safe(s.getIdSesion()) + "\n" +
                "Estudiante: " + safe(s.getNombreEstudiante()) + " (ID: " + s.getIdEstudiante() + ")\n" +
                "Aula: " + safe(s.getAula()) + "\n" +
                "Juego: " + juego + " | Dificultad: " + s.getDificultad() + "\n" +
                "Puntaje: " + s.getPuntaje() + " | Intentos: " + s.getIntentosTotales() + " | Errores: " + s.getErroresTotales() + "\n" +
                "Duración: " + durTxt + " | PIA: " + piaTxt + "\n" +
                "Fecha: " + fecha;
    }

    private void eliminarSesionSeleccionada() {
        int viewRow = tblResultados.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = tblResultados.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filasTabla.size()) return;

        SesionJuego s = filasTabla.get(modelRow);
        if (s == null || s.getIdSesion() == null || s.getIdSesion().isBlank()) {
            JOptionPane.showMessageDialog(this, "No se pudo identificar la sesión seleccionada.", "Eliminar sesión", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = (s.getNombreEstudiante() != null) ? s.getNombreEstudiante() : "(sin nombre)";
        String juego = (s.getJuego() != null) ? s.getJuego().toString() : "(sin juego)";

        int ok = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar esta sesión?\n\n" +
                        "Estudiante: " + nombre + "\n" +
                        "Juego: " + juego + "\n" +
                        "Fecha: " + (s.getFechaFin() != null ? s.getFechaFin().toLocalDate().toString() : "-") + "\n\n" +
                        "Esta acción no se puede deshacer.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (ok != JOptionPane.YES_OPTION) return;

        boolean removed = sesionService.eliminarSesion(s.getIdSesion());
        // Mantener consistencia del PIA si esta sesión aportaba a un objetivo
        String idPiaAfectado = (s.getIdPia() != null) ? s.getIdPia().trim() : null;
        if (removed && idPiaAfectado != null && !idPiaAfectado.isBlank()) {
            piaService.recalcularProgresoPIA(idPiaAfectado, sesionService.obtenerTodos());
        }
        if (!removed) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar la sesión (puede que ya no exista).", "Eliminar sesión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Refrescar tabla y filtros
        recargarCombosFiltros(true);
        actualizarTabla(false);

        // Mantener el reporte PIA sincronizado
        refrescarReportePia();
    }

    private void limpiarFiltros() {
        cbFiltroJuego.setSelectedItem("Todos");
        cbFiltroAula.setSelectedItem("Todas");
        cbFiltroDificultad.setSelectedItem("Todas");
        cbFiltroRango.setSelectedItem("Todo");
        cbOrden.setSelectedItem("Fecha (más reciente)");
        txtBuscar.setText("");
        if (chkSoloPia != null) chkSoloPia.setSelected(false);
        actualizarTabla(false);
    }

    private void recargarCombosFiltros(boolean mantenerSelecciones) {
        Object juegoSelObj = mantenerSelecciones ? cbFiltroJuego.getSelectedItem() : "Todos";
        Integer juegoSelId = (juegoSelObj instanceof Juego) ? ((Juego) juegoSelObj).getId() : null;
        String aulaSel = mantenerSelecciones ? (String) cbFiltroAula.getSelectedItem() : "Todas";
        Object difSel = mantenerSelecciones ? cbFiltroDificultad.getSelectedItem() : "Todas";

        // ---- Juegos ----
        cbFiltroJuego.removeAllItems();
        cbFiltroJuego.addItem("Todos");

        Map<Integer, Juego> juegosPorId = new LinkedHashMap<>();
        for (SesionJuego r : sesionService.obtenerTodos()) {
            Juego j = r.getJuego();
            if (j != null) juegosPorId.putIfAbsent(j.getId(), j);
        }
        for (Juego j : juegosPorId.values()) cbFiltroJuego.addItem(j);

        // ---- Aulas ----
        cbFiltroAula.removeAllItems();
        cbFiltroAula.addItem("Todas");

        Set<String> aulasBase = new LinkedHashSet<>();
        if (aulaServiceField != null) {
            // Asegura que el combo muestre el catálogo más reciente (si se modificó desde otra pestaña)
            try {
                aulaServiceField.refrescarDesdeDisco();
            } catch (Exception ignored) {
                // Si falla la recarga, usamos lo que ya tenga cargado.
            }
            aulasBase.addAll(aulaServiceField.obtenerNombres());
        } else {
            aulasBase.addAll(Arrays.asList(AULAS_PREDEFINIDAS));
        }
        for (String a : aulasBase) cbFiltroAula.addItem(a);

        Set<String> aulasExtras = new LinkedHashSet<>();
        for (SesionJuego r : sesionService.obtenerTodos()) {
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
        if (mantenerSelecciones) {
            if (juegoSelId == null) {
                if (juegoSelObj != null) cbFiltroJuego.setSelectedItem(juegoSelObj);
            } else {
                for (int i = 0; i < cbFiltroJuego.getItemCount(); i++) {
                    Object it = cbFiltroJuego.getItemAt(i);
                    if (it instanceof Juego && ((Juego) it).getId() == juegoSelId) {
                        cbFiltroJuego.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (aulaSel != null) cbFiltroAula.setSelectedItem(aulaSel);
            if (difSel != null) cbFiltroDificultad.setSelectedItem(difSel);
        }
    }

    private void actualizarTabla(boolean atajoOrdenarPorPuntajeDesc) {
        tablaModelo.setRowCount(0);
        filasTabla.clear();

        List<SesionJuego> lista = new ArrayList<>(sesionService.obtenerTodos());

        // ----- 1) Filtro: Juego -----
        Object juegoSel = cbFiltroJuego.getSelectedItem();
        if (juegoSel instanceof Juego) {
            Juego j = (Juego) juegoSel;
            lista.removeIf(r -> r.getJuego() == null || r.getJuego().getId() != j.getId());
        }

        // ----- 2) Filtro: Solo PIA -----
        if (chkSoloPia != null && chkSoloPia.isSelected()) {
            lista.removeIf(r -> r.getIdPia() == null || r.getIdPia().isBlank());
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
        actualizarKpiPia();

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

            // métricas
            int intentos = r.getIntentosTotales();
            int errores = r.getErroresTotales();
            int pistas = r.getPistasUsadas();
            double durS = r.getDuracionMs() <= 0 ? 0.0 : (r.getDuracionMs() / 1000.0);
            String durTxt = (durS <= 0) ? "—" : String.format(java.util.Locale.US, "%.1f", durS);

            String precTxt = "—";
            if (intentos > 0) {
                double prec = (double) r.getAciertosTotales() / (double) intentos;
                precTxt = String.format(java.util.Locale.US, "%.0f%%", prec * 100.0);
            }

            String piaTxt = (r.getIdPia() != null && !r.getIdPia().isBlank()) ? "Sí" : "—";
            String objTxt = obtenerTextoObjetivo(r);

            filasTabla.add(r);

            tablaModelo.addRow(new Object[]{
                    r.getIdEstudiante(),
                    r.getNombreEstudiante(),
                    r.getAula(),
                    nombreJuego,
                    r.getDificultad(),
                    r.getPuntaje(),
                    intentos,
                    errores,
                    pistas,
                    durTxt,
                    precTxt,
                    piaTxt,
                    objTxt,
                    fecha,
                    hora
            });
        }
    }
    
    
    private String obtenerTextoObjetivo(SesionJuego s) {
        if (s == null) return "—";
        if (s.getIdPia() == null || s.getIdPia().isBlank()) return "—";
        if (s.getIdObjetivoPia() == null || s.getIdObjetivoPia().isBlank()) return "—";

        PIA pia = null;
        for (PIA p : piaService.obtenerTodos()) {
            if (p != null && s.getIdPia().equals(p.getIdPia())) { pia = p; break; }
        }
        if (pia == null) return "—";

        ObjetivoPIA obj = pia.getObjetivoPorId(s.getIdObjetivoPia());
        if (obj == null) return "—";

        String desc = (obj.getDescripcion() == null) ? "" : obj.getDescripcion().trim();
        if (desc.length() > 40) desc = desc.substring(0, 40) + "…";
        return "J" + obj.getJuegoId() + " - " + desc;
    }

    private void mostrarDetalleSesion(SesionJuego s) {
        if (s == null) return;

        String nombreJuego = (s.getJuego() != null) ? s.getJuego().getNombre() : "(sin juego)";
        String inicio = (s.getFechaHora() != null) ? s.getFechaHora().toString().replace("T", " ") : "—";
        String fin = (s.getFechaFin() != null) ? s.getFechaFin().toString().replace("T", " ") : "—";

        int intentos = s.getIntentosTotales();
        int errores = s.getErroresTotales();
        int pistas = s.getPistasUsadas();

        String durTxt = "—";
        if (s.getDuracionMs() > 0) durTxt = String.format(java.util.Locale.US, "%.1f s", s.getDuracionMs() / 1000.0);

        String precision = "—";
        if (intentos > 0) {
            double prec = (double) s.getAciertosTotales() / (double) intentos;
            precision = String.format(java.util.Locale.US, "%.0f%%", prec * 100.0);
        }

        String primerIntentoTxt = "—";
        if (s.getRondasCompletadas() > 0) {
            double rate = (double) s.getAciertosPrimerIntento() / (double) s.getRondasCompletadas();
            primerIntentoTxt = String.format(java.util.Locale.US, "%.0f%%", rate * 100.0);
        }

        String piaInfo = "—";
        String objetivoInfo = "—";

        if (s.getIdPia() != null && !s.getIdPia().isBlank()) {
            PIA pia = null;
            for (PIA p : piaService.obtenerTodos()) {
                if (p != null && s.getIdPia().equals(p.getIdPia())) { pia = p; break; }
            }
            if (pia != null) {
                piaInfo = "Activo/Registrado (id: " + pia.getIdPia() + ")";
                ObjetivoPIA obj = (s.getIdObjetivoPia() != null) ? pia.getObjetivoPorId(s.getIdObjetivoPia()) : null;
                if (obj != null) {
                    objetivoInfo = "Juego " + obj.getJuegoId() + " — " +
                            (obj.getDescripcion() == null ? "" : obj.getDescripcion()) +
                            "\nProgreso rondas: " + obj.getProgresoRondasCorrectas() + "/" + obj.getMetaRondasCorrectas() +
                            " | sesiones: " + obj.getProgresoSesionesCompletadas() + "/" + obj.getMetaSesionesCompletadas() +
                            (obj.isCompletado() ? " (COMPLETADO)" : "");
                } else {
                    objetivoInfo = "(No se encontró el objetivo en el PIA)";
                }
            }
        }

        String texto =
                "DETALLE DE SESIÓN\n\n" +
                "Estudiante: " + safe(s.getNombreEstudiante()) + " (ID: " + s.getIdEstudiante() + ")\n" +
                "Aula: " + safe(s.getAula()) + "\n" +
                "Juego: " + safe(nombreJuego) + "\n" +
                "Dificultad: " + s.getDificultad() + "\n" +
                "Puntaje: " + s.getPuntaje() + "\n\n" +
                "Inicio: " + inicio + "\n" +
                "Fin: " + fin + "\n" +
                "Duración: " + durTxt + "\n\n" +
                "Rondas: " + s.getRondasCompletadas() + "/" + s.getRondasTotales() + "\n" +
                "Aciertos totales: " + s.getAciertosTotales() + "\n" +
                "Aciertos 1er intento: " + s.getAciertosPrimerIntento() + " (" + primerIntentoTxt + ")\n" +
                "Intentos: " + intentos + "\n" +
                "Errores: " + errores + "\n" +
                "Pistas usadas: " + pistas + "\n" +
                "Precisión: " + precision + "\n\n" +
                "PIA: " + piaInfo + "\n" +
                "Objetivo: " + objetivoInfo;

        JTextArea area = new JTextArea(texto, 18, 55);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new java.awt.Dimension(720, 460));

        JOptionPane.showMessageDialog(
                this,
                sp,
                "Detalle de sesión",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
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

    private void actualizarKpiPia() {
        if (lblKpiPia == null) return;

        // Solo intentamos si el buscador es un ID numérico
        String txt = (txtBuscar != null) ? txtBuscar.getText().trim() : "";
        int id = -1;

        try {
            if (!txt.isBlank()) id = Integer.parseInt(txt);
        } catch (NumberFormatException ignored) {}

        if (id <= 0) {
            lblKpiPia.setText("<html><div style='text-align:center;'>" +
                    "<div style='font-size:12px;'>PIA</div>" +
                    "<div style='font-size:20px; font-weight:bold;'>—</div>" +
                    "</div></html>");
            return;
        }

        PIA pia = piaService.obtenerActivo(id);
        if (pia == null) {
            lblKpiPia.setText("<html><div style='text-align:center;'>" +
                    "<div style='font-size:12px;'>PIA</div>" +
                    "<div style='font-size:20px; font-weight:bold;'>Sin PIA</div>" +
                    "</div></html>");
            return;
        }

        ObjetivoPIA obj = pia.getObjetivoActivo();
        if (obj == null) {
            lblKpiPia.setText("<html><div style='text-align:center;'>" +
                    "<div style='font-size:12px;'>PIA</div>" +
                    "<div style='font-size:20px; font-weight:bold;'>Sin objetivos</div>" +
                    "</div></html>");
            return;
        }

        String valor = "J" + obj.getJuegoId() + " " +
                obj.getProgresoRondasCorrectas() + "/" + obj.getMetaRondasCorrectas();

        lblKpiPia.setText("<html><div style='text-align:center;'>" +
                "<div style='font-size:12px;'>PIA</div>" +
                "<div style='font-size:20px; font-weight:bold;'>" + valor + "</div>" +
                "</div></html>");
    }

    private String kpiHtml(String titulo, String valor) {
        return "<html><div style='text-align:center;'>" +
                "<div style='font-size:12px;'>" + titulo + "</div>" +
                "<div style='font-size:18px; font-weight:bold;'>" + valor + "</div>" +
                "</div></html>";
    }
}