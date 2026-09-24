package com.rescued.simulation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The main game surface: draws the grid, hospitals and vehicles, and runs
 * the real-time game loop with a {@link Timer}.
 *
 * <p>The grid is 9 rows by 12 columns of 72-pixel tiles. Each mission
 * randomly generates 2-5 hospitals at scattered road tiles, plus a
 * fixed base in the top-left. The mouse listener handles three
 * interaction modes:</p>
 * <ul>
 *   <li>Vehicle selection (click on a vehicle)</li>
 *   <li>Dispatch (click any cell with a vehicle selected)</li>
 *   <li>Repair (click a debris tile while Repair Mode is on)</li>
 * </ul>
 *
 * <p>Mission flow:</p>
 * <ol>
 *   <li>Empty vehicle departs base, picks up supplies by clicking the
 *       base, then is dispatched to a hospital.</li>
 *   <li>On hospital arrival the vehicle unloads, scores lives saved,
 *       becomes exhausted for a few ticks, and must be sent back to
 *       the base to reload.</li>
 *   <li>Every ~15 seconds a random hospital triggers an emergency
 *       spike - reaching it in time yields a bonus.</li>
 * </ol>
 *
 * @author Ahsan Haris Ahmed
 */
public class GamePanel extends JPanel {

    /** Default width/height of a single map tile in pixels (used as a base size). */
    public static final int CELL_SIZE = 72;

    /** Extra height below the grid used for label rendering. */
    public static final int LABEL_HEIGHT = 14;

    /** Minimum allowed cell size when the panel is shrunk. */
    public static final int MIN_CELL_SIZE = 36;

    /** Number of rows on the map. */
    public static final int ROWS = 9;

    /** Number of columns on the map. */
    public static final int COLS = 12;

    /** Game loop tick interval in milliseconds. */
    private static final int TICK_MS = 150;

    /** Every Nth tick a vehicle steps forward one cell. */
    private static final int MOVE_EVERY_N_TICKS = 2;

    /** Every Nth tick hospital need grows (≈3s between growths). */
    private static final int GROWTH_EVERY_N_TICKS = 20;

    /** Total ticks before the Golden Hour ends (≈90 seconds). */
    private static final int GOLDEN_HOUR_TICKS = 600;

    /** Alpha value for path highlight overlay. */
    private static final int PATH_HIGHLIGHT_ALPHA = 140;

    /** Row coordinate of the base. */
    public static final int BASE_ROW = 0;

    /** Column coordinate of the base. */
    public static final int BASE_COL = 0;

    /** Number of rows occupied by the base zone (height). */
    public static final int BASE_ROWS = 2;

    /** Number of columns occupied by the base zone (width). */
    public static final int BASE_COLS = 2;

    private Grid grid;
    private GameState state;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Hospital> hospitals = new ArrayList<>();

    private Vehicle selectedVehicle;
    private boolean repairMode = false;
    private int tickCount = 0;
    private int nextSpikeTick = GameState.SPIKE_INTERVAL_TICKS;
    private final Random spikeRng = new Random();

    private int hoverRow = -1;
    private int hoverCol = -1;

    /** Cached current cell size in pixels (recomputed on every paint). */
    private int currentCellSize = CELL_SIZE;

    private final Timer timer;
    private final Runnable hudUpdater;

    /**
     * Constructs the game panel.
     *
     * @param hudUpdater runnable invoked after every game tick so the
     *                   sidebar labels can refresh
     */
    public GamePanel(Runnable hudUpdater) {
        this.hudUpdater = hudUpdater;
        setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE + LABEL_HEIGHT));
        setMinimumSize(new Dimension(COLS * MIN_CELL_SIZE, ROWS * MIN_CELL_SIZE + LABEL_HEIGHT));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int newRow = e.getY() / Math.max(1, currentCellSize);
                int newCol = e.getX() / Math.max(1, currentCellSize);
                if (newRow != hoverRow || newCol != hoverCol) {
                    hoverRow = newRow;
                    hoverCol = newCol;
                    repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                hoverCol = -1;
                repaint();
            }
        });

        newGame();

        timer = new Timer(TICK_MS, e -> tick());
        timer.start();

        Sounds.playMusic("theme.wav");
    }

    /** Resets everything back to the starting conditions. */
    public void newGame() {
        // Try up to MAX_REGEN_ATTEMPTS times to roll a fair map. If a
        // generated map is unfair (too many debris between base and
        // hospitals for the budget to fix) we reroll. After the cap we
        // accept the best map seen so far.
        Grid bestGrid = null;
        List<Cell> bestHospitalCells = null;
        for (int attempt = 0; attempt < GameState.MAX_REGEN_ATTEMPTS; attempt++) {
            Grid candidate = new Grid(ROWS, COLS);
            candidate.clearArea(BASE_ROW, BASE_COL);
            candidate.clearArea(BASE_ROW, BASE_COL + 1);
            candidate.clearArea(BASE_ROW + 1, BASE_COL);
            candidate.clearArea(BASE_ROW + 1, BASE_COL + 1);

            List<Cell> candidateCells = pickHospitalCells(candidate);
            int count = chooseHospitalCount(candidateCells.size());
            List<Cell> candidateHospitalCells = candidateCells.subList(0, count);

            // thicken obstacles around hospitals BEFORE the fairness
            // check so the check accounts for them.
            candidate.thickenObstaclesAround(candidateHospitalCells);

            if (isMapFair(candidate, candidateHospitalCells)) {
                grid = candidate;
                bestHospitalCells = new ArrayList<>(candidateHospitalCells);
                break;
            }
            if (bestGrid == null) {
                bestGrid = candidate;
                bestHospitalCells = new ArrayList<>(candidateHospitalCells);
            }
        }
        if (grid == null) {
            // ran out of attempts - fall back to whatever we had
            grid = bestGrid;
        }

        state = new GameState();
        tickCount = 0;
        nextSpikeTick = GameState.SPIKE_INTERVAL_TICKS;

        vehicles.clear();
        hospitals.clear();
        spawnHospitalsFromCells(bestHospitalCells);
        spawnFuelStations();
        spawnVehicles();

        selectedVehicle = null;
        repairMode = false;
        hoverRow = -1;
        hoverCol = -1;
        state.setGameOver(false);
        repaint();
        hudUpdater.run();
    }

    /**
     * Returns {@code true} if the given cell is part of the base zone.
     *
     * @param row row to check
     * @param col column to check
     * @return {@code true} if the cell is a base cell
     */
    public static boolean isBaseCell(int row, int col) {
        return row >= BASE_ROW && row < BASE_ROW + BASE_ROWS
                && col >= BASE_COL && col < BASE_COL + BASE_COLS;
    }

    /**
     * Picks a random number of road cells (outside the base zone) to
     * serve as hospital locations. The cells are returned in shuffled
     * order so the caller can take the first N.
     *
     * @param g the grid to scan
     * @return shuffled list of candidate road cells
     */
    private List<Cell> pickHospitalCells(Grid g) {
        List<Cell> roads = g.getRoadCells();
        for (int r = BASE_ROW; r < BASE_ROW + BASE_ROWS; r++) {
            for (int c = BASE_COL; c < BASE_COL + BASE_COLS; c++) {
                roads.remove(g.get(r, c));
            }
        }
        java.util.Collections.shuffle(roads, new Random());
        return roads;
    }

    /**
     * Returns the random number of hospitals this mission will spawn,
     * clamped to {@code maxCandidates} available road cells.
     */
    private int chooseHospitalCount(int maxCandidates) {
        Random rng = new Random();
        int count = GameState.MIN_HOSPITALS
                + rng.nextInt(GameState.MAX_HOSPITALS - GameState.MIN_HOSPITALS + 1);
        return Math.min(count, maxCandidates);
    }

    /**
     * Validates a freshly-generated map: every hospital in
     * {@code hospitalCells} must be reachable from the base by a Truck
     * path that crosses at most {@code BUDGET / REPAIR_COST} debris
     * tiles in total. If any hospital is flood-isolated, the map is
     * rejected outright.
     */
    private boolean isMapFair(Grid g, List<Cell> hospitalCells) {
        Cell baseCell = g.get(BASE_ROW, BASE_COL);
        if (baseCell == null) return false;
        int totalDebris = 0;
        int maxRepairs = GameState.STARTING_BUDGET / GameState.REPAIR_COST;
        for (Cell h : hospitalCells) {
            List<Cell> path = g.shortestTruckReachablePath(baseCell, h);
            if (path == null) {
                return false; // flood-isolated - impossible even with infinite budget
            }
            totalDebris += g.countDebrisAlong(path);
            if (totalDebris > maxRepairs) {
                return false;
            }
        }
        return true;
    }

    /**
     * Spawns hospitals at the supplied pre-validated cells. Each hospital
     * gets a small randomised starting need so the player must triage
     * priorities.
     *
     * @param cells the cells where hospitals will be placed (already
     *              validated for truck reachability)
     */
    private void spawnHospitalsFromCells(List<Cell> cells) {
        Random rng = new Random();
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            grid.clearArea(cell.row, cell.col); // guarantee passable
            double startingNeed = GameState.STARTING_HOSPITAL_NEED
                    + rng.nextInt(20); // 30-49
            String name = "Hospital " + (char) ('A' + i);
            hospitals.add(new Hospital(cell, startingNeed, name));
        }
    }

    /**
     * Spawns 1–3 fuel stations at random road cells that are at least
     * one tile away from every hospital and from the base zone. The
     * candidate set is the set of road cells minus those reserved for
     * the base and hospitals.
     */
    private void spawnFuelStations() {
        Random rng = new Random();
        int count = GameState.MIN_FUEL_STATIONS
                + rng.nextInt(GameState.MAX_FUEL_STATIONS - GameState.MIN_FUEL_STATIONS + 1);

        List<Cell> roads = grid.getRoadCells();
        // exclude base + hospitals
        for (int r = BASE_ROW; r < BASE_ROW + BASE_ROWS; r++) {
            for (int c = BASE_COL; c < BASE_COL + BASE_COLS; c++) {
                roads.remove(grid.get(r, c));
            }
        }
        for (Hospital h : hospitals) roads.remove(h.location);
        java.util.Collections.shuffle(roads, rng);

        int placed = 0;
        for (Cell c : roads) {
            if (placed >= count) break;
            // require at least one tile of distance from every hospital
            boolean tooClose = false;
            for (Hospital h : hospitals) {
                if (Math.abs(c.row - h.location.row) + Math.abs(c.col - h.location.col) < 2) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;
            c.terrain = TerrainType.FUEL_STATION;
            placed++;
        }
    }

    /**
     * Spawns one vehicle per hospital, starting with a Truck and
     * alternating Drone/Truck. All starting vehicles are pre-loaded
     * with supplies so the player can dispatch immediately. Excess
     * vehicles beyond the base zone spill out below the base.
     */
    private void spawnVehicles() {
        int n = hospitals.size();
        // alternate: index 0 = truck, 1 = drone, 2 = truck, ...
        for (int i = 0; i < n; i++) {
            Vehicle v;
            if (i % 2 == 0) {
                v = new Truck(BASE_ROW, BASE_COL);
            } else {
                v = new Drone(BASE_ROW, BASE_COL);
            }
            v.loadSupplies(); // start loaded
            // place vehicles in the base zone (4 cells for 2x2 base);
            // overflow spills below the base, one vehicle per cell
            int r, c;
            if (i < BASE_ROWS * BASE_COLS) {
                r = BASE_ROW + i / BASE_COLS;
                c = BASE_COL + i % BASE_COLS;
            } else {
                // overflow: stack vertically below the base
                r = BASE_ROW + BASE_ROWS + (i - BASE_ROWS * BASE_COLS);
                c = BASE_COL;
                if (r >= ROWS) r = BASE_ROW; // map too small, fallback
            }
            v.setPosition(r, c);
            vehicles.add(v);
        }
    }

    /**
     * Turns repair mode on or off. When on, clicking a debris tile
     * converts it to a road tile (at a budget cost).
     *
     * @param on {@code true} to enable repair mode
     */
    public void setRepairMode(boolean on) {
        this.repairMode = on;
        repaint();
    }

    /**
     * Selects the first vehicle matching the given type, if any.
     *
     * @param type the vehicle class to look for (e.g. {@code Drone.class})
     */
    public void selectVehicleOfType(Class<? extends Vehicle> type) {
        for (Vehicle v : vehicles) {
            if (type.isInstance(v)) {
                selectedVehicle = v;
                break;
            }
        }
        repaint();
    }

    /** @return the current game state */
    public GameState getState() { return state; }

    /** @return the currently selected vehicle, or {@code null} */
    public Vehicle getSelectedVehicle() { return selectedVehicle; }

    /** @return the seconds remaining before the Golden Hour ends */
    public int getRemainingSeconds() {
        return Math.max(0, (GOLDEN_HOUR_TICKS - tickCount) * TICK_MS / 1000);
    }

    /** @return {@code true} if repair mode is currently active */
    public boolean isRepairMode() { return repairMode; }

    /** @return the list of all hospitals in the mission */
    public List<Hospital> getHospitals() { return hospitals; }

    /** @return the list of all vehicles in the mission */
    public List<Vehicle> getVehicles() { return vehicles; }

    // ---------------- game loop ----------------

    private void tick() {
        if (state.isGameOver()) return;
        tickCount++;

        // exhaust countdown for resting vehicles
        for (Vehicle v : vehicles) v.tickExhaustion();

        if (tickCount % MOVE_EVERY_N_TICKS == 0) {
            for (Vehicle v : vehicles) advanceVehicle(v);
        }

        if (tickCount % GROWTH_EVERY_N_TICKS == 0) {
            for (Hospital h : hospitals) {
                h.growNeed(1.15); // exponential escalation
                if (h.getNeed() > GameState.HOSPITAL_DANGER_THRESHOLD) {
                    state.damageHealth((h.getNeed() - GameState.HOSPITAL_DANGER_THRESHOLD) * 0.05);
                }
            }
        }

        // tick down any active emergency spikes
        for (Hospital h : hospitals) h.tickSpike(state);

        // schedule and fire emergency spikes
        if (tickCount >= nextSpikeTick) {
            triggerRandomSpike();
            nextSpikeTick = tickCount + GameState.SPIKE_INTERVAL_TICKS;
        }

        if (tickCount >= GOLDEN_HOUR_TICKS || state.getGlobalHealth() <= 0) {
            state.setGameOver(true);
            SwingUtilities.invokeLater(this::showGameOverDialog);
        }

        hudUpdater.run();
        repaint();
    }

    /**
     * Triggers an emergency spike on a random non-spiking hospital.
     */
    private void triggerRandomSpike() {
        List<Hospital> candidates = new ArrayList<>();
        for (Hospital h : hospitals) {
            if (!h.isSpikeActive()) candidates.add(h);
        }
        if (candidates.isEmpty()) return;
        Hospital target = candidates.get(spikeRng.nextInt(candidates.size()));
        target.triggerSpike(2.5); // need jumps to 2.5x current value
    }

    private void advanceVehicle(Vehicle v) {
        if (!v.isBusy()) return;
        Cell next = v.getPath().get(v.getPathIndex());
        int cost = v.moveCost(next);

        if (!v.hasFuelFor(cost)) {
            v.setPath(null); // stranded - out of fuel, mission cancelled
            Sounds.playOnce("error.wav");
            return;
        }

        v.consumeFuel(cost);
        state.addFuelSpent(cost);
        v.setPosition(next.row, next.col);
        v.advancePathIndex();

        if (!v.isBusy()) { // just arrived at the final cell of the path
            handleArrival(v, next);
        }
    }

    /**
     * Called whenever a vehicle finishes its path. Routes the arrival
     * to the correct handler (base pickup, hospital delivery, or plain
     * idle if neither).
     */
    private void handleArrival(Vehicle v, Cell cell) {
        // arrival at any base cell - load supplies if vehicle is empty
        if (isBaseCell(cell.row, cell.col)) {
            if (v.getLoadState() == Vehicle.LoadState.EMPTY) {
                v.loadSupplies();
                Sounds.playOnce("delivery.wav");
            }
            return;
        }

        // arrival at a fuel station - refuel (small fee)
        if (cell.terrain == TerrainType.FUEL_STATION) {
            if (v.getFuel() < v.getMaxFuel()) {
                if (GameState.FUEL_STATION_COST == 0 || state.spendBudget(GameState.FUEL_STATION_COST)) {
                    v.refuel();
                } else {
                    Sounds.playOnce("error.wav"); // not enough budget
                }
            }
            return;
        }

        // arrival at a hospital - unload if carrying supplies
        for (Hospital h : hospitals) {
            if (h.location == cell) {
                if (v.getLoadState() == Vehicle.LoadState.LOADED) {
                    double saved = h.serve();
                    state.addLivesSaved(saved);
                    v.unloadSupplies(GameState.VEHICLE_REST_TICKS);
                    Sounds.playOnce("delivery.wav");
                } else {
                    Sounds.playOnce("error.wav"); // arrived with no supplies
                }
                return;
            }
        }
        // arrived at a non-special cell - just stop
    }

    private void showGameOverDialog() {
        int score = state.missionScore();
        boolean newBest = HighScore.recordIfBest(score);
        int best = HighScore.get();
        String msg = String.format(
            "Mission complete!%n%nLives saved: %.0f%nFuel spent: %.0f%nEfficiency: %.2f%nMission score: %d%nRemaining health: %.0f%n%n%s%nBest score: %d",
            state.getLivesSaved(), state.getFuelSpent(), state.efficiency(),
            score, state.getGlobalHealth(),
            newBest ? "*** NEW BEST SCORE! ***" : "Best score so far:",
            best);
        JOptionPane.showMessageDialog(this, msg, "RescuED - Golden Hour Ended", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------- input ----------------

    private void handleClick(int x, int y) {
        int cs = Math.max(1, currentCellSize);
        int col = x / cs;
        int row = y / cs;
        Cell clicked = grid.get(row, col);
        if (clicked == null || state.isGameOver()) return;

        if (repairMode) {
            if (clicked.terrain == TerrainType.DEBRIS) {
                if (state.spendBudget(GameState.REPAIR_COST)) {
                    clicked.terrain = TerrainType.ROAD;
                } else {
                    Sounds.playOnce("error.wav"); // not enough budget
                }
            }
            hudUpdater.run();
            repaint();
            return;
        }

        // clicking a vehicle selects it (and cancels its current path)
        for (Vehicle v : vehicles) {
            if (v.getRow() == row && v.getCol() == col) {
                if (v.canAcceptOrders()) {
                    selectedVehicle = v;
                    repaint();
                    return;
                } else {
                    Sounds.playOnce("error.wav");
                    return;
                }
            }
        }

        // otherwise, if a vehicle is selected, dispatch it toward the clicked cell
        if (selectedVehicle != null) {
            if (!selectedVehicle.canAcceptOrders()) {
                Sounds.playOnce("error.wav");
                return;
            }
            Cell start = grid.get(selectedVehicle.getRow(), selectedVehicle.getCol());
            if (start == clicked) {
                selectedVehicle.setPath(null);
                repaint();
                return;
            }
            List<Cell> path = Pathfinder.findPath(grid, start, clicked, selectedVehicle);
            if (path == null) {
                Sounds.playOnce("error.wav");
            } else {
                selectedVehicle.setPath(path);
            }
            repaint();
        }
    }

    // ---------------- drawing ----------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // recompute cell size based on current panel size
        int availableW = getWidth();
        int availableH = getHeight() - LABEL_HEIGHT;
        int size = Math.min(availableW / COLS, availableH / ROWS);
        currentCellSize = Math.max(MIN_CELL_SIZE, size);

        drawTerrain(g2);
        drawFuelStationLabels(g2);
        drawPathHighlight(g2);
        drawBase(g2);
        drawHospitals(g2);
        drawVehicles(g2);
        drawHoverHighlight(g2);
    }

    private void drawTerrain(Graphics2D g2) {
        int cs = currentCellSize;
        for (int r = 0; r < grid.rows; r++) {
            for (int c = 0; c < grid.cols; c++) {
                Cell cell = grid.get(r, c);
                Image terrainImg = cell.terrain.getImage();
                int x = c * cs, y = r * cs;
                if (terrainImg != null) {
                    g2.drawImage(terrainImg, x, y, cs + 1, cs + 1, null);
                } else {
                    g2.setColor(cell.terrain.color);
                    g2.fillRect(x, y, cs, cs);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawRect(x, y, cs, cs);
                }
            }
        }
    }

    /**
     * Draws a "Fuel Station" label below every fuel station tile so
     * the player can identify them at a glance. Falls back to a small
     * indicator inside the cell if drawing outside the panel bounds
     * (e.g. when the station is on the bottom row).
     */
    private void drawFuelStationLabels(Graphics2D g2) {
        int cs = currentCellSize;
        g2.setColor(new Color(180, 240, 250));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(8f, cs * 0.12f)));
        for (int r = 0; r < grid.rows; r++) {
            for (int c = 0; c < grid.cols; c++) {
                Cell cell = grid.get(r, c);
                if (cell.terrain != TerrainType.FUEL_STATION) continue;
                int x = c * cs;
                int y = r * cs;
                int labelY = y + cs + 10;
                if (labelY > getHeight() - 2) {
                    // would draw outside the panel - put a small "F" badge inside
                    g2.setColor(new Color(20, 20, 20));
                    g2.fillRoundRect(x + cs - 18, y + 4, 14, 14, 4, 4);
                    g2.setColor(new Color(180, 240, 250));
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    g2.drawString("F", x + cs - 14, y + 15);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(8f, cs * 0.12f)));
                } else {
                    g2.drawString("Fuel Station", x + 4, labelY);
                }
            }
        }
    }

    private void drawPathHighlight(Graphics2D g2) {
        if (selectedVehicle == null || selectedVehicle.getPath() == null) return;
        int cs = currentCellSize;
        g2.setColor(new Color(255, 255, 0, PATH_HIGHLIGHT_ALPHA));
        List<Cell> path = selectedVehicle.getPath();
        int fromIndex = Math.max(0, selectedVehicle.getPathIndex() - 1);
        for (int i = fromIndex; i < path.size(); i++) {
            Cell c = path.get(i);
            int inset = Math.max(4, cs / 9);
            g2.fillRect(c.col * cs + inset, c.row * cs + inset,
                    cs - 2 * inset, cs - 2 * inset);
        }
    }

    private void drawBase(Graphics2D g2) {
        Image baseImg = Assets.load("base.png");
        int cs = currentCellSize;

        // shared border around the whole base zone
        int zoneX = BASE_COL * cs;
        int zoneY = BASE_ROW * cs;
        int zoneW = BASE_COLS * cs;
        int zoneH = BASE_ROWS * cs;
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(zoneX + 1, zoneY + 1, zoneW - 2, zoneH - 2);
        g2.setStroke(new BasicStroke(1f));

        // base icon + label per cell
        for (int r = BASE_ROW; r < BASE_ROW + BASE_ROWS; r++) {
            for (int c = BASE_COL; c < BASE_COL + BASE_COLS; c++) {
                int x = c * cs;
                int y = r * cs;
                int inset = Math.max(4, cs / 6);
                if (baseImg != null) {
                    g2.drawImage(baseImg, x + inset, y + inset, cs - 2 * inset, cs - 2 * inset, null);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(x + inset, y + inset, cs - 2 * inset, cs - 2 * inset);
                    g2.setColor(Color.BLACK);
                    g2.drawString("BASE", x + 10, y + cs / 2);
                }
            }
        }

        // single label below the zone
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(9f, cs * 0.15f)));
        g2.drawString("BASE - click to load supplies",
                zoneX + 4, zoneY + zoneH + 12);
    }

    private void drawHospitals(Graphics2D g2) {
        Image hospitalImg = Assets.load("hospital.png");
        int cs = currentCellSize;
        for (Hospital h : hospitals) {
            int hx = h.location.col * cs;
            int hy = h.location.row * cs;

            // status halo
            int haloAlpha = h.isSpikeActive() ? 110 : 50;
            g2.setColor(new Color(
                    h.getStatusColor().getRed(),
                    h.getStatusColor().getGreen(),
                    h.getStatusColor().getBlue(),
                    haloAlpha));
            g2.fillOval(hx + 4, hy + 4, cs - 8, cs - 8);

            int inner = Math.max(4, cs / 6);
            if (hospitalImg != null) {
                g2.drawImage(hospitalImg, hx + inner, hy + inner, cs - 2 * inner, cs - 2 * inner, null);
            } else {
                g2.setColor(new Color(220, 30, 30));
                g2.fillOval(hx + inner + 4, hy + inner + 4, cs - 2 * (inner + 4), cs - 2 * (inner + 4));
            }

            // hospital name
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(9f, cs * 0.14f)));
            g2.drawString(h.getName(), hx + 4, hy + cs + 12);

            // need number
            String needText = String.format("%.0f", h.getNeed());
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, Math.max(11f, cs * 0.20f)));
            g2.setColor(Color.WHITE);
            g2.drawString(needText, hx + 10, hy + cs - 8);
            g2.setColor(new Color(220, 30, 30));
            g2.drawString(needText, hx + 11, hy + cs - 7);
        }
    }

    private void drawVehicles(Graphics2D g2) {
        int cs = currentCellSize;
        for (Vehicle v : vehicles) {
            int vx = v.getCol() * cs;
            int vy = v.getRow() * cs;

            // exhausted: dim the vehicle
            if (v.getLoadState() == Vehicle.LoadState.EXHAUSTED) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillOval(vx + 8, vy + 8, cs - 16, cs - 16);
            }

            int pad = Math.max(4, cs / 9);
            Image icon = v.getIcon();
            if (icon != null) {
                g2.drawImage(icon, vx + pad, vy + pad, cs - 2 * pad, cs - 2 * pad, null);
            } else {
                g2.setColor(v.getColor());
                g2.fillOval(vx + 14, vy + 14, cs - 28, cs - 28);
            }

            if (v == selectedVehicle) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(vx + pad, vy + pad, cs - 2 * pad, cs - 2 * pad);
                g2.setStroke(new BasicStroke(1f));
            }

            // fuel + supply bars
            int barWidth = cs - 16;
            int barX = vx + 8;
            int fuelY = vy + cs - 14;
            int supplyY = vy + cs - 8;

            // fuel
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, fuelY, barWidth, 4);
            g2.setColor(v.hasFuelFor(1) ? new Color(255, 180, 60) : Color.RED);
            int fuelFilled = (int) (barWidth * ((double) v.getFuel() / v.getMaxFuel()));
            g2.fillRect(barX, fuelY, fuelFilled, 4);

            // supplies (green)
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, supplyY, barWidth, 4);
            int supplyFilled = (int) (barWidth * ((double) v.getSupplies() / v.getMaxSupplies()));
            g2.setColor(new Color(80, 200, 120));
            g2.fillRect(barX, supplyY, supplyFilled, 4);
        }
    }

    private void drawHoverHighlight(Graphics2D g2) {
        if (hoverRow < 0 || hoverCol < 0) return;
        if (hoverRow >= ROWS || hoverCol >= COLS) return;
        int cs = currentCellSize;
        g2.setColor(new Color(255, 255, 255, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(hoverCol * cs + 1, hoverRow * cs + 1,
                cs - 2, cs - 2);
        g2.setStroke(new BasicStroke(1f));
    }
}
