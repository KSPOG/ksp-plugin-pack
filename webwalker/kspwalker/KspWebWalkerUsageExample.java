package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

/**
 * Copy the relevant parts of this example into your plugin/script.
 *
 * Do not run this class directly. It is only a wiring example.
 */
public final class KspWebWalkerUsageExample
{
    private final KspWebWalker walker = new KspWebWalker(
        KspWalkSettings.builder()
            .finishDistance(2)
            .localSearchRadius(13)
            .maxStepDistance(13)
            .idleTimeoutMs(2500)
            .recoveryCooldownMs(1400)
            .debugLogging(true)
            .build(),
        new KspWebGraph()
    );

    public KspWebWalkerUsageExample()
    {
        registerExampleEdges();
    }

    public boolean walkTick(WorldPoint target)
    {
        KspWalkResult result = walker.walkTo(target);
        return result.getStatus() == KspWalkStatus.ARRIVED;
    }

    private void registerExampleEdges()
    {
        /*
         * Add your known transports/obstacles here.
         *
         * Example shape:
         *
         * walker.getGraph().add(
         *     KspWebEdge.object(
         *         "romeo_juliet:staircase_up",
         *         new WorldPoint(3164, 3435, 0), // object / interaction side
         *         new WorldPoint(3164, 3435, 1), // expected landing side
         *         11799,                         // object id
         *         "Climb-up"
         *     )
         * );
         *
         * For an object by name instead of id:
         *
         * walker.getGraph().add(
         *     KspWebEdge.objectName(
         *         "generic:gate",
         *         new WorldPoint(0000, 0000, 0),
         *         new WorldPoint(0001, 0000, 0),
         *         "Gate",
         *         "Open"
         *     )
         * );
         *
         * For NPC travel:
         *
         * walker.getGraph().add(
         *     KspWebEdge.builder(
         *         "travel:npc",
         *         KspWebEdgeType.NPC,
         *         new WorldPoint(0000, 0000, 0),
         *         new WorldPoint(0005, 0005, 0)
         *     )
         *     .npcName("Sailor")
         *     .action("Travel")
         *     .dialogueOptions("Yes", "Travel")
         *     .cost(25)
         *     .build()
         * );
         */
    }
}
