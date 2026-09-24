package com.rescued.simulation;

import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * Plays WAV files from {@code resources/sounds/} using
 * {@code javax.sound.sampled} (built into Java - no extra libraries
 * needed).
 *
 * <ul>
 *   <li>{@link #playOnce(String)} fires a short sound effect and cleans
 *       itself up when done.</li>
 *   <li>{@link #playMusic(String)} loops a background track forever
 *       until {@link #stopMusic()} is called.</li>
 * </ul>
 *
 * <p>If a file is missing, methods just print a message and do nothing -
 * the game keeps running silently instead of crashing.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public final class Sounds {

    /** Currently-playing looping music clip, or {@code null} if stopped. */
    private static Clip musicClip;

    private Sounds() {
        // utility class - prevent instantiation
    }

    /**
     * Plays a one-shot sound effect from {@code resources/sounds/}.
     *
     * @param fileName the file name to play
     */
    public static void playOnce(String fileName) {
        Clip clip = loadClip(fileName);
        if (clip == null) return;
        // free the clip's resources automatically once it finishes playing
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) clip.close();
        });
        clip.start();
    }

    /**
     * Starts looping background music. Any previously-playing music is
     * stopped first.
     *
     * @param fileName the file name to play
     */
    public static void playMusic(String fileName) {
        stopMusic();
        musicClip = loadClip(fileName);
        if (musicClip != null) musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** Stops any currently looping music and releases its resources. */
    public static void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }

    /** @return {@code true} if background music is currently playing */
    public static boolean isMusicPlaying() {
        return musicClip != null && musicClip.isRunning();
    }

    /**
     * Loads (but does not start) a clip from {@code resources/sounds/}.
     *
     * @param fileName the file name to load
     * @return the loaded {@link Clip} or {@code null} on failure
     */
    private static Clip loadClip(String fileName) {
        try {
            URL url = Sounds.class.getResource("/sounds/" + fileName);
            if (url == null) {
                System.out.println("[Sounds] Missing sounds/" + fileName);
                return null;
            }
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            System.out.println("[Sounds] Could not play " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
