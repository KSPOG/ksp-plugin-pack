package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspWalkerDestination
{
    private final String name;
    private final KspWalkerDestinationType type;
    private final WorldPoint point;

    public KspWalkerDestination(String name, KspWalkerDestinationType type, WorldPoint point)
    {
        this.name = name == null ? "Unknown" : name;
        this.type = type == null ? KspWalkerDestinationType.MANUAL : type;
        this.point = point;
    }

    public String getName()
    {
        return name;
    }

    public KspWalkerDestinationType getType()
    {
        return type;
    }

    public WorldPoint getPoint()
    {
        return point;
    }

    @Override
    public String toString()
    {
        return name + " @ " + format(point);
    }

    private String format(WorldPoint worldPoint)
    {
        if (worldPoint == null)
        {
            return "?";
        }

        return worldPoint.getX() + "," + worldPoint.getY() + "," + worldPoint.getPlane();
    }
}
