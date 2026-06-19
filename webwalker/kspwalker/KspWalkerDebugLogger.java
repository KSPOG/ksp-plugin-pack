package net.runelite.client.plugins.microbot.kspwalker;

import org.slf4j.Logger;

public final class KspWalkerDebugLogger
{
    private KspWalkerDebugLogger()
    {
    }

    public static void log(Logger log, KspWalkSettings settings, KspWalkerDebugState state)
    {
        if (log == null || settings == null || state == null)
        {
            return;
        }

        KspWalkerDebugLevel level = settings.getDebugLevel();

        if (level == KspWalkerDebugLevel.OFF)
        {
            return;
        }

        if (level == KspWalkerDebugLevel.BASIC)
        {
            log.info("[KSP-WALKER] {}", state.compactLine());
            return;
        }

        if (level == KspWalkerDebugLevel.VERBOSE)
        {
            log.info("[KSP-WALKER] {}", state.verboseLine());
            log.info("[KSP-WALKER-MAP] {}", state.explvLine());
            return;
        }

        log.info(state.traceBlock());
        log.info("[KSP-WALKER-MAP] {}", state.explvLine());
    }
}
