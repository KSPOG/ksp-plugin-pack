package net.runelite.client.plugins.microbot.kspaccountbuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public final class KspWalkerGuard
{
    private static final int SAME_TARGET_DISTANCE = 8;
    private static final Map<String, WalkRequest> WALK_REQUESTS = new ConcurrentHashMap<>();

    private KspWalkerGuard()
    {
    }

    public static boolean walkToDestination(
            String key,
            Supplier<WorldPoint> targetSupplier,
            Predicate<WorldPoint> destinationMatcher,
            int arriveDistance,
            long refireCooldownMs)
    {
        if (key == null || targetSupplier == null || destinationMatcher == null)
        {
            return false;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (matches(destinationMatcher, playerLocation))
        {
            clear(key);
            return false;
        }

        long now = System.currentTimeMillis();
        WalkRequest previous = WALK_REQUESTS.get(key);
        WorldPoint walkerTarget = Rs2Walker.getCurrentTarget();

        if (matches(destinationMatcher, walkerTarget))
        {
            if (previous == null)
            {
                WALK_REQUESTS.put(key, new WalkRequest(walkerTarget, now));
                return false;
            }

            if (now - previous.requestedAtMs < refireCooldownMs)
            {
                return false;
            }
        }

        if (previous != null
                && matches(destinationMatcher, previous.target)
                && now - previous.requestedAtMs < refireCooldownMs)
        {
            return false;
        }

        WorldPoint target = targetSupplier.get();
        if (target == null)
        {
            return false;
        }

        WALK_REQUESTS.put(key, new WalkRequest(target, now));
        Rs2Walker.walkTo(target, arriveDistance);
        return true;
    }

    public static boolean walkToPoint(String key, WorldPoint target, int arriveDistance, long refireCooldownMs)
    {
        if (!shouldWalkToPoint(key, target, arriveDistance, refireCooldownMs))
        {
            return false;
        }

        Rs2Walker.walkTo(target, arriveDistance);
        return true;
    }

    public static boolean walkFastCanvasToPoint(String key, WorldPoint target, int arriveDistance, long refireCooldownMs)
    {
        if (!shouldWalkToPoint(key, target, arriveDistance, refireCooldownMs))
        {
            return false;
        }

        Rs2Walker.walkFastCanvas(target);
        return true;
    }

    public static void clear(String key)
    {
        if (key != null)
        {
            WALK_REQUESTS.remove(key);
        }
    }

    private static boolean shouldWalkToPoint(String key, WorldPoint target, int arriveDistance, long refireCooldownMs)
    {
        if (key == null || target == null)
        {
            return false;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (isSameDestination(playerLocation, target, Math.max(0, arriveDistance)))
        {
            clear(key);
            return false;
        }

        long now = System.currentTimeMillis();
        WalkRequest previous = WALK_REQUESTS.get(key);
        int sameTargetDistance = Math.max(SAME_TARGET_DISTANCE, arriveDistance + 2);

        if (isSameDestination(Rs2Walker.getCurrentTarget(), target, sameTargetDistance))
        {
            if (previous == null)
            {
                WALK_REQUESTS.put(key, new WalkRequest(target, now));
                return false;
            }

            if (now - previous.requestedAtMs < refireCooldownMs)
            {
                return false;
            }
        }

        if (previous != null
                && isSameDestination(previous.target, target, sameTargetDistance)
                && now - previous.requestedAtMs < refireCooldownMs)
        {
            return false;
        }

        WALK_REQUESTS.put(key, new WalkRequest(target, now));
        return true;
    }

    private static boolean matches(Predicate<WorldPoint> matcher, WorldPoint point)
    {
        return point != null && matcher.test(point);
    }

    private static boolean isSameDestination(WorldPoint first, WorldPoint second, int distance)
    {
        return first != null
                && second != null
                && first.getPlane() == second.getPlane()
                && first.distanceTo(second) <= distance;
    }

    private static final class WalkRequest
    {
        private final WorldPoint target;
        private final long requestedAtMs;

        private WalkRequest(WorldPoint target, long requestedAtMs)
        {
            this.target = target;
            this.requestedAtMs = requestedAtMs;
        }
    }
}
