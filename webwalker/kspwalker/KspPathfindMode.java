package net.runelite.client.plugins.microbot.kspwalker;

public enum KspPathfindMode
{
    FAST_GREEDY("Fast greedy"),
    REACHABLE_SCAN("Reachable scan"),
    FAST_THEN_SCAN("Fast then scan");

    private final String label;

    KspPathfindMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
