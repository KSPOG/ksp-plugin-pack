package net.runelite.client.plugins.microbot.balancedattackstyle;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginConstants.DEFAULT_PREFIX + "Balanced Attack Style",
        description = "Automatically switches melee attack styles to keep stats balanced",
        tags = {"combat", "attack", "strength", "defence"},
        authors = {"Microbot"},
        version = BalancedAttackStylePlugin.version,
        minClientVersion = "2.0.0",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
@Slf4j
public class BalancedAttackStylePlugin extends Plugin {
    public static final String version = "1.0.0";

    @Inject
    private BalancedAttackStyleConfig config;

    @Inject
    private BalancedAttackStyleScript script;

    @Provides
    BalancedAttackStyleConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BalancedAttackStyleConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
    }
}