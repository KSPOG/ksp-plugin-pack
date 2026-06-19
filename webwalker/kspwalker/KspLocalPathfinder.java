package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KspLocalPathfinder
{
    private static final Logger log = LoggerFactory.getLogger(KspLocalPathfinder.class);

    private WorldPoint cachePlayer;
    private WorldPoint cacheTarget;
    private WorldPoint cacheResult;
    private KspPathfindMode cacheMode;
    private long cacheAtMs;

    private long lastCalculationMs;
    private String lastDecision = "-";
    private int lastCandidateCount;
    private List<WorldPoint> lastDebugPath = Collections.emptyList();

    public Optional<WorldPoint> findNextStep(WorldPoint target, KspWalkSettings settings)
    {
        long started = System.currentTimeMillis();

        try
        {
            WorldPoint player = Rs2Player.getWorldLocation();

            if (player == null || target == null || settings == null)
            {
                lastDecision = "missing player/target/settings";
                lastDebugPath = Collections.emptyList();
                return Optional.empty();
            }

            if (player.getPlane() != target.getPlane())
            {
                lastDecision = "different plane player=" + compact(player) + " target=" + compact(target);
                lastDebugPath = Collections.emptyList();
                return Optional.empty();
            }

            int currentDistance = player.distanceTo2D(target);

            if (currentDistance <= settings.getFinishDistance())
            {
                lastDecision = "already within finish distance";
                lastDebugPath = List.of(player, target);
                return Optional.of(target);
            }

            Optional<WorldPoint> cached = getCached(player, target, settings);

            if (cached != null)
            {
                if (cached.isPresent())
                {
                    lastDecision = "cache hit " + compact(cached.get());
                }
                else
                {
                    lastDecision = "cache hit empty";
                    lastDebugPath = Collections.emptyList();
                }

                return cached;
            }

            Optional<WorldPoint> fast = Optional.empty();

            if (settings.getPathfindMode() == KspPathfindMode.FAST_GREEDY
                || settings.getPathfindMode() == KspPathfindMode.FAST_THEN_SCAN)
            {
                fast = findFastStep(player, target, settings, currentDistance);

                if (fast.isPresent())
                {
                    putCache(player, target, settings, fast.orElse(null));
                    lastDecision = "fast greedy selected " + compact(fast.get())
                        + " candidates=" + lastCandidateCount
                        + " pathLen=" + lastDebugPath.size();
                    return fast;
                }

                if (settings.getPathfindMode() == KspPathfindMode.FAST_GREEDY)
                {
                    putCache(player, target, settings, null);
                    lastDecision = "fast greedy found no candidate candidates=" + lastCandidateCount;
                    lastDebugPath = Collections.emptyList();
                    return Optional.empty();
                }
            }

            Optional<WorldPoint> scan = findReachableScanStep(player, target, settings, currentDistance);
            putCache(player, target, settings, scan.orElse(null));
            lastDecision = scan.isPresent()
                ? "reachable scan selected " + compact(scan.get()) + " candidates=" + lastCandidateCount + " pathLen=" + lastDebugPath.size()
                : "reachable scan empty candidates=" + lastCandidateCount;
            return scan;
        }
        finally
        {
            lastCalculationMs = Math.max(0L, System.currentTimeMillis() - started);
        }
    }

    public boolean canReach(WorldPoint target)
    {
        if (target == null)
        {
            return false;
        }

        try
        {
            return Rs2Tile.isTileReachable(target);
        }
        catch (RuntimeException ex)
        {
            if (isClientThreadInterrupted(ex))
            {
                return false;
            }

            throw ex;
        }
    }

    public long getLastCalculationMs()
    {
        return lastCalculationMs;
    }

    public String getLastDecision()
    {
        return lastDecision;
    }

    public int getLastCandidateCount()
    {
        return lastCandidateCount;
    }

    public List<WorldPoint> getLastDebugPath()
    {
        return Collections.unmodifiableList(new ArrayList<>(lastDebugPath));
    }

    public void reset()
    {
        cachePlayer = null;
        cacheTarget = null;
        cacheResult = null;
        cacheMode = null;
        cacheAtMs = 0L;
        lastDebugPath = Collections.emptyList();
        lastDecision = "reset";
        lastCandidateCount = 0;
        lastCalculationMs = 0L;
    }

    private Optional<WorldPoint> findFastStep(
        WorldPoint player,
        WorldPoint target,
        KspWalkSettings settings,
        int currentDistance
    )
    {
        List<WorldPoint> candidates = buildFastCandidates(player, target, settings, currentDistance);
        lastCandidateCount = candidates.size();

        Optional<ScoredTile> selected = candidates.stream()
            .map(tile -> scoreTile(player, target, tile, settings, currentDistance, true))
            .filter(score -> score != null)
            .filter(score -> isReachableFast(score.tile))
            .filter(score -> score.progressDistance >= requiredProgress(currentDistance, settings))
            .max(Comparator.comparingDouble(score -> score.score));

        if (selected.isEmpty())
        {
            lastDebugPath = Collections.emptyList();
            return Optional.empty();
        }

        /*
         * Important:
         *
         * Fast greedy uses a small candidate set, but the visual path should still
         * be collision-aware. After selecting the candidate, reconstruct the actual
         * reachable path to that candidate from Rs2Tile's reachable-distance map.
         *
         * If reconstruction fails, do not draw a misleading straight line.
         */
        lastDebugPath = reconstructPathTo(player, selected.get().tile, settings);
        return Optional.of(selected.get().tile);
    }

    private Optional<WorldPoint> findReachableScanStep(
        WorldPoint player,
        WorldPoint target,
        KspWalkSettings settings,
        int currentDistance
    )
    {
        HashMap<WorldPoint, Integer> reachableMap = getReachableTileMap(player, settings.getLocalSearchRadius());
        Set<WorldPoint> reachable = reachableMap == null ? new HashSet<>() : new HashSet<>(reachableMap.keySet());
        lastCandidateCount = reachable.size();

        if (reachable.isEmpty())
        {
            lastDebugPath = Collections.emptyList();
            return Optional.empty();
        }

        Optional<ScoredTile> strongCandidate = reachable.stream()
            .map(tile -> scoreTile(player, target, tile, settings, currentDistance, false))
            .filter(score -> score != null)
            .filter(score -> score.progressDistance >= requiredProgress(currentDistance, settings))
            .max(Comparator.comparingDouble(score -> score.score));

        if (strongCandidate.isPresent())
        {
            WorldPoint selected = strongCandidate.get().tile;
            lastDebugPath = reconstructPath(reachableMap, player, selected);
            return Optional.of(selected);
        }

        Optional<ScoredTile> fallback = reachable.stream()
            .map(tile -> scoreTile(player, target, tile, settings, currentDistance, false))
            .filter(score -> score != null)
            .filter(score -> score.distanceToTarget < currentDistance)
            .max(Comparator.comparingDouble(score -> score.fallbackScore));

        if (fallback.isPresent())
        {
            WorldPoint selected = fallback.get().tile;
            lastDebugPath = reconstructPath(reachableMap, player, selected);
            return Optional.of(selected);
        }

        lastDebugPath = Collections.emptyList();
        return Optional.empty();
    }

    private List<WorldPoint> buildFastCandidates(
        WorldPoint player,
        WorldPoint target,
        KspWalkSettings settings,
        int currentDistance
    )
    {
        LinkedHashSet<WorldPoint> candidates = new LinkedHashSet<>();

        double dx = target.getX() - player.getX();
        double dy = target.getY() - player.getY();
        double len = Math.hypot(dx, dy);

        if (len <= 0.0)
        {
            return new ArrayList<>(candidates);
        }

        double ux = dx / len;
        double uy = dy / len;

        double px = -uy;
        double py = ux;

        int maxStep = Math.min(settings.getMaxStepDistance(), Math.max(settings.getMinStepDistance(), currentDistance - settings.getFinishDistance()));
        int minStep = settings.getMinStepDistance();
        int stepCount = Math.max(1, settings.getFastCandidateStepCount());
        int sideOffset = Math.max(0, settings.getFastCandidateSideOffset());

        for (int i = 0; i < stepCount; i++)
        {
            int step;

            if (stepCount == 1)
            {
                step = maxStep;
            }
            else
            {
                double t = i / (double) (stepCount - 1);
                step = (int) Math.round(maxStep - (maxStep - minStep) * t);
            }

            step = Math.max(minStep, Math.min(maxStep, step));

            for (int offset = 0; offset <= sideOffset; offset++)
            {
                if (offset == 0)
                {
                    addProjectedCandidate(candidates, player, ux, uy, px, py, step, 0);
                }
                else
                {
                    addProjectedCandidate(candidates, player, ux, uy, px, py, step, offset);
                    addProjectedCandidate(candidates, player, ux, uy, px, py, step, -offset);
                }
            }
        }

        return new ArrayList<>(candidates);
    }

    private void addProjectedCandidate(
        Set<WorldPoint> candidates,
        WorldPoint player,
        double ux,
        double uy,
        double px,
        double py,
        int step,
        int sideOffset
    )
    {
        int x = player.getX() + (int) Math.round(ux * step + px * sideOffset);
        int y = player.getY() + (int) Math.round(uy * step + py * sideOffset);
        candidates.add(new WorldPoint(x, y, player.getPlane()));
    }

    private boolean isReachableFast(WorldPoint tile)
    {
        try
        {
            return Rs2Tile.isTileReachable(tile);
        }
        catch (RuntimeException ex)
        {
            if (isClientThreadInterrupted(ex))
            {
                return false;
            }

            throw ex;
        }
    }

    private List<WorldPoint> reconstructPathTo(WorldPoint player, WorldPoint endpoint, KspWalkSettings settings)
    {
        if (player == null || endpoint == null || settings == null || player.getPlane() != endpoint.getPlane())
        {
            return Collections.emptyList();
        }

        int radius = Math.max(settings.getLocalSearchRadius(), player.distanceTo2D(endpoint) + 2);
        radius = Math.min(Math.max(radius, 3), Math.max(settings.getLocalSearchRadius(), settings.getMaxStepDistance() + 3));

        HashMap<WorldPoint, Integer> reachableMap = getReachableTileMap(player, radius);
        return reconstructPath(reachableMap, player, endpoint);
    }

    private List<WorldPoint> reconstructPath(
        HashMap<WorldPoint, Integer> reachableMap,
        WorldPoint player,
        WorldPoint endpoint
    )
    {
        if (reachableMap == null || reachableMap.isEmpty() || player == null || endpoint == null)
        {
            return Collections.emptyList();
        }

        if (!reachableMap.containsKey(endpoint))
        {
            return Collections.emptyList();
        }

        List<WorldPoint> reverse = new ArrayList<>();
        WorldPoint current = endpoint;
        reverse.add(current);

        int guard = 0;

        while (!current.equals(player) && guard++ < 256)
        {
            Integer currentDistance = reachableMap.get(current);

            if (currentDistance == null || currentDistance <= 0)
            {
                break;
            }

            WorldPoint next = bestPreviousStep(reachableMap, current, currentDistance);

            if (next == null || next.equals(current))
            {
                break;
            }

            current = next;
            reverse.add(current);
        }

        Collections.reverse(reverse);

        if (reverse.isEmpty() || !reverse.get(0).equals(player))
        {
            if (!reverse.contains(player))
            {
                reverse.add(0, player);
            }
        }

        return reverse;
    }

    private WorldPoint bestPreviousStep(
        HashMap<WorldPoint, Integer> reachableMap,
        WorldPoint current,
        int currentDistance
    )
    {
        WorldPoint best = null;
        int bestDistance = currentDistance;

        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dy = -1; dy <= 1; dy++)
            {
                if (dx == 0 && dy == 0)
                {
                    continue;
                }

                WorldPoint candidate = new WorldPoint(
                    current.getX() + dx,
                    current.getY() + dy,
                    current.getPlane()
                );

                Integer distance = reachableMap.get(candidate);

                if (distance == null)
                {
                    continue;
                }

                if (distance < bestDistance)
                {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }

        return best;
    }

    private Optional<WorldPoint> getCached(WorldPoint player, WorldPoint target, KspWalkSettings settings)
    {
        long cacheMs = settings.getLocalPathCacheMs();

        if (cacheMs <= 0L)
        {
            return null;
        }

        if (cacheAtMs <= 0L)
        {
            return null;
        }

        if (System.currentTimeMillis() - cacheAtMs > cacheMs)
        {
            return null;
        }

        if (!player.equals(cachePlayer) || !target.equals(cacheTarget) || settings.getPathfindMode() != cacheMode)
        {
            return null;
        }

        return Optional.ofNullable(cacheResult);
    }

    private void putCache(WorldPoint player, WorldPoint target, KspWalkSettings settings, WorldPoint result)
    {
        cachePlayer = player;
        cacheTarget = target;
        cacheMode = settings.getPathfindMode();
        cacheResult = result;
        cacheAtMs = System.currentTimeMillis();
    }

    private HashMap<WorldPoint, Integer> getReachableTileMap(WorldPoint player, int radius)
    {
        try
        {
            HashMap<WorldPoint, Integer> tiles = Rs2Tile.getReachableTilesFromTile(player, radius);
            return tiles == null ? new HashMap<>() : tiles;
        }
        catch (RuntimeException ex)
        {
            if (isClientThreadInterrupted(ex))
            {
                log.debug("[KspLocalPathfinder] reachable tile read interrupted");
                return new HashMap<>();
            }

            throw ex;
        }
    }

    private ScoredTile scoreTile(
        WorldPoint player,
        WorldPoint target,
        WorldPoint tile,
        KspWalkSettings settings,
        int currentDistance,
        boolean fastCandidate
    )
    {
        if (tile == null || tile.getPlane() != player.getPlane())
        {
            return null;
        }

        int distanceFromPlayer = player.distanceTo2D(tile);
        int distanceToTarget = tile.distanceTo2D(target);

        if (distanceFromPlayer < settings.getMinStepDistance())
        {
            return null;
        }

        if (distanceFromPlayer > settings.getMaxStepDistance())
        {
            return null;
        }

        if (distanceToTarget > currentDistance)
        {
            return null;
        }

        if (tile.equals(target) && currentDistance > settings.getDirectTargetClickDistance())
        {
            return null;
        }

        VectorMetrics vector = vectorMetrics(player, target, tile);
        int progressDistance = currentDistance - distanceToTarget;

        double fastBonus = fastCandidate ? 250.0 : 0.0;

        double score = progressDistance * 10_000.0
            + vector.forwardProgress * 800.0
            + distanceFromPlayer * 35.0
            + fastBonus
            - vector.lineDistance * 1_200.0
            - sidewaysPenalty(vector, distanceFromPlayer) * 500.0
            - distanceToTarget * 4.0;

        double fallbackScore = progressDistance * 5_000.0
            + vector.forwardProgress * 400.0
            + fastBonus
            - vector.lineDistance * 1_500.0
            - sidewaysPenalty(vector, distanceFromPlayer) * 800.0
            + distanceFromPlayer * 10.0;

        return new ScoredTile(tile, score, fallbackScore, progressDistance, distanceToTarget);
    }

    private int requiredProgress(int currentDistance, KspWalkSettings settings)
    {
        if (currentDistance <= settings.getDirectTargetClickDistance() + 2)
        {
            return 1;
        }

        if (currentDistance <= 12)
        {
            return 2;
        }

        if (currentDistance <= 30)
        {
            return 3;
        }

        return 4;
    }

    private double sidewaysPenalty(VectorMetrics vector, int distanceFromPlayer)
    {
        if (distanceFromPlayer <= 0)
        {
            return 0.0;
        }

        double expectedProgress = distanceFromPlayer * 0.45;

        if (vector.forwardProgress >= expectedProgress)
        {
            return 0.0;
        }

        return expectedProgress - vector.forwardProgress;
    }

    private VectorMetrics vectorMetrics(WorldPoint player, WorldPoint target, WorldPoint tile)
    {
        double ax = player.getX();
        double ay = player.getY();
        double bx = target.getX();
        double by = target.getY();
        double px = tile.getX();
        double py = tile.getY();

        double vx = bx - ax;
        double vy = by - ay;
        double wx = px - ax;
        double wy = py - ay;

        double len = Math.hypot(vx, vy);

        if (len <= 0.0)
        {
            return new VectorMetrics(0.0, 0.0);
        }

        double unitX = vx / len;
        double unitY = vy / len;

        double forwardProgress = wx * unitX + wy * unitY;

        double closestX = ax + unitX * forwardProgress;
        double closestY = ay + unitY * forwardProgress;
        double lineDistance = Math.hypot(px - closestX, py - closestY);

        return new VectorMetrics(forwardProgress, lineDistance);
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

    private String compact(WorldPoint point)
    {
        if (point == null)
        {
            return "?";
        }

        return point.getX() + "," + point.getY() + ",p" + point.getPlane();
    }

    private static final class ScoredTile
    {
        private final WorldPoint tile;
        private final double score;
        private final double fallbackScore;
        private final int progressDistance;
        private final int distanceToTarget;

        private ScoredTile(
            WorldPoint tile,
            double score,
            double fallbackScore,
            int progressDistance,
            int distanceToTarget
        )
        {
            this.tile = tile;
            this.score = score;
            this.fallbackScore = fallbackScore;
            this.progressDistance = progressDistance;
            this.distanceToTarget = distanceToTarget;
        }
    }

    private static final class VectorMetrics
    {
        private final double forwardProgress;
        private final double lineDistance;

        private VectorMetrics(double forwardProgress, double lineDistance)
        {
            this.forwardProgress = forwardProgress;
            this.lineDistance = lineDistance;
        }
    }
}
