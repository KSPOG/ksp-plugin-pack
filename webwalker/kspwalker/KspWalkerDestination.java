package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspWalkerDestination
{
    private final String name;
    private final KspWalkerDestinationType type;
    private final WorldPoint point;
    private final boolean members;

    public KspWalkerDestination(String name, KspWalkerDestinationType type, WorldPoint point)
    {
        this(name, type, point, false);
    }

    public KspWalkerDestination(String name, KspWalkerDestinationType type, WorldPoint point, boolean members)
    {
        this.name = name == null ? "Unknown" : name;
        this.type = type == null ? KspWalkerDestinationType.MANUAL : type;
        this.point = point;
        this.members = members;
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

    public boolean isMembers()
    {
        return members;
    }

    public boolean isMembersOnly()
    {
        return members;
    }

    @Override
    public String toString()
    {
        return name + " @ " + format(point) + " members=" + (members ? "yes" : "no");
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
