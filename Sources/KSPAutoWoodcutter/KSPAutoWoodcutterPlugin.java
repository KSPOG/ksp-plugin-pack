package net.runelite.client.plugins.microbot.KSPAutoWoodcutter;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.AWTException;

@PluginDescriptor(
        name = PluginConstants.KSP + "Auto Woodcutter",
        description = "Progressive woodcutting with banking or dropping",
        tags = {"woodcutting", "microbot", "ksp"},
        version = KSPAutoWoodcutterPlugin.version,
        minClientVersion = "2.0.13",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
public class KSPAutoWoodcutterPlugin extends Plugin {

    public static final String version = "0.1.7";


    @Inject
    private KSPAutoWoodcutterConfig config;

    @Provides
    KSPAutoWoodcutterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(KSPAutoWoodcutterConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KSPAutoWoodcutterOverlay overlay;

    @Inject
    private KSPAutoWoodcutterScript script;

    @Override
    protected void startUp() throws AWTException {
        overlayManager.add(overlay);
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
}
