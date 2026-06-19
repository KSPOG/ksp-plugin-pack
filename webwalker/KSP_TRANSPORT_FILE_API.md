# KSP Walker Transport File API

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.21"
```

## Runtime file location

The plugin creates/loads this file:

```text
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
```

Example:

```text
C:\Users\Proje\.kspwalker\kspwalker_transports.txt
```

## Simple format

Use this format for most transports:

```text
NPC Name @ StartX,StartY,Plane <Menu entry> EndX,EndY,Plane | Optional dialogue
```

Example:

```text
NPC Captain Tobias @ 3029,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
```

## Presets included by default

These are now placed in the generated `.txt` file as real examples:

```text
NPC Captain Tobias @ 3029,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Thresnor @ 3027,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
```

These represent the Port Sarim dock NPCs that sail to Karamja. Older `Pay-fare` entries are automatically normalized to `Travel` by the loader.

## Object examples

```text
OBJECT Door @ 3234,3383,0 <Open> 3235,3383,0
OBJECT_ID 1530 @ 3234,3383,0 <Open> 3235,3383,0
```

## ID format

Use this when the name is unreliable:

```text
NPC_ID 3645 @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please.
OBJECT_ID 1530 @ 3234,3383,0 <Open> 3235,3383,0
```

## Pipe format

The older pipe format still works:

```text
NPC|Captain Tobias|3029,3217,0|Travel|2956,3146,0|Yes please.
OBJECT|Door|3234,3383,0|Open|3235,3383,0
```

## Notes

Lines beginning with `#` are ignored.

Each valid line becomes a `KspWebEdge` and is added to:

```java
walker.getGraph()
```

File-based edge IDs start with:

```text
transport:file:
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
