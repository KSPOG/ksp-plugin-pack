package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;

public final class KspPlayerState
{
    private final WorldPoint location;
    private final boolean moving;
    private final boolean animating;
    private final long capturedAtMs;

    private KspPlayerState(WorldPoint location, boolean moving, boolean animating)
    {
        this.location = location;
        this.moving = moving;
        this.animating = animating;
        this.capturedAtMs = System.currentTimeMillis();
    }

    public static KspPlayerState capture()
    {
        return new KspPlayerState(
            Rs2Player.getWorldLocation(),
            Rs2Player.isMoving(),
            Rs2Player.isAnimating(900)
        );
    }

    public WorldPoint getLocation()
    {
        return location;
    }

    public boolean isMoving()
    {
        return moving;
    }

    public boolean isAnimating()
    {
        return animating;
    }

    public long getCapturedAtMs()
    {
        return capturedAtMs;
    }

    public boolean hasLocation()
    {
        return location != null;
    }

    public boolean isSamePlane(WorldPoint point)
    {
        return location != null && point != null && location.getPlane() == point.getPlane();
    }

    public int distanceTo(WorldPoint point)
    {
        if (location == null || point == null)
        {
            return Integer.MAX_VALUE;
        }

        if (location.getPlane() != point.getPlane())
        {
            return Integer.MAX_VALUE;
        }

        return location.distanceTo2D(point);
    }

    public boolean isNear(WorldPoint point, int distance)
    {
        return distanceTo(point) <= Math.max(0, distance);
    }

    public boolean canReach(WorldPoint point)
    {
        return point != null && isSamePlane(point) && Rs2Tile.isTileReachable(point);
    }
}
