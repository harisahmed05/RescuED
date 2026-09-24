package com.rescued.simulation;

import java.awt.Color;

/**
 * A delivery destination. Its "need" value grows over time to represent
 * the Golden Hour urgency. Delivering a vehicle carrying supplies here
 * banks the current need as "lives saved".
 *
 * <p>Each hospital also tracks whether it is currently in an
 * <b>emergency spike</b>: an event triggered by {@link GamePanel} where
 * the need jumps sharply and the hospital flashes red. Reaching a
 * spiked hospital in time yields a bonus, but letting it expire drains
 * global health.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public class Hospital {

    /** How many ticks an emergency spike lasts before it expires. */
    public static final int SPIKE_DURATION_TICKS = 80; // ≈12 seconds

    /** Bonus multiplier applied to lives saved when delivering during a spike. */
    public static final double SPIKE_BONUS_MULTIPLIER = 1.5;

    /** Damage applied to global health when a spike expires undelivered. */
    public static final double SPIKE_EXPIRE_DAMAGE = 60;

    /** The map cell this hospital sits on. */
    public final Cell location;

    /** Display name for the UI (e.g. "Hospital A"). */
    private final String name;

    /** Current accumulated need. */
    private double need;

    /** True while the hospital is in an emergency spike. */
    private boolean spikeActive = false;

    /** Ticks remaining before the current spike expires. */
    private int spikeTicksRemaining = 0;

    /**
     * Creates a hospital at the given cell.
     *
     * @param location     the cell the hospital is on
     * @param startingNeed initial need value
     * @param name         display name for the UI
     */
    public Hospital(Cell location, double startingNeed, String name) {
        this.location = location;
        this.need = startingNeed;
        this.name = name;
    }

    /** @return the current need value */
    public double getNeed() { return need; }

    /** @return the display name */
    public String getName() { return name; }

    /** @return {@code true} if the hospital is currently in an emergency spike */
    public boolean isSpikeActive() { return spikeActive; }

    /** @return ticks remaining before the current spike expires */
    public int getSpikeTicksRemaining() { return spikeTicksRemaining; }

    /**
     * Multiplies the current need by the given factor - exponential urgency
     * growth that simulates escalating patient arrivals over time.
     *
     * @param multiplier factor to multiply by
     */
    public void growNeed(double multiplier) {
        need *= multiplier;
    }

    /**
     * Triggers an emergency spike: the hospital's need jumps by a large
     * factor and the spike countdown begins. Reaching this hospital while
     * the spike is active yields a bonus, but letting the countdown
     * reach zero drains global health.
     *
     * @param needJump factor by which to multiply the current need
     */
    public void triggerSpike(double needJump) {
        spikeActive = true;
        spikeTicksRemaining = SPIKE_DURATION_TICKS;
        need *= needJump;
    }

    /**
     * Advances the spike countdown by one tick. If the spike expires,
     * deals damage to global health and clears the spike flag.
     *
     * @param state the mission state to damage when a spike expires
     */
    public void tickSpike(GameState state) {
        if (!spikeActive) return;
        spikeTicksRemaining--;
        if (spikeTicksRemaining <= 0) {
            spikeActive = false;
            state.damageHealth(SPIKE_EXPIRE_DAMAGE);
        }
    }

    /**
     * Called when a vehicle carrying supplies arrives at this hospital.
     * Banks the current need as lives saved and resets the need to a
     * small residual demand. Returns the amount saved.
     *
     * @return the amount banked as lives saved
     */
    public double serve() {
        double saved = need;
        if (spikeActive) {
            saved *= SPIKE_BONUS_MULTIPLIER;
        }
        need = GameState.STARTING_HOSPITAL_NEED;
        spikeActive = false;
        spikeTicksRemaining = 0;
        return saved;
    }

    /**
     * Returns a color that represents the hospital's current urgency
     * (used by the renderer to draw a status halo around the icon).
     *
     * @return the rendering color for this hospital's status
     */
    public Color getStatusColor() {
        if (spikeActive) {
            return new Color(255, 60, 60); // red flash during emergency
        }
        double ratio = need / GameState.HOSPITAL_DANGER_THRESHOLD;
        if (ratio < 0.5) {
            return new Color(80, 200, 80); // safe
        } else if (ratio < 1.0) {
            return new Color(220, 200, 50); // warning
        } else {
            return new Color(220, 120, 30); // danger
        }
    }
}
