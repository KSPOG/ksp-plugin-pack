package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspPathObstacleHandler
{
    private static final int DEFAULT_OBSTACLE_SCAN_RADIUS = 8;
    private static final double DEFAULT_LINE_TOLERANCE = 0.65;
    private static final double DEFAULT_EXTRA_DISTANCE = 1.0;

    private static final String[] BLOCKING_ACTIONS =
    {
        "Open",
        "Climb-up",
        "Climb-down",
        "Climb",
        "Enter",
        "Exit",
        "Pass",
        "Pass-through",
        "Cross",
        "Jump",
        "Squeeze-through"
    };

    public KspPathObstacle findBlockingObstacle(WorldPoint checkpoint, KspWalkSettings settings)
    {
        if (checkpoint == null || settings == null || !settings.isAutoObstacleInteraction())
        {
            return null;
        }

        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || player.getPlane() != checkpoint.getPlane())
        {
            return null;
        }

        return scanCandidateObjects(player, checkpoint).stream()
            .map(object -> toBlockingObstacle(object, player, checkpoint, "blocking-required"))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator
                .comparingDouble(KspPathObstacle::getSegmentProgress)
                .thenComparingDouble(KspPathObstacle::getLineDistance))
            .orElse(null);
    }

    public KspPathObstacle findFallbackObstacle(WorldPoint target, KspWalkSettings settings)
    {
        if (target == null || settings == null || !settings.isAutoObstacleInteraction())
        {
            return null;
        }

        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null || player.getPlane() != target.getPlane())
        {
            return null;
        }

        return scanCandidateObjects(player, target).stream()
            .map(object -> toBlockingObstacle(object, player, target, "fallback-required"))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator
                .comparingDouble(KspPathObstacle::getSegmentProgress)
                .thenComparingDouble(KspPathObstacle::getLineDistance))
            .orElse(null);
    }

    public KspWalkResult interact(KspPathObstacle obstacle, WorldPoint target)
    {
        if (obstacle == null)
        {
            return KspWalkResult.failed(target, "Path obstacle is null");
        }

        WorldPoint tile = obstacle.getWorldPoint();

        if (tile == null)
        {
            return KspWalkResult.failed(target, "Path obstacle tile is null");
        }

        String primaryAction = obstacle.getAction();

        if (tryInteract(tile, primaryAction))
        {
            return KspWalkResult.walking(
                target,
                tile,
                "Interacted with path obstacle: "
                    + obstacle.getDebugName()
                    + " action="
                    + primaryAction
                    + " tile="
                    + compact(tile)
            );
        }

        for (String action : BLOCKING_ACTIONS)
        {
            if (action == null || action.equalsIgnoreCase(primaryAction))
            {
                continue;
            }

            if (tryInteract(tile, action))
            {
                return KspWalkResult.walking(
                    target,
                    tile,
                    "Interacted with path obstacle: "
                        + obstacle.getDebugName()
                        + " action="
                        + action
                        + " tile="
                        + compact(tile)
                );
            }
        }

        return KspWalkResult.failed(
            target,
            "Failed to interact with path obstacle: "
                + obstacle.getDebugName()
                + " tile="
                + compact(tile)
        );
    }

    private boolean tryInteract(WorldPoint tile, String action)
    {
        if (tile == null || action == null || action.isBlank())
        {
            return false;
        }

        try
        {
            return Rs2GameObject.interact(tile, action);
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private List<GameObject> scanCandidateObjects(WorldPoint player, WorldPoint toward)
    {
        int radius = Math.max(2, Math.min(DEFAULT_OBSTACLE_SCAN_RADIUS, Math.max(3, player.distanceTo2D(toward) + 1)));

        return Rs2GameObject.getGameObjects(
            object -> object != null
                && object.getWorldLocation() != null
                && object.getWorldLocation().getPlane() == player.getPlane()
                && player.distanceTo2D(object.getWorldLocation()) <= radius
        );
    }

    private Optional<KspPathObstacle> toBlockingObstacle(
        GameObject object,
        WorldPoint player,
        WorldPoint checkpoint,
        String reason
    )
    {
        if (object == null || object.getWorldLocation() == null)
        {
            return Optional.empty();
        }

        WorldPoint obstacleTile = object.getWorldLocation();

        if (obstacleTile.getPlane() != player.getPlane())
        {
            return Optional.empty();
        }

        SegmentProjection projection = project(player, checkpoint, obstacleTile);

        /*
         * Must be physically between player and checkpoint/target.
         */
        if (projection.progress < 0.05 || projection.progress > 0.98)
        {
            return Optional.empty();
        }

        /*
         * Main side-room protection:
         * The object tile must be near the exact movement segment, not merely nearby.
         */
        if (projection.lineDistance > DEFAULT_LINE_TOLERANCE)
        {
            return Optional.empty();
        }

        double directDistance = distance(player, checkpoint);
        double viaObstacleDistance = distance(player, obstacleTile) + distance(obstacleTile, checkpoint);

        if (viaObstacleDistance > directDistance + DEFAULT_EXTRA_DISTANCE)
        {
            return Optional.empty();
        }

        if (!isOnSegmentThreshold(player, checkpoint, obstacleTile))
        {
            return Optional.empty();
        }

        /*
         * This branch cannot query GameObject names/actions directly, so we keep the
         * detection purely geometry-based and let interact(...) try known blocking
         * actions on the exact obstacle tile.
         */
        return Optional.of(new KspPathObstacle(
            object,
            obstacleTile,
            "Open",
            reason + ":Object",
            projection.progress,
            projection.lineDistance
        ));
    }

    private boolean isOnSegmentThreshold(WorldPoint player, WorldPoint checkpoint, WorldPoint obstacleTile)
    {
        List<WorldPoint> segmentTiles = KspDebugPathBuilder.line(player, checkpoint);

        for (WorldPoint segmentTile : segmentTiles)
        {
            if (segmentTile.equals(obstacleTile))
            {
                return true;
            }

            /*
             * Allows exact threshold-adjacent door/gate tiles while still preventing
             * side-room doors that are not on this movement segment.
             */
            if (segmentTile.distanceTo2D(obstacleTile) <= 1)
            {
                return true;
            }
        }

        return false;
    }

    private SegmentProjection project(WorldPoint start, WorldPoint end, WorldPoint point)
    {
        double ax = start.getX();
        double ay = start.getY();
        double bx = end.getX();
        double by = end.getY();
        double px = point.getX();
        double py = point.getY();

        double vx = bx - ax;
        double vy = by - ay;
        double wx = px - ax;
        double wy = py - ay;

        double lenSq = vx * vx + vy * vy;

        if (lenSq <= 0.0)
        {
            return new SegmentProjection(0.0, distance(start, point));
        }

        double progress = (wx * vx + wy * vy) / lenSq;
        double clamped = Math.max(0.0, Math.min(1.0, progress));

        double closestX = ax + clamped * vx;
        double closestY = ay + clamped * vy;
        double lineDistance = Math.hypot(px - closestX, py - closestY);

        return new SegmentProjection(progress, lineDistance);
    }

    private double distance(WorldPoint a, WorldPoint b)
    {
        if (a == null || b == null)
        {
            return Double.MAX_VALUE;
        }

        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private String compact(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + ",p" + point.getPlane();
    }

    private static final class SegmentProjection
    {
        private final double progress;
        private final double lineDistance;

        private SegmentProjection(double progress, double lineDistance)
        {
            this.progress = progress;
            this.lineDistance = lineDistance;
        }
    }

    public static final class KspPathObstacle
    {
        private final GameObject object;
        private final WorldPoint tile;
        private final String action;
        private final String debugName;
        private final double segmentProgress;
        private final double lineDistance;

        public KspPathObstacle(
            GameObject object,
            WorldPoint tile,
            String action,
            String debugName,
            double segmentProgress,
            double lineDistance
        )
        {
            this.object = object;
            this.tile = tile;
            this.action = action == null || action.isBlank() ? "Open" : action;
            this.debugName = debugName == null || debugName.isBlank() ? "Object" : debugName;
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
            return debugName;
        }

        public String getDebugName()
        {
            return debugName;
        }

        public double getSegmentProgress()
        {
            return segmentProgress;
        }

        public double getLineDistance()
        {
            return lineDistance;
        }
    }
}
