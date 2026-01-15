package com.jasgames.model;

/**
 * Preferencias de UI/accesibilidad.
 *
 * Nota: se guarda en JSON (data/ui_settings.json).
 */
public class UiSettings {

    private int schemaVersion = 1;

    // Docente
    private boolean docenteLetraGrande = false;

    // Estudiante
    private boolean estudianteLetraGrande = false;
    private boolean estudianteAltoContraste = false;
    private boolean estudiantePantallaCompleta = false;

    public UiSettings copy() {
        UiSettings c = new UiSettings();
        c.schemaVersion = this.schemaVersion;
        c.docenteLetraGrande = this.docenteLetraGrande;
        c.estudianteLetraGrande = this.estudianteLetraGrande;
        c.estudianteAltoContraste = this.estudianteAltoContraste;
        c.estudiantePantallaCompleta = this.estudiantePantallaCompleta;
        return c;
    }

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public boolean isDocenteLetraGrande() { return docenteLetraGrande; }
    public void setDocenteLetraGrande(boolean docenteLetraGrande) { this.docenteLetraGrande = docenteLetraGrande; }

    public boolean isEstudianteLetraGrande() { return estudianteLetraGrande; }
    public void setEstudianteLetraGrande(boolean estudianteLetraGrande) { this.estudianteLetraGrande = estudianteLetraGrande; }

    public boolean isEstudianteAltoContraste() { return estudianteAltoContraste; }
    public void setEstudianteAltoContraste(boolean estudianteAltoContraste) { this.estudianteAltoContraste = estudianteAltoContraste; }

    public boolean isEstudiantePantallaCompleta() { return estudiantePantallaCompleta; }
    public void setEstudiantePantallaCompleta(boolean estudiantePantallaCompleta) { this.estudiantePantallaCompleta = estudiantePantallaCompleta; }
}
