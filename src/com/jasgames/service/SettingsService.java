package com.jasgames.service;

import com.jasgames.util.AppLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jasgames.model.UiSettings;
import com.jasgames.util.AtomicFiles;
import com.jasgames.util.DataBackups;
import com.jasgames.util.FileLocks;
import com.jasgames.util.JsonSafeIO;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Preferencias de UI/accesibilidad (persistentes).
 *
 * Se guardan en: data/ui_settings.json
 */
public class SettingsService {

    private static final String ARCHIVO_SETTINGS = "data/ui_settings.json";

    private final Path path = Paths.get(ARCHIVO_SETTINGS);
    private final ReentrantLock ioLock = FileLocks.of(path);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private UiSettings settings;

    public SettingsService() {
        cargar();
    }

    public UiSettings getSettings() {
        ioLock.lock();
        try {
            // devolvemos copia para evitar mutaciones externas
            return (settings != null) ? settings.copy() : new UiSettings();
        } finally {
            ioLock.unlock();
        }
    }

    public void update(Consumer<UiSettings> mutator) {
        Objects.requireNonNull(mutator, "mutator");
        ioLock.lock();
        try {
            if (settings == null) settings = new UiSettings();
            mutator.accept(settings);
            guardar();
        } finally {
            ioLock.unlock();
        }
    }

    private void cargar() {
        ioLock.lock();
        try {
            UiSettings def = new UiSettings();
            settings = JsonSafeIO.readOrRecover(path, gson, UiSettings.class, def);
            if (settings == null) settings = def;
        } finally {
            ioLock.unlock();
        }
    }

    private void guardar() {
        try {
            DataBackups.backupIfExists(path);
            String json = gson.toJson(settings == null ? new UiSettings() : settings);
            AtomicFiles.writeStringAtomic(path, json, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            AppLog.error("No se pudo guardar settings: " + ex.getMessage(), ex);
        }
    }
}
