package com.jasgames.audio;

import javax.sound.sampled.*;
import javax.swing.SwingUtilities;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class AudioPlayer {

    private static final AtomicReference<Clip> current = new AtomicReference<>(null);

    public static void stop() {
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

        new Thread(() -> {
            try {
                URL url = AudioPlayer.class.getResource(resourcePath);
                if (url == null) {
                    System.err.println("Audio no encontrado: " + resourcePath);
                    if (onDone != null) SwingUtilities.invokeLater(onDone);
                    return;
                }

                try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                    Clip clip = AudioSystem.getClip();
                    current.set(clip);

                    clip.addLineListener(ev -> {
                        if (ev.getType() == LineEvent.Type.STOP) {
                            try { clip.close(); } catch (Exception ignored) {}
                            if (onDone != null) SwingUtilities.invokeLater(onDone);
                        }
                    });

                    clip.open(ais);
                    clip.start();
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (onDone != null) SwingUtilities.invokeLater(onDone);
            }
        }, "AudioPlayerThread").start();
    }

    /** Reproduce primero a, luego b, y al terminar b llama onDone. */
    public static void playSequence(String a, String b, Runnable onDone) {
        play(a, () -> play(b, onDone));
    }
}
