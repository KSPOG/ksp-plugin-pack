package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspWalkResult
{
    private final KspWalkStatus status;
    private final String message;
    private final WorldPoint target;
    private final WorldPoint clickedTile;
    private final boolean success;
    private final long timestampMs;

    private KspWalkResult(
        KspWalkStatus status,
        String message,
        WorldPoint target,
        WorldPoint clickedTile,
        boolean success
    )
    {
        this.status = status;
        this.message = message == null ? "" : message;
        this.target = target;
        this.clickedTile = clickedTile;
        this.success = success;
        this.timestampMs = System.currentTimeMillis();
    }

    public static KspWalkResult arrived(WorldPoint target)
    {
        return new KspWalkResult(KspWalkStatus.ARRIVED, "Arrived", target, null, true);
    }

    public static KspWalkResult walking(WorldPoint target, WorldPoint clickedTile, String message)
    {
        return new KspWalkResult(KspWalkStatus.WALKING, message, target, clickedTile, true);
    }

    public static KspWalkResult waiting(WorldPoint target, String message)
    {
        return new KspWalkResult(KspWalkStatus.WAITING, message, target, null, true);
    }

    public static KspWalkResult edgeExecuted(KspWebEdge edge, String message)
    {
        return new KspWalkResult(
            KspWalkStatus.EDGE_EXECUTED,
            message,
            edge == null ? null : edge.getEnd(),
            null,
            true
        );
    }

    public static KspWalkResult stuckRecovery(WorldPoint target, WorldPoint clickedTile, String message)
    {
        return new KspWalkResult(KspWalkStatus.STUCK_RECOVERY, message, target, clickedTile, true);
    }

    public static KspWalkResult noLocalStep(WorldPoint target, String message)
    {
        return new KspWalkResult(KspWalkStatus.NO_LOCAL_STEP, message, target, null, false);
    }

    public static KspWalkResult noRoute(WorldPoint target, String message)
    {
        return new KspWalkResult(KspWalkStatus.NO_ROUTE, message, target, null, false);
    }

    public static KspWalkResult failed(WorldPoint target, String message)
    {
        return new KspWalkResult(KspWalkStatus.FAILED, message, target, null, false);
    }

    public KspWalkStatus getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }

    public WorldPoint getTarget()
    {
        return target;
    }

    public WorldPoint getClickedTile()
    {
        return clickedTile;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public long getTimestampMs()
    {
        return timestampMs;
    }

    @Override
    public String toString()
    {
        return "KspWalkResult{" +
            "status=" + status +
            ", message='" + message + '\'' +
            ", target=" + target +
            ", clickedTile=" + clickedTile +
            ", success=" + success +
            '}';
    }
}
