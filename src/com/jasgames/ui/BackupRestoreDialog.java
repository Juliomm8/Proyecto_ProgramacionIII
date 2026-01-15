package com.jasgames.ui;

import com.jasgames.service.AppContext;
import com.jasgames.util.DataBackups;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Diálogo para restaurar archivos desde data/backups/.
 *
 * Nota: algunos servicios mantienen datos en memoria; tras restaurar es recomendable
 * cerrar y volver a abrir Modo Docente para recargar desde disco.
 */
public class BackupRestoreDialog extends JDialog {

    private final AppContext context;

    private final DefaultListModel<Path> modelBackups = new DefaultListModel<>();
    private final JList<Path> listBackups = new JList<>(modelBackups);
    private final DefaultListModel<String> modelFiles = new DefaultListModel<>();
    private final JList<String> listFiles = new JList<>(modelFiles);

    private final JButton btnRestore = new JButton("Restaurar");
    private final JLabel lblEstado = new JLabel(" ");

    public BackupRestoreDialog(Window owner, AppContext context) {
        super(owner, "Backups - Restaurar", ModalityType.APPLICATION_MODAL);
        this.context = context;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(owner);

        setContentPane(build());
        cargarBackups();
        configurarListeners();
    }

    private JComponent build() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Restaurar desde backups", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        listBackups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listBackups.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Path p) {
                    setText(p.getFileName().toString());
                }
                return c;
            }
        });

        listFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.add(new JLabel("Backups disponibles:"), BorderLayout.NORTH);
        left.add(new JScrollPane(listBackups), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(new JLabel("Archivos dentro del backup:"), BorderLayout.NORTH);
        right.add(new JScrollPane(listFiles), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.45);
        root.add(split, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        lblEstado.setForeground(new Color(70, 70, 70));
        south.add(lblEstado, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnClose = new JButton("Cerrar");

        btnRestore.setEnabled(false);
        actions.add(btnRestore);
        actions.add(btnClose);

        btnClose.addActionListener(e -> dispose());
        south.add(actions, BorderLayout.EAST);

        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private void configurarListeners() {
        listBackups.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarListaArchivos();
            }
        });

        btnRestore.addActionListener(e -> restaurarSeleccionado());
    }

    private void cargarBackups() {
        modelBackups.clear();
        modelFiles.clear();
        btnRestore.setEnabled(false);

        Path backupsRoot = Paths.get("data", "backups");
        if (!Files.isDirectory(backupsRoot)) {
            lblEstado.setText("No hay backups aún (se crean al guardar cambios en data/)." );
            return;
        }

        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(backupsRoot)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) dirs.add(p);
            }
        } catch (Exception ex) {
            lblEstado.setText("Error leyendo backups: " + ex.getMessage());
            return;
        }

        dirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        for (Path d : dirs) modelBackups.addElement(d);

        if (!dirs.isEmpty()) {
            listBackups.setSelectedIndex(0);
        } else {
            lblEstado.setText("No hay backups aún.");
        }
    }

    private void actualizarListaArchivos() {
        modelFiles.clear();
        btnRestore.setEnabled(false);

        Path selected = listBackups.getSelectedValue();
        if (selected == null || !Files.isDirectory(selected)) {
            lblEstado.setText("Selecciona un backup.");
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(selected)) {
            int count = 0;
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) continue;
                long size = 0L;
                try { size = Files.size(p); } catch (Exception ignored) {}
                modelFiles.addElement(p.getFileName().toString() + "  (" + size + " bytes)");
                count++;
            }
            btnRestore.setEnabled(count > 0);
            lblEstado.setText("Backup: " + selected.getFileName() + " | archivos: " + count);
        } catch (Exception ex) {
            lblEstado.setText("Error leyendo archivos del backup: " + ex.getMessage());
        }
    }

    private void restaurarSeleccionado() {
        Path backupDir = listBackups.getSelectedValue();
        if (backupDir == null || !Files.isDirectory(backupDir)) {
            JOptionPane.showMessageDialog(this, "Selecciona un backup válido.", "Backups", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(
                this,
                "Esto sobrescribirá archivos en la carpeta data/.\n" +
                        "Se hará un backup automático de lo actual antes de restaurar.\n\n" +
                        "¿Deseas continuar?",
                "Confirmar restauración",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (r != JOptionPane.YES_OPTION) return;

        Path dataDir = Paths.get("data");
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {}

        int restored = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(backupDir)) {
            for (Path src : ds) {
                if (!Files.isRegularFile(src)) continue;
                Path dest = dataDir.resolve(src.getFileName());

                // backup de seguridad antes de sobreescribir
                DataBackups.backupIfExists(dest);

                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                restored++;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error restaurando: " + ex.getMessage(), "Backups", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (context != null && context.getAuditoriaService() != null) {
                context.getAuditoriaService().registrar("RESTORE_BACKUP", "dir=" + backupDir.getFileName() + " files=" + restored);
            }
        } catch (Exception ignored) {}

        JOptionPane.showMessageDialog(
                this,
                "Restauración completada. Archivos restaurados: " + restored + "\n\n" +
                        "Recomendación: cierra y vuelve a abrir Modo Docente para recargar datos.",
                "Backups",
                JOptionPane.INFORMATION_MESSAGE
        );

        cargarBackups();
    }
}
