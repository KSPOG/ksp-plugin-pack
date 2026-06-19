# KSP WebWalker Teleport API

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.9"
```

## Concept

Teleports are treated as weighted route options.

For each registered teleport, the planner calculates:

```text
currentDistance = distance(player, finalTarget)
postTeleportDistance = distance(teleportLandingTile, finalTarget)
savingDistance = currentDistance - postTeleportDistance
score = teleportFixedCost + postTeleportDistance * teleportWalkCostMultiplier
```

A teleport can be selected only when:

```text
teleportsEnabled == true
teleport requirement returns true
savingDistance >= minTeleportSavingDistance
teleport cooldown has passed
player is not already moving
```

The lowest score wins.

---

## Main classes

```java
KspTeleportType
KspTeleportOption
KspTeleportPlan
KspTeleportRegistry
KspTeleportPlanner
KspTeleportExecutor
```

---

## Register a spell teleport

```java
walker.getTeleportRegistry().add(
    KspTeleportOption.spell(
        "spell:varrock",
        "Varrock Teleport",
        new WorldPoint(3212, 3424, 0),
        35,
        () -> hasVarrockTeleportRunesAndLevel(),
        () ->
        {
            boolean cast = castVarrockTeleport();

            if (!cast)
            {
                return KspWalkResult.failed(null, "Failed to cast Varrock Teleport");
            }

            return KspWalkResult.waiting(null, "Casting Varrock Teleport");
        }
    )
);
```

The walker core does not hardcode `Rs2Magic` calls because Microbot method signatures can differ by version. Put the exact spell-cast code inside the lambda.

---

## Register an item teleport

```java
walker.getTeleportRegistry().add(
    KspTeleportOption.item(
        "item:ring_of_dueling:castle_wars",
        "Ring of dueling -> Castle Wars",
        new WorldPoint(2440, 3090, 0),
        20,
        () -> hasRingOfDueling(),
        () ->
        {
            boolean used = useRingOfDuelingCastleWars();

            if (!used)
            {
                return KspWalkResult.failed(null, "Failed to use Ring of dueling");
            }

            return KspWalkResult.waiting(null, "Using Ring of dueling");
        }
    )
);
```

---

## Register a custom teleport

```java
walker.getTeleportRegistry().add(
    KspTeleportOption.custom(
        "custom:chronicle",
        "Chronicle teleport",
        new WorldPoint(3206, 3359, 0),
        15,
        () -> hasChronicleCharge(),
        () ->
        {
            boolean used = useChronicle();

            if (!used)
            {
                return KspWalkResult.failed(null, "Failed to use Chronicle");
            }

            return KspWalkResult.waiting(null, "Using Chronicle");
        }
    )
);
```

---

## Settings

```java
.teleportsEnabled(true)
.minTeleportSavingDistance(40)
.teleportWalkCostMultiplier(1)
.teleportCooldownMs(8000)
.teleportPostCastWaitMs(4500)
```

### `teleportsEnabled`

Master toggle.

### `minTeleportSavingDistance`

Teleport must save at least this many tiles.

Default:

```text
40
```

### `teleportWalkCostMultiplier`

Weight applied to walking distance after teleport.

Default:

```text
1
```

### `teleportCooldownMs`

Minimum delay between teleport attempts.

Default:

```text
8000
```

### `teleportPostCastWaitMs`

Reserved setting for your plugin-specific teleport actions.

The core stores the setting, but your lambda decides how long to wait, because different spells/items require different transition handling.

---

## Accessors

```java
walker.getTeleportRegistry()
walker.getLastTeleportName()
walker.getLastTeleportDestination()
walker.getLastTeleportPlan()
```

Example:

```java
KspTeleportPlan plan = walker.getLastTeleportPlan();

if (plan != null)
{
    log.info("Teleport used: {} saved {} tiles",
        plan.getOption().getName(),
        plan.getSavingDistance()
    );
}
```

---

## Recommended costs

Use lower cost for cheap/reliable teleports and higher cost for expensive or slow teleports.

```text
Home teleport: 80-120
Basic spell teleport: 30-50
Charged jewelry teleport: 15-35
Tablet teleport: 25-45
Quest/slow teleport: 60+
```

The planner minimizes:

```text
cost + remaining walking distance
```

So a cheap teleport landing slightly farther away may beat an expensive teleport landing closer.

---

## Notes

The teleport API calculates whether a teleport is useful. It does not know your inventory, spellbook, rune pouch, equipment, charges, or spellbook state unless you provide that logic in the requirement lambda.

That is intentional so the same walker core can support:

```text
spell teleports
tablet teleports
jewelry teleports
chronicle
minigame teleports
quest teleports
plugin-specific custom teleports
```


---

# v10 obstacle behavior change

Generic obstacle handling is now **fallback-only**.

Previous behavior:

```text
scan for doors/gates/stairs/ladders before every checkpoint click
```

That was too aggressive because it interacted with doors/gates/stairs the walker only passed near.

New behavior:

```text
1. try normal local checkpoint walking first
2. if a reachable checkpoint exists, walk normally
3. only scan/interact with doors/gates/stairs/ladders/trapdoors when no local checkpoint exists
4. explicit KspWebEdge object/NPC/dialogue/custom edges still execute immediately
```

This means the walker only opens/uses generic obstacles when walking is actually blocked.

Relevant file:

```text
KspMovementExecutor.java
```

The obstacle overlay message now starts with:

```text
fallback-required:
```

when a generic obstacle was used because no local step existed.


---

# v11 blocking obstacle behavior

v11 adds **blocking obstacle detection**.

The walker now uses two different generic obstacle modes:

## 1. Blocking obstacle mode

Used when the walker already found a normal reachable checkpoint.

Before clicking the checkpoint, it checks only the short segment:

```text
player -> next checkpoint
```

If a door/gate/stair/ladder/trapdoor lies physically between the player and that checkpoint, the walker interacts with it.

Overlay message prefix:

```text
blocking-required:
```

This is the case where the next checkpoint or target is behind a door.

## 2. Fallback obstacle mode

Used only when no local checkpoint can be found.

Overlay message prefix:

```text
fallback-required:
```

## New config options

```text
Blocking obstacles: true
Blocking obstacle line tolerance: 1
Blocking obstacle extra distance: 2
```

Recommended tuning:

```text
If it misses the door: increase Blocking obstacle line tolerance to 2.
If it clicks nearby wrong doors: keep tolerance at 1 and reduce extra distance.
```


## v11.1 compile fix

`KspPathObstacleHandler.java` was replaced cleanly.

Fixes:

```text
missing toBlockingObstacle(...)
Comparator<KspPathObstacle> cannot be converted to Comparator<? super Object>
```

The stream comparators now use explicit `KspPathObstacle` lambda typing.

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.11.1"
```


---

# v12 bank / Grand Exchange buttons

v12 adds config buttons:

```text
Walk nearest bank
Walk Grand Exchange
```

These are actual RuneLite config buttons using:

```java
net.runelite.client.config.Button
net.runelite.client.events.ConfigButtonClicked
```

## Behavior

### Walk nearest bank

Finds the nearest known bank target from `KspBankTargetRegistry` based on the player's current `WorldPoint`, then sets that as an external target and starts walking.

### Walk Grand Exchange

Sets the external target to:

```java
new WorldPoint(3164, 3487, 0)
```

## New classes

```text
KspBankTargetRegistry.java
KspWalkerDestination.java
KspWalkerDestinationType.java
```

## Notes

The bank registry is a static list of common banks/deposit boxes. It is not yet a full RuneScape bank database. Add more banks by editing:

```java
KspBankTargetRegistry
```

Example:

```java
add("Bank Name", x, y, plane);
```


---

# v13 minimap-preferred movement

v13 changes checkpoint clicking so the custom walker prefers the minimap for most walking.

New classes:

```text
KspMovementClicker.java
KspMovementClickMethod.java
```

New config options:

```text
Prefer minimap: true
Canvas max distance: 4
Canvas fallback: true
```

## Click policy

When `Prefer minimap` is enabled:

```text
1. If the next checkpoint is visible AND within Canvas max distance:
   use canvas.

2. Otherwise:
   use minimap.

3. If minimap fails AND the tile is visible AND Canvas fallback is enabled:
   use canvas fallback.
```

This means canvas is only used for close visible tiles or fallback cases. Longer checkpoint movement should use the minimap.

The overlay now shows:

```text
Click method
```

Values:

```text
MINIMAP
CANVAS
MINIMAP_THEN_CANVAS
CANVAS_THEN_MINIMAP
NONE
```

Note: Microbot's built-in `Rs2Walker.walkFastCanvas(...)` already uses a canvas-first policy and falls back to minimap when a tile is not on screen. v13 adds a minimap-first policy for this custom walker.


---

# v13.1 quick target compatibility fix

Some Microbot/RuneLite branches do not include:

```java
net.runelite.client.config.Button
net.runelite.client.events.ConfigButtonClicked
```

v13.1 removes the button-based config API and replaces it with a compatible enum dropdown:

```text
Quick target
```

Options:

```text
Manual coords
Nearest bank
Grand Exchange
```

Usage:

```text
1. Set Quick target to Nearest bank or Grand Exchange.
2. Enable Start walking.
```

This avoids compile errors from missing `Button` and `ConfigButtonClicked`.


## v13.2 compile fix

Fixes missing helper methods in `KspWalkerTesterScript.java`:

```java
private WorldPoint resolveConfiguredTarget(KspWalkerTesterConfig config)
private WorldPoint manualTarget(KspWalkerTesterConfig config)
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.13.2"
```


---

# v14 debug logging

v14 adds structured walker diagnostics.

New files:

```text
KspWalkerDebugLevel.java
KspWalkerDebugState.java
KspWalkerDebugLogger.java
KSP_WALKER_DEBUGGING.md
```

New config section:

```text
Debug
```

Recommended issue-report settings:

```text
Debug level: Trace
Log every walker tick: true
```

Send 20-50 lines around the issue that start with:

```text
[KSP-WALKER]
```

or:

```text
[KSP WALKER TRACE]
```

The logs now include:

```text
player
target
quick target
status/message
decision
next checkpoint
last clicked tile
click method
obstacle decision
teleport decision
route/edge state
stuck recovery state
tick duration
```


## v14.1 compile fix

Fixes `KspWebWalker.java` using a non-existing `KspWebRoute.size()` method.

Changed:

```java
route.size()
```

to:

```java
route.getEdges().size()
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.14.1"
```


---

# v15 movement/stuck fixes from log review

The logs showed:

```text
Recovery skipped; player already moving
```

right after a successful minimap click. That meant stuck recovery was being triggered too early.

v15 fixes:

```text
1. Stuck recovery no longer triggers while the player is moving or animating.
2. Local checkpoint scoring now strongly prefers real progress toward the target.
3. Sideways/weak-progress tiles are heavily penalized.
4. Local steps must make minimum target-distance progress before being preferred.
5. Shutdown clears external target and arrival state.
6. Debug active edge is cleared when route is cleared.
```

The important change is in:

```text
KspLocalPathfinder.java
KspStuckRecovery.java
```

Expected improvement:

```text
less east/west oscillation
fewer repeated clicks on the same weak checkpoint
no stuck recovery immediately after a valid movement click
```


---

# v16 debug path drawing

v16 adds visual path drawing to help diagnose bad checkpoint choices.

New file:

```text
KspDebugPathBuilder.java
```

New overlay config options:

```text
Show debug path
Show checkpoint segment
Show route edge segment
Debug path max tiles
```

## Drawn lines

### PATH

Draws a sampled straight-line path from the current player tile to the final target.

Color intent:

```text
cyan / blue
```

### NEXT-LINE

Draws the segment from the current player tile to the selected next checkpoint.

Color intent:

```text
yellow
```

### EDGE-LINE

Draws the active graph edge start-to-end segment.

Color intent:

```text
purple
```

## Why this helps

If the walker clicks bad tiles, the overlay should make it obvious whether:

```text
the straight path crosses a wall
the next checkpoint is sideways or backwards
the active edge points through the wrong area
a door/gate should be detected between player and next checkpoint
```

Recommended when testing:

```text
Show debug path: true
Show checkpoint segment: true
Show route edge segment: true
Debug path max tiles: 80
Debug level: Trace
```


---

# v17 faster local path calculation

v17 adds a fast local pathfinder so normal checkpoint calculation should feel instant.

New file:

```text
KspPathfindMode.java
```

## What changed

Previous local pathing relied heavily on:

```java
Rs2Tile.getReachableTilesFromTile(player, radius)
```

That can be expensive because it scans many reachable tiles.

v17 now uses:

```text
FAST_GREEDY
REACHABLE_SCAN
FAST_THEN_SCAN
```

Default:

```text
FAST_THEN_SCAN
```

## Fast greedy mode

Fast greedy checks a small set of projected candidate tiles in the player -> target direction:

```text
forward steps
small left/right offsets
reachable check for only those candidates
```

This avoids flood-scanning the loaded scene on normal walking ticks.

## New config options

```text
Pathfind mode: Fast then scan
Fast candidate steps: 4
Fast side offset: 2
Local path cache ms: 450
```

## Debugging

Movement result messages now include:

```text
pathMs=...
pathDecision=...
```

Example:

```text
Clicked local step 3230,3400,p0 method=MINIMAP pathMs=8 pathDecision=fast greedy selected 3230,3400,p0 candidates=20
```

## Recommended settings

For speed:

```text
Pathfind mode: Fast greedy
Fast candidate steps: 4
Fast side offset: 2
Local path cache ms: 450
```

For safer fallback behavior:

```text
Pathfind mode: Fast then scan
```

If a path is complex or indoors, `Fast then scan` lets the walker use the expensive scan only when fast candidate selection fails.


## v17.1 compile fix

Your Microbot branch returns:

```java
HashMap<WorldPoint, Integer>
```

from:

```java
Rs2Tile.getReachableTilesFromTile(...)
```

v17.1 fixes `KspLocalPathfinder.java` by converting the map keys:

```java
HashMap<WorldPoint, Integer> tiles = Rs2Tile.getReachableTilesFromTile(player, radius);
return tiles == null ? new HashSet<>() : new HashSet<>(tiles.keySet());
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.17.1"
```


---

# v18 transport file support

v18 adds an editable transport file.

Runtime file:

```text
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
```

ZIP sample file:

```text
kspwalker_transports.txt
```

New classes:

```text
KspTransportFileLoader.java
KspTransportLoadResult.java
```

Supported lines:

```text
NPC|Sailor|3028,3217,0|Travel|2956,3146,0|Yes
NPC Sailor @ 3028,3217,0 <Travel> 2956,3146,0 | Yes
NPC_ID|3645|3028,3217,0|Travel|2956,3146,0|Yes
OBJECT|Door|3234,3383,0|Open|3235,3383,0
OBJECT_ID|1530|3234,3383,0|Open|3235,3383,0
```

Docs:

```text
KSP_TRANSPORT_FILE_API.md
```


---

# v19 static debug path overlay

v19 fixes the debug path overlay following the player every frame.

Previous behavior:

```text
PATH line start = current player tile
```

That made the PATH line slide as the player moved.

New behavior:

```text
PATH line start = locked start tile when the target changes
PATH line end = locked target tile
```

The PATH overlay now stays static until:

```text
target changes
walker resets
plugin stops
```

The dynamic player-to-checkpoint line is still shown separately as:

```text
NEXT-DYNAMIC
```

This line is only for debugging the next click/checkpoint decision.


---

# v20 obstacle-aware path overlay + moving click chaining

v20 fixes two issues shown by the screenshot.

## 1. Debug path no longer draws a fake straight line through obstacles

Previous behavior:

```text
PATH = straight line from start to target
```

That looked wrong because it crossed walls/fences.

New behavior:

```text
PATH = reconstructed reachable path from the local pathfinder
```

The local pathfinder now stores `lastDebugPath`, reconstructed from Microbot's reachable-tile distance map:

```java
HashMap<WorldPoint, Integer> reachable = Rs2Tile.getReachableTilesFromTile(player, radius);
```

The overlay draws those path tiles instead of a straight line.

New API:

```java
walker.getLastDebugPath()
```

## 2. Walker no longer waits for full idle before queueing the next click

The movement executor now allows the next checkpoint click once the click cooldown has passed, even while the player is moving.

Defaults changed:

```text
Click cooldown ms: 650
Moving reclick delay ms: 650
Loop delay ms: 150
```

The old values were too slow:

```text
Click cooldown ms: 1200
Moving reclick delay ms: 2400
Loop delay ms: 350
```

## 3. Misleading straight debug lines disabled by default

Defaults changed:

```text
Show checkpoint segment: false
Show route edge segment: false
```

`Show debug path` now draws the actual reconstructed local reachable path.

## Patched files

```text
KspLocalPathfinder.java
KspMovementExecutor.java
KspWebWalker.java
KspWalkerTileOverlay.java
KspWalkerTesterConfig.java
KspWalkerTesterOverlay.java
KspWalkSettings.java
```


---

# v21 simplified transport file

v21 simplifies the generated transport file and fixes the literal `\n` text issue.

Runtime file:

```text
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
```

Simple format:

```text
NPC Name @ StartX,StartY,Plane <Menu entry> EndX,EndY,Plane | Optional dialogue
```

Default Port Sarim -> Karamja presets:

```text
NPC Captain Tobias @ 3029,3217,0 <Pay-fare> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Lorris @ 3028,3217,0 <Pay-fare> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Thresnor @ 3027,3217,0 <Pay-fare> 2956,3146,0 | Yes please. | Yes, please.
```

The file is now short and clean by default. Full details remain in:

```text
KSP_TRANSPORT_FILE_API.md
```


## v21.1 compile fix

Fixes two corrupted `@Range` annotations in `KspWalkerTesterConfig.java`.

Broken:

```java
H0, max = 5000)
H0, max = 8000)
```

Fixed:

```java
@Range(min = 100, max = 5000)
@Range(min = 100, max = 8000)
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.21.1"
```


---

# v22 Karamja Travel prep

v22 changes the Port Sarim -> Karamja transport action from:

```text
Pay-fare
```

to:

```text
Travel
```

The loader also normalizes old transport file entries:

```text
Pay-fare -> Travel
Pay-Fare -> Travel
Pay fare -> Travel
```

## Auto Karamja fare

New class:

```text
KspKaramjaTravelHelper.java
```

New config section:

```text
Travel
```

New options:

```text
Auto Karamja fare: true
Karamja fare coins: 30
Bank open distance: 8
```

If the final target is on Musa Point/Karamja and the player is not already there, the script checks inventory coins first.

If fewer than 30 coins are found:

```text
1. Walk to nearest bank.
2. Open bank.
3. Withdraw missing coins.
4. Close bank.
5. Continue to the Karamja target.
```

Karamja target detection currently covers the common F2P Musa Point/Karamja area:

```text
x = 2810..2978
y = 3050..3200
plane = 0
```

## Updated presets

```text
NPC Captain Tobias @ 3029,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Thresnor @ 3027,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
```


---

# v23 strict obstacle threshold detection

v23 fixes false door interactions like the purple door shown in the screenshot.

Problem:

```text
The walker interacted with a nearby door even though the player -> next tile path did not cross that door threshold.
```

Fix:

```text
1. While a valid next checkpoint exists, only blocking obstacle detection is allowed.
2. A blocking obstacle must be between player and checkpoint.
3. The obstacle tile must be very close to the actual movement segment.
4. The obstacle tile must be on/adjacent to the exact segment tiles.
5. Side-room doors are ignored unless the current movement segment actually crosses that threshold.
```

Important file:

```text
KspPathObstacleHandler.java
```

Default extra-distance tolerance is now stricter:

```text
Blocking obstacle extra distance: 1
```

If the walker misses a real door later, increase:

```text
Blocking obstacle line tolerance
```

from `1` to `2`, but keep it at `1` while testing roads/open walking.


---

# v24 run-toggle spam fix

v24 fixes repeated run orb/run-toggle clicking.

Problem:

```text
The movement clicker attempted to toggle run on every movement click.
```

Fix:

```text
Run toggle attempts are now throttled.
```

New config option:

```text
Run toggle cooldown ms: 8000
```

Patched files:

```text
KspMovementClicker.java
KspWalkSettings.java
KspWalkerTesterConfig.java
KspWalkerTesterScript.java
```

If it still toggles too often, increase:

```text
Run toggle cooldown ms
```

to:

```text
15000
```

or turn off:

```text
Toggle run
```


---

# v25 randomized run-energy threshold

v25 changes run toggling from cooldown-only to threshold-based.

New behavior:

```text
1. If Toggle run is disabled, do nothing.
2. If run is already ON, do nothing.
3. Pick a random threshold between Run energy min and Run energy max.
4. Default range is 10-30.
5. Only enable run when current energy reaches the random threshold.
6. Still respects Run toggle cooldown ms to prevent repeated attempts.
```

New config options:

```text
Run energy min: 10
Run energy max: 30
```

Existing config:

```text
Run toggle cooldown ms: 8000
```

Patched files:

```text
KspMovementClicker.java
KspWalkSettings.java
KspWalkerTesterConfig.java
KspWalkerTesterScript.java
```


---

# v26 Explv map links

v26 adds Explv map-link generation for debugging.

New file:

```text
KspExplvMapLink.java
```

The helper generates URLs in this format:

```text
https://explv.github.io/?centreX=X&centreY=Y&centreZ=Z&zoom=11
```

New debug log line:

```text
[KSP-WALKER-MAP]
```

It includes:

```text
playerMap
targetMap
nextMap
clickedMap
obstacleMap
teleportMap
```

This makes it easier to verify bad pathing or wrong obstacle interaction by opening the exact tile on the OSRS map.

API:

```java
String url = KspExplvMapLink.forTile(new WorldPoint(3234, 3384, 0));
String compact = KspExplvMapLink.compact(new WorldPoint(3234, 3384, 0));
```

Recommended debug setting:

```text
Debug level: Verbose
```

or:

```text
Debug level: Trace
```


## v26.1 compile fix

Adds the missing model class:

```text
KspPathObstacle.java
```

This fixes compile errors in:

```text
KspPathObstacleHandler.java
```

where `Optional<KspPathObstacle>` was referenced.

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.26.1"
```


---

# v27 separate teleport file support

v27 adds a separate teleport file:

```text
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
```

New sample file in the ZIP:

```text
kspwalker_teleports.txt
```

New supported definitions:

```text
OBJECT_TELEPORT
ITEM_TELEPORT
SPELL_TELEPORT
```

Examples:

```text
OBJECT_TELEPORT Spirit tree @ 3184,3509,0 <Travel> 2461,3444,0 | Tree Gnome Stronghold
ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0
SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1
```

Object teleports are graph transports. Item and spell teleports are registered in the teleport planner.

New classes:

```text
KspTeleportFileLoader.java
KspTeleportFileLoadResult.java
KspTeleportActionExecutor.java
```

Full docs:

```text
KSP_TELEPORT_FILE_API.md
```


## v27.1 compile fix

Fixes compatibility errors:

```text
KspMovementExecutor expected KspPathObstacleHandler.KspPathObstacle
KspPathObstacleHandler used GameObject.getName()/getActions(), unavailable in this branch
KspPathObstacleHandler referenced missing KspWalkSettings obstacle getters
KspWalkSettings builder was missing runToggleCooldownMs in some patched builds
```

Changes:

```text
KspPathObstacle is now nested inside KspPathObstacleHandler again.
GameObject.getName()/getActions() usage removed.
Obstacle detection keeps strict segment-threshold behavior.
Run-threshold builder methods are ensured in KspWalkSettings.
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.27.1"
```


## v27.2 compile fix

Aligns `KspPathObstacleHandler` with `KspMovementExecutor`.

Fixed signatures:

```java
KspPathObstacleHandler.KspPathObstacle findBlockingObstacle(...)
KspPathObstacleHandler.KspPathObstacle findFallbackObstacle(...)
KspWalkResult interact(KspPathObstacle obstacle, WorldPoint target)
String getDebugName()
```

`findBlockingObstacle` and `findFallbackObstacle` now return `null` when no obstacle is found, matching the existing executor code.

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.27.2"
```
