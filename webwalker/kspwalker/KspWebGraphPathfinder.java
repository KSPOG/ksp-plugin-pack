package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

public final class KspWebGraphPathfinder
{
    public KspWebRoute findRoute(
        KspWebGraph graph,
        KspPlayerState state,
        WorldPoint target,
        KspWalkSettings settings
    )
    {
        if (state == null || !state.hasLocation() || target == null || settings == null)
        {
            return KspWebRoute.empty(null, target);
        }

        WorldPoint start = state.getLocation();

        if (state.isNear(target, settings.getFinishDistance()))
        {
            return KspWebRoute.empty(start, target);
        }

        if (state.canReach(target))
        {
            return new KspWebRoute(
                start,
                target,
                List.of(KspWebEdge.walk("local:direct-target", start, target)),
                start.distanceTo2D(target)
            );
        }

        Optional<KspWebEdge> bestActionEdge = findBestReachableActionEdge(graph, state, target, settings);

        if (bestActionEdge.isPresent())
        {
            KspWebEdge edge = bestActionEdge.get();
            List<KspWebEdge> route = new ArrayList<>();

            if (edge.getStart() != null && !state.isNear(edge.getStart(), settings.getEdgeStartDistance()))
            {
                route.add(KspWebEdge.walk("local:to-edge:" + edge.getId(), start, edge.getStart()));
            }

            route.add(edge);

            return new KspWebRoute(start, target, route, estimateCost(start, target, route));
        }

        // No registered special edge is usable from the current local area.
        // The movement executor will still try a local-vector step toward the final target.
        return new KspWebRoute(
            start,
            target,
            List.of(KspWebEdge.walk("local:best-effort", start, target)),
            start.getPlane() == target.getPlane() ? start.distanceTo2D(target) : Integer.MAX_VALUE
        );
    }

    private Optional<KspWebEdge> findBestReachableActionEdge(
        KspWebGraph graph,
        KspPlayerState state,
        WorldPoint target,
        KspWalkSettings settings
    )
    {
        if (graph == null)
        {
            return Optional.empty();
        }

        int currentDistance = state.distanceTo(target);

        return graph.getEnabledEdges()
            .stream()
            .filter(this::isRegisteredRoutingEdge)
            .filter(edge -> edge.getStart() != null)
            .filter(edge -> state.isSamePlane(edge.getStart()))
            .filter(edge -> state.canReach(edge.getStart()) || state.isNear(edge.getStart(), settings.getGraphLinkDistance()))
            .filter(edge -> edge.getEnd() == null || improvesDistance(edge, target, currentDistance))
            .min(Comparator.comparingInt(edge -> score(state, edge, target)));
    }

    private boolean isRegisteredRoutingEdge(KspWebEdge edge)
    {
        if (edge == null || edge.getId() == null)
        {
            return false;
        }

        if (edge.getId().startsWith("local:"))
        {
            return false;
        }

        return edge.getStart() != null && edge.getEnd() != null;
    }

    private boolean improvesDistance(KspWebEdge edge, WorldPoint target, int currentDistance)
    {
        if (edge.getEnd() == null || target == null)
        {
            return true;
        }

        if (edge.getEnd().getPlane() != target.getPlane())
        {
            return true;
        }

        if (currentDistance == Integer.MAX_VALUE)
        {
            return true;
        }

        return edge.getEnd().distanceTo2D(target) < currentDistance;
    }

    private int score(KspPlayerState state, KspWebEdge edge, WorldPoint target)
    {
        int toStart = state.distanceTo(edge.getStart());
        int fromEnd = 0;

        if (edge.getEnd() != null && target != null && edge.getEnd().getPlane() == target.getPlane())
        {
            fromEnd = edge.getEnd().distanceTo2D(target);
        }

        return safe(toStart) + edge.getCost() + safe(fromEnd);
    }

    private int estimateCost(WorldPoint start, WorldPoint target, List<KspWebEdge> route)
    {
        int cost = 0;

        for (KspWebEdge edge : route)
        {
            cost += edge.getCost();
        }

        if (cost == 0 && start != null && target != null && start.getPlane() == target.getPlane())
        {
            cost = start.distanceTo2D(target);
        }

        return cost;
    }

    private int safe(int value)
    {
        return value == Integer.MAX_VALUE ? 100_000 : value;
    }
}
