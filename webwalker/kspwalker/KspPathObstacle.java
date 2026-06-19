package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

public final class KspPathObstacle
{
    private final GameObject object;
    private final WorldPoint tile;
    private final String action;
    private final String name;
    private final double segmentProgress;
    private final double lineDistance;

    public KspPathObstacle(
        GameObject object,
        WorldPoint tile,
        String action,
        String name,
        double segmentProgress,
        double lineDistance
    )
    {
        this.object = object;
        this.tile = tile;
        this.action = action;
        this.name = name;
        this.segmentProgress = segmentProgress;
        this.lineDistance = lineDistance;
    }

    public GameObject getObject()
    {
        return object;
    }

    public WorldPoint getTile()
    {
        return tile;
    }

    public WorldPoint getWorldPoint()
    {
        return tile;
    }

    public String getAction()
    {
        return action;
    }

    public String getName()
    {
        return name;
    }

    public double getSegmentProgress()
    {
        return segmentProgress;
    }

    public double getLineDistance()
    {
        return lineDistance;
    }

    @Override
    public String toString()
    {
        return "KspPathObstacle{"
            + "name='" + name + '\''
            + ", action='" + action + '\''
            + ", tile=" + format(tile)
            + ", segmentProgress=" + segmentProgress
            + ", lineDistance=" + lineDistance
            + '}';
    }

    private String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }
}
