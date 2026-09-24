# Changelog

All notable changes to RescuED are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-beta] - 2026-09-24

**Beta release.** The game is fully playable end-to-end: map generation,
pathfinding, supply cycle, emergency spikes, fuel stations, repair mode,
and persistent high score all work. This release exists to gather
feedback on gameplay balance, UI clarity, and overall fun before locking
in a 1.0 API.

Expect:
- Balance changes (hospital growth rate, repair cost, fuel prices, etc.)
- Possible breaking changes to internal class names or game constants
- New features being added on top of the current systems

Do NOT expect:
- A polished tutorial / onboarding flow
- Mobile / web ports
- Localisation beyond English

### Added
- Initial release of RescuED
- Real-time disaster-relief logistics simulation built in Java Swing
- Dijkstra-based pathfinding with automatic route selection per vehicle
- Two vehicle classes with polymorphic movement rules:
  - **Drone** - flies over absolutely anything (including buildings), small fuel + supply capacity
  - **Truck** - pays full terrain cost, can't cross flood / debris / buildings, large capacity
- Procedurally generated random maps (with optional fixed seed for reproducibility)
- Repair-budget reachability guarantee - the map is rerolled up to 30 times until every hospital is reachable by a Truck using at most the starting repair budget
- Random spawn of 2–5 hospitals per mission, scattered across the map
- One vehicle per hospital, alternating Truck / Drone
- All vehicles start pre-loaded with supplies
- 2×2 base zone in the top-left of the map (multi-cell supply depot)
- 1–3 random fuel stations per mission, with on-screen labels and $15 refuel cost
- Random extra debris in the 3×3 neighborhood of every hospital (the final approach is the hardest part)
- Emergency spike mechanic - every ~15 seconds a random hospital flashes red; reach it in time for a 1.5× lives-saved bonus, or take 60 global-health damage
- Vehicle exhaustion - a vehicle that just delivered must rest for ~2.4 seconds before it can move again
- Supply load / unload cycle - vehicles must pick up supplies at the Base, deliver them, then return to Base
- Persistent best score saved to `~/.rescued/highscore.txt`, shown in the HUD
- Game-over dialog with mission summary and "NEW BEST SCORE!" banner
- Stretchable UI - drag the window edges and the grid scales to fit
- "Reset Window Size" button to restore the default window size
- Mission HUD with budget + health progress bars, color-coded by severity
- Mouse hover highlight on the cell under the cursor
- Per-vehicle fuel bar (orange) and supply bar (green)
- Status halos around hospitals (green / yellow / orange / red)
- Exhausted-vehicle translucent overlay
- Background music loop and one-shot sound effects
- Automatic fallback when asset files are missing (silent continue with shape-based drawing)
- Mouse hover feedback and live HUD refresh on every game tick

### Documentation
- Full README with Features, Gameplay, Project Structure, OOP Design, Build & Run, Controls, Roadmap, Contributing, and License sections
- MIT License
- Contributing guidelines
- Maven `pom.xml` build configuration
- `.gitignore` for Java / Maven / IDE files

### Asset Notes
- The artwork in `src/main/resources/images/` and the sounds in `src/main/resources/sounds/` are sourced from various free / royalty-free sources on the web (public-domain tile sets, free sound-effect libraries, AI-generated art). Original sources claimed no license / public-domain usage; specific per-asset attribution was not tracked. See the **Acknowledgments** section of the README for the full note and a contact path for attribution or removal requests.

### Technical
- 20 Java classes across one package (`com.rescued.simulation`)
- Maven `src/main/java` + `src/main/resources` directory layout
- Uses only the JDK standard library (no third-party dependencies)
- Verified to compile cleanly on OpenJDK 11, 17, and 27
- Reachability smoke-tested across 100 random seeds (75–95 % first-attempt pass rate)
- High score persistence smoke-tested across cache resets

[0.1.0-beta]: https://github.com/harisahmed05/RescuED/releases/tag/v0.1.0-beta
