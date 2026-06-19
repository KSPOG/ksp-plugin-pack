package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspExplvMapLink
{
    private static final String BASE_URL = "https://explv.github.io/";

    private KspExplvMapLink()
    {
    }

    public static String forTile(WorldPoint tile)
    {
        return forTile(tile, 11);
    }

    public static String forTile(WorldPoint tile, int zoom)
    {
        if (tile == null)
        {
            return "-";
        }

        int safeZoom = Math.max(1, Math.min(20, zoom));

        return BASE_URL
            + "?centreX=" + tile.getX()
            + "&centreY=" + tile.getY()
            + "&centreZ=" + tile.getPlane()
            + "&zoom=" + safeZoom;
    }

    public static String compact(WorldPoint tile)
    {
        if (tile == null)
        {
            return "-";
        }

        return tile.getX() + "," + tile.getY() + "," + tile.getPlane()
            + " " + forTile(tile);
    }
}
