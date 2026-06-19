package net.runelite.client.plugins.microbot.kspwalker;

public enum KspWalkerDebugLevel
{
    OFF("Off"),
    BASIC("Basic"),
    VERBOSE("Verbose"),
    TRACE("Trace");

    private final String label;

    KspWalkerDebugLevel(String label)
    {
        this.label = label;
    }

    public boolean isAtLeast(KspWalkerDebugLevel other)
    {
        return this.ordinal() >= other.ordinal();
    }

    @Override
    public String toString()
    {
        return label;
    }
}
