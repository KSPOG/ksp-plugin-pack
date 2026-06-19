package net.runelite.client.plugins.microbot.kspwalker;

import java.util.List;
import java.util.Objects;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KspWebWalker
{
    private static final Logger log = LoggerFactory.getLogger(KspWebWalker.class);

    private final KspWebGraph graph;
    private final KspTeleportRegistry teleportRegistry;
    private final KspTeleportExecutor teleportExecutor;
    private final KspWebGraphPathfinder graphPathfinder;
    private final KspLocalPathfinder localPathfinder;
    private final KspMovementExecutor movementExecutor;
    private final KspObstacleExecutor obstacleExecutor;
    private final KspStuckRecovery stuckRecovery;
    private final KspWalkerDebugState debugState = new KspWalkerDebugState();

    private KspWalkSettings settings;
    private KspWebRoute activeRoute;
    private WorldPoint activeTarget;

    public KspWebWalker()
    {
        this(KspWalkSettings.defaults(), new KspWebGraph(), new KspTeleportRegistry());
    }

    public KspWebWalker(KspWalkSettings settings, KspWebGraph graph)
    {
        this(settings, graph, new KspTeleportRegistry());
    }

    public KspWebWalker(KspWalkSettings settings, KspWebGraph graph, KspTeleportRegistry teleportRegistry)
    {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.graph = Objects.requireNonNull(graph, "graph");
        this.teleportRegistry = Objects.requireNonNull(teleportRegistry, "teleportRegistry");
        this.teleportExecutor = new KspTeleportExecutor();
        this.graphPathfinder = new KspWebGraphPathfinder();
        this.localPathfinder = new KspLocalPathfinder();
        this.movementExecutor = new KspMovementExecutor(localPathfinder);
        this.obstacleExecutor = new KspObstacleExecutor(movementExecutor);
        this.stuckRecovery = new KspStuckRecovery();
    }

    public KspWalkResult walkTo(WorldPoint target)
    {
        KspWalkResult finalResult;

        if (target == null)
        {
            finalResult = KspWalkResult.failed(null, "Target is null");
            debugState.finishTick(finalResult);
            return finalResult;
        }

        KspPlayerState state = KspPlayerState.capture();
        debugState.startTick(state.getLocation(), target, activeTarget);

        try
        {
            debugState.setRecoveryCount(stuckRecovery.getRecoveryCount());
            debugState.setNextCheckpoint(movementExecutor.getLastPlannedTile());
            debugState.setLastClickedTile(movementExecutor.getLastClickedTile());
            debugState.setLastObstacleTile(movementExecutor.getLastObstacleTile());
            debugState.setObstacle(movementExecutor.getLastObstacleName());
            debugState.setClickMethod(String.valueOf(movementExecutor.getLastClickMethod()));
            debugState.setTeleport(teleportExecutor.getLastTeleportName());
            debugState.setLastTeleportDestination(teleportExecutor.getLastTeleportDestination());

            if (!state.hasLocation())
            {
                finalResult = KspWalkResult.failed(target, "Player location unavailable");
                debugState.setDecision("failed: player location unavailable");
                return finishDebug(finalResult);
            }

            if (!target.equals(activeTarget))
            {
                debugState.setDecision("target changed; reset route");
                resetRoute(target);
            }

            if (state.isNear(target, settings.getFinishDistance()))
            {
                activeRoute = null;
                finalResult = KspWalkResult.arrived(target);
                debugState.setDecision("arrived within finish distance " + settings.getFinishDistance());
                return finishDebug(finalResult);
            }

            KspWalkResult teleportResult = teleportExecutor.tryTeleport(state, target, settings, teleportRegistry);

            if (teleportResult != null)
            {
                activeRoute = null;
                debugState.setTeleportDecision("result=" + teleportResult.getStatus() + " " + teleportExecutor.getLastTeleportDecision()
                    + " dest=" + format(teleportExecutor.getLastTeleportDestination()));
                finalResult = teleportResult;
                return finishDebug(finalResult);
            }

            debugState.setTeleportDecision(teleportExecutor.getLastTeleportDecision());

            if (stuckRecovery.isStuck(state, target, settings))
            {
                KspWalkResult recovery = stuckRecovery.recover(target, settings, movementExecutor);
                debugState.setStuckDecision("stuck recovery triggered");
                debug("stuck recovery result={}", recovery);
                finalResult = recovery;
                return finishDebug(finalResult);
            }

            debugState.setStuckDecision("not stuck");

            if (activeRoute == null || activeRoute.isEmpty() || activeRoute.isExpired(settings))
            {
                activeRoute = graphPathfinder.findRoute(graph, state, target, settings);
                debug("new route={}", activeRoute);
                debugState.setRoute(describeRoute(activeRoute));
            }
            else
            {
                debugState.setRoute(describeRoute(activeRoute));
            }

            if (activeRoute == null || activeRoute.isEmpty())
            {
                debugState.setDecision("no graph route; local movement");
                finalResult = movementExecutor.walkLocalTo(target, settings);
                return finishDebug(finalResult);
            }

            KspWebEdge edge = activeRoute.peek();

            if (edge == null)
            {
                debugState.setDecision("route edge null; local movement");
                finalResult = movementExecutor.walkLocalTo(target, settings);
                return finishDebug(finalResult);
            }

            debugState.setActiveEdge(describeEdge(edge));

            if (edge.isComplete())
            {
                activeRoute.poll();
                finalResult = KspWalkResult.waiting(target, "Advanced past completed edge");
                debugState.setDecision("edge complete; polled route");
                return finishDebug(finalResult);
            }

            KspWalkResult result = edge.isWalkingEdge()
                ? movementExecutor.walkLocalTo(edge.getEnd() == null ? target : edge.getEnd(), settings)
                : obstacleExecutor.execute(edge, settings);

            if (result.getStatus() == KspWalkStatus.ARRIVED || result.getStatus() == KspWalkStatus.EDGE_EXECUTED)
            {
                activeRoute.poll();
                debugState.setDecision("edge advanced after result=" + result.getStatus());
            }
            else
            {
                debugState.setDecision("edge/local tick result=" + result.getStatus());
            }

            if (result.getStatus() == KspWalkStatus.NO_LOCAL_STEP && edge.isWalkingEdge())
            {
                activeRoute = null;
                debugState.setRoute("cleared: no local step on walking edge");
            }

            debug("walk tick target={} edge={} result={}", target, edge.getId(), result);
            finalResult = result;
            return finishDebug(finalResult);
        }
        catch (RuntimeException ex)
        {
            debugState.setDecision("exception: " + ex.getClass().getSimpleName() + " " + ex.getMessage());
            finalResult = KspWalkResult.failed(target, "Walker exception: " + ex.getClass().getSimpleName());
            finishDebug(finalResult);
            throw ex;
        }
    }

    private KspWalkResult finishDebug(KspWalkResult result)
    {
        debugState.setNextCheckpoint(movementExecutor.getLastPlannedTile());
        debugState.setLastClickedTile(movementExecutor.getLastClickedTile());
        debugState.setLastObstacleTile(movementExecutor.getLastObstacleTile());
        debugState.setObstacle(movementExecutor.getLastObstacleName());
        debugState.setClickMethod(String.valueOf(movementExecutor.getLastClickMethod()));
        debugState.setTeleport(teleportExecutor.getLastTeleportName());
        debugState.setLastTeleportDestination(teleportExecutor.getLastTeleportDestination());
        debugState.setRecoveryCount(stuckRecovery.getRecoveryCount());
        debugState.setRoute(describeRoute(activeRoute));
        debugState.setActiveEdge(activeRoute == null || activeRoute.isEmpty() ? "-" : describeEdge(activeRoute.peek()));
        debugState.finishTick(result);

        if (settings.isLogEveryWalkerTick())
        {
            KspWalkerDebugLogger.log(log, settings, debugState);
        }

        return result;
    }

    private String describeRoute(KspWebRoute route)
    {
        if (route == null)
        {
            return "-";
        }

        if (route.isEmpty())
        {
            return "empty";
        }

        KspWebEdge edge = route.peek();
        return "size=" + route.getEdges().size() + " next=" + describeEdge(edge);
    }

    private String describeEdge(KspWebEdge edge)
    {
        if (edge == null)
        {
            return "-";
        }

        return edge.getId()
            + "/" + edge.getType()
            + " start=" + format(edge.getStart())
            + " end=" + format(edge.getEnd())
            + " cost=" + edge.getCost();
    }

    private String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    public void reset()
    {
        activeRoute = null;
        activeTarget = null;
        stuckRecovery.reset();
        movementExecutor.reset();
        teleportExecutor.reset();
        debugState.clearRoutePath();
    }

    public void resetRoute(WorldPoint target)
    {
        WorldPoint start = KspPlayerState.capture().getLocation();
        activeRoute = null;
        activeTarget = target;
        stuckRecovery.reset();
        movementExecutor.reset();
        debugState.lockRoutePath(start, target);
    }

    public KspWebGraph getGraph()
    {
        return graph;
    }

    public KspTeleportRegistry getTeleportRegistry()
    {
        return teleportRegistry;
    }

    public KspWalkSettings getSettings()
    {
        return settings;
    }

    public void setSettings(KspWalkSettings settings)
    {
        KspWalkSettings newSettings = Objects.requireNonNull(settings, "settings");

        if (this.settings.equals(newSettings))
        {
            return;
        }

        this.settings = newSettings;
        activeRoute = null;
    }

    public KspWebRoute getActiveRoute()
    {
        return activeRoute;
    }

    public WorldPoint getActiveTarget()
    {
        return activeTarget;
    }

    public int getRecoveryCount()
    {
        return stuckRecovery.getRecoveryCount();
    }

    public WorldPoint getLastPlannedTile()
    {
        return movementExecutor.getLastPlannedTile();
    }

    public WorldPoint getLastClickedTile()
    {
        return movementExecutor.getLastClickedTile();
    }

    public KspWebEdge getActiveEdge()
    {
        return activeRoute == null ? null : activeRoute.peek();
    }

    public WorldPoint getLastObstacleTile()
    {
        return movementExecutor.getLastObstacleTile();
    }

    public String getLastObstacleName()
    {
        return movementExecutor.getLastObstacleName();
    }

    public KspMovementClickMethod getLastClickMethod()
    {
        return movementExecutor.getLastClickMethod();
    }

    public List<WorldPoint> getLastDebugPath()
    {
        return movementExecutor.getLastDebugPath();
    }

    public String getLastTeleportName()
    {
        return teleportExecutor.getLastTeleportName();
    }

    public WorldPoint getLastTeleportDestination()
    {
        return teleportExecutor.getLastTeleportDestination();
    }

    public KspTeleportPlan getLastTeleportPlan()
    {
        return teleportExecutor.getLastTeleportPlan();
    }

    public KspWalkerDebugState getDebugState()
    {
        return debugState;
    }

    private void debug(String message, Object... args)
    {
        if (settings.isDebugLogging())
        {
            log.debug("[KspWebWalker] " + message, args);
        }
    }
}
