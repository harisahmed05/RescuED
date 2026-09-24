package com.rescued.simulation;

/**
 * Holds the mission-wide numbers shown on the HUD.
 *
 * <p>All fields are private - outside code can only change these values
 * through the methods below. This is the "Encapsulation" pillar from
 * the design.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public class GameState {

    /** Cost in budget to clear a single debris tile. */
    public static final int REPAIR_COST = 40;

    /** Initial budget for the mission. */
    public static final int STARTING_BUDGET = 500;

    /** Initial global health (the mission's collective well-being). */
    public static final double STARTING_HEALTH = 1000;

    /** Starting "need" value placed at each hospital at mission start. */
    public static final double STARTING_HOSPITAL_NEED = 30;

    /** Maximum safe hospital need before global health starts to drop. */
    public static final double HOSPITAL_DANGER_THRESHOLD = 150;

    /** Minimum number of hospitals spawned per mission. */
    public static final int MIN_HOSPITALS = 2;

    /** Maximum number of hospitals spawned per mission. */
    public static final int MAX_HOSPITALS = 5;

    /** Minimum number of fuel stations spawned per mission. */
    public static final int MIN_FUEL_STATIONS = 1;

    /** Maximum number of fuel stations spawned per mission. */
    public static final int MAX_FUEL_STATIONS = 3;

    /** Cost in budget to refuel at a fuel station (0 = free). */
    public static final int FUEL_STATION_COST = 15;

    /** Ticks between emergency spike events. */
    public static final int SPIKE_INTERVAL_TICKS = 100; // ≈15 seconds

    /** Ticks a vehicle must rest after a delivery. */
    public static final int VEHICLE_REST_TICKS = 16; // ≈2.4 seconds

    /** Lives-saved multiplier used to compute the mission score. */
    public static final double MISSION_SCORE_DIVISOR = 10.0;

    /** Maximum number of regeneration attempts if a random map fails reachability. */
    public static final int MAX_REGEN_ATTEMPTS = 30;

    private int budget = STARTING_BUDGET;
    private double globalHealth = STARTING_HEALTH;
    private double livesSaved = 0;
    private double fuelSpent = 0;
    private boolean gameOver = false;

    /** @return remaining budget */
    public int getBudget() { return budget; }

    /** @return current global health */
    public double getGlobalHealth() { return globalHealth; }

    /** @return total lives saved so far */
    public double getLivesSaved() { return livesSaved; }

    /** @return total fuel spent so far */
    public double getFuelSpent() { return fuelSpent; }

    /** @return whether the mission has ended */
    public boolean isGameOver() { return gameOver; }

    /**
     * Marks the mission as ended or not.
     *
     * @param over the new game-over state
     */
    public void setGameOver(boolean over) { gameOver = over; }

    /**
     * Attempts to spend budget on something (e.g. a debris repair).
     *
     * @param amount cost to spend
     * @return {@code true} if the budget was sufficient and deducted
     */
    public boolean spendBudget(int amount) {
        if (budget < amount) return false;
        budget -= amount;
        return true;
    }

    /**
     * Adds to the lives-saved counter (typically on hospital arrival).
     *
     * @param amount amount to add
     */
    public void addLivesSaved(double amount) {
        livesSaved += amount;
    }

    /**
     * Adds to the fuel-spent counter.
     *
     * @param amount amount to add
     */
    public void addFuelSpent(double amount) {
        fuelSpent += amount;
    }

    /**
     * Reduces global health and ends the mission if it reaches zero.
     *
     * @param amount damage to apply
     */
    public void damageHealth(double amount) {
        globalHealth = Math.max(0, globalHealth - amount);
        if (globalHealth <= 0) gameOver = true;
    }

    /**
     * Efficiency ratio shown on the dashboard: lives saved per fuel spent.
     * Returns {@code 0} when no fuel has been spent to avoid division by zero.
     *
     * @return the efficiency ratio
     */
    public double efficiency() {
        if (fuelSpent == 0) return 0;
        return livesSaved / fuelSpent;
    }

    /**
     * Resets all mission state back to its starting values.
     * Used by {@link GamePanel#newGame()}.
     */
    public void reset() {
        budget = STARTING_BUDGET;
        globalHealth = STARTING_HEALTH;
        livesSaved = 0;
        fuelSpent = 0;
        gameOver = false;
    }

    /**
     * Returns a single integer mission score that combines every
     * positive action the player has taken into one comparable number.
     *
     * @return the composite mission score
     */
    public int missionScore() {
        return (int) (livesSaved - fuelSpent / MISSION_SCORE_DIVISOR);
    }
}
