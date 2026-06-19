package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspTeleportExecutor
{
    private final KspTeleportPlanner planner = new KspTeleportPlanner();

    private long lastTeleportAtMs;
    private volatile String lastTeleportName = "-";
    private volatile WorldPoint lastTeleportDestination;
    private volatile KspTeleportPlan lastTeleportPlan;
    private volatile String lastTeleportDecision = "-";

    public KspWalkResult tryTeleport(
        KspPlayerState state,
        WorldPoint target,
        KspWalkSettings settings,
        KspTeleportRegistry registry
    )
    {
        if (state == null || settings == null || target == null || registry == null)
        {
            lastTeleportDecision = "missing state/settings/target/registry";
            return null;
        }

        if (!settings.isTeleportsEnabled())
        {
            lastTeleportDecision = "teleports disabled";
            return null;
        }

        if (Rs2Player.isMoving())
        {
            lastTeleportDecision = "player moving";
            return null;
        }

        long now = System.currentTimeMillis();

        if (now - lastTeleportAtMs < settings.getTeleportCooldownMs())
        {
            lastTeleportDecision = "cooldown remainingMs=" + Math.max(0L, settings.getTeleportCooldownMs() - (now - lastTeleportAtMs));
            return null;
        }

        Optional<KspTeleportPlan> plan = planner.findBestTeleport(state, target, settings, registry);

        if (plan.isEmpty())
        {
            lastTeleportDecision = "no beneficial usable teleport";
            return null;
        }

        KspTeleportPlan selected = plan.get();
        KspTeleportOption option = selected.getOption();

        lastTeleportDecision = "selected " + option.getName() + " saving=" + selected.getSavingDistance() + " postDistance=" + selected.getPostTeleportDistance() + " score=" + selected.getScore();

        KspWalkResult result = option.execute(target);

        if (result == null)
        {
            return KspWalkResult.failed(target, "Teleport returned null: " + option.getId());
        }

        if (result.isSuccess())
        {
            lastTeleportAtMs = now;
            lastTeleportName = option.getName();
            lastTeleportDestination = option.getDestination();
            lastTeleportPlan = selected;
        }

        return result;
    }

    public String getLastTeleportName()
    {
        return lastTeleportName;
    }

    public WorldPoint getLastTeleportDestination()
    {
        return lastTeleportDestination;
    }

    public KspTeleportPlan getLastTeleportPlan()
    {
        return lastTeleportPlan;
    }

    public long getLastTeleportAtMs()
    {
        return lastTeleportAtMs;
    }

    public String getLastTeleportDecision()
    {
        return lastTeleportDecision;
    }

    public void reset()
    {
        lastTeleportName = "-";
        lastTeleportDestination = null;
        lastTeleportPlan = null;
        lastTeleportAtMs = 0L;
        lastTeleportDecision = "reset";
    }
}
