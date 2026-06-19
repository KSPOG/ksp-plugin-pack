# KSP Custom WebWalker

KSP Custom WebWalker is a Microbot/RuneLite walking framework for coordinate-based movement, local path stepping, explicit transport edges, teleport planning, bank routing, and debugging.

The plugin is intended to be used as a reusable walking core. The included tester plugin lets you enter target coordinates, walk to common quick targets, test obstacle handling, and gradually build a custom world travel database through editable `.txt` files.

---

## Package

```java
package net.runelite.client.plugins.microbot.kspwalker;
```

Main source folder:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker
```

---

## Main plugin

```text
KspWalkerTesterPlugin.java
```

This is the RuneLite/Microbot plugin entry point.

It starts the tester script, registers overlays, and loads the editable runtime databases:

```text
bank.txt
kspwalker_transports.txt
kspwalker_teleports.txt
kspwalker_edges.txt
```

Plugin display name:

```text
KSP Custom Walker Tester
```

---

## What the walker does

The walker accepts a destination `WorldPoint` and tries to move the player there using several layers:

```text
1. Resolve the target.
2. Check travel preparation, such as Karamja fare coins.
3. Consider registered teleports.
4. Consider explicit graph edges and transports.
5. Use local pathfinding to find the next reachable tile.
6. Click movement using minimap or canvas rules.
7. Detect stuck state and recover.
8. Handle strict path obstacles only when needed.
9. Keep debug state updated for overlays/logs.
```

The basic API is:

```java
KspWebWalker.walkTo(WorldPoint target)
```

The tester script calls this repeatedly while the plugin is running.

---

## Key classes

### Core walker

```text
KspWebWalker.java
```

Coordinates the whole walking process. It owns the graph, pathfinder, teleport planner, movement executor, stuck recovery, and debug state.

### Graph database

```text
KspWebGraph.java
KspWebEdge.java
KspWebEdgeType.java
KspWebGraphPathfinder.java
KspWebRoute.java
```

These classes represent explicit route edges such as:

```text
walking anchors
doors
stairs
ladders
NPC transports
object transports
teleports
multi-plane transitions
```

### Movement

```text
KspMovementExecutor.java
KspMovementClicker.java
KspMovementClickMethod.java
```

Movement execution handles the selected next checkpoint and decides whether to click:

```text
minimap
canvas
minimap with canvas fallback
canvas with minimap fallback
```

Run energy handling is randomized. The walker only tries to enable run when:

```text
run is currently off
energy is at or above a random threshold
the run-toggle cooldown has passed
```

Default energy threshold:

```text
10-30
```

### Local pathfinding

```text
KspLocalPathfinder.java
KspPathfindMode.java
```

Finds the next local reachable step toward the target.

Supported path modes:

```text
FAST_GREEDY
REACHABLE_SCAN
FAST_THEN_SCAN
```

`FAST_THEN_SCAN` is the recommended default.

### Obstacles

```text
KspPathObstacleHandler.java
KspObstacleExecutor.java
```

There are two obstacle systems.

`KspObstacleExecutor` handles explicit graph edges, such as a known staircase or gate.

`KspPathObstacleHandler` is a conservative fallback for generic blocking objects. It only interacts with an object when the object is on or directly adjacent to the current player-to-checkpoint movement segment. This avoids interacting with unrelated side-room doors.

For stairs, ladders, caves, special gates, and multi-plane movement, explicit graph edges are preferred.

### Stuck recovery

```text
KspStuckRecovery.java
```

Detects when the player is not making progress and asks the walker to retry, repath, or recover.

Stuck recovery does not trigger while the player is already moving or animating.

### Teleports

```text
KspTeleportRegistry.java
KspTeleportPlanner.java
KspTeleportExecutor.java
KspTeleportOption.java
KspTeleportType.java
KspTeleportFileLoader.java
KspTeleportActionExecutor.java
```

Teleports can be hardcoded or loaded from the teleport file. Teleports are considered before local walking when they are useful and usable.

### Banks

```text
KspBankTargetRegistry.java
KspBankFileLoader.java
KspMembershipDetector.java
```

Bank targets are loaded from `bank.txt`.

Banks can be marked as:

```text
yes = members-only
no  = free-to-play
```

The walker automatically checks whether the current client/world is members before choosing a members-only bank.

### Debugging

```text
KspWalkerDebugState.java
KspWalkerDebugLogger.java
KspDebugPathBuilder.java
KspExplvMapLink.java
```

Debug logs can show:

```text
player tile
target tile
next checkpoint
clicked tile
obstacle tile
teleport destination
route state
path decision
click method
Explv map links
```

Explv links use this format:

```text
https://explv.github.io/?centreX=X&centreY=Y&centreZ=Z&zoom=11
```

---

## Tester plugin files

```text
KspWalkerTesterPlugin.java
KspWalkerTesterScript.java
KspWalkerTesterConfig.java
KspWalkerTesterOverlay.java
KspWalkerTileOverlay.java
```

### Tester config

The tester config controls:

```text
manual target coordinates
quick target mode
walking behavior
pathfinding mode
movement click method
run toggling
teleports
travel preparation
overlay drawing
debug logging
```

### Quick targets

The tester supports:

```text
Manual coords
Nearest bank
Grand Exchange
```

`Nearest bank` uses `bank.txt` and membership filtering.

`Grand Exchange` also uses `bank.txt` if a Grand Exchange entry exists there.

---

## Runtime files

The plugin creates runtime files in:

```text
%USERPROFILE%\.kspwalker
```

Example:

```text
C:\Users\Proje\.kspwalker
```

Runtime files:

```text
bank.txt
kspwalker_transports.txt
kspwalker_teleports.txt
kspwalker_edges.txt
```

The ZIP also includes package-local examples inside:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker
```

Package-local examples:

```text
bank.txt
kspwalker_transports.txt
kspwalker_teleports.txt
kspwalker_edges.txt
```

Important: runtime files are created if missing. Existing runtime files are not overwritten automatically, so your local edits are preserved.

---

# bank.txt

`bank.txt` defines bank targets.

Runtime path:

```text
%USERPROFILE%\.kspwalker\bank.txt
```

Package-local default:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/bank.txt
```

## Format

Preferred:

```text
BankName | x,y,plane | yes/no
```

Meaning:

```text
yes = members-only bank
no  = free-to-play bank
```

Examples:

```text
Varrock East | 3251,3420,0 | no
Varrock West | 3182,3440,0 | no
Grand Exchange | 3164,3486,0 | no
Catherby | 2809,3441,0 | yes
Seers' Village | 2725,3492,0 | yes
```

Shorthand is also supported:

```text
Varrock East 3251,3420,0 no
Grand Exchange 3164,3486,0 no
Catherby 2809,3441,0 yes
```

Old two-field format still works and is treated as F2P:

```text
Varrock East | 3251,3420,0
```

## Membership filtering

There is no manual config for members banks.

The walker automatically checks current membership/world state:

```text
members detected     -> banks marked yes are allowed
non-members detected -> banks marked yes are skipped
```

---

# kspwalker_transports.txt

`kspwalker_transports.txt` defines simple NPC/object travel edges.

Runtime path:

```text
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
```

Package-local default:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_transports.txt
```

## Simple format

```text
TYPE NameOrId @ StartX,StartY,Plane <Menu action> EndX,EndY,Plane | Optional dialogue | Optional requirement
```

Supported types:

```text
NPC
NPC_ID
OBJECT
OBJECT_ID
```

## Examples

Port Sarim to Karamja:

```text
NPC Captain Tobias @ 3029,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
NPC Seaman Thresnor @ 3027,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
```

Object example:

```text
OBJECT Door @ 3234,3383,0 <Open> 3235,3383,0
```

Object ID example:

```text
OBJECT_ID 1530 @ 3234,3383,0 <Open> 3235,3383,0
```

NPC ID example:

```text
NPC_ID 3645 @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please.
```

## Requirements

Requirements can be added with `REQ`.

Example:

```text
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | REQ coins>=30 | Yes please.
```

---

# kspwalker_teleports.txt

`kspwalker_teleports.txt` defines object, item, and spell teleports.

Runtime path:

```text
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
```

Package-local default:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_teleports.txt
```

## Object teleports

Object teleports become graph transports.

Format:

```text
OBJECT_TELEPORT Object name @ StartX,StartY,Plane <Menu entry> EndX,EndY,Plane | Optional dialogue
```

Example:

```text
OBJECT_TELEPORT Spirit tree @ 3184,3509,0 <Travel> 2461,3444,0 | Tree Gnome Stronghold
```

Object ID format:

```text
OBJECT_TELEPORT_ID 12345 @ 3184,3509,0 <Travel> 2461,3444,0 | Tree Gnome Stronghold
```

## Item teleports

Item teleports are registered in the teleport planner.

Format:

```text
ITEM_TELEPORT Item name | item action | dialogue/destination option | EndX,EndY,Plane
```

Examples:

```text
ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0
ITEM_TELEPORT Amulet of glory | Rub | Draynor Village | 3105,3251,0
```

Common item actions:

```text
Rub
Teleport
Break
Operate
```

## Spell teleports

Spell teleports are registered in the teleport planner.

Format:

```text
SPELL_TELEPORT Spell name | EndX,EndY,Plane | Rune name amount, Rune name amount
```

Example:

```text
SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1
```

Rune requirement examples:

```text
Law rune 1
Law rune <1>
Law rune x1
```

Spell casting support depends on whether your Microbot branch exposes a compatible `Rs2Magic` cast method.

---

# kspwalker_edges.txt

`kspwalker_edges.txt` is the explicit world/region edge database.

Use this file for things the generic walker should not guess:

```text
region-to-region route anchors
unusual doors/gates
stairs
ladders
caves
multi-plane transitions
quest/object shortcuts
NPC transports with requirements
```

Runtime path:

```text
%USERPROFILE%\.kspwalker\kspwalker_edges.txt
```

Package-local default:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_edges.txt
```

## Supported edge types

```text
WALK_EDGE
REGION_WALK
OBJECT_EDGE
OBJECT_ID_EDGE
NPC_EDGE
NPC_ID_EDGE
DIALOGUE_EDGE
```

## Walk / region edge

Used as a long-distance route anchor.

Format:

```text
WALK_EDGE | id | start x,y,p | end x,y,p | optional cost | optional REQ ...
```

Example:

```text
WALK_EDGE | varrock_square_to_west_bank | 3212,3429,0 | 3185,3436,0 | 30
```

## Object edge

Used for doors, gates, stairs, ladders, caves, and plane changes.

Format:

```text
OBJECT_EDGE | id | start x,y,p | end x,y,p | object name | action | optional cost | optional REQ ... | optional dialogue
```

Examples:

```text
OBJECT_EDGE | lumbridge_stairs_up | 3204,3207,0 | 3204,3207,1 | Staircase | Climb-up | 8
OBJECT_EDGE | lumbridge_stairs_down | 3204,3207,1 | 3204,3207,0 | Staircase | Climb-down | 8
```

Object ID format:

```text
OBJECT_ID_EDGE | id | start x,y,p | end x,y,p | object id | action | optional cost | optional REQ ... | optional dialogue
```

## NPC edge

Used for boats, travel NPCs, guides, carts, and other NPC movement.

Format:

```text
NPC_EDGE | id | start x,y,p | end x,y,p | npc name | action | optional cost | optional REQ ... | optional dialogue
```

Example:

```text
NPC_EDGE | karamja_ship_port_sarim | 3028,3217,0 | 2956,3146,0 | Seaman Lorris | Travel | 25 | REQ coins>=30 | Yes please. | Yes, please.
```

NPC ID format:

```text
NPC_ID_EDGE | id | start x,y,p | end x,y,p | npc id | action | optional cost | optional REQ ... | optional dialogue
```

## Dialogue edge

Used when a dialogue option moves the player or completes a transition.

Format:

```text
DIALOGUE_EDGE | id | start x,y,p | end x,y,p | dialogue option | optional cost | optional REQ ...
```

---

# Requirements

Requirements are supported in graph edges and transports.

Examples:

```text
REQ coins>=30
REQ item:Amulet of glory>=1
REQ bank:Coins>=30
REQ skill:Agility>=5
REQ varp:123=1
REQ varbit:456=0
```

Multiple requirements:

```text
REQ coins>=30; skill:Agility>=5
```

Requirement behavior:

```text
all requirements must pass
unknown requirement types fail closed
failed requirements make the edge unusable
```

The requirement parser is:

```text
KspRequirementParser.java
```

---

# Karamja travel preparation

The walker has special preparation logic for common F2P Karamja travel.

Class:

```text
KspKaramjaTravelHelper.java
```

If the target is on Musa Point/Karamja and the player is not already there, the script checks for fare coins.

Default fare:

```text
30 coins
```

If the player has fewer than the required coins:

```text
1. Walk to nearest usable bank.
2. Open bank.
3. Withdraw missing coins.
4. Close bank.
5. Continue toward Karamja.
```

The nearest bank uses `bank.txt` and automatic membership filtering.

---

# Debugging

Enable debug in the plugin config.

Recommended settings when reporting issues:

```text
Debug level: Trace
Log every walker tick: true
Show debug path: true
```

Verbose/Trace logs include:

```text
status
message
player tile
target tile
next checkpoint
last clicked tile
obstacle tile
teleport destination
route decision
path decision
click method
stuck recovery state
Explv map URLs
```

Map links are printed as:

```text
[KSP-WALKER-MAP]
```

Example format:

```text
https://explv.github.io/?centreX=3234&centreY=3384&centreZ=0&zoom=11
```

Use those links to verify:

```text
wrong next tile
door threshold mistakes
bad obstacle interaction
wrong transport start/end tile
multi-plane edge issues
```

---

# Overlays

The plugin includes two overlays.

## Status overlay

```text
KspWalkerTesterOverlay.java
```

Shows current walker status, target, click method, debug state, and path information.

## Tile overlay

```text
KspWalkerTileOverlay.java
```

Draws the current path/next checkpoint/route information on the game scene.

---

# How to add a new bank

Edit:

```text
%USERPROFILE%\.kspwalker\bank.txt
```

Add:

```text
BankName | x,y,plane | yes/no
```

Example:

```text
My Bank | 3000,3000,0 | no
```

Restart the plugin/client after editing.

---

# How to add a new boat/NPC transport

Edit:

```text
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
```

Add:

```text
NPC Sailor Name @ startX,startY,plane <Travel> endX,endY,plane | Dialogue option
```

Example:

```text
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please.
```

Restart the plugin/client after editing.

---

# How to add a staircase or ladder

For stairs/ladders, prefer `kspwalker_edges.txt`.

Example:

```text
OBJECT_EDGE | my_stairs_up | 3204,3207,0 | 3204,3207,1 | Staircase | Climb-up | 8
OBJECT_EDGE | my_stairs_down | 3204,3207,1 | 3204,3207,0 | Staircase | Climb-down | 8
```

Use object IDs when names/actions are ambiguous:

```text
OBJECT_ID_EDGE | my_stairs_up | 3204,3207,0 | 3204,3207,1 | 12345 | Climb-up | 8
```

Restart the plugin/client after editing.

---

# How to add a teleport item

Edit:

```text
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
```

Add:

```text
ITEM_TELEPORT Item name | action | option | endX,endY,plane
```

Example:

```text
ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0
```

Restart the plugin/client after editing.

---

# How to add a spell teleport

Edit:

```text
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
```

Add:

```text
SPELL_TELEPORT Spell name | endX,endY,plane | Rune name amount, Rune name amount
```

Example:

```text
SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1
```

Restart the plugin/client after editing.

---

# Current design limits

This walker is not a complete full-world web database by itself.

It can local-step and use registered graph edges, but it still needs explicit data for:

```text
long-distance routes
region-to-region transport
multi-plane transitions
unusual object names/actions
quest-locked shortcuts
skill/item/coin requirements
teleports and transports
```

The intended workflow is:

```text
1. Try local walking.
2. If it fails at an obstacle/region/plane transition, add a graph edge.
3. If transport is involved, add a transport or teleport entry.
4. If requirements are needed, add REQ fields.
5. Re-test with Trace debug and Explv links.
```

Over time, the `.txt` files become the world database.

---

# Troubleshooting

## Runtime file did not update

Runtime files are not overwritten if they already exist.

Delete the relevant file from:

```text
%USERPROFILE%\.kspwalker
```

Then restart the plugin/client to regenerate it from the default.

## It clicks a wrong door

Add an explicit `OBJECT_EDGE` or `OBJECT_ID_EDGE` in:

```text
kspwalker_edges.txt
```

The generic obstacle handler is conservative, but explicit graph edges are safer.

## It cannot go upstairs/downstairs

Add explicit multi-plane edges in:

```text
kspwalker_edges.txt
```

Example:

```text
OBJECT_EDGE | stairs_up | x,y,0 | x,y,1 | Staircase | Climb-up | 8
OBJECT_EDGE | stairs_down | x,y,1 | x,y,0 | Staircase | Climb-down | 8
```

## It chooses a members bank on F2P

Make sure the bank line has:

```text
| yes
```

for members-only banks.

Example:

```text
Catherby | 2809,3441,0 | yes
```

The walker skips those automatically when membership is not detected.

## Karamja does not work

Check:

```text
inventory has 30 coins or bank has coins
transport file has the Port Sarim sailor entries
menu action is Travel
dialogue text matches the client
target is on Musa Point/Karamja
```

Useful Karamja transport example:

```text
NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.
```

## Teleport spell does not cast

Spell teleport execution depends on the Microbot branch exposing a compatible `Rs2Magic` cast method.

Item teleports and object teleports are usually easier to support reliably.

---

# Recommended debug report

When reporting walker issues, include:

```text
1. Target coordinates.
2. Player start coordinates.
3. Debug level Trace logs.
4. [KSP-WALKER-MAP] links.
5. Screenshot with the wrong tile/object marked.
6. Relevant lines from bank/transport/teleport/edge txt files.
```

## Package text file note

`PACKAGE_TEXT_FILES.md` is only a short package-local pointer for the bundled `.txt` examples. The root `README.md` is the main documentation.

## Clean ZIP layout

The editable `.txt` database files are included only inside the Java package folder:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/bank.txt
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_transports.txt
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_teleports.txt
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker/kspwalker_edges.txt
```

They belong to:

```java
package net.runelite.client.plugins.microbot.kspwalker;
```

Runtime copies are still created/read from:

```text
%USERPROFILE%\.kspwalker\bank.txt
%USERPROFILE%\.kspwalker\kspwalker_transports.txt
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
%USERPROFILE%\.kspwalker\kspwalker_edges.txt
```
