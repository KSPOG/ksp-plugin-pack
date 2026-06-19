package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

@Slf4j
public class KspWalkerTesterScript extends Script
{
    @Getter
    private final KspWebWalker walker = new KspWebWalker();

    @Getter
    private volatile KspWalkResult lastResult = KspWalkResult.waiting(null, "Not started");

    @Getter
    private volatile WorldPoint lastTarget;

    private volatile boolean wasEnabled;
    private volatile boolean arrivalLocked;
    private volatile WorldPoint arrivalLockedTarget;

    private volatile WorldPoint externalTarget;
    private volatile String externalTargetName;

    public void setExternalTarget(WorldPoint target, String name)
    {
        if (target == null)
        {
            lastResult = KspWalkResult.failed(lastTarget, name == null ? "External target is null" : name);
            return;
        }

        externalTarget = target;
        externalTargetName = name == null || name.isBlank() ? "External target" : name;
        arrivalLocked = false;
        arrivalLockedTarget = null;
        walker.resetRoute(target);
        lastTarget = target;
        lastResult = KspWalkResult.waiting(target, "Target set: " + externalTargetName);
        Microbot.log("KSP Walker target set: " + externalTargetName + " -> " + format(target));
    }

    public boolean run(KspWalkerTesterConfig config)
    {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            try
            {
                if (!super.run())
                {
                    return;
                }

                boolean hasExternalTarget = externalTarget != null;

                if (!config.enabled() && !hasExternalTarget)
                {
                    if (wasEnabled)
                    {
                        walker.reset();
                        arrivalLocked = false;
                        arrivalLockedTarget = null;
                        lastResult = KspWalkResult.waiting(lastTarget, "Disabled");
                    }

                    wasEnabled = false;
                    return;
                }

                wasEnabled = true;

                WorldPoint target;

                if (hasExternalTarget)
                {
                    target = externalTarget;
                }
                else
                {
                    target = resolveConfiguredTarget(config);
                }

                if (!Objects.equals(lastTarget, target))
                {
                    lastTarget = target;
                    arrivalLocked = false;
                    arrivalLockedTarget = null;
                    walker.resetRoute(target);
                    Microbot.log("KSP Walker target changed: " + format(target));
                }

                if (arrivalLocked && Objects.equals(arrivalLockedTarget, target))
                {
                    lastResult = KspWalkResult.arrived(target);
                    return;
                }

                walker.setSettings(KspWalkSettings.builder()
                    .finishDistance(config.finishDistance())
                    .localSearchRadius(config.localSearchRadius())
                    .maxStepDistance(config.maxStepDistance())
                    .directTargetClickDistance(config.directTargetClickDistance())
                    .checkpointAdvanceDistance(config.checkpointAdvanceDistance())
                    .clickCooldownMs(config.clickCooldownMs())
                    .movingReclickDelayMs(config.movingReclickDelayMs())
                    .idleTimeoutMs(config.idleTimeoutMs())
                    .recoveryCooldownMs(config.recoveryCooldownMs())
                    .toggleRun(config.toggleRun())
                    .runToggleCooldownMs(config.runToggleCooldownMs())
                    .runEnergyThreshold(config.runEnergyMin(), config.runEnergyMax())
                    .preferMinimap(config.preferMinimap())
                    .canvasMaxDistance(config.canvasMaxDistance())
                    .canvasFallbackWhenMinimapFails(config.canvasFallbackWhenMinimapFails())
                    .pathfindMode(config.pathfindMode())
                    .fastCandidateStepCount(config.fastCandidateStepCount())
                    .fastCandidateSideOffset(config.fastCandidateSideOffset())
                    .localPathCacheMs(config.localPathCacheMs())
                    .autoObstacleInteraction(config.autoObstacleInteraction())
                    .blockingObstacleInteraction(config.blockingObstacleInteraction())
                    .blockingObstacleLineTolerance(config.blockingObstacleLineTolerance())
                    .blockingObstacleExtraDistance(config.blockingObstacleExtraDistance())
                    .obstacleScanDistance(config.obstacleScanDistance())
                    .obstacleLineTolerance(config.obstacleLineTolerance())
                    .obstacleFallbackDistance(config.obstacleFallbackDistance())
                    .obstacleFallbackLineTolerance(config.obstacleFallbackLineTolerance())
                    .obstacleCooldownMs(config.obstacleCooldownMs())
                    .teleportsEnabled(config.teleportsEnabled())
                    .minTeleportSavingDistance(config.minTeleportSavingDistance())
                    .teleportWalkCostMultiplier(config.teleportWalkCostMultiplier())
                    .teleportCooldownMs(config.teleportCooldownMs())
                    .teleportPostCastWaitMs(config.teleportPostCastWaitMs())
                    .debugLogging(config.debugLogging())
                    .debugLevel(config.debugLevel())
                    .logEveryWalkerTick(config.logEveryWalkerTick())
                    .build()
                );

                KspWalkResult travelPrepResult = KspKaramjaTravelHelper.prepareFareIfNeeded(walker, config, target);

                if (travelPrepResult != null)
                {
                    walker.getDebugState().setQuickTarget("Travel prep -> " + describeQuickTarget(config, target));
                    lastResult = travelPrepResult;
                    return;
                }

                walker.getDebugState().setQuickTarget(describeQuickTarget(config, target));
                lastResult = walker.walkTo(target);

                if (lastResult.getStatus() == KspWalkStatus.ARRIVED && config.stopWhenArrived())
                {
                    arrivalLocked = true;
                    arrivalLockedTarget = target;
                    externalTarget = null;
                    externalTargetName = null;
                    walker.reset();
                    Microbot.log("KSP Walker arrived and stopped: " + format(target));
                    return;
                }

                if (config.debugLogging())
                {
                    log.debug("[KspWalkerTester] target={} result={}", format(target), lastResult);
                }
            }
            catch (Exception ex)
            {
                if (isClientThreadInterrupted(ex))
                {
                    lastResult = KspWalkResult.waiting(lastTarget, "Client thread read interrupted; retrying");
                    log.debug("KSP Walker Tester client-thread read interrupted; retrying");
                    return;
                }

                lastResult = KspWalkResult.failed(lastTarget, "Tester error: " + ex.getClass().getSimpleName());
                log.error("KSP Walker Tester loop failed", ex);
            }
        }, 0, Math.max(100, config.loopDelayMs()), TimeUnit.MILLISECONDS);

        Microbot.log("KSP Custom Walker Tester initialized");
        return true;
    }

    @Override
    public void shutdown()
    {
        externalTarget = null;
        externalTargetName = null;
        arrivalLocked = false;
        arrivalLockedTarget = null;
        wasEnabled = false;
        walker.reset();
        lastResult = KspWalkResult.waiting(lastTarget, "Shutdown");
        super.shutdown();
    }



    private String describeQuickTarget(KspWalkerTesterConfig config, WorldPoint target)
    {
        if (externalTargetName != null && !externalTargetName.isBlank())
        {
            return externalTargetName + " -> " + format(target);
        }

        KspQuickTargetMode mode = config.quickTargetMode();

        if (mode == KspQuickTargetMode.NEAREST_BANK)
        {
            return "Nearest bank -> " + format(target);
        }

        if (mode == KspQuickTargetMode.GRAND_EXCHANGE)
        {
            return "Grand Exchange -> " + format(target);
        }

        return "Manual coords -> " + format(target);
    }

    private WorldPoint resolveConfiguredTarget(KspWalkerTesterConfig config)
    {
        KspQuickTargetMode mode = config.quickTargetMode();

        if (mode == KspQuickTargetMode.NEAREST_BANK)
        {
            return KspBankTargetRegistry.nearestBank()
                .map(KspWalkerDestination::getPoint)
                .orElseGet(() -> manualTarget(config));
        }

        if (mode == KspQuickTargetMode.GRAND_EXCHANGE)
        {
            return KspBankTargetRegistry.grandExchange().getPoint();
        }

        return manualTarget(config);
    }

    private WorldPoint manualTarget(KspWalkerTesterConfig config)
    {
        int targetX = parseCoordinate(config.targetX(), 3164);
        int targetY = parseCoordinate(config.targetY(), 3487);

        return new WorldPoint(
            targetX,
            targetY,
            config.targetPlane()
        );
    }

    public String getExternalTargetName()
    {
        return externalTargetName == null ? "-" : externalTargetName;
    }

    public boolean hasExternalTarget()
    {
        return externalTarget != null;
    }

    private boolean isClientThreadInterrupted(Throwable throwable)
    {
        Throwable current = throwable;

        while (current != null)
        {
            if (current instanceof InterruptedException)
            {
                return true;
            }

            String message = current.getMessage();

            if (message != null && message.contains("Interrupted waiting for client thread"))
            {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private int parseCoordinate(String raw, int fallback)
    {
        if (raw == null)
        {
            return fallback;
        }

        String cleaned = raw.trim()
            .replace(",", "")
            .replace(".", "")
            .replace(" ", "");

        if (cleaned.isEmpty())
        {
            return fallback;
        }

        try
        {
            return Integer.parseInt(cleaned);
        }
        catch (NumberFormatException ex)
        {
            log.warn("Invalid KSP walker coordinate '{}', using fallback {}", raw, fallback);
            return fallback;
        }
    }

    private String format(WorldPoint point)
    {
        if (point == null)
        {
            return "?";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }
}
