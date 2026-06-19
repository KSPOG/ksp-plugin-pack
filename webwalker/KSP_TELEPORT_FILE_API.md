# KSP Walker Teleport File API

Version:

```java
KspWalkerTesterPlugin.VERSION = "1.0.27"
```

## Runtime file

The plugin creates/loads:

```text
%USERPROFILE%\.kspwalker\kspwalker_teleports.txt
```

The ZIP also includes:

```text
kspwalker_teleports.txt
```

---

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

ID format is also supported:

```text
OBJECT_TELEPORT_ID 12345 @ 3184,3509,0 <Travel> 2461,3444,0 | Tree Gnome Stronghold
```

---

## Item teleports

Item teleports are registered into the teleport planner.

Format:

```text
ITEM_TELEPORT Item name | item action | dialogue/destination option | EndX,EndY,Plane
```

Examples:

```text
ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0
ITEM_TELEPORT Amulet of glory | Rub | Draynor Village | 3105,3251,0
```

The item action can be:

```text
Rub
Teleport
Break
Operate
```

depending on the item.

---

## Spell teleports

Spell teleports are registered into the teleport planner when requirements pass.

Format:

```text
SPELL_TELEPORT Spell name | EndX,EndY,Plane | Rune name amount, Rune name amount
```

Example:

```text
SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1
```

The rune requirement parser accepts:

```text
Law rune 1
Law rune <1>
Law rune x1
```

Important: spell execution is only enabled if the Microbot branch exposes a string-based `Rs2Magic` cast method. If not, the spell remains loaded but is not considered usable by the teleport planner.

---

## New files

```text
KspTeleportFileLoader.java
KspTeleportFileLoadResult.java
KspTeleportActionExecutor.java
kspwalker_teleports.txt
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
