# Gameplay Guide

A deeper look at the game mechanics for players and contributors.

## The Mission Loop

Every mission follows the same rhythm:

```
1. SPAWN
   ├── 2-5 hospitals appear at random road cells
   ├── 1-3 fuel stations appear at random road cells
   ├── One vehicle per hospital (Truck / Drone alternating)
   └── All vehicles start at the base, pre-loaded with supplies

2. PLAY
   ├── Dispatch a LOADED vehicle to a hospital
   ├── On arrival: vehicle unloads, banked lives saved, EXHAUSTED
   ├── Wait ~2.4 seconds for exhaustion to end
   ├── Dispatch back to base → supplies reloaded → LOADED again
   └── Repeat

3. WIN / LOSE
   ├── WIN: survive 90 seconds with positive global health
   ├── LOSE: global health reaches 0
   └── Either way: mission score saved if it's the new best
```

## Vehicle Reference

### Drone

| Stat | Value |
|---|---|
| Fuel | 25 |
| Supply capacity | 2 units |
| `moveCost` | always 1 (ignores terrain) |
| `canEnter` | any terrain except BUILDING |
| Visual | green circle with a quadcopter icon |

The drone's role is the nimble scout / emergency responder. It can fly over floods and debris to reach a hospital that the truck cannot, but it carries very few supplies and burns through fuel fast on long routes.

### Truck

| Stat | Value |
|---|---|
| Fuel | 60 |
| Supply capacity | 6 units |
| `moveCost` | full terrain weight (ROAD=1, MUD=3, FUEL_STATION=1) |
| `canEnter` | ROAD, MUD, FUEL_STATION |
| Visual | orange / amber circle with a box-truck icon |

The truck is the workhorse. It carries three times the supplies of a drone and has more than twice the fuel, but it cannot cross flood or debris. Use it for the long hauls where the path is clear, and use Repair Mode to clear debris ahead of it.

## Terrain Reference

| Terrain | Truck cost | Drone cost | Passable by truck? | Notes |
|---|---|---|---|---|
| ROAD | 1 | 1 | yes | Cheapest path. |
| MUD | 3 | 1 | yes | Slow but passable. |
| FLOOD | 5 (blocked) | 1 | **no** | Cannot be cleared - reroute or send drone. |
| DEBRIS | 4 (blocked) | 1 | **no** | Click in Repair Mode to clear ($40). |
| BUILDING | impassable | impassable | **no** | Permanent obstacle for every vehicle. |
| FUEL_STATION | 1 | 1 | yes | Drive over to refuel ($15). |

## Emergency Spike Lifecycle

```
Every ~15 seconds:

  triggerRandomSpike()
       │
       ├── pick a random non-spiking hospital
       ├── hospital.need *= 2.5   // need jumps sharply
       └── hospital.spikeActive = true, countdown = 80 ticks (~12 s)

  While spike is active:
       ├── hospital flashes red
       ├── countdown decrements each tick
       └── reaching the hospital during the spike: lives_saved × 1.5

  If countdown reaches 0:
       ├── spike ends
       └── global health -= 60
```

The risk / reward is intentional: chasing every spike is unsustainable, but ignoring every spike will drain global health. The player has to triage.

## Repair-Budget Reachability

After the random map is generated, the game validates:

```
for each hospital:
    path = BFS(base, hospital, allow={road, mud, debris, fuel_station})
    if path == null:
        reroll the map   # flood-isolated
    else:
        total_debris += count(debris tiles in path)
        if total_debris * REPAIR_COST > STARTING_BUDGET:
            reroll the map   # too expensive to repair
```

After up to **30** regeneration attempts the most-recent map is accepted as a fallback (this almost never triggers - smoke tests show ~95 % first-attempt success rate).

The implication for players: **you can always win with a Truck if you spend your repair budget wisely**. The challenge is spending it efficiently, not finding a hidden shortcut.

## Mission Score

```
missionScore = livesSaved - (fuelSpent / 10)
```

Two players who save the same number of lives but one uses less fuel will score higher. **Efficiency** (lives saved per unit fuel) is shown alongside the score so you can see which lever to pull.

## High Score Persistence

Your best score is saved to `~/.rescued/highscore.txt` (created automatically on the first new record). On Windows the file lives at `%USERPROFILE%\.rescued\highscore.txt`. If the file can't be written (e.g. read-only filesystem), the score is kept in memory for the current session only.
