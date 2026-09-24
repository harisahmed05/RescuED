package com.rescued.simulation;

import javax.swing.SwingUtilities;

/**
 * Entry point for the RescuED disaster-relief logistics simulation.
 *
 * <p>This class is intentionally tiny: it only schedules the construction of
 * the main {@link GameFrame} on the Swing Event Dispatch Thread (EDT). All
 * game state, rendering, and input handling live in the other classes in
 * this package.</p>
 *
 * @author Ahsan Haris Ahmed
 * @version 1.0
 */
public final class Main {

    private Main() {
        // utility class - prevent instantiation
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
