package net.runelite.client.plugins.microbot.kspwalker;

import java.util.concurrent.ThreadLocalRandom;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public final class KspMovementClicker
{
    private static final long RUN_TOGGLE_THROTTLE_MS = 8_000L;
    private static final int RUN_VARP = 173;

    private long lastRunToggleAtMs;
    private int nextRunEnergyThreshold = -1;

    private volatile KspMovementClickMethod lastMethod = KspMovementClickMethod.NONE;

    public boolean click(WorldPoint tile, KspWalkSettings settings)
    {
        lastMethod = KspMovementClickMethod.NONE;

        if (tile == null || settings == null)
        {
            return false;
        }

        maybeToggleRun(settings);

        boolean onScreen = isTileOnScreen(tile);
        int distance = distanceToPlayer(tile);

        if (settings.isPreferMinimap())
        {
            /*
             * Preferred policy:
             *
             * - Use canvas only for close tiles that are actually visible.
             * - Use minimap for most checkpoint walking.
             * - If minimap fails but the tile is visible, fall back to canvas.
             */
            if (onScreen && distance <= settings.getCanvasMaxDistance())
            {
                if (Rs2Walker.walkFastCanvas(tile, false))
                {
                    lastMethod = KspMovementClickMethod.CANVAS;
                    return true;
                }

                if (Rs2Walker.walkMiniMap(tile))
                {
                    lastMethod = KspMovementClickMethod.CANVAS_THEN_MINIMAP;
                    return true;
                }

                return false;
            }

            if (Rs2Walker.walkMiniMap(tile))
            {
                lastMethod = KspMovementClickMethod.MINIMAP;
                return true;
            }

            if (onScreen && settings.isCanvasFallbackWhenMinimapFails())
            {
                if (Rs2Walker.walkFastCanvas(tile, false))
                {
                    lastMethod = KspMovementClickMethod.MINIMAP_THEN_CANVAS;
                    return true;
                }
            }

            return false;
        }

        /*
         * Old-style behavior:
         * use Microbot's walkFastCanvas helper, which internally uses canvas when possible
         * and falls back to minimap when the tile is not on screen.
         */
        if (Rs2Walker.walkFastCanvas(tile, false))
        {
            lastMethod = onScreen ? KspMovementClickMethod.CANVAS : KspMovementClickMethod.CANVAS_THEN_MINIMAP;
            return true;
        }

        return false;
    }

    public KspMovementClickMethod getLastMethod()
    {
        return lastMethod;
    }


    private void maybeToggleRun(KspWalkSettings settings)
    {
        if (settings == null || !settings.isToggleRun())
        {
            return;
        }

        /*
         * Run behavior:
         *
         * - never spam-click the run orb
         * - never try to toggle while run is already enabled
         * - pick a random threshold between min/max, default 10-30
         * - only enable run once current energy reaches that threshold
         */
        if (isRunEnabled())
        {
            return;
        }

        int energy = currentRunEnergyPercent();
        int threshold = nextRunThreshold(settings);

        if (energy < threshold)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = settings.getRunToggleCooldownMs() <= 0L
            ? RUN_TOGGLE_THROTTLE_MS
            : settings.getRunToggleCooldownMs();

        if (now - lastRunToggleAtMs < cooldown)
        {
            return;
        }

        lastRunToggleAtMs = now;
        nextRunEnergyThreshold = -1;
        Rs2Player.toggleRunEnergy(true);
    }

    private int nextRunThreshold(KspWalkSettings settings)
    {
        int min = Math.max(0, Math.min(100, settings.getRunEnergyMin()));
        int max = Math.max(0, Math.min(100, settings.getRunEnergyMax()));

        if (max < min)
        {
            int tmp = min;
            min = max;
            max = tmp;
        }

        if (nextRunEnergyThreshold < min || nextRunEnergyThreshold > max)
        {
            nextRunEnergyThreshold = ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        return nextRunEnergyThreshold;
    }

    private boolean isRunEnabled()
    {
        try
        {
            return Microbot.getClient().getVarpValue(RUN_VARP) == 1;
        }
        catch (RuntimeException ex)
        {
            return false;
        }
    }

    private int currentRunEnergyPercent()
    {
        try
        {
            int energy = Microbot.getClient().getEnergy();

            /*
             * RuneLite usually exposes run energy as percent * 100, e.g. 10000 = 100%.
             * Some helper branches may expose 0-100. Support both.
             */
            if (energy > 100)
            {
                energy /= 100;
            }

            return Math.max(0, Math.min(100, energy));
        }
        catch (RuntimeException ex)
        {
            return 0;
        }
    }

    private boolean isTileOnScreen(WorldPoint tile)
    {
        try
        {
            LocalPoint localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), tile);

            if (localPoint == null)
            {
                return false;
            }

            return Rs2Camera.isTileOnScreen(localPoint);
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private int distanceToPlayer(WorldPoint tile)
    {
        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || tile == null || player.getPlane() != tile.getPlane())
        {
            return Integer.MAX_VALUE;
        }

        return player.distanceTo2D(tile);
    }
}
