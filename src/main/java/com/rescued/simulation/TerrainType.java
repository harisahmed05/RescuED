package com.rescued.simulation;

import java.awt.Color;
import java.awt.Image;

/**
 * All the terrain types that can appear on the map.
 *
 * <p>Each type carries a "weight" (how much fuel it costs a {@link Truck}
 * to cross it), a fallback color, and an image file name to draw instead
 * of the color once art has been added under {@code resources/images/}.</p>
 *
 * <p>Drones ignore most of these weights (see {@link Drone}) - that is
 * what makes them useful during floods and debris, at the cost of low
 * fuel capacity.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public enum TerrainType {
    /** Open, paved road. Cheapest to traverse. */
    ROAD(1, new Color(90, 90, 90), "road.png"),

    /** Soft ground - slow but passable for trucks. */
    MUD(3, new Color(120, 85, 40), "mud.png"),

    /** Standing water - trucks cannot cross this; drones fly over it. */
    FLOOD(5, new Color(40, 110, 200), "flood.png"),

    /** Rubble blocking a road. Trucks cannot cross until repaired. */
    DEBRIS(4, new Color(150, 60, 30), "debris.png"),

    /** Permanent structure - impassable for every vehicle. */
    BUILDING(Integer.MAX_VALUE, new Color(20, 20, 20), "building.png"),

    /** Fuel station - drive over to refuel, costs a small fee. */
    FUEL_STATION(1, new Color(80, 200, 220), "fuel_station.png");

    /** Terrain cost weight (used by {@link Truck#moveCost(Cell)}). */
    public final int weight;

    /** Fallback color when no image asset is available. */
    public final Color color;

    /** Image file name under {@code resources/images/}. */
    public final String imageFile;

    TerrainType(int weight, Color color, String imageFile) {
        this.weight = weight;
        this.color = color;
        this.imageFile = imageFile;
    }

    /**
     * Returns the loaded image for this terrain type, or {@code null} if
     * the asset file is missing. Callers should fall back to drawing the
     * solid {@link #color} when {@code null} is returned.
     *
     * @return the loaded {@link Image} or {@code null}
     */
    public Image getImage() {
        return Assets.load(imageFile);
    }
}
