package com.rescued.simulation;

import java.awt.Color;

/**
 * Rescue Drone: flies over every type of terrain (ignores both terrain
 * weight and building obstacles). Fast and nimble, but limited fuel
 * and a small supply bay.
 *
 * @author Ahsan Haris Ahmed
 */
public class Drone extends Vehicle {

    /**
     * Creates a drone at the given starting position with a full tank.
     *
     * @param startRow starting row
     * @param startCol starting column
     */
    public Drone(int startRow, int startCol) {
        super("Drone", new Color(150, 230, 90), "drone.png",
              25, 2,  // 25 fuel, 2 supply units (small payload)
              startRow, startCol);
    }

    @Override
    public int moveCost(Cell cell) {
        return 1; // drones ignore terrain weight entirely
    }

    @Override
    public boolean canEnter(Cell cell) {
        return true; // drones fly over absolutely anything, including buildings
    }
}
