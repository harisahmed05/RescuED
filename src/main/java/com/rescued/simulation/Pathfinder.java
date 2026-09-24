package com.rescued.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Deterministic pathfinding using Dijkstra's Algorithm.
 *
 * <p>Notice this class knows nothing about {@link Drone}s or {@link Truck}s
 * - it only calls {@link Vehicle#canEnter(Cell)} and
 * {@link Vehicle#moveCost(Cell)}. Because those two methods are
 * polymorphic, the exact same {@link #findPath} method automatically
 * produces different routes for different vehicle types.</p>
 *
 * @author Ahsan Haris Ahmed
 */
public final class Pathfinder {

    private Pathfinder() {
        // utility class - prevent instantiation
    }

    /** A node in the priority queue: a cell plus the best known cost to reach it. */
    private static final class Node implements Comparable<Node> {
        final Cell cell;
        final int cost;

        Node(Cell cell, int cost) {
            this.cell = cell;
            this.cost = cost;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.cost, other.cost);
        }
    }

    /**
     * Returns the list of cells to walk through (not including the start
     * cell) to get from {@code start} to {@code goal}, or {@code null} if
     * the goal is unreachable by this vehicle.
     *
     * @param grid    the map to search
     * @param start   the start cell (must be in the grid)
     * @param goal    the goal cell (must be in the grid)
     * @param vehicle the vehicle doing the travelling (drives canEnter/moveCost)
     * @return the path (excluding start), or {@code null} if unreachable
     */
    public static List<Cell> findPath(Grid grid, Cell start, Cell goal, Vehicle vehicle) {
        if (start == null || goal == null) return null;
        if (start == goal) return new ArrayList<>();

        Map<Cell, Integer> bestCost = new HashMap<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        bestCost.put(start, 0);
        open.add(new Node(start, 0));

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.cell == goal) break;
            if (current.cost > bestCost.getOrDefault(current.cell, Integer.MAX_VALUE)) continue;

            for (Cell neighbor : grid.neighbors(current.cell)) {
                if (!vehicle.canEnter(neighbor)) continue;
                int newCost = current.cost + vehicle.moveCost(neighbor);
                if (newCost < bestCost.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    bestCost.put(neighbor, newCost);
                    cameFrom.put(neighbor, current.cell);
                    open.add(new Node(neighbor, newCost));
                }
            }
        }

        if (!cameFrom.containsKey(goal)) return null; // never reached

        // walk backwards from goal to start, then reverse
        List<Cell> path = new ArrayList<>();
        Cell step = goal;
        while (step != start) {
            path.add(step);
            step = cameFrom.get(step);
        }
        Collections.reverse(path);
        return path;
    }
}
