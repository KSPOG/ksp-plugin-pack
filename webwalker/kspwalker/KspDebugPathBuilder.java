package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public final class KspDebugPathBuilder
{
    private KspDebugPathBuilder()
    {
    }

    public static List<WorldPoint> line(WorldPoint start, WorldPoint end)
    {
        if (start == null || end == null)
        {
            return Collections.emptyList();
        }

        if (start.getPlane() != end.getPlane())
        {
            return Collections.emptyList();
        }

        List<WorldPoint> points = new ArrayList<>();

        int x0 = start.getX();
        int y0 = start.getY();
        int x1 = end.getX();
        int y1 = end.getY();
        int plane = start.getPlane();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;

        while (true)
        {
            points.add(new WorldPoint(x0, y0, plane));

            if (x0 == x1 && y0 == y1)
            {
                break;
            }

            int e2 = 2 * err;

            if (e2 > -dy)
            {
                err -= dy;
                x0 += sx;
            }

            if (e2 < dx)
            {
                err += dx;
                y0 += sy;
            }

            if (points.size() > 256)
            {
                break;
            }
        }

        return points;
    }

    public static List<WorldPoint> sampledLine(WorldPoint start, WorldPoint end, int maxTiles)
    {
        List<WorldPoint> line = line(start, end);

        if (maxTiles <= 0 || line.size() <= maxTiles)
        {
            return line;
        }

        List<WorldPoint> sampled = new ArrayList<>();
        double step = (line.size() - 1) / (double) (maxTiles - 1);

        for (int i = 0; i < maxTiles; i++)
        {
            int index = (int) Math.round(i * step);
            index = Math.max(0, Math.min(line.size() - 1, index));
            WorldPoint point = line.get(index);

            if (sampled.isEmpty() || !sampled.get(sampled.size() - 1).equals(point))
            {
                sampled.add(point);
            }
        }

        return sampled;
    }
}
