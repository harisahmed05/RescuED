# Contributing to RescuED

Thank you for your interest in contributing to **RescuED**! This document covers everything you need to know to get started.

## Code of Conduct

By participating in this project you agree to be respectful and constructive. Be kind to other contributors, accept feedback gracefully, and focus on what is best for the game and its players.

## How to Report a Bug

1. Check the [issue tracker](https://github.com/harisahmed05/RescuED/issues) to see if the bug has already been reported.
2. If not, open a new issue and include:
   - Your operating system and Java version (`java -version`)
   - How you built the project (Maven / plain `javac` / IDE)
   - Steps to reproduce the bug
   - What you expected to happen
   - What actually happened
   - A screenshot if it's a visual bug
3. If the bug involves a specific map, mention the seed (if you can reproduce it) - the `Grid(int, int, long)` constructor takes a seed.

## How to Suggest a Feature

Open an issue with the **`enhancement`** label and describe:
- What problem the feature solves
- How you imagine it working in-game
- Any UI mockups (even ASCII art helps)

## Development Setup

### Prerequisites

- **JDK 11 or newer** (OpenJDK works fine; the project has no other runtime dependencies)
- **Maven 3.6+** *only if you want to use the Maven build*
- An IDE - IntelliJ IDEA, Eclipse, or VS Code with the Java Extension Pack all work

### Building

```bash
# Maven
mvn compile

# Plain javac
mkdir -p build
javac -d build src/main/java/com/rescued/simulation/*.java
cp -r src/main/resources build/
```

### Running Tests

This project currently has no automated test suite. Reachability, high-score persistence, and other logic are verified manually via small smoke-test programs (you can see the patterns in the development history).

Contributions that add a JUnit test suite are very welcome - the natural starting points would be:
- `GridTest` - terrain generation, neighbor lookups, `shortestTruckReachablePath`
- `PathfinderTest` - Dijkstra correctness on known small grids
- `VehicleTest` - drone vs truck `canEnter` and `moveCost` matrices
- `HighScoreTest` - persistence and cache reset behavior

### Coding Style

- **Java**: standard Oracle / Google style. 4-space indent, no tabs.
- **Comments**: every public class and every public method should have a Javadoc comment. Private helpers can have a one-line comment.
- **Naming**: classes `PascalCase`, methods / fields `camelCase`, constants `SCREAMING_SNAKE_CASE`, packages lowercase.
- **One responsibility per class**. If a class is over ~300 lines it probably needs splitting.
- **Prefer composition over inheritance.** The `Vehicle` hierarchy is the one exception, and it's intentional for polymorphism.
- **No third-party runtime dependencies.** Everything must build with only the JDK on the classpath.

### Pull Request Workflow

1. **Fork** the repository.
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/my-cool-thing
   ```
3. **Make your changes.** Keep commits small and focused. A good commit message looks like:
   ```
   Add helicopter vehicle class
   
   - New Helicopter extends Vehicle with canEnter: !BUILDING and moveCost: 2
   - Spawn a Helicopter at the base alongside the Drone and Truck
   - Document the new vehicle in README
   ```
4. **Build and test locally** before pushing:
   ```bash
   mvn clean compile
   ```
5. **Push your branch** and open a pull request against `main`.
6. **Describe your change** in the PR body: what it does, why, screenshots if visual.
7. Be prepared to iterate on review feedback.

## Project Structure

```
src/main/java/com/rescued/simulation/
├── Main.java          # entry point
├── GameFrame.java     # main window + sidebar UI
├── GamePanel.java     # game grid + game loop
├── Grid.java          # map generation + neighbor lookups
├── Cell.java          # one map tile
├── TerrainType.java   # enum: ROAD, MUD, FLOOD, DEBRIS, BUILDING, FUEL_STATION
├── Vehicle.java       # abstract base, LoadState enum
├── Drone.java         # flies over everything, small payload
├── Truck.java         # pays full terrain cost, large payload
├── Hospital.java      # destination with growing need + emergency spikes
├── GameState.java     # mission-wide numbers (budget, health, score)
├── HighScore.java     # persistent best score
├── Pathfinder.java    # Dijkstra's algorithm
├── Assets.java        # PNG loader + cache
└── Sounds.java        # WAV player
```

When you add a new feature, place new classes in this package and update the **Project Structure** section of `README.md`.

## Adding a New Vehicle Type

The cleanest way to add a new vehicle (e.g. a Helicopter) is:

1. Add a new constructor to `TerrainType` if it has a unique terrain (you usually don't need to).
2. Create a `Helicopter extends Vehicle` class overriding `canEnter` and `moveCost`.
3. Optionally tweak fuel and supply capacity in the super-constructor call.
4. Add an `iconFile` (`helicopter.png`) to `src/main/resources/images/`.
5. Wire it into `GamePanel.spawnVehicles()` (and consider the vehicle-scaling rule).

The pathfinder, click handlers, and rendering will pick it up automatically - that's the point of the polymorphic contract.

## Adding a New Terrain Type

1. Add the new constant to `TerrainType` with weight, color, and image filename.
2. Update `Truck.canEnter` / `Drone.canEnter` if the new terrain has different rules per vehicle.
3. Update `Grid.shortestTruckReachablePath` if the terrain should block trucks entirely.
4. Update the smoke test in the development workflow to confirm reachability still passes.
5. Add a `terrain.png` asset to `src/main/resources/images/`.

## Releasing a New Version

1. Bump the `<version>` in `pom.xml`.
2. Add a new section to the top of `CHANGELOG.md`.
3. Open a PR titled `Release vX.Y.Z`.
4. After merge, tag the commit and push the tag:
   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

## Questions?

Open an issue or email the maintainer at `ahsanharisahmed@gmail.com`.
