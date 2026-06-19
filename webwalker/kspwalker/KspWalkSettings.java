package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Objects;

public final class KspWalkSettings
{
    private final int finishDistance;
    private final int localSearchRadius;
    private final int minStepDistance;
    private final int maxStepDistance;
    private final int edgeStartDistance;
    private final int edgeEndDistance;
    private final int directTargetClickDistance;
    private final int checkpointAdvanceDistance;
    private final int graphLinkDistance;
    private final KspPathfindMode pathfindMode;
    private final int fastCandidateStepCount;
    private final int fastCandidateSideOffset;
    private final long localPathCacheMs;

    private final long clickCooldownMs;
    private final long movingReclickDelayMs;
    private final long idleTimeoutMs;
    private final long recoveryCooldownMs;
    private final long routeTtlMs;
    private final long postInteractionTimeoutMs;

    private final boolean toggleRun;
    private final long runToggleCooldownMs;
    private final int runEnergyMin;
    private final int runEnergyMax;
    private final boolean preferMinimap;
    private final boolean canvasFallbackWhenMinimapFails;
    private final int canvasMaxDistance;

    private final boolean autoObstacleInteraction;
    private final boolean blockingObstacleInteraction;
    private final int blockingObstacleLineTolerance;
    private final int blockingObstacleExtraDistance;
    private final int obstacleScanDistance;
    private final int obstacleLineTolerance;
    private final int obstacleFallbackDistance;
    private final int obstacleFallbackLineTolerance;
    private final long obstacleCooldownMs;

    private final boolean teleportsEnabled;
    private final int minTeleportSavingDistance;
    private final int teleportWalkCostMultiplier;
    private final long teleportCooldownMs;
    private final long teleportPostCastWaitMs;

    private final boolean debugLogging;
    private final KspWalkerDebugLevel debugLevel;
    private final boolean logEveryWalkerTick;

    private KspWalkSettings(Builder builder)
    {
        this.finishDistance = builder.finishDistance;
        this.localSearchRadius = builder.localSearchRadius;
        this.minStepDistance = builder.minStepDistance;
        this.maxStepDistance = builder.maxStepDistance;
        this.edgeStartDistance = builder.edgeStartDistance;
        this.edgeEndDistance = builder.edgeEndDistance;
        this.directTargetClickDistance = builder.directTargetClickDistance;
        this.checkpointAdvanceDistance = builder.checkpointAdvanceDistance;
        this.graphLinkDistance = builder.graphLinkDistance;
        this.pathfindMode = builder.pathfindMode;
        this.fastCandidateStepCount = builder.fastCandidateStepCount;
        this.fastCandidateSideOffset = builder.fastCandidateSideOffset;
        this.localPathCacheMs = builder.localPathCacheMs;

        this.clickCooldownMs = builder.clickCooldownMs;
        this.movingReclickDelayMs = builder.movingReclickDelayMs;
        this.idleTimeoutMs = builder.idleTimeoutMs;
        this.recoveryCooldownMs = builder.recoveryCooldownMs;
        this.routeTtlMs = builder.routeTtlMs;
        this.postInteractionTimeoutMs = builder.postInteractionTimeoutMs;

        this.toggleRun = builder.toggleRun;
        this.runToggleCooldownMs = builder.runToggleCooldownMs;
        this.runEnergyMin = builder.runEnergyMin;
        this.runEnergyMax = builder.runEnergyMax;
        this.preferMinimap = builder.preferMinimap;
        this.canvasFallbackWhenMinimapFails = builder.canvasFallbackWhenMinimapFails;
        this.canvasMaxDistance = builder.canvasMaxDistance;

        this.autoObstacleInteraction = builder.autoObstacleInteraction;
        this.blockingObstacleInteraction = builder.blockingObstacleInteraction;
        this.blockingObstacleLineTolerance = builder.blockingObstacleLineTolerance;
        this.blockingObstacleExtraDistance = builder.blockingObstacleExtraDistance;
        this.obstacleScanDistance = builder.obstacleScanDistance;
        this.obstacleLineTolerance = builder.obstacleLineTolerance;
        this.obstacleFallbackDistance = builder.obstacleFallbackDistance;
        this.obstacleFallbackLineTolerance = builder.obstacleFallbackLineTolerance;
        this.obstacleCooldownMs = builder.obstacleCooldownMs;

        this.teleportsEnabled = builder.teleportsEnabled;
        this.minTeleportSavingDistance = builder.minTeleportSavingDistance;
        this.teleportWalkCostMultiplier = builder.teleportWalkCostMultiplier;
        this.teleportCooldownMs = builder.teleportCooldownMs;
        this.teleportPostCastWaitMs = builder.teleportPostCastWaitMs;

        this.debugLogging = builder.debugLogging;
        this.debugLevel = builder.debugLevel;
        this.logEveryWalkerTick = builder.logEveryWalkerTick;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static KspWalkSettings defaults()
    {
        return builder().build();
    }

    public int getFinishDistance()
    {
        return finishDistance;
    }

    public int getLocalSearchRadius()
    {
        return localSearchRadius;
    }

    public int getMinStepDistance()
    {
        return minStepDistance;
    }

    public int getMaxStepDistance()
    {
        return maxStepDistance;
    }

    public int getEdgeStartDistance()
    {
        return edgeStartDistance;
    }

    public int getEdgeEndDistance()
    {
        return edgeEndDistance;
    }

    public int getDirectTargetClickDistance()
    {
        return directTargetClickDistance;
    }

    public int getCheckpointAdvanceDistance()
    {
        return checkpointAdvanceDistance;
    }

    public int getGraphLinkDistance()
    {
        return graphLinkDistance;
    }

    public KspPathfindMode getPathfindMode()
    {
        return pathfindMode;
    }

    public int getFastCandidateStepCount()
    {
        return fastCandidateStepCount;
    }

    public int getFastCandidateSideOffset()
    {
        return fastCandidateSideOffset;
    }

    public long getLocalPathCacheMs()
    {
        return localPathCacheMs;
    }

    public long getClickCooldownMs()
    {
        return clickCooldownMs;
    }

    public long getMovingReclickDelayMs()
    {
        return movingReclickDelayMs;
    }

    public long getIdleTimeoutMs()
    {
        return idleTimeoutMs;
    }

    public long getRecoveryCooldownMs()
    {
        return recoveryCooldownMs;
    }

    public long getRouteTtlMs()
    {
        return routeTtlMs;
    }

    public long getPostInteractionTimeoutMs()
    {
        return postInteractionTimeoutMs;
    }

    public boolean isToggleRun()
    {
        return toggleRun;
    }

    public long getRunToggleCooldownMs()
    {
        return runToggleCooldownMs;
    }

    public int getRunEnergyMin()
    {
        return runEnergyMin;
    }

    public int getRunEnergyMax()
    {
        return runEnergyMax;
    }

    public boolean isPreferMinimap()
    {
        return preferMinimap;
    }

    public boolean isCanvasFallbackWhenMinimapFails()
    {
        return canvasFallbackWhenMinimapFails;
    }

    public int getCanvasMaxDistance()
    {
        return canvasMaxDistance;
    }

    public boolean isAutoObstacleInteraction()
    {
        return autoObstacleInteraction;
    }

    public boolean isBlockingObstacleInteraction()
    {
        return blockingObstacleInteraction;
    }

    public int getBlockingObstacleLineTolerance()
    {
        return blockingObstacleLineTolerance;
    }

    public int getBlockingObstacleExtraDistance()
    {
        return blockingObstacleExtraDistance;
    }

    public int getObstacleScanDistance()
    {
        return obstacleScanDistance;
    }

    public int getObstacleLineTolerance()
    {
        return obstacleLineTolerance;
    }

    public int getObstacleFallbackDistance()
    {
        return obstacleFallbackDistance;
    }

    public int getObstacleFallbackLineTolerance()
    {
        return obstacleFallbackLineTolerance;
    }

    public long getObstacleCooldownMs()
    {
        return obstacleCooldownMs;
    }

    public boolean isTeleportsEnabled()
    {
        return teleportsEnabled;
    }

    public int getMinTeleportSavingDistance()
    {
        return minTeleportSavingDistance;
    }

    public int getTeleportWalkCostMultiplier()
    {
        return teleportWalkCostMultiplier;
    }

    public long getTeleportCooldownMs()
    {
        return teleportCooldownMs;
    }

    public long getTeleportPostCastWaitMs()
    {
        return teleportPostCastWaitMs;
    }

    public boolean isDebugLogging()
    {
        return debugLogging;
    }

    public KspWalkerDebugLevel getDebugLevel()
    {
        return debugLevel;
    }

    public boolean isLogEveryWalkerTick()
    {
        return logEveryWalkerTick;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof KspWalkSettings))
        {
            return false;
        }

        KspWalkSettings other = (KspWalkSettings) obj;

        return finishDistance == other.finishDistance
            && localSearchRadius == other.localSearchRadius
            && minStepDistance == other.minStepDistance
            && maxStepDistance == other.maxStepDistance
            && edgeStartDistance == other.edgeStartDistance
            && edgeEndDistance == other.edgeEndDistance
            && directTargetClickDistance == other.directTargetClickDistance
            && checkpointAdvanceDistance == other.checkpointAdvanceDistance
            && graphLinkDistance == other.graphLinkDistance
            && pathfindMode == other.pathfindMode
            && fastCandidateStepCount == other.fastCandidateStepCount
            && fastCandidateSideOffset == other.fastCandidateSideOffset
            && localPathCacheMs == other.localPathCacheMs
            && clickCooldownMs == other.clickCooldownMs
            && movingReclickDelayMs == other.movingReclickDelayMs
            && idleTimeoutMs == other.idleTimeoutMs
            && recoveryCooldownMs == other.recoveryCooldownMs
            && routeTtlMs == other.routeTtlMs
            && postInteractionTimeoutMs == other.postInteractionTimeoutMs
            && toggleRun == other.toggleRun
            && runToggleCooldownMs == other.runToggleCooldownMs
            && runEnergyMin == other.runEnergyMin
            && runEnergyMax == other.runEnergyMax
            && preferMinimap == other.preferMinimap
            && canvasFallbackWhenMinimapFails == other.canvasFallbackWhenMinimapFails
            && canvasMaxDistance == other.canvasMaxDistance
            && autoObstacleInteraction == other.autoObstacleInteraction
            && blockingObstacleInteraction == other.blockingObstacleInteraction
            && blockingObstacleLineTolerance == other.blockingObstacleLineTolerance
            && blockingObstacleExtraDistance == other.blockingObstacleExtraDistance
            && obstacleScanDistance == other.obstacleScanDistance
            && obstacleLineTolerance == other.obstacleLineTolerance
            && obstacleFallbackDistance == other.obstacleFallbackDistance
            && obstacleFallbackLineTolerance == other.obstacleFallbackLineTolerance
            && obstacleCooldownMs == other.obstacleCooldownMs
            && teleportsEnabled == other.teleportsEnabled
            && minTeleportSavingDistance == other.minTeleportSavingDistance
            && teleportWalkCostMultiplier == other.teleportWalkCostMultiplier
            && teleportCooldownMs == other.teleportCooldownMs
            && teleportPostCastWaitMs == other.teleportPostCastWaitMs
            && debugLogging == other.debugLogging
            && debugLevel == other.debugLevel
            && logEveryWalkerTick == other.logEveryWalkerTick;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(
            finishDistance,
            localSearchRadius,
            minStepDistance,
            maxStepDistance,
            edgeStartDistance,
            edgeEndDistance,
            directTargetClickDistance,
            checkpointAdvanceDistance,
            graphLinkDistance,
            pathfindMode,
            fastCandidateStepCount,
            fastCandidateSideOffset,
            localPathCacheMs,
            clickCooldownMs,
            movingReclickDelayMs,
            idleTimeoutMs,
            recoveryCooldownMs,
            routeTtlMs,
            postInteractionTimeoutMs,
            toggleRun,
            preferMinimap,
            canvasFallbackWhenMinimapFails,
            canvasMaxDistance,
            autoObstacleInteraction,
            blockingObstacleInteraction,
            blockingObstacleLineTolerance,
            blockingObstacleExtraDistance,
            obstacleScanDistance,
            obstacleLineTolerance,
            obstacleFallbackDistance,
            obstacleFallbackLineTolerance,
            obstacleCooldownMs,
            teleportsEnabled,
            minTeleportSavingDistance,
            teleportWalkCostMultiplier,
            teleportCooldownMs,
            teleportPostCastWaitMs,
            debugLogging,
            debugLevel,
            logEveryWalkerTick
        );
    }

    public static final class Builder
    {
        private int finishDistance = 2;
        private int localSearchRadius = 13;
        private int minStepDistance = 2;
        private int maxStepDistance = 13;
        private int edgeStartDistance = 2;
        private int edgeEndDistance = 2;
        private int directTargetClickDistance = 2;
        private int checkpointAdvanceDistance = 5;
        private int graphLinkDistance = 40;
        private KspPathfindMode pathfindMode = KspPathfindMode.FAST_THEN_SCAN;
        private int fastCandidateStepCount = 4;
        private int fastCandidateSideOffset = 2;
        private long localPathCacheMs = 450L;

        private long clickCooldownMs = 650L;
        private long movingReclickDelayMs = 650L;
        private long idleTimeoutMs = 2500L;
        private long recoveryCooldownMs = 1400L;
        private long routeTtlMs = 5000L;
        private long postInteractionTimeoutMs = 8000L;

        private boolean toggleRun = true;
        private long runToggleCooldownMs = 8_000L;
        private int runEnergyMin = 10;
        private int runEnergyMax = 30;
        private boolean preferMinimap = true;
        private boolean canvasFallbackWhenMinimapFails = true;
        private int canvasMaxDistance = 4;

        private boolean autoObstacleInteraction = true;
        private boolean blockingObstacleInteraction = true;
        private int blockingObstacleLineTolerance = 1;
        private int blockingObstacleExtraDistance = 1;
        private int obstacleScanDistance = 8;
        private int obstacleLineTolerance = 2;
        private int obstacleFallbackDistance = 12;
        private int obstacleFallbackLineTolerance = 6;
        private long obstacleCooldownMs = 1200L;

        private boolean teleportsEnabled = true;
        private int minTeleportSavingDistance = 40;
        private int teleportWalkCostMultiplier = 1;
        private long teleportCooldownMs = 8000L;
        private long teleportPostCastWaitMs = 4500L;

        private boolean debugLogging = true;
        private KspWalkerDebugLevel debugLevel = KspWalkerDebugLevel.VERBOSE;
        private boolean logEveryWalkerTick = true;

        public Builder finishDistance(int finishDistance)
        {
            this.finishDistance = Math.max(0, finishDistance);
            return this;
        }

        public Builder localSearchRadius(int localSearchRadius)
        {
            this.localSearchRadius = Math.max(3, localSearchRadius);
            return this;
        }

        public Builder minStepDistance(int minStepDistance)
        {
            this.minStepDistance = Math.max(1, minStepDistance);
            return this;
        }

        public Builder maxStepDistance(int maxStepDistance)
        {
            this.maxStepDistance = Math.max(2, maxStepDistance);
            return this;
        }

        public Builder edgeStartDistance(int edgeStartDistance)
        {
            this.edgeStartDistance = Math.max(0, edgeStartDistance);
            return this;
        }

        public Builder edgeEndDistance(int edgeEndDistance)
        {
            this.edgeEndDistance = Math.max(0, edgeEndDistance);
            return this;
        }

        public Builder directTargetClickDistance(int directTargetClickDistance)
        {
            this.directTargetClickDistance = Math.max(0, directTargetClickDistance);
            return this;
        }

        public Builder checkpointAdvanceDistance(int checkpointAdvanceDistance)
        {
            this.checkpointAdvanceDistance = Math.max(0, checkpointAdvanceDistance);
            return this;
        }

        public Builder graphLinkDistance(int graphLinkDistance)
        {
            this.graphLinkDistance = Math.max(1, graphLinkDistance);
            return this;
        }

        public Builder pathfindMode(KspPathfindMode pathfindMode)
        {
            this.pathfindMode = pathfindMode == null ? KspPathfindMode.FAST_THEN_SCAN : pathfindMode;
            return this;
        }

        public Builder fastCandidateStepCount(int fastCandidateStepCount)
        {
            this.fastCandidateStepCount = Math.max(1, fastCandidateStepCount);
            return this;
        }

        public Builder fastCandidateSideOffset(int fastCandidateSideOffset)
        {
            this.fastCandidateSideOffset = Math.max(0, fastCandidateSideOffset);
            return this;
        }

        public Builder localPathCacheMs(long localPathCacheMs)
        {
            this.localPathCacheMs = Math.max(0L, localPathCacheMs);
            return this;
        }

        public Builder clickCooldownMs(long clickCooldownMs)
        {
            this.clickCooldownMs = Math.max(0L, clickCooldownMs);
            return this;
        }

        public Builder movingReclickDelayMs(long movingReclickDelayMs)
        {
            this.movingReclickDelayMs = Math.max(0L, movingReclickDelayMs);
            return this;
        }

        public Builder idleTimeoutMs(long idleTimeoutMs)
        {
            this.idleTimeoutMs = Math.max(500L, idleTimeoutMs);
            return this;
        }

        public Builder recoveryCooldownMs(long recoveryCooldownMs)
        {
            this.recoveryCooldownMs = Math.max(0L, recoveryCooldownMs);
            return this;
        }

        public Builder routeTtlMs(long routeTtlMs)
        {
            this.routeTtlMs = Math.max(500L, routeTtlMs);
            return this;
        }

        public Builder postInteractionTimeoutMs(long postInteractionTimeoutMs)
        {
            this.postInteractionTimeoutMs = Math.max(500L, postInteractionTimeoutMs);
            return this;
        }

        public Builder toggleRun(boolean toggleRun)
        {
            this.toggleRun = toggleRun;
            return this;
        }

        public Builder runToggleCooldownMs(long runToggleCooldownMs)
        {
            this.runToggleCooldownMs = Math.max(0L, runToggleCooldownMs);
            return this;
        }

        public Builder runEnergyThreshold(int min, int max)
        {
            int clampedMin = Math.max(0, Math.min(100, min));
            int clampedMax = Math.max(0, Math.min(100, max));

            if (clampedMax < clampedMin)
            {
                int tmp = clampedMin;
                clampedMin = clampedMax;
                clampedMax = tmp;
            }

            this.runEnergyMin = clampedMin;
            this.runEnergyMax = clampedMax;
            return this;
        }

        public Builder preferMinimap(boolean preferMinimap)
        {
            this.preferMinimap = preferMinimap;
            return this;
        }

        public Builder canvasFallbackWhenMinimapFails(boolean canvasFallbackWhenMinimapFails)
        {
            this.canvasFallbackWhenMinimapFails = canvasFallbackWhenMinimapFails;
            return this;
        }

        public Builder canvasMaxDistance(int canvasMaxDistance)
        {
            this.canvasMaxDistance = Math.max(0, canvasMaxDistance);
            return this;
        }

        public Builder autoObstacleInteraction(boolean autoObstacleInteraction)
        {
            this.autoObstacleInteraction = autoObstacleInteraction;
            return this;
        }

        public Builder blockingObstacleInteraction(boolean blockingObstacleInteraction)
        {
            this.blockingObstacleInteraction = blockingObstacleInteraction;
            return this;
        }

        public Builder blockingObstacleLineTolerance(int blockingObstacleLineTolerance)
        {
            this.blockingObstacleLineTolerance = Math.max(0, blockingObstacleLineTolerance);
            return this;
        }

        public Builder blockingObstacleExtraDistance(int blockingObstacleExtraDistance)
        {
            this.blockingObstacleExtraDistance = Math.max(0, blockingObstacleExtraDistance);
            return this;
        }

        public Builder obstacleScanDistance(int obstacleScanDistance)
        {
            this.obstacleScanDistance = Math.max(1, obstacleScanDistance);
            return this;
        }

        public Builder obstacleLineTolerance(int obstacleLineTolerance)
        {
            this.obstacleLineTolerance = Math.max(0, obstacleLineTolerance);
            return this;
        }

        public Builder obstacleFallbackDistance(int obstacleFallbackDistance)
        {
            this.obstacleFallbackDistance = Math.max(1, obstacleFallbackDistance);
            return this;
        }

        public Builder obstacleFallbackLineTolerance(int obstacleFallbackLineTolerance)
        {
            this.obstacleFallbackLineTolerance = Math.max(0, obstacleFallbackLineTolerance);
            return this;
        }

        public Builder obstacleCooldownMs(long obstacleCooldownMs)
        {
            this.obstacleCooldownMs = Math.max(0L, obstacleCooldownMs);
            return this;
        }

        public Builder teleportsEnabled(boolean teleportsEnabled)
        {
            this.teleportsEnabled = teleportsEnabled;
            return this;
        }

        public Builder minTeleportSavingDistance(int minTeleportSavingDistance)
        {
            this.minTeleportSavingDistance = Math.max(0, minTeleportSavingDistance);
            return this;
        }

        public Builder teleportWalkCostMultiplier(int teleportWalkCostMultiplier)
        {
            this.teleportWalkCostMultiplier = Math.max(1, teleportWalkCostMultiplier);
            return this;
        }

        public Builder teleportCooldownMs(long teleportCooldownMs)
        {
            this.teleportCooldownMs = Math.max(0L, teleportCooldownMs);
            return this;
        }

        public Builder teleportPostCastWaitMs(long teleportPostCastWaitMs)
        {
            this.teleportPostCastWaitMs = Math.max(0L, teleportPostCastWaitMs);
            return this;
        }

        public Builder debugLogging(boolean debugLogging)
        {
            this.debugLogging = debugLogging;
            return this;
        }

        public Builder debugLevel(KspWalkerDebugLevel debugLevel)
        {
            this.debugLevel = debugLevel == null ? KspWalkerDebugLevel.OFF : debugLevel;
            return this;
        }

        public Builder logEveryWalkerTick(boolean logEveryWalkerTick)
        {
            this.logEveryWalkerTick = logEveryWalkerTick;
            return this;
        }

        public KspWalkSettings build()
        {
            if (maxStepDistance < minStepDistance)
            {
                maxStepDistance = minStepDistance;
            }

            if (localSearchRadius < maxStepDistance)
            {
                localSearchRadius = maxStepDistance;
            }

            return new KspWalkSettings(this);
        }
    }
}
