package com.rescued.simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Persistent best-score storage.
 *
 * <p>The high score is written to {@code ~/.rescued/highscore.txt} so
 * it survives across game restarts. If the file cannot be read or
 * written (e.g. permission errors on the home directory), the class
 * silently falls back to a transient in-memory score so the game
 * remains playable.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public final class HighScore {

    /** File name inside the user's home directory. */
    private static final String FILE_NAME = ".rescued/highscore.txt";

    /** In-memory fallback when the file is unavailable. */
    private static int memoryScore = 0;

    /** Cached best score (loaded once on first access). */
    private static int cached = -1;

    private HighScore() {
        // utility class - prevent instantiation
    }

    /**
     * Returns the current best score. Loads from disk on first call.
     *
     * @return the best score, or 0 if no run has been recorded yet
     */
    public static synchronized int get() {
        if (cached >= 0) return cached;
        try {
            Path file = path();
            if (Files.exists(file)) {
                String content = new String(Files.readAllBytes(file)).trim();
                cached = Integer.parseInt(content);
            } else {
                cached = 0;
            }
        } catch (Exception e) {
            System.out.println("[HighScore] Could not read high score: " + e.getMessage());
            cached = memoryScore;
        }
        return cached;
    }

    /**
     * If {@code score} is greater than the current best, saves it as the
     * new best score (both in memory and on disk). Returns {@code true}
     * if a new record was set.
     *
     * @param score the new score to consider
     * @return {@code true} if the score beat the previous record
     */
    public static synchronized boolean recordIfBest(int score) {
        int best = get();
        if (score > best) {
            cached = score;
            memoryScore = score;
            try {
                Path file = path();
                Files.createDirectories(file.getParent());
                Files.write(file, Integer.toString(score).getBytes());
            } catch (IOException e) {
                System.out.println("[HighScore] Could not persist high score: " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    /** Resets the in-memory cache (useful for tests). */
    public static synchronized void resetCache() {
        cached = -1;
    }

    /** @return the file the high score is persisted to */
    private static Path path() {
        return Paths.get(System.getProperty("user.home"), FILE_NAME.split("/"));
    }
}
