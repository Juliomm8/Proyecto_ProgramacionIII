package com.jasgames.ui;

import com.jasgames.util.DataBackups;

import javax.swing.*;
import java.awt.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BackupRestoreDialog extends JDialog {

    private final DefaultListModel<Path> model = new DefaultListModel<>();
    private final JList<Path> lstBackups = new JList<>(model);
    private final JTextArea txtDetalle = new JTextArea();

    private DataBackups.RestoreResult restoreResult;
    private Path selectedBackup;

    public BackupRestoreDialog(Window owner) {
        super(owner, "Restaurar desde backups", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(owner);

        setContentPane(buildUI());
        cargarBackups();
    }

    public DataBackups.RestoreResult getRestoreResult() {
        return restoreResult;
    }

    public Path getSelectedBackup() {
        return selectedBackup;
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Restaurar datos desde un backup");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        root.add(title, BorderLayout.NORTH);

        // Lista backups
        lstBackups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstBackups.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(value != null ? value.getFileName().toString() : "");
            lbl.setOpaque(true);
            if (isSelected) {
                lbl.setBackground(list.getSelectionBackground());
                lbl.setForeground(list.getSelectionForeground());
            } else {
                lbl.setBackground(list.getBackground());
                lbl.setForeground(list.getForeground());
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            return lbl;
        });

        lstBackups.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedBackup = lstBackups.getSelectedValue();
                mostrarDetalle(selectedBackup);
            }
        });

        JScrollPane leftScroll = new JScrollPane(lstBackups);
        leftScroll.setPreferredSize(new Dimension(260, 300));

        // Detalle
        txtDetalle.setEditable(false);
        txtDetalle.setLineWrap(true);
        txtDetalle.setWrapStyleWord(true);
        JScrollPane rightScroll = new JScrollPane(txtDetalle);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.35);
        root.add(split, BorderLayout.CENTER);

        // Botones
        JButton btnRefrescar = new JButton("Refrescar");
        JButton btnRestaurar = new JButton("Restaurar");
        JButton btnCancelar = new JButton("Cancelar");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(btnRefrescar);
        buttons.add(btnCancelar);
        buttons.add(btnRestaurar);

        btnRefrescar.addActionListener(e -> cargarBackups());
        btnCancelar.addActionListener(e -> dispose());
        btnRestaurar.addActionListener(e -> restaurarSeleccionado());

        // Nota
        JLabel note = new JLabel("Nota: Restaurar reemplaza archivos en data/. Se crea un backup de seguridad antes de sobrescribir.");
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.add(note, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    private void cargarBackups() {
        model.clear();
        List<Path> dirs = DataBackups.listBackupDirs();
        for (Path d : dirs) model.addElement(d);

        if (model.getSize() == 0) {
            txtDetalle.setText("No se encontraron backups.\n\nSe crean automáticamente cuando el sistema guarda archivos en data/.");
            selectedBackup = null;
        } else {
            lstBackups.setSelectedIndex(0);
        }
    }

    private void mostrarDetalle(Path dir) {
        if (dir == null) {
            txtDetalle.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Backup: ").append(dir.getFileName().toString()).append("\n");

        try {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                sb.append("\nArchivos:\n");
                int count = 0;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path p : stream) {
                        if (!Files.isRegularFile(p)) continue;
                        sb.append("• ").append(p.getFileName().toString()).append("\n");
                        count++;
                    }
                }
                if (count == 0) sb.append("(vacío)\n");
            }
        } catch (Exception ex) {
            sb.append("\nNo se pudo leer el contenido: ").append(ex.getMessage()).append("\n");
        }

        txtDetalle.setText(sb.toString());
        txtDetalle.setCaretPosition(0);
    }

    private void restaurarSeleccionado() {
        Path dir = lstBackups.getSelectedValue();
        if (dir == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un backup primero.", "Backups", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Esto reemplazará los archivos actuales en data/ con los del backup seleccionado.\n\n¿Deseas continuar?",
                "Confirmar restauración",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        restoreResult = DataBackups.restoreBackupDir(dir);
        selectedBackup = dir;

        if (restoreResult != null && restoreResult.ok) {
            JOptionPane.showMessageDialog(
                    this,
                    "Restauración completada: " + restoreResult.restoredFiles + " archivo(s).\nSe recomienda reiniciar la aplicación.",
                    "Backups",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } else {
            String msg = (restoreResult == null) ? "Error desconocido." : restoreResult.message;
            JOptionPane.showMessageDialog(this, msg, "No se pudo restaurar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
