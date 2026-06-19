package net.runelite.client.plugins.microbot.kspwalker;

import net.runelite.api.coords.WorldPoint;

public final class KspStuckRecovery
{
    private WorldPoint lastPosition;
    private long lastMovedAtMs;
    private long lastRecoveryAtMs;
    private int recoveryCount;

    public boolean isStuck(KspPlayerState state, WorldPoint target, KspWalkSettings settings)
    {
        if (state == null || !state.hasLocation() || target == null || settings == null)
        {
            return false;
        }

        WorldPoint current = state.getLocation();
        long now = System.currentTimeMillis();

        if (state.isNear(target, settings.getFinishDistance()))
        {
            updateMovementBaseline(current, now);
            return false;
        }

        /*
         * Critical fix:
         *
         * Do not classify the player as stuck while Microbot/RuneLite reports moving
         * or animating. The previous version could trigger recovery immediately after
         * a minimap click because the location had not changed yet.
         */
        if (state.isMoving() || state.isAnimating())
        {
            updateMovementBaseline(current, now);
            return false;
        }

        if (lastPosition == null || !lastPosition.equals(current))
        {
            updateMovementBaseline(current, now);
            return false;
        }

        if (now - lastMovedAtMs < settings.getIdleTimeoutMs())
        {
            return false;
        }

        return now - lastRecoveryAtMs >= settings.getRecoveryCooldownMs();
    }

    public KspWalkResult recover(WorldPoint target, KspWalkSettings settings, KspMovementExecutor movementExecutor)
    {
        lastRecoveryAtMs = System.currentTimeMillis();
        recoveryCount++;
        return movementExecutor.forceNudge(target, settings);
    }

    public void reset()
    {
        lastPosition = null;
        lastMovedAtMs = 0L;
        lastRecoveryAtMs = 0L;
        recoveryCount = 0;
    }

    public int getRecoveryCount()
    {
        return recoveryCount;
    }

    public long getLastMovedAtMs()
    {
        return lastMovedAtMs;
    }

    public WorldPoint getLastPosition()
    {
        return lastPosition;
    }

    private void updateMovementBaseline(WorldPoint current, long now)
    {
        lastPosition = current;
        lastMovedAtMs = now;
    }
}
