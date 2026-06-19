# KSP Custom WebWalker Core

A standalone Microbot-compatible walking core designed to test and gradually replace plugin logic that depends too heavily on `ShortestPathPlugin` execution.

This walker is built around:

- local reachable-tile checkpoint walking
- direct `walkFastCanvas(...)` execution
- stuck recovery
- checkpoint advancement
- scene tile markers
- configurable coordinate tester
- generic obstacle interaction for doors, gates, stairs, ladders, trapdoors, cellar entrances, and hatches
- optional custom web graph edges for known transports or quest-specific transitions

Current version documented here:

```java
KspWalkerTesterPlugin.VERSION = "1.0.8"
```

---

## Package

```java
package net.runelite.client.plugins.microbot.kspwalker;
```

Install path:

```text
runelite-client/src/main/java/net/runelite/client/plugins/microbot/kspwalker
```

---

## Main API overview

The main class is:

```java
KspWebWalker
```

Typical usage:

```java
private final KspWebWalker walker = new KspWebWalker();

public boolean walkTo(WorldPoint target)
{
    KspWalkResult result = walker.walkTo(target);
    return result.getStatus() == KspWalkStatus.ARRIVED;
}
```

Recommended loop usage inside a Microbot script:

```java
WorldPoint target = new WorldPoint(3235, 3383, 0);

KspWalkResult result = walker.walkTo(target);

switch (result.getStatus())
{
    case ARRIVED:
        // Continue next task
        break;

    case WALKING:
    case WAITING:
    case STUCK_RECOVERY:
    case EDGE_EXECUTED:
        // Keep looping
        break;

    case NO_LOCAL_STEP:
    case NO_ROUTE:
    case FAILED:
        // Log/debug/fallback
        break;
}
```

---

# Public classes

## `KspWebWalker`

Main walking controller.

### Constructors

```java
public KspWebWalker()
```

Creates a walker using default settings and an empty graph.

```java
public KspWebWalker(KspWalkSettings settings, KspWebGraph graph)
```

Creates a walker with custom settings and a supplied graph.

### Main method

```java
public KspWalkResult walkTo(WorldPoint target)
```

Attempts one walking tick toward the target.

This method is designed to be called repeatedly from your script loop. It does not block until arrival.

Behavior:

1. Captures current player state.
2. Checks arrival.
3. Checks stuck state.
4. Rebuilds or reuses a route.
5. Handles action edges if needed.
6. Handles path obstacles if detected.
7. Clicks a local checkpoint tile using `walkFastCanvas(...)`.
8. Returns a `KspWalkResult`.

Example:

```java
KspWalkResult result = walker.walkTo(new WorldPoint(3235, 3383, 0));
```

### Route control

```java
public void reset()
```

Clears current target, route, and stuck state.

```java
public void resetRoute(WorldPoint target)
```

Clears the active route and sets a new target context.

Use this when your plugin changes goals.

```java
walker.resetRoute(newTarget);
```

### Graph access

```java
public KspWebGraph getGraph()
```

Returns the walker graph so you can register custom edges.

Example:

```java
walker.getGraph().add(
    KspWebEdge.objectName(
        "varrock:door",
        new WorldPoint(3232, 3384, 0),
        new WorldPoint(3232, 3385, 0),
        "Door",
        "Open"
    )
);
```

### Settings

```java
public KspWalkSettings getSettings()
```

Returns current settings.

```java
public void setSettings(KspWalkSettings settings)
```

Updates walker settings. The active route is only cleared if the new settings are different.

Example:

```java
walker.setSettings(
    KspWalkSettings.builder()
        .finishDistance(2)
        .checkpointAdvanceDistance(5)
        .clickCooldownMs(1200)
        .debugLogging(true)
        .build()
);
```

### Debug/state getters

```java
public KspWebRoute getActiveRoute()
public WorldPoint getActiveTarget()
public KspWebEdge getActiveEdge()
public int getRecoveryCount()
public WorldPoint getLastPlannedTile()
public WorldPoint getLastClickedTile()
public WorldPoint getLastObstacleTile()
public String getLastObstacleName()
```

Useful for overlays and debugging.

Example:

```java
WorldPoint nextTile = walker.getLastPlannedTile();
WorldPoint lastClicked = walker.getLastClickedTile();
String obstacle = walker.getLastObstacleName();
```

---

## `KspWalkResult`

Represents the result of one walker tick.

### Important methods

```java
public KspWalkStatus getStatus()
public String getMessage()
public WorldPoint getTarget()
public WorldPoint getClickedTile()
public boolean isSuccess()
public long getTimestampMs()
```

Example:

```java
KspWalkResult result = walker.walkTo(target);

log.info("Walker status={} message={}", result.getStatus(), result.getMessage());
```

### Static factories

Normally you do not need these unless writing custom edges.

```java
KspWalkResult.arrived(WorldPoint target)
KspWalkResult.walking(WorldPoint target, WorldPoint clickedTile, String message)
KspWalkResult.waiting(WorldPoint target, String message)
KspWalkResult.edgeExecuted(KspWebEdge edge, String message)
KspWalkResult.stuckRecovery(WorldPoint target, WorldPoint clickedTile, String message)
KspWalkResult.noLocalStep(WorldPoint target, String message)
KspWalkResult.noRoute(WorldPoint target, String message)
KspWalkResult.failed(WorldPoint target, String message)
```

Custom edge example:

```java
KspWebEdge customEdge = KspWebEdge.custom(
    "custom:wait_for_region",
    start,
    end,
    20,
    () -> KspWalkResult.waiting(end, "Waiting for region transition")
);
```

---

## `KspWalkStatus`

Enum returned by `KspWalkResult`.

```java
ARRIVED
WALKING
WAITING
EDGE_EXECUTED
STUCK_RECOVERY
NO_LOCAL_STEP
NO_ROUTE
FAILED
```

### Status meaning

| Status | Meaning |
|---|---|
| `ARRIVED` | Player is within `finishDistance` of the target. |
| `WALKING` | Walker clicked a local checkpoint. |
| `WAITING` | Walker is waiting due to movement, cooldown, obstacle interaction, or state. |
| `EDGE_EXECUTED` | A registered graph edge was completed. |
| `STUCK_RECOVERY` | Walker forced a recovery nudge. |
| `NO_LOCAL_STEP` | No reachable checkpoint was found. |
| `NO_ROUTE` | No graph route was available. |
| `FAILED` | A hard failure occurred. |

---

# Settings API

## `KspWalkSettings`

Immutable settings object built with:

```java
KspWalkSettings.builder()
```

Default settings:

```java
KspWalkSettings.defaults()
```

### Builder example

```java
KspWalkSettings settings = KspWalkSettings.builder()
    .finishDistance(2)
    .localSearchRadius(13)
    .minStepDistance(2)
    .maxStepDistance(13)
    .directTargetClickDistance(2)
    .checkpointAdvanceDistance(5)
    .clickCooldownMs(1200)
    .movingReclickDelayMs(2400)
    .idleTimeoutMs(2500)
    .recoveryCooldownMs(1400)
    .autoObstacleInteraction(true)
    .obstacleScanDistance(8)
    .obstacleLineTolerance(2)
    .obstacleFallbackDistance(12)
    .obstacleFallbackLineTolerance(6)
    .obstacleCooldownMs(1200)
    .toggleRun(true)
    .debugLogging(true)
    .build();

walker.setSettings(settings);
```

---

## Movement settings

### `finishDistance(int value)`

Distance from the target that counts as arrival.

Default:

```text
2
```

Example:

```java
.finishDistance(2)
```

Use `1` for stricter exact walking. Use `3-5` for area-based walking.

---

### `localSearchRadius(int value)`

How far the local pathfinder scans for reachable tiles.

Default:

```text
13
```

Example:

```java
.localSearchRadius(13)
```

---

### `minStepDistance(int value)`

Minimum distance from the player for a checkpoint tile.

Default:

```text
2
```

This avoids tiny step spam.

---

### `maxStepDistance(int value)`

Maximum distance from the player for a checkpoint tile.

Default:

```text
13
```

This controls how far one `walkFastCanvas(...)` click may be.

---

### `directTargetClickDistance(int value)`

Only allows the final target tile itself to be clicked when the player is this close.

Default:

```text
2
```

Set to `0` or `1` to force intermediate checkpoints more aggressively.

---

### `checkpointAdvanceDistance(int value)`

Allows the next checkpoint click once the player is within this distance of the last clicked tile.

Default:

```text
5
```

Recommended:

```text
4-6
```

This gives smoother movement:

```text
click checkpoint A
walk near A
when within 4-6 tiles -> click checkpoint B
```

---

## Timing settings

### `clickCooldownMs(long value)`

Minimum delay between normal walking clicks.

Default:

```text
1200
```

---

### `movingReclickDelayMs(long value)`

Fallback delay before allowing another click if the player never gets within `checkpointAdvanceDistance` of the previous checkpoint.

Default:

```text
2400
```

---

### `idleTimeoutMs(long value)`

How long the player can remain idle on the same tile before stuck recovery activates.

Default:

```text
2500
```

---

### `recoveryCooldownMs(long value)`

Minimum delay between stuck recovery nudges.

Default:

```text
1400
```

---

### `routeTtlMs(long value)`

How long a generated route can be reused before it is considered stale.

Default:

```text
5000
```

---

### `postInteractionTimeoutMs(long value)`

How long action-edge execution waits for completion.

Default:

```text
8000
```

---

## Obstacle settings

### `autoObstacleInteraction(boolean value)`

Enables generic door/gate/stair/ladder/trapdoor handling.

Default:

```text
true
```

---

### `obstacleScanDistance(int value)`

Normal forward obstacle scan distance.

Default:

```text
8
```

Used before normal checkpoint walking.

---

### `obstacleLineTolerance(int value)`

How far from the player-to-target line an obstacle may be and still count as on-path.

Default:

```text
2
```

---

### `obstacleFallbackDistance(int value)`

Broader scan distance used when no local walking step exists.

Default:

```text
12
```

---

### `obstacleFallbackLineTolerance(int value)`

Broader line tolerance used only when no local walking step exists.

Default:

```text
6
```

This is useful when the target is behind a building wall and the door/stair is offset from the straight line.

---

### `obstacleCooldownMs(long value)`

Minimum delay between obstacle interactions.

Default:

```text
1200
```

---

## Other settings

### `toggleRun(boolean value)`

Allows `walkFastCanvas(...)` to toggle run.

Default:

```text
true
```

---

### `debugLogging(boolean value)`

Enables walker debug logging.

Default:

```text
true
```

---

# Graph API

## `KspWebGraph`

Stores custom web edges.

This is not a full world web yet. It is meant for plugin-specific known transitions:

- doors
- stairs
- ladders
- quest objects
- NPC travel
- widget/dialogue travel
- teleport transitions
- custom actions

### Methods

```java
public KspWebGraph add(KspWebEdge edge)
public KspWebGraph addAll(List<KspWebEdge> newEdges)
public KspWebGraph remove(String edgeId)
public void clear()
public List<KspWebEdge> getEdges()
public List<KspWebEdge> getEnabledEdges()
public Optional<KspWebEdge> findById(String edgeId)
public KspWebGraph addBidirectionalWalk(String id, WorldPoint a, WorldPoint b)
```

### Add an edge

```java
walker.getGraph().add(edge);
```

### Remove an edge

```java
walker.getGraph().remove("edge:id");
```

### Clear graph

```java
walker.getGraph().clear();
```

---

## `KspWebEdge`

Represents one route edge.

Edge types are:

```java
KspWebEdgeType.WALK
KspWebEdgeType.OBJECT
KspWebEdgeType.NPC
KspWebEdgeType.DIALOGUE
KspWebEdgeType.CUSTOM
```

### Common getters

```java
public String getId()
public KspWebEdgeType getType()
public WorldPoint getStart()
public WorldPoint getEnd()
public int getCost()
public int getObjectId()
public String getObjectName()
public int getNpcId()
public String getNpcName()
public String getAction()
public String[] getDialogueOptions()
public boolean isEnabled()
public boolean isComplete()
public boolean isWalkingEdge()
public boolean isActionEdge()
```

---

## Edge factories

### Walk edge

```java
KspWebEdge.walk(String id, WorldPoint start, WorldPoint end)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.walk(
        "local:walk_to_gate",
        new WorldPoint(3230, 3380, 0),
        new WorldPoint(3235, 3383, 0)
    )
);
```

---

### Object edge by ID

```java
KspWebEdge.object(String id, WorldPoint start, WorldPoint end, int objectId, String action)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.object(
        "building:staircase_up",
        new WorldPoint(3235, 3383, 0),
        new WorldPoint(3235, 3383, 1),
        11799,
        "Climb-up"
    )
);
```

---

### Object edge by name

```java
KspWebEdge.objectName(String id, WorldPoint start, WorldPoint end, String objectName, String action)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.objectName(
        "building:door_open",
        new WorldPoint(3234, 3383, 0),
        new WorldPoint(3235, 3383, 0),
        "Door",
        "Open"
    )
);
```

---

### NPC edge by ID

```java
KspWebEdge.npc(String id, WorldPoint start, WorldPoint end, int npcId, String action)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.npc(
        "travel:sailor",
        new WorldPoint(3028, 3217, 0),
        new WorldPoint(2956, 3146, 0),
        3645,
        "Travel"
    )
);
```

---

### NPC edge by name

```java
KspWebEdge.npcName(String id, WorldPoint start, WorldPoint end, String npcName, String action)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.npcName(
        "travel:sailor",
        new WorldPoint(3028, 3217, 0),
        new WorldPoint(2956, 3146, 0),
        "Sailor",
        "Travel"
    )
);
```

---

### Custom edge

```java
KspWebEdge.custom(
    String id,
    WorldPoint start,
    WorldPoint end,
    int cost,
    Supplier<KspWalkResult> customAction
)
```

Example:

```java
walker.getGraph().add(
    KspWebEdge.custom(
        "custom:teleport_home",
        new WorldPoint(3210, 3424, 0),
        new WorldPoint(3222, 3218, 0),
        50,
        () ->
        {
            boolean clicked = castHomeTeleport();

            if (!clicked)
            {
                return KspWalkResult.failed(null, "Failed to cast home teleport");
            }

            return KspWalkResult.waiting(null, "Casting home teleport");
        }
    )
);
```

---

## Builder API

For advanced edges:

```java
KspWebEdge.builder(String id, KspWebEdgeType type, WorldPoint start, WorldPoint end)
```

Available builder methods:

```java
.cost(int cost)
.objectId(int objectId)
.objectName(String objectName)
.npcId(int npcId)
.npcName(String npcName)
.action(String action)
.dialogueOptions(String... dialogueOptions)
.requirement(BooleanSupplier requirement)
.completion(BooleanSupplier completion)
.customAction(Supplier<KspWalkResult> customAction)
.build()
```

### NPC with dialogue options

```java
walker.getGraph().add(
    KspWebEdge.builder(
        "travel:npc_with_dialogue",
        KspWebEdgeType.NPC,
        new WorldPoint(3028, 3217, 0),
        new WorldPoint(2956, 3146, 0)
    )
    .npcName("Sailor")
    .action("Travel")
    .dialogueOptions("Yes", "Travel")
    .cost(25)
    .build()
);
```

### Edge with requirement

```java
walker.getGraph().add(
    KspWebEdge.builder(
        "quest:locked_door",
        KspWebEdgeType.OBJECT,
        new WorldPoint(3200, 3200, 0),
        new WorldPoint(3201, 3200, 0)
    )
    .objectName("Door")
    .action("Open")
    .requirement(() -> hasRequiredKey())
    .cost(10)
    .build()
);
```

### Edge with completion condition

```java
walker.getGraph().add(
    KspWebEdge.builder(
        "staircase:up",
        KspWebEdgeType.OBJECT,
        new WorldPoint(3235, 3383, 0),
        new WorldPoint(3235, 3383, 1)
    )
    .objectName("Staircase")
    .action("Climb-up")
    .completion(() -> Rs2Player.getWorldLocation().getPlane() == 1)
    .cost(15)
    .build()
);
```

---

# Obstacle handling API

## `KspPathObstacleHandler`

Handles generic path obstacles.

Normally this is used internally by `KspMovementExecutor`.

It detects obstacle names containing:

```text
door
gate
stair
staircase
ladder
trapdoor
cellar
hatch
```

It tries actions in this order:

```text
Open
Climb-up
Climb-down
Climb
Walk-up
Walk-down
Enter
Exit
```

### Normal scan

```java
public KspPathObstacle findObstacleOnPath(WorldPoint target, KspWalkSettings settings)
```

Used before normal checkpoint walking.

### Fallback scan

```java
public KspPathObstacle findFallbackObstacle(WorldPoint target, KspWalkSettings settings)
```

Used when no local walking checkpoint can be found.

### Interact

```java
public KspWalkResult interact(KspPathObstacle obstacle, WorldPoint target)
```

---

## `KspPathObstacle`

Nested class:

```java
KspPathObstacleHandler.KspPathObstacle
```

Methods:

```java
public TileObject getObject()
public WorldPoint getWorldPoint()
public String getName()
public String getAction()
public double getSegmentProgress()
public double getLineDistance()
public String getDebugName()
```

---

# Local pathing API

## `KspLocalPathfinder`

Selects the next local checkpoint tile.

### Methods

```java
public Optional<WorldPoint> findNextStep(WorldPoint target, KspWalkSettings settings)
public boolean canReach(WorldPoint target)
```

The scoring prefers:

1. tiles that reduce distance to target
2. tiles near the straight player-to-target line
3. longer valid intermediate steps
4. non-final target tiles unless already close

This class uses `Rs2Tile.getReachableTilesFromTile(...)` once per scan and avoids calling `Rs2Tile.isTileReachable(...)` repeatedly for every candidate.

---

# Movement execution API

## `KspMovementExecutor`

Usually used internally by `KspWebWalker`.

### Methods

```java
public KspWalkResult walkLocalTo(WorldPoint target, KspWalkSettings settings)
public KspWalkResult forceNudge(WorldPoint target, KspWalkSettings settings)
public WorldPoint getLastPlannedTile()
public WorldPoint getLastClickedTile()
public WorldPoint getLastObstacleTile()
public String getLastObstacleName()
```

### Normal walking behavior

`walkLocalTo(...)`:

1. checks arrival
2. waits if the player is moving toward the last checkpoint
3. waits for click cooldown
4. advances once within `checkpointAdvanceDistance`
5. checks for path obstacles
6. finds next local checkpoint
7. clicks it with `Rs2Walker.walkFastCanvas(...)`

---

# Stuck recovery API

## `KspStuckRecovery`

Usually used internally by `KspWebWalker`.

### Methods

```java
public boolean isStuck(KspPlayerState state, WorldPoint target, KspWalkSettings settings)
public KspWalkResult recover(WorldPoint target, KspWalkSettings settings, KspMovementExecutor movementExecutor)
public void reset()
public int getRecoveryCount()
public long getLastMovedAtMs()
public WorldPoint getLastPosition()
```

Stuck detection checks:

- player location unchanged
- player not moving
- player not animating
- target not reached
- idle timeout exceeded
- recovery cooldown passed

---

# Player state API

## `KspPlayerState`

Snapshot of player state.

### Methods

```java
public static KspPlayerState capture()
public WorldPoint getLocation()
public boolean isMoving()
public boolean isAnimating()
public long getCapturedAtMs()
public boolean hasLocation()
public boolean isSamePlane(WorldPoint point)
public int distanceTo(WorldPoint point)
public boolean isNear(WorldPoint point, int distance)
public boolean canReach(WorldPoint point)
```

Example:

```java
KspPlayerState state = KspPlayerState.capture();

if (state.isNear(target, 2))
{
    // arrived
}
```

---

# Tester plugin

## `KspWalkerTesterPlugin`

A coordinate-based test plugin.

Plugin descriptor:

```java
@PluginDescriptor(
    name = PluginConstants.KSP + "Custom Walker Tester",
    description = "Coordinate-based tester for the custom Microbot webwalker core",
    tags = {"microbot", "ksp", "walker", "webwalker"},
    authors = {"KSP"},
    version = KspWalkerTesterPlugin.VERSION,
    minClientVersion = "2.0.13",
    enabledByDefault = true,
    isExternal = true
)
```

Version:

```java
public static final String VERSION = "1.0.8";
```

---

## `KspWalkerTesterConfig`

Config fields:

### Target

```text
Start walking
Target X
Target Y
Target Plane
```

`Target X` and `Target Y` are text fields so RuneLite does not format them as `3,286`.

The parser accepts:

```text
3286
3,286
3.286
3 286
```

and converts them to:

```text
3286
```

### Walking

```text
Finish distance
Local search radius
Max step distance
Direct target click distance
Idle timeout ms
Recovery cooldown ms
Loop delay ms
Toggle run
Debug logging
Stop when arrived
Click cooldown ms
Moving reclick delay ms
Checkpoint advance distance
```

### Overlay/markers

```text
Show tile markers
Show edge markers
```

### Obstacles

```text
Auto obstacles
Obstacle scan distance
Obstacle line tolerance
Obstacle cooldown ms
Obstacle fallback distance
Obstacle fallback line tolerance
```

---

## `KspWalkerTesterScript`

Runs the tester loop.

Behavior:

1. Reads config coordinates.
2. Updates walker settings.
3. Calls `walker.walkTo(target)`.
4. Stops once arrived if `Stop when arrived` is enabled.
5. Resets if target changes or plugin is disabled.

Important getters:

```java
public KspWebWalker getWalker()
public KspWalkResult getLastResult()
public WorldPoint getLastTarget()
```

---

## `KspWalkerTesterOverlay`

Panel overlay showing:

```text
Target
Status
Message
Next tile
Clicked
Checkpoint dist
Obstacle
Recoveries
```

---

## `KspWalkerTileOverlay`

Scene overlay markers:

| Marker | Meaning |
|---|---|
| `TARGET` | Configured destination tile. |
| `NEXT` | Next planned checkpoint/click tile. |
| `CLICK` | Last clicked tile if no next tile is available. |
| `OBSTACLE` | Last obstacle attempted. |
| `EDGE START` | Active graph edge start tile. |
| `EDGE END` | Active graph edge end/landing tile. |

---

# Integration examples

## Basic integration into a script

```java
public class MyScript extends Script
{
    private final KspWebWalker walker = new KspWebWalker();

    public boolean run()
    {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            WorldPoint target = new WorldPoint(3235, 3383, 0);
            KspWalkResult result = walker.walkTo(target);

            if (result.getStatus() == KspWalkStatus.ARRIVED)
            {
                Microbot.log("Arrived at target");
            }
        }, 0, 350, TimeUnit.MILLISECONDS);

        return true;
    }

    @Override
    public void shutdown()
    {
        walker.reset();
        super.shutdown();
    }
}
```

---

## Plugin-specific walker wrapper

```java
public final class MyPluginWalker
{
    private final KspWebWalker walker;

    public MyPluginWalker()
    {
        this.walker = new KspWebWalker(
            KspWalkSettings.builder()
                .finishDistance(2)
                .checkpointAdvanceDistance(5)
                .clickCooldownMs(1200)
                .autoObstacleInteraction(true)
                .debugLogging(true)
                .build(),
            new KspWebGraph()
        );

        registerEdges();
    }

    public boolean walkTo(WorldPoint target)
    {
        KspWalkResult result = walker.walkTo(target);
        return result.getStatus() == KspWalkStatus.ARRIVED;
    }

    public void reset()
    {
        walker.reset();
    }

    private void registerEdges()
    {
        walker.getGraph().add(
            KspWebEdge.objectName(
                "example:door",
                new WorldPoint(3234, 3383, 0),
                new WorldPoint(3235, 3383, 0),
                "Door",
                "Open"
            )
        );
    }
}
```

---

## Quest/plugin area example

```java
private void registerRomeoAndJulietEdges()
{
    walker.getGraph().add(
        KspWebEdge.object(
            "romeo_juliet:cadava_bush",
            new WorldPoint(3277, 3374, 0),
            new WorldPoint(3277, 3374, 0),
            23625,
            "Pick-from"
        )
    );

    walker.getGraph().add(
        KspWebEdge.objectName(
            "romeo_juliet:stairs_up",
            new WorldPoint(3162, 3434, 0),
            new WorldPoint(3162, 3434, 1),
            "Staircase",
            "Climb-up"
        )
    );
}
```

---

# Recommended config presets

## General walking test

```text
Finish distance: 2
Local search radius: 13
Max step distance: 13
Direct target click distance: 2
Checkpoint advance distance: 5
Click cooldown ms: 1200
Moving reclick delay ms: 2400
Idle timeout ms: 2500
Recovery cooldown ms: 1400
Auto obstacles: true
Obstacle scan distance: 8
Obstacle line tolerance: 2
Obstacle fallback distance: 12
Obstacle fallback line tolerance: 6
```

## More aggressive checkpoint clicking

```text
Checkpoint advance distance: 6
Click cooldown ms: 900
Moving reclick delay ms: 1800
```

## Safer/slower clicking

```text
Checkpoint advance distance: 4
Click cooldown ms: 1600
Moving reclick delay ms: 3000
```

## Better indoor/door testing

```text
Auto obstacles: true
Obstacle scan distance: 10
Obstacle line tolerance: 3
Obstacle fallback distance: 16
Obstacle fallback line tolerance: 8
```

---

# Debugging checklist

## Overlay says `No reachable local step toward target`

Likely causes:

- target is inside a building
- target is behind a wall
- target is on another plane
- a door/stair/ladder is required
- obstacle scan distance is too low
- obstacle line tolerance is too strict

Try:

```text
Obstacle fallback distance: 16
Obstacle fallback line tolerance: 8
```

---

## Walker clicks side tiles

Try:

```text
Max step distance: 10-12
Obstacle line tolerance: 2
Checkpoint advance distance: 5
Direct target click distance: 1
```

---

## Walker spam-clicks

Try:

```text
Click cooldown ms: 1400-1800
Moving reclick delay ms: 2600-3200
Checkpoint advance distance: 4
```

---

## Walker waits too long before next click

Try:

```text
Checkpoint advance distance: 6
Click cooldown ms: 900-1200
Moving reclick delay ms: 1800-2400
```

---

## Walker does not interact with door/gate/stairs

Try:

```text
Auto obstacles: true
Obstacle scan distance: 12
Obstacle line tolerance: 3
Obstacle fallback distance: 18
Obstacle fallback line tolerance: 8
```

Also check the object’s actual name/action using your Object Examiner plugin.

---

# Known limitations

This is a custom walker core, not yet a complete global OSRS web.

Current limitations:

- no full world web database
- no region-to-region transport database
- no automatic quest/skill/item requirement solver
- generic obstacle handling may miss unusual object names or actions
- some stairs/ladders need explicit graph edges if the generic handler chooses the wrong action
- multi-plane navigation is best handled with explicit `KspWebEdge` transitions
- long-distance travel still needs registered edges or local stepping where possible

Best practice:

Use generic obstacle handling for simple doors/gates/ladders, and register explicit `KspWebEdge` entries for quest-critical objects or multi-plane transitions.

---

# Class list

```text
KspWebWalker.java
KspWebGraph.java
KspWebGraphPathfinder.java
KspWebRoute.java
KspWebEdge.java
KspWebEdgeType.java
KspWalkSettings.java
KspWalkResult.java
KspWalkStatus.java
KspPlayerState.java
KspLocalPathfinder.java
KspMovementExecutor.java
KspObstacleExecutor.java
KspPathObstacleHandler.java
KspStuckRecovery.java
KspWalkerTesterPlugin.java
KspWalkerTesterConfig.java
KspWalkerTesterScript.java
KspWalkerTesterOverlay.java
KspWalkerTileOverlay.java
KspWebWalkerUsageExample.java
```


## v8.1 compile fix

`KspPathObstacleHandler.java` was replaced cleanly.

Fixes:

```text
missing toFallbackObstacle(...)
missing fallbackScore(...)
stream inference returning Object instead of KspPathObstacle
```

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.8.1"
```


---

# Teleport route calculation

Version `1.0.9` adds teleport route calculation.

New classes:

```text
KspTeleportType.java
KspTeleportOption.java
KspTeleportPlan.java
KspTeleportRegistry.java
KspTeleportPlanner.java
KspTeleportExecutor.java
KspTeleportUsageExample.java
```

Teleports are registered through:

```java
walker.getTeleportRegistry().add(...);
```

The planner compares:

```text
walk directly
vs.
teleport first, then walk from landing tile
```

See:

```text
KSP_TELEPORT_API.md
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
