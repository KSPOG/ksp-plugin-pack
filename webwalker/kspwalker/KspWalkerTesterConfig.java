package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("kspwalkertester")
public interface KspWalkerTesterConfig extends Config
{
    @ConfigSection(
        name = "Target",
        description = "Coordinate target for testing the custom KSP walker.",
        position = 0
    )
    String targetSection = "targetSection";

    @ConfigSection(
        name = "Walking",
        description = "Walker tuning settings.",
        position = 1
    )
    String walkingSection = "walkingSection";

    @ConfigSection(
        name = "Obstacles",
        description = "Door, gate, stair, ladder, trapdoor handling.",
        position = 2
    )
    String obstacleSection = "obstacleSection";

    @ConfigSection(
        name = "Bank / GE",
        description = "Quick walking targets.",
        position = 3
    )
    String bankSection = "bankSection";

    @ConfigSection(
        name = "Travel",
        description = "Special travel preparation such as ship fares.",
        position = 4
    )
    String travelSection = "travelSection";

    @ConfigSection(
        name = "Teleports",
        description = "Teleport planning settings.",
        position = 5
    )
    String teleportSection = "teleportSection";

    @ConfigSection(
        name = "Overlay",
        description = "Tile marker settings.",
        position = 6
    )
    String overlaySection = "overlaySection";

    @ConfigSection(
        name = "Debug",
        description = "Walker diagnostic logging.",
        position = 7
    )
    String debugSection = "debugSection";

    @ConfigItem(
        keyName = "enabled",
        name = "Start walking",
        description = "Enable this to start walking to the configured coordinates.",
        position = 0
    )
    default boolean enabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "targetX",
        name = "Target X",
        description = "World X coordinate. Text field prevents formatting like 3,286.",
        section = targetSection,
        position = 1
    )
    default String targetX()
    {
        return "3164";
    }

    @ConfigItem(
        keyName = "targetY",
        name = "Target Y",
        description = "World Y coordinate. Text field prevents formatting like 3,340.",
        section = targetSection,
        position = 2
    )
    default String targetY()
    {
        return "3487";
    }

    @Range(min = 0, max = 3)
    @ConfigItem(
        keyName = "targetPlane",
        name = "Target Plane",
        description = "Plane to walk to.",
        section = targetSection,
        position = 3
    )
    default int targetPlane()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "quickTargetMode",
        name = "Quick target",
        description = "Choose a quick target. Start walking will use this instead of manual coords when not set to Manual coords.",
        section = bankSection,
        position = 4
    )
    default KspQuickTargetMode quickTargetMode()
    {
        return KspQuickTargetMode.MANUAL_COORDS;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "finishDistance", name = "Finish distance", description = "How close counts as arrived.", section = walkingSection, position = 10)
    default int finishDistance()
    {
        return 2;
    }

    @Range(min = 3, max = 20)
    @ConfigItem(keyName = "localSearchRadius", name = "Local search radius", description = "Reachable tile scan radius.", section = walkingSection, position = 11)
    default int localSearchRadius()
    {
        return 13;
    }

    @Range(min = 2, max = 20)
    @ConfigItem(keyName = "maxStepDistance", name = "Max step distance", description = "Maximum checkpoint click distance.", section = walkingSection, position = 12)
    default int maxStepDistance()
    {
        return 13;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "directTargetClickDistance", name = "Direct target click distance", description = "Only click final target tile if this close.", section = walkingSection, position = 13)
    default int directTargetClickDistance()
    {
        return 2;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "checkpointAdvanceDistance", name = "Checkpoint advance distance", description = "Click next checkpoint once this close to last clicked tile.", section = walkingSection, position = 14)
    default int checkpointAdvanceDistance()
    {
        return 5;
    }

    @Range(min = 100, max = 5000)
    @ConfigItem(keyName = "clickCooldownMs", name = "Click cooldown ms", description = "Minimum delay between normal walking clicks.", section = walkingSection, position = 15)
    default int clickCooldownMs()
    {
        return 650;
    }

    @Range(min = 100, max = 8000)
    @ConfigItem(keyName = "movingReclickDelayMs", name = "Moving reclick delay ms", description = "Fallback delay if checkpoint is not reached.", section = walkingSection, position = 16)
    default int movingReclickDelayMs()
    {
        return 650;
    }

    @Range(min = 500, max = 10000)
    @ConfigItem(keyName = "idleTimeoutMs", name = "Idle timeout ms", description = "Idle time before recovery.", section = walkingSection, position = 17)
    default int idleTimeoutMs()
    {
        return 2500;
    }

    @Range(min = 300, max = 10000)
    @ConfigItem(keyName = "recoveryCooldownMs", name = "Recovery cooldown ms", description = "Delay between stuck recovery nudges.", section = walkingSection, position = 18)
    default int recoveryCooldownMs()
    {
        return 1400;
    }

    @Range(min = 100, max = 2000)
    @ConfigItem(keyName = "loopDelayMs", name = "Loop delay ms", description = "How often tester calls walkTo.", section = walkingSection, position = 19)
    default int loopDelayMs()
    {
        return 150;
    }

    @ConfigItem(keyName = "toggleRun", name = "Toggle run", description = "Allow walkFastCanvas to toggle run.", section = walkingSection, position = 20)
    default boolean toggleRun()
    {
        return true;
    }

    @ConfigItem(keyName = "stopWhenArrived", name = "Stop when arrived", description = "Stop tester after destination is reached.", section = walkingSection, position = 21)
    default boolean stopWhenArrived()
    {
        return true;
    }

    @ConfigItem(keyName = "debugLogging", name = "Debug logging", description = "Enable walker debug logs.", section = walkingSection, position = 22)
    default boolean debugLogging()
    {
        return true;
    }


    @ConfigItem(
        keyName = "preferMinimap",
        name = "Prefer minimap",
        description = "Use minimap for most walking. Canvas is only used for close visible tiles or as fallback.",
        section = walkingSection,
        position = 23
    )
    default boolean preferMinimap()
    {
        return true;
    }

    @Range(min = 0, max = 15)
    @ConfigItem(
        keyName = "canvasMaxDistance",
        name = "Canvas max distance",
        description = "Only use canvas directly when the next checkpoint is visible and within this many tiles.",
        section = walkingSection,
        position = 24
    )
    default int canvasMaxDistance()
    {
        return 4;
    }

    @ConfigItem(
        keyName = "canvasFallbackWhenMinimapFails",
        name = "Canvas fallback",
        description = "If minimap click fails and the tile is visible, fall back to canvas.",
        section = walkingSection,
        position = 25
    )
    default boolean canvasFallbackWhenMinimapFails()
    {
        return true;
    }


    @ConfigItem(
        keyName = "pathfindMode",
        name = "Pathfind mode",
        description = "Fast greedy is quickest. Fast then scan tries fast candidates first and only uses the expensive reachable scan if needed.",
        section = walkingSection,
        position = 26
    )
    default KspPathfindMode pathfindMode()
    {
        return KspPathfindMode.FAST_THEN_SCAN;
    }

    @Range(min = 1, max = 8)
    @ConfigItem(
        keyName = "fastCandidateStepCount",
        name = "Fast candidate steps",
        description = "How many forward distances to test in fast greedy mode.",
        section = walkingSection,
        position = 27
    )
    default int fastCandidateStepCount()
    {
        return 4;
    }

    @Range(min = 0, max = 5)
    @ConfigItem(
        keyName = "fastCandidateSideOffset",
        name = "Fast side offset",
        description = "How far left/right of the direct target vector fast greedy mode may test.",
        section = walkingSection,
        position = 28
    )
    default int fastCandidateSideOffset()
    {
        return 2;
    }

    @Range(min = 0, max = 2000)
    @ConfigItem(
        keyName = "localPathCacheMs",
        name = "Local path cache ms",
        description = "Briefly cache the selected local checkpoint to avoid recalculating path every loop.",
        section = walkingSection,
        position = 29
    )
    default int localPathCacheMs()
    {
        return 450;
    }

    @ConfigItem(keyName = "autoObstacleInteraction", name = "Auto obstacles", description = "Enable generic door/gate/stair/ladder/trapdoor handling. Blocking mode handles obstacles between player and next checkpoint; fallback mode handles no-local-step cases.", section = obstacleSection, position = 30)
    default boolean autoObstacleInteraction()
    {
        return true;
    }

    @Range(min = 3, max = 20)
    @ConfigItem(keyName = "obstacleScanDistance", name = "Obstacle scan distance", description = "Reserved normal scan distance. Generic obstacle handling is fallback-only by default.", section = obstacleSection, position = 31)
    default int obstacleScanDistance()
    {
        return 8;
    }

    @Range(min = 0, max = 4)
    @ConfigItem(keyName = "obstacleLineTolerance", name = "Obstacle line tolerance", description = "Reserved normal line tolerance. Generic obstacle handling is fallback-only by default.", section = obstacleSection, position = 32)
    default int obstacleLineTolerance()
    {
        return 2;
    }

    @Range(min = 3, max = 25)
    @ConfigItem(keyName = "obstacleFallbackDistance", name = "Obstacle fallback distance", description = "Broader obstacle scan when no local step exists.", section = obstacleSection, position = 33)
    default int obstacleFallbackDistance()
    {
        return 12;
    }

    @Range(min = 2, max = 10)
    @ConfigItem(keyName = "obstacleFallbackLineTolerance", name = "Obstacle fallback line tolerance", description = "Broader line tolerance when no local step exists.", section = obstacleSection, position = 34)
    default int obstacleFallbackLineTolerance()
    {
        return 6;
    }

    @Range(min = 300, max = 5000)
    @ConfigItem(keyName = "obstacleCooldownMs", name = "Obstacle cooldown ms", description = "Delay between obstacle interactions.", section = obstacleSection, position = 35)
    default int obstacleCooldownMs()
    {
        return 1200;
    }


    @ConfigItem(
        keyName = "blockingObstacleInteraction",
        name = "Blocking obstacles",
        description = "Interact with a door/gate/stair/ladder/trapdoor only when it lies between the player and the next checkpoint.",
        section = obstacleSection,
        position = 36
    )
    default boolean blockingObstacleInteraction()
    {
        return true;
    }

    @Range(min = 0, max = 4)
    @ConfigItem(
        keyName = "blockingObstacleLineTolerance",
        name = "Blocking obstacle line tolerance",
        description = "How close an obstacle must be to the player-to-next-checkpoint segment.",
        section = obstacleSection,
        position = 37
    )
    default int blockingObstacleLineTolerance()
    {
        return 1;
    }

    @Range(min = 0, max = 5)
    @ConfigItem(
        keyName = "blockingObstacleExtraDistance",
        name = "Blocking obstacle extra distance",
        description = "Extra scan distance beyond the next checkpoint for large/offset objects.",
        section = obstacleSection,
        position = 38
    )
    default int blockingObstacleExtraDistance()
    {
        return 1;
    }


    @ConfigItem(
        keyName = "autoWithdrawKaramjaFare",
        name = "Auto Karamja fare",
        description = "If the target is on Musa Point/Karamja and you have fewer than the required coins, walk to bank and withdraw coins first.",
        section = travelSection,
        position = 45
    )
    default boolean autoWithdrawKaramjaFare()
    {
        return true;
    }

    @Range(min = 0, max = 1000)
    @ConfigItem(
        keyName = "karamjaFareCoins",
        name = "Karamja fare coins",
        description = "Coins required before using the Port Sarim to Karamja ship.",
        section = travelSection,
        position = 46
    )
    default int karamjaFareCoins()
    {
        return 30;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
        keyName = "bankOpenDistance",
        name = "Bank open distance",
        description = "When within this distance of the selected bank target, try opening the bank.",
        section = travelSection,
        position = 47
    )
    default int bankOpenDistance()
    {
        return 8;
    }

    @ConfigItem(keyName = "teleportsEnabled", name = "Enable teleports", description = "Allow registered teleport spells/items in route calculation.", section = teleportSection, position = 40)
    default boolean teleportsEnabled()
    {
        return true;
    }

    @Range(min = 0, max = 500)
    @ConfigItem(keyName = "minTeleportSavingDistance", name = "Min teleport saving distance", description = "Teleport must save at least this many tiles.", section = teleportSection, position = 41)
    default int minTeleportSavingDistance()
    {
        return 40;
    }

    @Range(min = 1, max = 20)
    @ConfigItem(keyName = "teleportWalkCostMultiplier", name = "Teleport walk cost multiplier", description = "Weight for post-teleport walking distance.", section = teleportSection, position = 42)
    default int teleportWalkCostMultiplier()
    {
        return 1;
    }

    @Range(min = 1000, max = 60000)
    @ConfigItem(keyName = "teleportCooldownMs", name = "Teleport cooldown ms", description = "Delay between teleport attempts.", section = teleportSection, position = 43)
    default int teleportCooldownMs()
    {
        return 8000;
    }

    @Range(min = 0, max = 15000)
    @ConfigItem(keyName = "teleportPostCastWaitMs", name = "Teleport post-cast wait ms", description = "Reserved wait setting for plugin-specific teleport actions.", section = teleportSection, position = 44)
    default int teleportPostCastWaitMs()
    {
        return 4500;
    }

    @ConfigItem(keyName = "showTileMarkers", name = "Show tile markers", description = "Draw scene markers.", section = overlaySection, position = 50)
    default boolean showTileMarkers()
    {
        return true;
    }

    @ConfigItem(keyName = "showActiveEdgeMarkers", name = "Show edge markers", description = "Draw active edge markers.", section = overlaySection, position = 51)
    default boolean showActiveEdgeMarkers()
    {
        return true;
    }

    @ConfigItem(
        keyName = "debugLevel",
        name = "Debug level",
        description = "Controls how much walker diagnostic information is logged.",
        section = debugSection,
        position = 60
    )
    default KspWalkerDebugLevel debugLevel()
    {
        return KspWalkerDebugLevel.VERBOSE;
    }

    @ConfigItem(
        keyName = "logEveryWalkerTick",
        name = "Log every walker tick",
        description = "Logs one structured diagnostic line for every walker loop.",
        section = debugSection,
        position = 61
    )
    default boolean logEveryWalkerTick()
    {
        return true;
    }



    @ConfigItem(
        keyName = "showDebugPath",
        name = "Show debug path",
        description = "Draw a line from the player/start tile to the target/end tile.",
        section = overlaySection,
        position = 52
    )
    default boolean showDebugPath()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showCheckpointSegment",
        name = "Show checkpoint segment",
        description = "Draw a line from the player to the currently selected next checkpoint.",
        section = overlaySection,
        position = 53
    )
    default boolean showCheckpointSegment()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showRouteEdgeSegment",
        name = "Show route edge segment",
        description = "Draw the active graph edge start-to-end segment.",
        section = overlaySection,
        position = 54
    )
    default boolean showRouteEdgeSegment()
    {
        return false;
    }

    @Range(min = 5, max = 150)
    @ConfigItem(
        keyName = "debugPathMaxTiles",
        name = "Debug path max tiles",
        description = "Maximum number of path tiles drawn for each debug line.",
        section = overlaySection,
        position = 55
    )
    default int debugPathMaxTiles()
    {
        return 80;
    }



    @Range(min = 0, max = 60000)
    @ConfigItem(
        keyName = "runToggleCooldownMs",
        name = "Run toggle cooldown ms",
        description = "Minimum delay between run-toggle attempts. Prevents spam-clicking the run orb.",
        section = walkingSection,
        position = 16
    )
    default int runToggleCooldownMs()
    {
        return 8000;
    }



    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "runEnergyMin",
        name = "Run energy min",
        description = "Minimum random run energy threshold.",
        section = walkingSection,
        position = 17
    )
    default int runEnergyMin()
    {
        return 10;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
        keyName = "runEnergyMax",
        name = "Run energy max",
        description = "Maximum random run energy threshold.",
        section = walkingSection,
        position = 18
    )
    default int runEnergyMax()
    {
        return 30;
    }


}
