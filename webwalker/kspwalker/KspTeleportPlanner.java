package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Comparator;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

public final class KspTeleportPlanner
{
    private static final int UNREACHABLE_DISTANCE = 100_000;

    public Optional<KspTeleportPlan> findBestTeleport(
        KspPlayerState state,
        WorldPoint target,
        KspWalkSettings settings,
        KspTeleportRegistry registry
    )
    {
        if (state == null || !state.hasLocation() || target == null || settings == null || registry == null)
        {
            return Optional.empty();
        }

        if (!settings.isTeleportsEnabled() || registry.isEmpty())
        {
            return Optional.empty();
        }

        WorldPoint player = state.getLocation();
        int currentDistance = distance(player, target);

        if (currentDistance <= settings.getMinTeleportSavingDistance())
        {
            return Optional.empty();
        }

        return registry.getUsableOptions()
            .stream()
            .map(option -> toPlan(option, currentDistance, target, settings))
            .filter(plan -> plan != null)
            .filter(plan -> plan.getSavingDistance() >= settings.getMinTeleportSavingDistance())
            .min(Comparator.comparingInt(KspTeleportPlan::getScore));
    }

    private KspTeleportPlan toPlan(
        KspTeleportOption option,
        int currentDistance,
        WorldPoint target,
        KspWalkSettings settings
    )
    {
        if (option == null || option.getDestination() == null)
        {
            return null;
        }

        int postTeleportDistance = distance(option.getDestination(), target);

        if (postTeleportDistance >= UNREACHABLE_DISTANCE)
        {
            return null;
        }

        int saving = currentDistance - postTeleportDistance;

        if (saving <= 0)
        {
            return null;
        }

        int score = option.getCost() + postTeleportDistance * settings.getTeleportWalkCostMultiplier();

        return new KspTeleportPlan(option, currentDistance, postTeleportDistance, saving, score);
    }

    private int distance(WorldPoint a, WorldPoint b)
    {
        if (a == null || b == null)
        {
            return UNREACHABLE_DISTANCE;
        }

        if (a.getPlane() != b.getPlane())
        {
            return UNREACHABLE_DISTANCE;
        }

        return a.distanceTo2D(b);
    }
}
