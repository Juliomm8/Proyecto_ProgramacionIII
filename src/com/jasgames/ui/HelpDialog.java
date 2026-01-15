package com.jasgames.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Ayuda rápida para Modo Docente.
 *
 * Mantiene el estilo "moderno": limpio, claro y sin saturar de opciones.
 */
public class HelpDialog extends JDialog {

    public HelpDialog(Window owner) {
        super(owner, "Ayuda - JAS Games", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(owner);

        setContentPane(build());
    }

    private JComponent build() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Ayuda rápida", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Empezar", htmlPane(empezarHtml()));
        tabs.addTab("PIA", htmlPane(piaHtml()));
        tabs.addTab("Dashboard", htmlPane(dashboardHtml()));
        tabs.addTab("Backups", htmlPane(backupsHtml()));
        tabs.addTab("Atajos", htmlPane(atajosHtml()));
        root.add(tabs, BorderLayout.CENTER);

        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        south.add(cerrar);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    private JComponent htmlPane(String html) {
        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane sp = new JScrollPane(pane);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
        return sp;
    }

    private String baseCss() {
        return "<style>" +
                "body{font-family:Dialog; font-size:14px; line-height:1.35;}" +
                "h2{font-size:16px; margin:8px 0 6px 0;}" +
                "ul{margin-top:6px;}" +
                "code{background:#f3f3f3; padding:2px 4px; border-radius:6px;}" +
                "</style>";
    }

    private String empezarHtml() {
        return "<html><head>" + baseCss() + "</head><body>" +
                "<h2>Flujo recomendado</h2>" +
                "<ul>" +
                "<li><b>Aulas</b>: crea/organiza aulas y asigna niños.</li>" +
                "<li><b>Perfiles</b>: revisa información del niño y sus objetivos PIA.</li>" +
                "<li><b>Estudiante</b>: el niño juega y el sistema guarda resultados automáticamente.</li>" +
                "<li><b>Dashboard</b>: filtra por niño/fechas/juego y exporta o analiza avances.</li>" +
                "</ul>" +
                "<h2>Consejos para una demo</h2>" +
                "<ul>" +
                "<li>Primero crea 1 aula y 1–2 niños de ejemplo.</li>" +
                "<li>Juega una sesión corta y luego muestra el Dashboard.</li>" +
                "</ul>" +
                "</body></html>";
    }

    private String piaHtml() {
        return "<html><head>" + baseCss() + "</head><body>" +
                "<h2>¿Qué es el PIA?</h2>" +
                "<p>El PIA registra objetivos por niño y el progreso basado en sesiones guardadas.</p>" +
                "<h2>Buenas prácticas</h2>" +
                "<ul>" +
                "<li>Activa objetivos claros y medibles.</li>" +
                "<li>Tras borrar o restaurar sesiones, el sistema recalcula progreso.</li>" +
                "</ul>" +
                "</body></html>";
    }

    private String dashboardHtml() {
        return "<html><head>" + baseCss() + "</head><body>" +
                "<h2>Filtros</h2>" +
                "<ul>" +
                "<li>Usa aula/niño para acotar la tabla rápidamente.</li>" +
                "<li>La búsqueda tiene <i>debounce</i>: espera un momento y filtra sin lag.</li>" +
                "</ul>" +
                "<h2>Eliminar sesión</h2>" +
                "<p>Si eliminas una sesión, aparece <b>Deshacer</b> durante unos segundos.</p>" +
                "</body></html>";
    }

    private String backupsHtml() {
        return "<html><head>" + baseCss() + "</head><body>" +
                "<h2>Backups automáticos</h2>" +
                "<p>Antes de sobrescribir archivos en <code>data/</code>, el sistema crea copias en <code>data/backups/</code>.</p>" +
                "<h2>Restaurar</h2>" +
                "<ul>" +
                "<li>En Modo Docente, presiona <b>Backups</b> (arriba a la derecha).</li>" +
                "<li>Elige una fecha/hora y pulsa <b>Restaurar</b>.</li>" +
                "<li>Después de restaurar, cierra y vuelve a abrir Modo Docente para recargar datos.</li>" +
                "</ul>" +
                "</body></html>";
    }

    private String atajosHtml() {
        return "<html><head>" + baseCss() + "</head><body>" +
                "<h2>Teclado</h2>" +
                "<ul>" +
                "<li><b>F1</b>: abrir Ayuda</li>" +
                "<li><b>Ctrl + Shift + B</b>: abrir Backups</li>" +
                "<li><b>Ctrl + I</b>: Acerca de</li>" +
                "</ul>" +
                "</body></html>";
    }
}
