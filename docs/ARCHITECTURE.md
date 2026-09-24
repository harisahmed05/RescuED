# Architecture

A high-level tour of the RescuED codebase for contributors.

## Design Goals

1. **Polymorphism over conditionals.** The vehicle hierarchy is designed so adding a new vehicle type requires zero changes to `Pathfinder`, `GamePanel`, or `GameFrame`.
2. **Encapsulation everywhere.** All mutable state lives behind getters / setters. The `Vehicle` base class and `GameState` are the canonical examples.
3. **Random but fair.** Every map is procedurally generated, but the generation algorithm rejects (and rerolls) any map that cannot be cleared with a Truck using the starting repair budget.
4. **No third-party dependencies.** The entire game builds with only the JDK on the classpath - no Maven Central, no Gradle plugins beyond `maven-compiler-plugin`, no transitive jars.

## High-Level Architecture

```
                     ┌────────────────────┐
                     │       Main         │
                     │ (entry point)      │
                     └─────────┬──────────┘
                               │
                               ▼
                     ┌────────────────────┐
                     │    GameFrame       │
                     │  (JFrame + UI)     │
                     └─────────┬──────────┘
                               │ owns
                               ▼
        ┌──────────────────────────────────────┐
        │            GamePanel                 │
        │  (rendering, game loop, input)       │
        └──┬─────────────┬─────────────┬────────┘
           │ owns        │ owns        │ owns
           ▼             ▼             ▼
     ┌──────────┐  ┌──────────┐  ┌──────────┐
     │   Grid   │  │ Vehicles │  │Hospitals │
     └──────────┘  └────┬─────┘  └──────────┘
                        │
                  ┌─────┴──────┐
                  │  Vehicle   │ (abstract)
                  └─────┬──────┘
                  ┌─────┴──────┐
                  │            │
              ┌───▼──┐    ┌────▼───┐
              │Drone │    │ Truck  │
              └──────┘    └────────┘
```

### Threading Model

Everything runs on the Swing Event Dispatch Thread (EDT). The game loop is a `javax.swing.Timer` that fires `tick()` every 150 ms. Mouse events arrive on the EDT via the `MouseAdapter`. There is no background worker thread - all state mutations happen inside the Swing thread, so we don't need any locks.

### Update Cycle

```
Timer fires every 150ms
        │
        ▼
    GamePanel.tick()
        │
        ├── for each vehicle: advance one cell along its path
        ├── for each hospital: grow need by 1.15×
        ├── tick down active spikes
        ├── maybe trigger a new emergency spike
        ├── check game-over (timer or health)
        └── repaint() and refresh sidebar labels
```

### Reachability Guarantee Flow

```
newGame()
    │
    ├── attempt 1..MAX_REGEN_ATTEMPTS
    │       ├── generate random Grid
    │       ├── clear base zone
    │       ├── pick hospital cells
    │       ├── thicken obstacles around hospitals
    │       └── check reachability (BFS avoiding flood)
    │              │
    │              ├── pass → use this map, break
    │              └── fail → save as best, retry
    │
    └── after loop: use best map seen, or last attempt
```

## Key Class Responsibilities

| Class | Lines (approx) | Responsibility |
|---|---|---|
| `Main` | 30 | Program entry; schedules `GameFrame` construction on the EDT. |
| `GameFrame` | 200 | JFrame, sidebar HUD, dispatch / repair / mute / restart buttons. |
| `GamePanel` | 850 | Grid rendering, game loop, mouse input, vehicle + hospital arrival handling. |
| `Grid` | 130 | Terrain generation, neighbour lookups, BFS reachability check, corridor-aware obstacle thickening. |
| `Cell` | 35 | Single map tile: row, col, mutable terrain. |
| `TerrainType` | 50 | Enum of terrain kinds with weight, color, image filename. |
| `Vehicle` | 170 | Abstract base: fuel, supplies, load state, path. Polymorphic `canEnter` / `moveCost`. |
| `Drone` | 30 | Flies over everything, small payload (2 supplies, 25 fuel). |
| `Truck` | 30 | Pays full terrain cost, can't cross flood / debris / building. Big payload (6 supplies, 60 fuel). |
| `Hospital` | 110 | Growing need, emergency spike mechanic, status color. |
| `GameState` | 90 | Mission-wide numbers (budget, health, lives saved, fuel spent, score). |
| `HighScore` | 80 | Persistent best score at `~/.rescued/highscore.txt`. |
| `Pathfinder` | 80 | Dijkstra's algorithm using a `PriorityQueue`. |
| `Assets` | 70 | PNG loader + cache + auto-crop to opaque bounds. |
| `Sounds` | 80 | WAV playback for one-shot effects and looping music. |

## State Ownership

```
GamePanel (owns)
    │
    ├── grid : Grid                  // immutable rows/cols + mutable cells
    ├── state : GameState            // mission-wide numbers
    ├── vehicles : List<Vehicle>     // owned, mutated by advanceVehicle
    ├── hospitals : List<Hospital>   // owned, mutated by growth/spike/serve
    ├── selectedVehicle : Vehicle    // UI selection (null if none)
    ├── repairMode : boolean         // UI state
    ├── tickCount : int              // wall-clock for animations
    └── nextSpikeTick : int          // when to fire the next emergency spike

GameState (owned by GamePanel)
    ├── budget
    ├── globalHealth
    ├── livesSaved
    ├── fuelSpent
    └── gameOver

Vehicle (owned by GamePanel)
    ├── name / color / iconFile       // immutable
    ├── maxFuel / fuel                // mutable
    ├── maxSupplies / supplies        // mutable
    ├── loadState / exhaustionTicks   // mutable
    ├── row / col                     // mutable
    └── path / pathIndex              // mutable, null when idle

Hospital (owned by GamePanel)
    ├── location                      // immutable
    ├── name                          // immutable
    ├── need                          // mutable
    └── spikeActive / spikeTicks      // mutable
```

## Why `extends JPanel` instead of `BufferedImage` rendering?

`GamePanel` paints directly with `Graphics2D` inside `paintComponent` for simplicity. The grid is only 9 × 12 = 108 cells, so the rendering cost is negligible - no need for an offscreen buffer. If the project ever grew to a much larger map, switching to a `BufferedImage` and `BufferStrategy` would be the next optimization step.

## Why `javax.swing.Timer` instead of `ScheduledExecutorService`?

Two reasons:

1. `javax.swing.Timer` fires on the EDT, which is where all Swing updates must happen anyway. Using `ScheduledExecutorService` would require marshalling every paint back to the EDT.
2. It's one fewer concurrency primitive to reason about. There are no locks, no race conditions, and no thread-safety annotations needed in the model classes.

The trade-off is that `Timer` doesn't run while the window is minimized or the OS thinks the app is in the background. For a real-time game this is the correct behaviour - we don't want to drain fuel while the player is alt-tabbed.

## Why a custom BFS for reachability instead of reusing `Pathfinder`?

The fairness check in `Grid.shortestTruckReachablePath` needs to find *some* path from base to hospital even through debris (so we can count how much repair is needed). The regular `Pathfinder.findPath` returns `null` for unreachable targets. If we used `Pathfinder` we'd conflate "flood-isolated" (truly unreachable even with infinite budget) with "debris-blocked" (reachable after paying for repairs). The custom BFS makes that distinction explicit.
