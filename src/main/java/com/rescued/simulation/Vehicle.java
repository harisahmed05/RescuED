package com.rescued.simulation;

import java.awt.Color;
import java.awt.Image;
import java.util.List;

/**
 * Base class for every rescue unit.
 *
 * <p><b>Encapsulation:</b> all mutable fields are private. Other classes
 * may only touch them through the getters/setters defined below.</p>
 *
 * <p><b>Polymorphism:</b> {@link #moveCost(Cell)} and {@link #canEnter(Cell)}
 * are abstract. {@link Drone} and {@link Truck} each implement them
 * differently, but the rest of the program ({@link Pathfinder},
 * {@link GamePanel}) only ever calls these two methods on a generic
 * {@code Vehicle} - it never needs to know which subclass it is holding.</p>
 *
 * <p>A vehicle carries one of three load states:</p>
 * <ul>
 *   <li>{@link LoadState#EMPTY} - just spawned, heading to base for supplies</li>
 *   <li>{@link LoadState#LOADED} - has supplies and can deliver to hospitals</li>
 *   <li>{@link LoadState#EXHAUSTED} - just delivered, must rest before moving again</li>
 * </ul>
 *
 * @author Ahsan Haris Ahmed
 */
public abstract class Vehicle {

    /** Display name shown in the UI. */
    private final String name;

    /** Fallback drawing color when the icon asset is missing. */
    private final Color color;

    /** Icon asset file under {@code resources/images/}. */
    private final String iconFile;

    /** Maximum fuel capacity. */
    private final int maxFuel;

    /** Maximum supply capacity (varies per subclass). */
    private final int maxSupplies;

    /** Current fuel remaining. */
    private int fuel;

    /** Current supply units being carried. */
    private int supplies;

    /** Current load state (EMPTY / LOADED / EXHAUSTED). */
    private LoadState loadState;

    /** Ticks remaining before an exhausted vehicle can move again. */
    private int exhaustionTicks;

    /** Current row position on the grid. */
    private int row;

    /** Current column position on the grid. */
    private int col;

    /** Current route being followed; {@code null} when idle. */
    private List<Cell> path;

    /** How far along {@link #path} we are. */
    private int pathIndex;

    /**
     * Constructs a vehicle at the given starting position with a full tank.
     *
     * @param name        display name
     * @param color       fallback drawing color
     * @param iconFile    icon asset file name
     * @param maxFuel     maximum fuel capacity
     * @param maxSupplies maximum supply capacity
     * @param startRow    starting row
     * @param startCol    starting column
     */
    protected Vehicle(String name, Color color, String iconFile,
                      int maxFuel, int maxSupplies,
                      int startRow, int startCol) {
        this.name = name;
        this.color = color;
        this.iconFile = iconFile;
        this.maxFuel = maxFuel;
        this.maxSupplies = maxSupplies;
        this.fuel = maxFuel;
        this.supplies = 0;
        this.loadState = LoadState.EMPTY;
        this.exhaustionTicks = 0;
        this.row = startRow;
        this.col = startCol;
    }

    /**
     * The three states a vehicle can be in during a mission.
     */
    public enum LoadState {
        /** Has no supplies - must go to base to load. */
        EMPTY,
        /** Carrying supplies - can deliver to hospitals. */
        LOADED,
        /** Just delivered - must rest before moving again. */
        EXHAUSTED
    }

    // ---- polymorphic contract every subclass must fill in ----

    /**
     * Returns the fuel cost of entering the given cell.
     *
     * @param cell the destination cell
     * @return the fuel cost
     */
    public abstract int moveCost(Cell cell);

    /**
     * Returns whether this vehicle is physically able to enter this cell.
     *
     * @param cell the destination cell
     * @return {@code true} if entry is allowed
     */
    public abstract boolean canEnter(Cell cell);

    // ---- shared behaviour ----

    /**
     * Deducts fuel for a move. Floors at zero rather than going negative.
     *
     * @param amount amount of fuel to consume
     */
    public void consumeFuel(int amount) {
        fuel = Math.max(0, fuel - amount);
    }

    /** Refills the fuel tank to maximum capacity. */
    public void refuel() {
        fuel = maxFuel;
    }

    /**
     * Returns whether the vehicle has at least the given fuel amount remaining.
     *
     * @param amount fuel needed for the next move
     * @return {@code true} if we have enough fuel
     */
    public boolean hasFuelFor(int amount) {
        return fuel >= amount;
    }

    /**
     * Loads supplies at the base. Refills supplies to max and flips the
     * vehicle into {@link LoadState#LOADED}. Exhaustion is also cleared
     * so the vehicle is immediately ready to move again.
     */
    public void loadSupplies() {
        supplies = maxSupplies;
        loadState = LoadState.LOADED;
        exhaustionTicks = 0;
        refuel();
    }

    /**
     * Drops off the carried supplies at a hospital. Flips the vehicle
     * into {@link LoadState#EXHAUSTED} and starts the rest countdown.
     *
     * @param restTicks how many ticks the vehicle must rest for
     */
    public void unloadSupplies(int restTicks) {
        supplies = 0;
        loadState = LoadState.EXHAUSTED;
        exhaustionTicks = restTicks;
    }

    /**
     * Advances the exhaustion countdown by one tick. When it reaches
     * zero, the vehicle transitions back to {@link LoadState#EMPTY}
     * (ready to load at base again).
     */
    public void tickExhaustion() {
        if (loadState != LoadState.EXHAUSTED) return;
        exhaustionTicks--;
        if (exhaustionTicks <= 0) {
            loadState = LoadState.EMPTY;
            exhaustionTicks = 0;
        }
    }

    // ---- getters / setters (encapsulation) ----

    /** @return the display name */
    public String getName() { return name; }

    /** @return the fallback drawing color */
    public Color getColor() { return color; }

    /** @return the loaded icon, or {@code null} if the asset is missing */
    public Image getIcon() { return Assets.load(iconFile); }

    /** @return current fuel */
    public int getFuel() { return fuel; }

    /** @return maximum fuel capacity */
    public int getMaxFuel() { return maxFuel; }

    /** @return current supplies being carried */
    public int getSupplies() { return supplies; }

    /** @return maximum supply capacity */
    public int getMaxSupplies() { return maxSupplies; }

    /** @return the current load state */
    public LoadState getLoadState() { return loadState; }

    /** @return ticks remaining before exhaustion ends */
    public int getExhaustionTicks() { return exhaustionTicks; }

    /** @return current row */
    public int getRow() { return row; }

    /** @return current column */
    public int getCol() { return col; }

    /**
     * Updates the vehicle's grid position.
     *
     * @param row new row
     * @param col new column
     */
    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** @return the current planned path, or {@code null} if idle */
    public List<Cell> getPath() { return path; }

    /**
     * Sets a new path for this vehicle to follow and resets progress.
     *
     * @param path the new path; {@code null} cancels any current path
     */
    public void setPath(List<Cell> path) {
        this.path = path;
        this.pathIndex = 0;
    }

    /** @return the index of the next cell to enter on the path */
    public int getPathIndex() { return pathIndex; }

    /** Advances progress along the path by one step. */
    public void advancePathIndex() { pathIndex++; }

    /**
     * Returns whether this vehicle is currently traversing a path.
     *
     * @return {@code true} if there is a non-empty path left to walk
     */
    public boolean isBusy() {
        return path != null && pathIndex < path.size();
    }

    /**
     * Returns whether the vehicle can currently accept orders to move.
     * Exhausted vehicles (and vehicles mid-route) cannot.
     *
     * @return {@code true} if a new dispatch is allowed
     */
    public boolean canAcceptOrders() {
        return loadState != LoadState.EXHAUSTED && !isBusy();
    }

    /** @return the icon filename (used by the UI for display) */
    public String getIconFile() { return iconFile; }
}
