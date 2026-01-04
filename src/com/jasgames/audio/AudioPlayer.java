package com.jasgames.audio;

import javax.sound.sampled.*;
import javax.swing.SwingUtilities;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AudioPlayer {

    private static final AtomicReference<Clip> current = new AtomicReference<>(null);

    /**
     * Token de reproducción para evitar que un stop() dispare callbacks (onDone) de audios anteriores.
     * Cada llamada a stop() o play() invalida los callbacks previos.
     */
    private static final AtomicLong token = new AtomicLong(0);

    public static void stop() {
        // Invalida cualquier callback pendiente ANTES de parar el clip.
        token.incrementAndGet();

        Clip c = current.getAndSet(null);
        if (c != null) {
            try {
                c.stop();
                c.close();
            } catch (Exception ignored) {}
        }
    }

    /** Reproduce un WAV del classpath y llama onDone al finalizar (STOP). */
    public static void play(String resourcePath, Runnable onDone) {
        stop();

        // Token propio de esta reproducción.
        final long myToken = token.incrementAndGet();

        new Thread(() -> {
            try {
                URL url = AudioPlayer.class.getResource(resourcePath);
                if (url == null) {
                    System.err.println("Audio no encontrado: " + resourcePath);
                    if (onDone != null && token.get() == myToken) SwingUtilities.invokeLater(onDone);
                    return;
                }

                try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                    Clip clip = AudioSystem.getClip();
                    current.set(clip);

                    // Evita doble ejecución del callback si se reciben múltiples eventos STOP.
                    AtomicBoolean fired = new AtomicBoolean(false);

                    clip.addLineListener(ev -> {
                        if (ev.getType() == LineEvent.Type.STOP) {
                            try { clip.close(); } catch (Exception ignored) {}
                            if (onDone != null && token.get() == myToken && fired.compareAndSet(false, true)) {
                                SwingUtilities.invokeLater(onDone);
                            }
                        }
                    });

                    clip.open(ais);
                    clip.start();
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (onDone != null && token.get() == myToken) SwingUtilities.invokeLater(onDone);
            }
        }, "AudioPlayerThread").start();
    }

    /** Reproduce primero a, luego b, y al terminar b llama onDone. */
    public static void playSequence(String a, String b, Runnable onDone) {
        play(a, () -> play(b, onDone));
    }
}
