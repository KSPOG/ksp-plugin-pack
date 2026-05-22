package net.runelite.client.plugins.microbot.kspaccountbuilder;

public enum KspTrainSingleSkillTask
{
    TUTORIAL_ISLAND("Tutorial Island"),
    MINING("Mining"),
    WOODCUTTING("Woodcutting"),
    FIREMAKING("Firemaking"),
    MELEE("Melee"),
    GE_SELL("GE Sell"),
    GE_BUY("GE Buy"),
    SMITHING("Smithing"),
    SMELTING("Smelting");

    private final String displayName;

    KspTrainSingleSkillTask(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
