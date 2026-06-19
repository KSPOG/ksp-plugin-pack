package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspWalkerDebugState
{
    private volatile long tick;
    private volatile long lastTickStartedAtMs;
    private volatile long lastTickFinishedAtMs;

    private volatile WorldPoint player;
    private volatile WorldPoint target;
    private volatile WorldPoint routeStart;
    private volatile WorldPoint routeEnd;
    private volatile WorldPoint activeTarget;
    private volatile WorldPoint nextCheckpoint;
    private volatile WorldPoint lastClickedTile;
    private volatile WorldPoint lastObstacleTile;
    private volatile WorldPoint lastTeleportDestination;

    private volatile String quickTarget = "-";
    private volatile String status = "-";
    private volatile String message = "-";
    private volatile String route = "-";
    private volatile String activeEdge = "-";
    private volatile String obstacle = "-";
    private volatile String teleport = "-";
    private volatile String clickMethod = "-";
    private volatile String decision = "-";
    private volatile String localStepDecision = "-";
    private volatile String obstacleDecision = "-";
    private volatile String teleportDecision = "-";
    private volatile String stuckDecision = "-";
    private volatile int recoveryCount;

    public void startTick(WorldPoint player, WorldPoint target, WorldPoint activeTarget)
    {
        this.tick++;
        this.lastTickStartedAtMs = System.currentTimeMillis();
        this.player = player;
        this.target = target;
        this.activeTarget = activeTarget;
        this.decision = "-";
        this.localStepDecision = "-";
        this.obstacleDecision = "-";
        this.teleportDecision = "-";
        this.stuckDecision = "-";
    }

    public void finishTick(KspWalkResult result)
    {
        this.lastTickFinishedAtMs = System.currentTimeMillis();

        if (result != null)
        {
            this.status = String.valueOf(result.getStatus());
            this.message = result.getMessage();
        }
    }

    public long getTick()
    {
        return tick;
    }

    public long getLastTickStartedAtMs()
    {
        return lastTickStartedAtMs;
    }

    public long getLastTickFinishedAtMs()
    {
        return lastTickFinishedAtMs;
    }

    public long getLastTickDurationMs()
    {
        if (lastTickStartedAtMs <= 0L || lastTickFinishedAtMs <= 0L)
        {
            return 0L;
        }

        return Math.max(0L, lastTickFinishedAtMs - lastTickStartedAtMs);
    }

    public WorldPoint getPlayer()
    {
        return player;
    }

    public WorldPoint getTarget()
    {
        return target;
    }

    public WorldPoint getActiveTarget()
    {
        return activeTarget;
    }


    public void lockRoutePath(WorldPoint start, WorldPoint end)
    {
        this.routeStart = start;
        this.routeEnd = end;
    }

    public void clearRoutePath()
    {
        this.routeStart = null;
        this.routeEnd = null;
    }

    public WorldPoint getRouteStart()
    {
        return routeStart;
    }

    public void setRouteStart(WorldPoint routeStart)
    {
        this.routeStart = routeStart;
    }

    public WorldPoint getRouteEnd()
    {
        return routeEnd;
    }

    public void setRouteEnd(WorldPoint routeEnd)
    {
        this.routeEnd = routeEnd;
    }

    public WorldPoint getNextCheckpoint()
    {
        return nextCheckpoint;
    }

    public void setNextCheckpoint(WorldPoint nextCheckpoint)
    {
        this.nextCheckpoint = nextCheckpoint;
    }

    public WorldPoint getLastClickedTile()
    {
        return lastClickedTile;
    }

    public void setLastClickedTile(WorldPoint lastClickedTile)
    {
        this.lastClickedTile = lastClickedTile;
    }

    public WorldPoint getLastObstacleTile()
    {
        return lastObstacleTile;
    }

    public void setLastObstacleTile(WorldPoint lastObstacleTile)
    {
        this.lastObstacleTile = lastObstacleTile;
    }

    public WorldPoint getLastTeleportDestination()
    {
        return lastTeleportDestination;
    }

    public void setLastTeleportDestination(WorldPoint lastTeleportDestination)
    {
        this.lastTeleportDestination = lastTeleportDestination;
    }

    public String getQuickTarget()
    {
        return quickTarget;
    }

    public void setQuickTarget(String quickTarget)
    {
        this.quickTarget = safe(quickTarget);
    }

    public String getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute(String route)
    {
        this.route = safe(route);
    }

    public String getActiveEdge()
    {
        return activeEdge;
    }

    public void setActiveEdge(String activeEdge)
    {
        this.activeEdge = safe(activeEdge);
    }

    public String getObstacle()
    {
        return obstacle;
    }

    public void setObstacle(String obstacle)
    {
        this.obstacle = safe(obstacle);
    }

    public String getTeleport()
    {
        return teleport;
    }

    public void setTeleport(String teleport)
    {
        this.teleport = safe(teleport);
    }

    public String getClickMethod()
    {
        return clickMethod;
    }

    public void setClickMethod(String clickMethod)
    {
        this.clickMethod = safe(clickMethod);
    }

    public String getDecision()
    {
        return decision;
    }

    public void setDecision(String decision)
    {
        this.decision = safe(decision);
    }

    public String getLocalStepDecision()
    {
        return localStepDecision;
    }

    public void setLocalStepDecision(String localStepDecision)
    {
        this.localStepDecision = safe(localStepDecision);
    }

    public String getObstacleDecision()
    {
        return obstacleDecision;
    }

    public void setObstacleDecision(String obstacleDecision)
    {
        this.obstacleDecision = safe(obstacleDecision);
    }

    public String getTeleportDecision()
    {
        return teleportDecision;
    }

    public void setTeleportDecision(String teleportDecision)
    {
        this.teleportDecision = safe(teleportDecision);
    }

    public String getStuckDecision()
    {
        return stuckDecision;
    }

    public void setStuckDecision(String stuckDecision)
    {
        this.stuckDecision = safe(stuckDecision);
    }

    public int getRecoveryCount()
    {
        return recoveryCount;
    }

    public void setRecoveryCount(int recoveryCount)
    {
        this.recoveryCount = recoveryCount;
    }

    public String compactLine()
    {
        return "tick=" + tick
            + " player=" + format(player)
            + " target=" + format(target)
            + " status=" + status
            + " msg=" + safe(message)
            + " next=" + format(nextCheckpoint)
            + " clicked=" + format(lastClickedTile)
            + " clickMethod=" + clickMethod
            + " obstacle=" + obstacle
            + " teleport=" + teleport
            + " route=" + route
            + " edge=" + activeEdge
            + " recoveries=" + recoveryCount
            + " durationMs=" + getLastTickDurationMs();
    }

    public String verboseLine()
    {
        return compactLine()
            + " decision=" + decision
            + " local=" + localStepDecision
            + " obstacleDecision=" + obstacleDecision
            + " teleportDecision=" + teleportDecision
            + " stuckDecision=" + stuckDecision
            + " quickTarget=" + quickTarget;
    }


    public String explvLine()
    {
        return "playerMap=" + KspExplvMapLink.forTile(player)
            + " targetMap=" + KspExplvMapLink.forTile(target)
            + " nextMap=" + KspExplvMapLink.forTile(nextCheckpoint)
            + " clickedMap=" + KspExplvMapLink.forTile(lastClickedTile)
            + " obstacleMap=" + KspExplvMapLink.forTile(lastObstacleTile)
            + " teleportMap=" + KspExplvMapLink.forTile(lastTeleportDestination);
    }

    public String traceBlock()
    {
        return "\n[KSP WALKER TRACE]"
            + "\n tick=" + tick
            + "\n player=" + format(player)
            + "\n target=" + format(target)
            + "\n activeTarget=" + format(activeTarget)
            + "\n routeStart=" + format(routeStart)
            + "\n routeEnd=" + format(routeEnd)
            + "\n quickTarget=" + quickTarget
            + "\n status=" + status
            + "\n message=" + safe(message)
            + "\n decision=" + decision
            + "\n localStepDecision=" + localStepDecision
            + "\n obstacleDecision=" + obstacleDecision
            + "\n teleportDecision=" + teleportDecision
            + "\n stuckDecision=" + stuckDecision
            + "\n route=" + route
            + "\n activeEdge=" + activeEdge
            + "\n nextCheckpoint=" + format(nextCheckpoint)
            + "\n lastClicked=" + format(lastClickedTile)
            + "\n clickMethod=" + clickMethod
            + "\n obstacle=" + obstacle
            + "\n obstacleTile=" + format(lastObstacleTile)
            + "\n teleport=" + teleport
            + "\n teleportDestination=" + format(lastTeleportDestination)
            + "\n recoveryCount=" + recoveryCount
            + "\n durationMs=" + getLastTickDurationMs();
    }

    private String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private String safe(String value)
    {
        return value == null || value.isBlank() ? "-" : value;
    }
}
