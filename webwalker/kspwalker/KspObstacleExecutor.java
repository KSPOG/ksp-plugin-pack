package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspObstacleExecutor
{
    private final KspMovementExecutor movementExecutor;

    public KspObstacleExecutor(KspMovementExecutor movementExecutor)
    {
        this.movementExecutor = movementExecutor;
    }

    public KspWalkResult execute(KspWebEdge edge, KspWalkSettings settings)
    {
        if (edge == null)
        {
            return KspWalkResult.failed(null, "Edge is null");
        }

        if (!edge.isEnabled())
        {
            return KspWalkResult.failed(edge.getEnd(), "Edge requirement failed: " + edge.getId());
        }

        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint start = edge.getStart();

        if (start != null && player != null && start.getPlane() == player.getPlane()
            && player.distanceTo2D(start) > settings.getEdgeStartDistance())
        {
            return movementExecutor.walkLocalTo(start, settings);
        }

        boolean started;

        switch (edge.getType())
        {
            case OBJECT:
                started = executeObject(edge);
                break;
            case NPC:
                started = executeNpc(edge);
                break;
            case DIALOGUE:
                started = executeDialogue(edge);
                break;
            case CUSTOM:
                if (edge.getCustomAction() == null)
                {
                    return KspWalkResult.failed(edge.getEnd(), "Custom edge has no action: " + edge.getId());
                }
                return edge.getCustomAction().get();
            default:
                return movementExecutor.walkLocalTo(edge.getEnd(), settings);
        }

        if (!started)
        {
            return KspWalkResult.failed(edge.getEnd(), "Failed to start edge: " + edge.getId());
        }

        boolean completed = waitForEdgeCompletion(edge, settings);
        return completed
            ? KspWalkResult.edgeExecuted(edge, "Completed edge: " + edge.getId())
            : KspWalkResult.waiting(edge.getEnd(), "Edge started, completion not confirmed yet: " + edge.getId());
    }

    private boolean executeObject(KspWebEdge edge)
    {
        String action = edge.getAction();

        if (edge.getObjectId() > 0)
        {
            return Rs2GameObject.interact(edge.getObjectId(), action);
        }

        if (edge.getObjectName() != null && !edge.getObjectName().isBlank())
        {
            return Rs2GameObject.interact(edge.getObjectName(), action);
        }

        if (edge.getStart() != null)
        {
            return Rs2GameObject.interact(edge.getStart(), action);
        }

        return false;
    }

    private boolean executeNpc(KspWebEdge edge)
    {
        String action = edge.getAction();

        if (edge.getNpcId() > 0)
        {
            return Rs2Npc.interact(edge.getNpcId(), action);
        }

        if (edge.getNpcName() != null && !edge.getNpcName().isBlank())
        {
            return Rs2Npc.interact(edge.getNpcName(), action);
        }

        return false;
    }

    private boolean executeDialogue(KspWebEdge edge)
    {
        String[] options = edge.getDialogueOptions();

        if (options.length == 0)
        {
            Rs2Dialogue.clickContinue();
            return true;
        }

        for (String option : options)
        {
            if (option == null || option.isBlank())
            {
                continue;
            }

            if (Rs2Dialogue.clickOption(option))
            {
                return true;
            }
        }

        return false;
    }

    private boolean waitForEdgeCompletion(KspWebEdge edge, KspWalkSettings settings)
    {
        long deadline = System.currentTimeMillis() + settings.getPostInteractionTimeoutMs();

        while (System.currentTimeMillis() < deadline && !Thread.currentThread().isInterrupted())
        {
            if (edge.isComplete())
            {
                return true;
            }

            if (edge.getEnd() != null)
            {
                WorldPoint player = Rs2Player.getWorldLocation();

                if (player != null && player.getPlane() == edge.getEnd().getPlane()
                    && player.distanceTo2D(edge.getEnd()) <= settings.getEdgeEndDistance())
                {
                    return true;
                }
            }

            handleDialogueOptions(edge);

            try
            {
                Thread.sleep(120L);
            }
            catch (InterruptedException interrupted)
            {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return edge.isComplete();
    }

    private void handleDialogueOptions(KspWebEdge edge)
    {
        if (Rs2Dialogue.hasContinue())
        {
            Rs2Dialogue.clickContinue();
            return;
        }

        for (String option : edge.getDialogueOptions())
        {
            if (option != null && !option.isBlank() && Rs2Dialogue.hasDialogueOption(option))
            {
                Rs2Dialogue.clickOption(option);
                return;
            }
        }
    }
}
