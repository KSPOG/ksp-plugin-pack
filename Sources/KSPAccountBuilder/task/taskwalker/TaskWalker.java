package net.runelite.client.plugins.microbot.kspaccountbuilder.task.taskwalker;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.function.Predicate;

public final class TaskWalker
{
    private TaskWalker()
    {
    }

    public static synchronized boolean walkToArea(WorldArea area, WorldPoint target, int reachedDistance)
    {
        if (area == null || target == null)
        {
            return false;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null && area.contains(playerLocation))
        {
            return true;
        }

        return Rs2Walker.walkTo(target, reachedDistance);
    }

    public static synchronized boolean walkToArea(Predicate<WorldPoint> containsTargetArea, WorldArea area, WorldPoint target, int reachedDistance)
    {
        if (containsTargetArea == null || target == null)
        {
            return false;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null && containsTargetArea.test(playerLocation))
        {
            return true;
        }

        return Rs2Walker.walkTo(target, reachedDistance);
    }

    public static synchronized boolean walkToNearestBankAndOpen()
    {
        return Rs2Bank.isOpen() || Rs2Bank.walkToBankAndUseBank() || Rs2Bank.openBank();
    }
}
