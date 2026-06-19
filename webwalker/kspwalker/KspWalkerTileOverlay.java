package net.runelite.client.plugins.microbot.kspwalker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class KspWalkerTileOverlay extends Overlay
{
    private static final Color TARGET_FILL = new Color(80, 180, 255, 70);
    private static final Color TARGET_OUTLINE = new Color(80, 180, 255, 230);

    private static final Color NEXT_FILL = new Color(255, 230, 40, 70);
    private static final Color NEXT_OUTLINE = new Color(255, 230, 40, 230);

    private static final Color CLICK_FILL = new Color(255, 255, 255, 45);
    private static final Color CLICK_OUTLINE = new Color(255, 255, 255, 200);

    private static final Color EDGE_START_FILL = new Color(40, 255, 120, 65);
    private static final Color EDGE_START_OUTLINE = new Color(40, 255, 120, 230);

    private static final Color EDGE_END_FILL = new Color(255, 80, 80, 65);
    private static final Color EDGE_END_OUTLINE = new Color(255, 80, 80, 230);

    private static final Color OBSTACLE_FILL = new Color(255, 150, 40, 65);
    private static final Color OBSTACLE_OUTLINE = new Color(255, 150, 40, 230);

    private static final Color TELEPORT_FILL = new Color(190, 120, 255, 55);
    private static final Color TELEPORT_OUTLINE = new Color(190, 120, 255, 230);

    private static final Color DEBUG_PATH_FILL = new Color(0, 220, 255, 35);
    private static final Color DEBUG_PATH_OUTLINE = new Color(0, 220, 255, 150);

    private static final Color CHECKPOINT_SEGMENT_FILL = new Color(255, 255, 0, 45);
    private static final Color CHECKPOINT_SEGMENT_OUTLINE = new Color(255, 255, 0, 180);

    private static final Color EDGE_SEGMENT_FILL = new Color(255, 90, 255, 35);
    private static final Color EDGE_SEGMENT_OUTLINE = new Color(255, 90, 255, 150);

    private final KspWalkerTesterPlugin plugin;
    private final KspWalkerTesterConfig config;

    @Inject
    public KspWalkerTileOverlay(KspWalkerTesterPlugin plugin, KspWalkerTesterConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showTileMarkers())
        {
            return null;
        }

        KspWalkerTesterScript script = plugin.getScript();
        KspWebWalker walker = script.getWalker();
        WorldPoint player = Rs2Player.getWorldLocation();

        KspWalkerDebugState debugState = walker.getDebugState();

        if (config.showDebugPath())
        {
            drawPathPoints(
                graphics,
                walker.getLastDebugPath(),
                DEBUG_PATH_FILL,
                DEBUG_PATH_OUTLINE,
                "PATH"
            );
        }

        if (config.showCheckpointSegment())
        {
            drawPath(
                graphics,
                player,
                walker.getLastPlannedTile(),
                CHECKPOINT_SEGMENT_FILL,
                CHECKPOINT_SEGMENT_OUTLINE,
                "NEXT-DYNAMIC",
                Math.min(config.debugPathMaxTiles(), 40)
            );
        }

        if (config.showRouteEdgeSegment())
        {
            KspWebEdge edge = walker.getActiveEdge();

            if (edge != null)
            {
                drawPath(
                    graphics,
                    edge.getStart(),
                    edge.getEnd(),
                    EDGE_SEGMENT_FILL,
                    EDGE_SEGMENT_OUTLINE,
                    "EDGE-LINE",
                    Math.min(config.debugPathMaxTiles(), 80)
                );
            }
        }

        drawTile(graphics, script.getLastTarget(), TARGET_FILL, TARGET_OUTLINE, "TARGET");
        drawTile(graphics, walker.getLastPlannedTile(), NEXT_FILL, NEXT_OUTLINE, "NEXT");

        if (walker.getLastPlannedTile() == null)
        {
            drawTile(graphics, walker.getLastClickedTile(), CLICK_FILL, CLICK_OUTLINE, "CLICK");
        }
        else
        {
            drawTile(graphics, walker.getLastClickedTile(), CLICK_FILL, CLICK_OUTLINE, "CLICK");
        }

        drawTile(graphics, walker.getLastObstacleTile(), OBSTACLE_FILL, OBSTACLE_OUTLINE, "OBSTACLE");
        drawTile(graphics, walker.getLastTeleportDestination(), TELEPORT_FILL, TELEPORT_OUTLINE, "TELEPORT");

        if (config.showActiveEdgeMarkers())
        {
            KspWebEdge edge = walker.getActiveEdge();

            if (edge != null)
            {
                drawTile(graphics, edge.getStart(), EDGE_START_FILL, EDGE_START_OUTLINE, "EDGE START");
                drawTile(graphics, edge.getEnd(), EDGE_END_FILL, EDGE_END_OUTLINE, "EDGE END");
            }
        }

        return null;
    }

    private void drawPathPoints(
        Graphics2D graphics,
        List<WorldPoint> points,
        Color fill,
        Color outline,
        String label
    )
    {
        if (points == null || points.isEmpty())
        {
            return;
        }

        int maxTiles = Math.max(1, config.debugPathMaxTiles());
        int drawn = 0;

        for (int i = 0; i < points.size(); i++)
        {
            if (drawn >= maxTiles)
            {
                break;
            }

            WorldPoint point = points.get(i);

            if (i == 0)
            {
                drawTile(graphics, point, fill, outline, label + " START");
            }
            else if (i == points.size() - 1)
            {
                drawTile(graphics, point, fill, outline, label + " END");
            }
            else
            {
                drawTile(graphics, point, fill, outline, null);
            }

            drawn++;
        }
    }

    private void drawPath(
        Graphics2D graphics,
        WorldPoint start,
        WorldPoint end,
        Color fill,
        Color outline,
        String label,
        int maxTiles
    )
    {
        if (start == null || end == null || start.getPlane() != end.getPlane())
        {
            return;
        }

        List<WorldPoint> points = KspDebugPathBuilder.sampledLine(start, end, maxTiles);

        for (int i = 0; i < points.size(); i++)
        {
            WorldPoint point = points.get(i);

            if (i == 0)
            {
                drawTile(graphics, point, fill, outline, label + " START");
            }
            else if (i == points.size() - 1)
            {
                drawTile(graphics, point, fill, outline, label + " END");
            }
            else
            {
                drawTile(graphics, point, fill, outline, null);
            }
        }
    }

    private void drawTile(Graphics2D graphics, WorldPoint worldPoint, Color fill, Color outline, String label)
    {
        if (worldPoint == null)
        {
            return;
        }

        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || player.getPlane() != worldPoint.getPlane())
        {
            return;
        }

        LocalPoint localPoint;

        try
        {
            localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint);
        }
        catch (RuntimeException ex)
        {
            return;
        }

        if (localPoint == null)
        {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(Microbot.getClient(), localPoint);

        if (polygon == null)
        {
            return;
        }

        graphics.setColor(fill);
        graphics.fillPolygon(polygon);

        graphics.setColor(outline);
        graphics.drawPolygon(polygon);

        if (label != null && !label.isBlank())
        {
            graphics.drawString(label, polygon.getBounds().x, polygon.getBounds().y);
        }
    }
}
