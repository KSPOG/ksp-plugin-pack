package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

/**
 * Usage examples only.
 *
 * The core teleport API is intentionally generic:
 * - spell requirements are supplied by your plugin
 * - item requirements are supplied by your plugin
 * - spell/item execution is supplied by your plugin
 *
 * This avoids forcing the walker core to depend on one exact Rs2Magic/Rs2Inventory
 * signature across Microbot versions.
 */
public final class KspTeleportUsageExample
{
    private final KspWebWalker walker = new KspWebWalker();

    public void registerExamples()
    {
        /*
         * Example spell teleport.
         *
         * Replace hasVarrockTeleportRequirements() and castVarrockTeleport()
         * with your own Rs2Magic/Rs2Inventory checks.
         */
        walker.getTeleportRegistry().add(
            KspTeleportOption.spell(
                "spell:varrock",
                "Varrock Teleport",
                new WorldPoint(3212, 3424, 0),
                35,
                this::hasVarrockTeleportRequirements,
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

        /*
         * Example item teleport.
         *
         * Replace hasTeleportItem() and useTeleportItem() with your own
         * Rs2Inventory/equipment checks and interaction.
         */
        walker.getTeleportRegistry().add(
            KspTeleportOption.item(
                "item:ring_of_dueling:castle_wars",
                "Ring of dueling -> Castle Wars",
                new WorldPoint(2440, 3090, 0),
                20,
                this::hasTeleportItem,
                () ->
                {
                    boolean used = useTeleportItem();

                    if (!used)
                    {
                        return KspWalkResult.failed(null, "Failed to use teleport item");
                    }

                    return KspWalkResult.waiting(null, "Using teleport item");
                }
            )
        );
    }

    private boolean hasVarrockTeleportRequirements()
    {
        return false;
    }

    private boolean castVarrockTeleport()
    {
        return false;
    }

    private boolean hasTeleportItem()
    {
        return false;
    }

    private boolean useTeleportItem()
    {
        return false;
    }
}
