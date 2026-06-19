package net.runelite.client.plugins.microbot.kspwalker;

public enum KspQuickTargetMode
{
    MANUAL_COORDS("Manual coords"),
    NEAREST_BANK("Nearest bank"),
    GRAND_EXCHANGE("Grand Exchange");

    private final String label;

    KspQuickTargetMode(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
