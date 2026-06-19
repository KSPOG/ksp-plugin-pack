package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public final class KspWebRoute
{
    private final WorldPoint start;
    private final WorldPoint target;
    private final LinkedList<KspWebEdge> edges;
    private final int totalCost;
    private final long createdAtMs;

    public KspWebRoute(WorldPoint start, WorldPoint target, List<KspWebEdge> edges, int totalCost)
    {
        this.start = start;
        this.target = target;
        this.edges = new LinkedList<>(edges == null ? Collections.emptyList() : edges);
        this.totalCost = totalCost;
        this.createdAtMs = System.currentTimeMillis();
    }

    public static KspWebRoute empty(WorldPoint start, WorldPoint target)
    {
        return new KspWebRoute(start, target, Collections.emptyList(), 0);
    }

    public WorldPoint getStart()
    {
        return start;
    }

    public WorldPoint getTarget()
    {
        return target;
    }

    public List<KspWebEdge> getEdges()
    {
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    public int getTotalCost()
    {
        return totalCost;
    }

    public long getCreatedAtMs()
    {
        return createdAtMs;
    }

    public boolean isExpired(KspWalkSettings settings)
    {
        return settings != null && System.currentTimeMillis() - createdAtMs > settings.getRouteTtlMs();
    }

    public boolean isEmpty()
    {
        return edges.isEmpty();
    }

    public KspWebEdge peek()
    {
        return edges.peekFirst();
    }

    public KspWebEdge poll()
    {
        return edges.pollFirst();
    }

    @Override
    public String toString()
    {
        return "KspWebRoute{" +
            "start=" + start +
            ", target=" + target +
            ", edges=" + edges +
            ", totalCost=" + totalCost +
            '}';
    }
}
