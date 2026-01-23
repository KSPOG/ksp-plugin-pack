package net.runelite.client.plugins.microbot.balancedattackstyle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("balancedattackstyle")
public interface BalancedAttackStyleConfig extends Config {
    @ConfigItem(
            keyName = "switchIntervalSeconds",
            name = "Switch interval (seconds)",
            description = "How often to check and switch attack styles",
            position = 0
    )
    default int switchIntervalSeconds() {
        return 10;
    }

    @ConfigItem(
            keyName = "avoidControlled",
            name = "Avoid controlled style",
            description = "Skip controlled styles that train multiple melee skills",
            position = 1
    )
    default boolean avoidControlled() {
        return true;
    }
}