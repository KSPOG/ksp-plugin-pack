package net.runelite.client.plugins.microbot.kspwalker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = PluginConstants.KSP + "Custom Walker Tester",
    description = "Coordinate-based tester for the custom Microbot webwalker core",
    tags = {"microbot", "ksp", "walker", "webwalker"},
    authors = {"KSP"},
    version = KspWalkerTesterPlugin.VERSION,
    minClientVersion = "2.0.13",
    enabledByDefault = true,
    isExternal = true
)
public class KspWalkerTesterPlugin extends Plugin
{
    public static final String VERSION = "1.0.32.3";

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KspWalkerTesterOverlay overlay;

    @Inject
    private KspWalkerTileOverlay tileOverlay;

    @Inject
    private KspWalkerTesterConfig config;

    @Getter
    private final KspWalkerTesterScript script = new KspWalkerTesterScript();

    @Provides
    KspWalkerTesterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(KspWalkerTesterConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        overlayManager.add(tileOverlay);

        KspBankFileLoadResult bankLoadResult = KspBankFileLoader.reloadDefaultFile();
        log.info(
            "KSP Walker loaded {} bank target(s) from {}",
            bankLoadResult.getLoaded(),
            bankLoadResult.getFile()
        );

        if (bankLoadResult.hasErrors())
        {
            for (String error : bankLoadResult.getErrors())
            {
                log.warn("KSP Walker bank file error: {}", error);
            }
        }

        KspTransportLoadResult transportLoadResult = KspTransportFileLoader.reloadDefaultFile(script.getWalker().getGraph());
        log.info(
            "KSP Walker loaded {} transport(s) from {}",
            transportLoadResult.getLoaded(),
            transportLoadResult.getFile()
        );

        if (transportLoadResult.hasErrors())
        {
            for (String error : transportLoadResult.getErrors())
            {
                log.warn("KSP Walker transport file error: {}", error);
            }
        }

        KspTeleportFileLoadResult teleportLoadResult = KspTeleportFileLoader.reloadDefaultFile(
            script.getWalker().getGraph(),
            script.getWalker().getTeleportRegistry()
        );

        log.info(
            "KSP Walker loaded teleports from {} | object={} item={} spell={} total={}",
            teleportLoadResult.getFile(),
            teleportLoadResult.getObjectTeleportsLoaded(),
            teleportLoadResult.getItemTeleportsLoaded(),
            teleportLoadResult.getSpellTeleportsLoaded(),
            teleportLoadResult.getTotalLoaded()
        );

        if (teleportLoadResult.hasErrors())
        {
            for (String error : teleportLoadResult.getErrors())
            {
                log.warn("KSP Walker teleport file error: {}", error);
            }
        }

        KspGraphEdgeFileLoadResult edgeLoadResult = KspGraphEdgeFileLoader.reloadDefaultFile(script.getWalker().getGraph());
        log.info(
            "KSP Walker loaded {} explicit graph edge(s) from {}",
            edgeLoadResult.getLoaded(),
            edgeLoadResult.getFile()
        );

        if (edgeLoadResult.hasErrors())
        {
            for (String error : edgeLoadResult.getErrors())
            {
                log.warn("KSP Walker edge database error: {}", error);
            }
        }

        script.run(config);
        log.info("KSP Custom Walker Tester started");
    }
    @Override
    protected void shutDown()
    {
        script.shutdown();
        overlayManager.remove(overlay);
        overlayManager.remove(tileOverlay);
        log.info("KSP Custom Walker Tester stopped");
    }
}
