package net.runelite.client.plugins.microbot.kspwalker;

import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public final class KspMovementExecutor
{
    private final KspLocalPathfinder localPathfinder;
    private final KspPathObstacleHandler pathObstacleHandler = new KspPathObstacleHandler();
    private final KspMovementClicker movementClicker = new KspMovementClicker();

    private volatile WorldPoint lastPlannedTile;
    private volatile WorldPoint lastClickedTile;
    private volatile WorldPoint lastObstacleTile;
    private volatile String lastObstacleName = "-";

    private long lastObstacleAtMs;
    private long lastClickAtMs;

    public KspMovementExecutor(KspLocalPathfinder localPathfinder)
    {
        this.localPathfinder = localPathfinder;
    }

    public WorldPoint getLastPlannedTile()
    {
        return lastPlannedTile;
    }

    public WorldPoint getLastClickedTile()
    {
        return lastClickedTile;
    }

    public WorldPoint getLastObstacleTile()
    {
        return lastObstacleTile;
    }

    public String getLastObstacleName()
    {
        return lastObstacleName;
    }

    public KspMovementClickMethod getLastClickMethod()
    {
        return movementClicker.getLastMethod();
    }

    public List<WorldPoint> getLastDebugPath()
    {
        return localPathfinder.getLastDebugPath();
    }

    public void reset()
    {
        lastPlannedTile = null;
        lastClickedTile = null;
        lastObstacleTile = null;
        lastObstacleName = "-";
        lastObstacleAtMs = 0L;
        lastClickAtMs = 0L;
        localPathfinder.reset();
    }

    public KspWalkResult walkLocalTo(WorldPoint target, KspWalkSettings settings)
    {
        if (target == null)
        {
            return KspWalkResult.failed(null, "Target is null");
        }

        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null)
        {
            return KspWalkResult.failed(target, "Player location is null");
        }

        if (player.getPlane() == target.getPlane()
            && player.distanceTo2D(target) <= settings.getFinishDistance())
        {
            return KspWalkResult.arrived(target);
        }

        long now = System.currentTimeMillis();

        int distanceToLastClicked = distanceToLastClicked(player);
        long sinceLastClickMs = now - lastClickAtMs;
        long movingAdvanceDelayMs = Math.min(settings.getMovingReclickDelayMs(), settings.getClickCooldownMs());

        boolean mayAdvanceCheckpoint = lastClickedTile == null
            || distanceToLastClicked <= settings.getCheckpointAdvanceDistance()
            || sinceLastClickMs >= movingAdvanceDelayMs;

        if (sinceLastClickMs < settings.getClickCooldownMs())
        {
            return KspWalkResult.waiting(
                target,
                "Click cooldown; remainingMs="
                    + Math.max(0L, settings.getClickCooldownMs() - sinceLastClickMs)
                    + " moving=" + Rs2Player.isMoving()
                    + " distanceToLastClicked=" + distanceToLastClicked
            );
        }

        if (!mayAdvanceCheckpoint)
        {
            return KspWalkResult.waiting(
                target,
                "Waiting for checkpoint advance; moving="
                    + Rs2Player.isMoving()
                    + " distance=" + distanceToLastClicked
                    + " advanceDistance=" + settings.getCheckpointAdvanceDistance()
                    + " reclickDueMs=" + movingAdvanceDelayMs
            );
        }

        Optional<WorldPoint> nextStep = localPathfinder.findNextStep(target, settings);

        if (nextStep.isEmpty())
        {
            KspWalkResult fallbackObstacleResult = tryHandleFallbackObstacle(target, settings, now);

            if (fallbackObstacleResult != null)
            {
                return fallbackObstacleResult;
            }

            return KspWalkResult.noLocalStep(target, "No reachable local step toward target; target=" + compact(target) + " pathMs=" + localPathfinder.getLastCalculationMs() + " pathDecision=" + localPathfinder.getLastDecision());
        }

        WorldPoint clickTile = nextStep.get();

        /*
         * Blocking obstacle mode:
         *
         * Now that we know the exact next checkpoint, check only the short segment:
         *
         * player -> clickTile
         *
         * If a door/gate/stair/ladder/trapdoor physically lies between those two points,
         * interact with it instead of clicking through it.
         *
         * This is strict enough to avoid clicking random doors that are merely near the
         * longer player -> target route.
         */
        KspWalkResult blockingObstacleResult = tryHandleBlockingObstacle(clickTile, target, settings, now);

        if (blockingObstacleResult != null)
        {
            return blockingObstacleResult;
        }

        lastPlannedTile = clickTile;

        boolean clicked = movementClicker.click(clickTile, settings);

        if (!clicked)
        {
            return KspWalkResult.failed(target, "walkFastCanvas failed for " + compact(clickTile));
        }

        lastClickAtMs = now;
        lastClickedTile = clickTile;

        return KspWalkResult.walking(target, clickTile, "Clicked local step " + compact(clickTile) + " method=" + movementClicker.getLastMethod() + " pathMs=" + localPathfinder.getLastCalculationMs() + " pathDecision=" + localPathfinder.getLastDecision());
    }

    public KspWalkResult forceNudge(WorldPoint target, KspWalkSettings settings)
    {
        if (target == null)
        {
            return KspWalkResult.failed(null, "Target is null");
        }

        long now = System.currentTimeMillis();

        if (Rs2Player.isMoving())
        {
            return KspWalkResult.waiting(target, "Recovery skipped; player already moving");
        }

        Optional<WorldPoint> nextStep = localPathfinder.findNextStep(target, settings);

        if (nextStep.isEmpty())
        {
            KspWalkResult fallbackObstacleResult = tryHandleFallbackObstacle(target, settings, now);

            if (fallbackObstacleResult != null)
            {
                return fallbackObstacleResult;
            }

            return KspWalkResult.noLocalStep(target, "Stuck recovery failed: no local nudge tile");
        }

        WorldPoint clickTile = nextStep.get();

        KspWalkResult blockingObstacleResult = tryHandleBlockingObstacle(clickTile, target, settings, now);

        if (blockingObstacleResult != null)
        {
            return blockingObstacleResult;
        }

        lastPlannedTile = clickTile;

        boolean clicked = movementClicker.click(clickTile, settings);

        if (!clicked)
        {
            return KspWalkResult.failed(target, "Stuck recovery click failed for " + compact(clickTile));
        }

        lastClickAtMs = now;
        lastClickedTile = clickTile;

        return KspWalkResult.stuckRecovery(target, clickTile, "Forced nudge " + compact(clickTile) + " method=" + movementClicker.getLastMethod() + " pathMs=" + localPathfinder.getLastCalculationMs() + " pathDecision=" + localPathfinder.getLastDecision());
    }

    private KspWalkResult tryHandleBlockingObstacle(
        WorldPoint checkpoint,
        WorldPoint finalTarget,
        KspWalkSettings settings,
        long now
    )
    {
        if (!settings.isAutoObstacleInteraction() || !settings.isBlockingObstacleInteraction())
        {
            return null;
        }

        if (now - lastObstacleAtMs < settings.getObstacleCooldownMs())
        {
            return null;
        }

        KspPathObstacleHandler.KspPathObstacle obstacle = pathObstacleHandler.findBlockingObstacle(checkpoint, settings);

        if (obstacle == null)
        {
            return null;
        }

        KspWalkResult result = pathObstacleHandler.interact(obstacle, finalTarget);

        if (result.isSuccess())
        {
            lastObstacleAtMs = now;
            lastObstacleTile = obstacle.getWorldPoint();
            lastObstacleName = "blocking-required: " + obstacle.getDebugName();

            lastPlannedTile = checkpoint;
            lastClickedTile = obstacle.getWorldPoint();
            lastClickAtMs = now;
        }

        return result;
    }

    private KspWalkResult tryHandleFallbackObstacle(WorldPoint target, KspWalkSettings settings, long now)
    {
        if (!settings.isAutoObstacleInteraction())
        {
            return null;
        }

        if (now - lastObstacleAtMs < settings.getObstacleCooldownMs())
        {
            return null;
        }

        KspPathObstacleHandler.KspPathObstacle obstacle = pathObstacleHandler.findFallbackObstacle(target, settings);

        if (obstacle == null)
        {
            return null;
        }

        KspWalkResult result = pathObstacleHandler.interact(obstacle, target);

        if (result.isSuccess())
        {
            lastObstacleAtMs = now;
            lastObstacleTile = obstacle.getWorldPoint();
            lastObstacleName = "fallback-required: " + obstacle.getDebugName();

            lastPlannedTile = obstacle.getWorldPoint();
            lastClickedTile = obstacle.getWorldPoint();
            lastClickAtMs = now;
        }

        return result;
    }

    private int distanceToLastClicked(WorldPoint player)
    {
        if (player == null || lastClickedTile == null)
        {
            return Integer.MAX_VALUE;
        }

        if (player.getPlane() != lastClickedTile.getPlane())
        {
            return Integer.MAX_VALUE;
        }

        return player.distanceTo2D(lastClickedTile);
    }

    private String compact(WorldPoint point)
    {
        if (point == null)
        {
            return "?";
        }

        return point.getX() + "," + point.getY() + ",p" + point.getPlane();
    }
}
