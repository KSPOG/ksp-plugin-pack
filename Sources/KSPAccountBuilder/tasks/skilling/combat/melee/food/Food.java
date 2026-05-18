/*
 * Decompiled with CFR 0.152.
 */
package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.food;

public enum Food {
    SARDINE("Sardine", 4),
    HERRING("Herring", 5),
    TROUT("Trout", 7),
    SALMON("Salmon", 9);

    private final String displayName;
    private final int healAmount;

    public String getDisplayName() {
        return this.displayName;
    }

    public int getHealAmount() {
        return this.healAmount;
    }

    private Food(String displayName, int healAmount) {
        this.displayName = displayName;
        this.healAmount = healAmount;
    }
}

