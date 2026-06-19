package net.runelite.client.plugins.microbot.kspwalker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class KspWalkerTesterOverlay extends OverlayPanel
{
    private final KspWalkerTesterPlugin plugin;

    @Inject
    public KspWalkerTesterOverlay(KspWalkerTesterPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        KspWalkerTesterScript script = plugin.getScript();
        KspWalkResult result = script.getLastResult();
        KspWebWalker walker = script.getWalker();

        panelComponent.getChildren().add(TitleComponent.builder()
            .text("KSP Walker Tester")
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Target")
            .right(format(script.getLastTarget()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Quick target")
            .right(trim(script.getExternalTargetName(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Status")
            .right(result == null ? "-" : String.valueOf(result.getStatus()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Message")
            .right(result == null ? "-" : trim(result.getMessage(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Next tile")
            .right(format(walker.getLastPlannedTile()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Clicked")
            .right(format(walker.getLastClickedTile()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Path distance")
            .right(pathDistance(script.getLastTarget()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Path start")
            .right(format(walker.getDebugState().getRouteStart()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Path tiles")
            .right(String.valueOf(walker.getLastDebugPath().size()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Explv target")
            .right(format(script.getLastTarget()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Checkpoint dist")
            .right(distanceTo(walker.getLastClickedTile()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Click method")
            .right(String.valueOf(walker.getLastClickMethod()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Obstacle")
            .right(trim(walker.getLastObstacleName(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Teleport")
            .right(trim(walker.getLastTeleportName(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Teleport dest")
            .right(format(walker.getLastTeleportDestination()))
            .build());

        KspWalkerDebugState debugState = walker.getDebugState();

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Decision")
            .right(trim(debugState.getDecision(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Teleport reason")
            .right(trim(debugState.getTeleportDecision(), 32))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Recoveries")
            .right(String.valueOf(walker.getRecoveryCount()))
            .build());

        panelComponent.getChildren().add(LineComponent.builder()
            .left("Tick")
            .right(String.valueOf(debugState.getTick()))
            .build());

        return super.render(graphics);
    }

    private String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private String pathDistance(WorldPoint target)
    {
        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || target == null || player.getPlane() != target.getPlane())
        {
            return "-";
        }

        return String.valueOf(player.distanceTo2D(target));
    }

    private String distanceTo(WorldPoint point)
    {
        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || point == null || player.getPlane() != point.getPlane())
        {
            return "-";
        }

        return String.valueOf(player.distanceTo2D(point));
    }

    private String trim(String value, int max)
    {
        if (value == null)
        {
            return "-";
        }

        if (value.length() <= max)
        {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }
}
