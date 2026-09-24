package com.rescued.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The map: a 2D array of {@link Cell}s.
 *
 * <p>By default the generator uses a fresh random seed based on the
 * current time, so every mission starts on a different map. A fixed
 * seed can also be supplied for reproducible test runs.</p>
 *
 * <p>The terrain distribution is balanced: roughly 55% road, 15% mud,
 * 13% flood, 11% debris, 6% building.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public class Grid {

    /** Number of rows on the map. */
    public final int rows;

    /** Number of columns on the map. */
    public final int cols;

    /** The underlying 2D cell array. */
    private final Cell[][] cells;

    /**
     * Creates a new grid of the given size and generates its terrain
     * using a time-based random seed (every game starts on a new map).
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public Grid(int rows, int cols) {
        this(rows, cols, System.currentTimeMillis());
    }

    /**
     * Creates a new grid of the given size using the supplied random seed.
     * Passing a fixed seed makes the resulting map fully reproducible
     * (useful for tests and bug reports).
     *
     * @param rows    number of rows
     * @param cols    number of columns
     * @param seed    the random seed to use for terrain generation
     */
    public Grid(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];
        generate(seed);
    }

    /**
     * Populates the grid with terrain according to the fixed distribution.
     *
     * @param seed the random seed
     */
    private void generate(long seed) {
        Random rnd = new Random(seed);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double p = rnd.nextDouble();
                TerrainType t;
                if (p < 0.55) {
                    t = TerrainType.ROAD;
                } else if (p < 0.70) {
                    t = TerrainType.MUD;
                } else if (p < 0.83) {
                    t = TerrainType.FLOOD;
                } else if (p < 0.94) {
                    t = TerrainType.DEBRIS;
                } else {
                    t = TerrainType.BUILDING;
                }
                cells[r][c] = new Cell(r, c, t);
            }
        }
    }

    /**
     * Force a cell and its immediate neighbours to be clear road tiles.
     * Used to guarantee the base and hospitals always sit on usable ground.
     *
     * @param row row of the central cell
     * @param col column of the central cell
     */
    public void clearArea(int row, int col) {
        get(row, col).terrain = TerrainType.ROAD;
        for (Cell n : neighbors(get(row, col))) {
            n.terrain = TerrainType.ROAD;
        }
    }

    /**
     * Returns the cell at the given coordinates, or {@code null} if out of bounds.
     *
     * @param row the row
     * @param col the column
     * @return the cell, or {@code null} if the coordinates are invalid
     */
    public Cell get(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
        return cells[row][col];
    }

    /**
     * Returns a list of every road cell in the grid (in row-major order).
     * Useful for randomly spawning hospitals, supply depots, etc.
     *
     * @return list of road cells (never {@code null}, may be empty)
     */
    public List<Cell> getRoadCells() {
        List<Cell> roads = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (cells[r][c].terrain == TerrainType.ROAD) {
                    roads.add(cells[r][c]);
                }
            }
        }
        return roads;
    }

    /**
     * BFS from {@code start} treating every non-{@link TerrainType#BUILDING}
     * and non-{@link TerrainType#FLOOD} tile as passable. Returns the
     * shortest such path (excluding start) to {@code goal}, or {@code null}
     * if no such path exists.
     *
     * <p>This models "truck-reachability": flood is impassable for trucks
     * even after repair, but debris can be cleared by spending budget.
     * If this returns a non-null path, a truck can reach the goal by
     * driving and (optionally) repairing debris along the way.</p>
     *
     * @param start starting cell
     * @param goal  target cell
     * @return shortest path avoiding flood + building, or {@code null}
     */
    public List<Cell> shortestTruckReachablePath(Cell start, Cell goal) {
        if (start == null || goal == null) return null;
        if (start == goal) return new ArrayList<>();
        java.util.Map<Cell, Cell> cameFrom = new java.util.HashMap<>();
        java.util.Queue<Cell> queue = new java.util.ArrayDeque<>();
        cameFrom.put(start, start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell cur = queue.poll();
            if (cur == goal) break;
            for (Cell n : neighbors(cur)) {
                if (cameFrom.containsKey(n)) continue;
                if (n.terrain == TerrainType.BUILDING) continue;
                if (n.terrain == TerrainType.FLOOD) continue;
                cameFrom.put(n, cur);
                queue.add(n);
            }
        }
        if (!cameFrom.containsKey(goal)) return null;
        List<Cell> path = new ArrayList<>();
        Cell step = goal;
        while (step != start) {
            path.add(step);
            step = cameFrom.get(step);
        }
        java.util.Collections.reverse(path);
        return path;
    }

    /**
     * Counts how many {@link TerrainType#DEBRIS} tiles are along the given
     * path. Used by reachability validation to budget how many repairs
     * a truck would need to make a path fully driveable.
     *
     * @param path the path to inspect
     * @return count of debris tiles along the path
     */
    public int countDebrisAlong(List<Cell> path) {
        if (path == null) return Integer.MAX_VALUE;
        int count = 0;
        for (Cell c : path) {
            if (c != null && c.terrain == TerrainType.DEBRIS) count++;
        }
        return count;
    }

    /**
     * Walks the 3x3 neighbourhood around each of the given cells and,
     * for any road tile inside, randomly converts it into
     * {@link TerrainType#DEBRIS}. This makes the approach to each
     * hospital harder and gives the player more meaningful use of the
     * Repair Mode. Flood is intentionally not added because it would
     * block trucks entirely (and the reachability guarantee would
     * just reroll the map anyway).
     *
     * @param anchors the cells to thicken around
     */
    public void thickenObstaclesAround(List<Cell> anchors) {
        Random rnd = new Random();
        for (Cell anchor : anchors) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue; // skip the anchor itself
                    Cell n = get(anchor.row + dr, anchor.col + dc);
                    if (n == null) continue;
                    if (n.terrain != TerrainType.ROAD) continue;
                    if (rnd.nextDouble() < 0.50) {
                        n.terrain = TerrainType.DEBRIS;
                    }
                }
            }
        }
    }

    /**
     * Returns the 4-directional (Manhattan) neighbours of the given cell.
     * Diagonal neighbours are intentionally excluded - this keeps
     * {@link Pathfinder}'s Dijkstra implementation simpler.
     *
     * @param c the centre cell
     * @return a list of neighbours, never {@code null}
     */
    public List<Cell> neighbors(Cell c) {
        List<Cell> result = new ArrayList<>(4);
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            Cell n = get(c.row + d[0], c.col + d[1]);
            if (n != null) result.add(n);
        }
        return result;
    }
}
