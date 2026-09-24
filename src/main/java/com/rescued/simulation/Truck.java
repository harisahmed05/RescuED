package com.rescued.simulation;

import java.awt.Color;

/**
 * Heavy Truck: pays full terrain weight (roads are cheap, mud is
 * expensive) and cannot cross FLOOD, DEBRIS or BUILDING tiles at all.
 * Has a much bigger fuel tank and a large supply bay - ideal for the
 * long haul.
 *
 * @author Ahsan Haris Ahmed
 */
public class Truck extends Vehicle {

    /**
     * Creates a truck at the given starting position with a full tank.
     *
     * @param startRow starting row
     * @param startCol starting column
     */
    public Truck(int startRow, int startCol) {
        super("Truck", new Color(240, 190, 60), "truck.png",
              60, 6,  // 60 fuel, 6 supply units (big payload)
              startRow, startCol);
    }

    @Override
    public int moveCost(Cell cell) {
        return cell.terrain.weight;
    }

    @Override
    public boolean canEnter(Cell cell) {
        TerrainType t = cell.terrain;
        return t == TerrainType.ROAD
                || t == TerrainType.MUD
                || t == TerrainType.FUEL_STATION;
    }
}
