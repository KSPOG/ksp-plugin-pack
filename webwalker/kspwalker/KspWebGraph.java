package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;

public final class KspWebGraph
{
    private final CopyOnWriteArrayList<KspWebEdge> edges = new CopyOnWriteArrayList<>();

    public KspWebGraph add(KspWebEdge edge)
    {
        if (edge != null)
        {
            edges.add(edge);
        }

        return this;
    }

    public KspWebGraph addAll(List<KspWebEdge> newEdges)
    {
        if (newEdges != null)
        {
            newEdges.stream().filter(e -> e != null).forEach(edges::add);
        }

        return this;
    }

    public KspWebGraph remove(String edgeId)
    {
        if (edgeId != null)
        {
            edges.removeIf(edge -> edgeId.equals(edge.getId()));
        }

        return this;
    }

    public void clear()
    {
        edges.clear();
    }

    public List<KspWebEdge> getEdges()
    {
        return Collections.unmodifiableList(new ArrayList<>(edges));
    }

    public List<KspWebEdge> getEnabledEdges()
    {
        return edges.stream()
            .filter(KspWebEdge::isEnabled)
            .collect(Collectors.toList());
    }

    public Optional<KspWebEdge> findById(String edgeId)
    {
        if (edgeId == null)
        {
            return Optional.empty();
        }

        return edges.stream()
            .filter(edge -> edgeId.equals(edge.getId()))
            .findFirst();
    }

    public KspWebGraph addBidirectionalWalk(String id, WorldPoint a, WorldPoint b)
    {
        add(KspWebEdge.walk(id + ":a_to_b", a, b));
        add(KspWebEdge.walk(id + ":b_to_a", b, a));
        return this;
    }
}
