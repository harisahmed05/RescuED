package com.rescued.simulation;

/**
 * One tile of the map.
 *
 * <p>A cell holds only its position and a mutable {@link TerrainType}
 * reference. The terrain field is mutable on purpose: when a player
 * repairs a debris tile in Repair Mode, the same cell is reused and
 * only the {@code terrain} reference changes.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public class Cell {

    /** Row coordinate (0-indexed, top-down). */
    public final int row;

    /** Column coordinate (0-indexed, left-to-right). */
    public final int col;

    /** Current terrain occupying this cell. May change during play. */
    public TerrainType terrain;

    /**
     * Creates a new map cell.
     *
     * @param row     the row coordinate
     * @param col     the column coordinate
     * @param terrain the terrain occupying this cell
     */
    public Cell(int row, int col, TerrainType terrain) {
        this.row = row;
        this.col = col;
        this.terrain = terrain;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Cell)) return false;
        Cell c = (Cell) other;
        return row == c.row && col == c.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return "Cell(" + row + "," + col + "," + terrain + ")";
    }
}
