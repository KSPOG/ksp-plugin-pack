package net.runelite.client.plugins.microbot.kspaccountbuilder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.accountselector.AutoLoginPlugin;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.equipment.weapon.Weapons;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.meleescript.MeleeScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.firemaking.firemakingscript.FireMakingScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.firemaking.fmarea.FireArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.firemaking.loglevels.LogsLvl;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.mining.areas.Areas;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.mining.miningscript.MiningScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.buyscript.Buy;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.buyscript.BuyScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.gearea.GEArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.sell.SellList;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.sellscript.SellScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smithing.smitharea.SmithArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smithing.smithlevels.SmithLevels;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smithing.smithscript.SmithScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smelting.barlevel.BarLevels;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smelting.oresreq.ReqOres;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smelting.smeltarea.SmeltArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smelting.smeltscript.SmeltScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.tutorialisland.TutorialIslandScript;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.treeareas.TreeAreas;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.woodcuttingscript.WoodCuttingScript;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.PlayStyle;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.security.LoginManager;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.ui.ClientUI;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class KspAccountBuilderScript extends Script
{
    private static final int[] SAFE_F2P_LOGIN_WORLDS = {301, 308, 316, 326, 335};

    private enum BuilderTask
    {
        TUTORIAL_ISLAND,
        MINING,
        WOODCUTTING,
        FIREMAKING,
        MELEE,
        GE_SELL,
        GE_BUY,
        SMITHING,
        SMELTING
    }

    private static final int LOOP_DELAY_MS = 600;

    @Inject
    private MiningScript miningScript;

    @Inject
    private WoodCuttingScript woodCuttingScript;

    @Inject
    private FireMakingScript fireMakingScript;

    @Inject
    private MeleeScript meleeScript;

    @Inject
    private BuyScript buyScript;

    @Inject
    private SellScript sellScript;

    @Inject
    private SmithScript smithScript;

    @Inject
    private SmeltScript smeltScript;

    @Inject
    private TutorialIslandScript tutorialIslandScript;

    @Getter
    private BuilderTask currentTask = getRandomTaskExcluding(null);

    @Getter
    private boolean breakActive;

    @Getter
    private long startedAtMillis;

    private boolean taskStarted;
    private long nextBreakAtMillis;
    private long breakEndsAtMillis;
    private long nextActivitySwitchAtMillis;
    private long lastStatusLogAt;
    private KspAccountBuilderConfig config;
    private boolean debugEnabled;
    private BuilderTask pendingTask;
    private boolean awaitingNextActivityStart;
    private boolean awaitingActivitySwitchTimerStart;
    private boolean breakLogoutRequested;
    private long lastBreakLoginAttemptAt;
    private String originalWindowTitle = "Microbot";

    public boolean run(KspAccountBuilderConfig config)
    {
        shutdown();
        this.config = config;
        this.debugEnabled = config.debugLogging();
        miningScript.setDebugLogging(debugEnabled);
        woodCuttingScript.setDebugLogging(debugEnabled);
        fireMakingScript.setDebugLogging(debugEnabled);
        meleeScript.setDebugLogging(debugEnabled);
        buyScript.setDebugLogging(debugEnabled);
        sellScript.setDebugLogging(debugEnabled);
        smithScript.setDebugLogging(debugEnabled);
        smeltScript.setDebugLogging(debugEnabled);
        applyAntibanSettings();

        currentTask = resolveStartingTask();
        taskStarted = false;
        breakActive = false;
        pendingTask = null;
        awaitingNextActivityStart = false;
        awaitingActivitySwitchTimerStart = isActivitySwitchRandomizationEnabled();
        breakLogoutRequested = false;
        lastBreakLoginAttemptAt = 0L;
        captureOriginalWindowTitle();

        startedAtMillis = System.currentTimeMillis();
        lastStatusLogAt = 0L;

        scheduleNextBreak();
        nextActivitySwitchAtMillis = -1L;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() ->
        {
            if (!super.run())
            {
                return;
            }

            debugEnabled = config.debugLogging();
            miningScript.setDebugLogging(debugEnabled);
            woodCuttingScript.setDebugLogging(debugEnabled);
            fireMakingScript.setDebugLogging(debugEnabled);
            meleeScript.setDebugLogging(debugEnabled);
            buyScript.setDebugLogging(debugEnabled);
            sellScript.setDebugLogging(debugEnabled);
            smithScript.setDebugLogging(debugEnabled);
            smeltScript.setDebugLogging(debugEnabled);

            processTimers();
            updateWindowTitle();
            if (breakActive || pendingTask != null)
            {
                maybeLogStatus();
                return;
            }

            if (!Microbot.isLoggedIn())
            {
                maybeLogStatus();
                return;
            }

            runAccountBuilderCycle();
            maybeLogStatus();
        }, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);

        return true;
    }

    private void processTimers()
    {
        long now = System.currentTimeMillis();
        boolean singleSkillTaskForced = isSingleSkillTaskForced();

        if (config.doBreaks())
        {
            if (!breakActive && now >= nextBreakAtMillis)
            {
                breakActive = true;
                taskStarted = false;
                pendingTask = null;
                awaitingNextActivityStart = false;
                breakLogoutRequested = false;
                lastBreakLoginAttemptAt = 0L;
                miningScript.shutdown();
                woodCuttingScript.shutdown();
                fireMakingScript.shutdown();
                meleeScript.shutdown();
                buyScript.shutdown();
                sellScript.shutdown();
                smithScript.shutdown();
                smeltScript.shutdown();
                tutorialIslandScript.shutdown();
                breakEndsAtMillis = now + TimeUnit.MINUTES.toMillis(randomMinutes(config.breakDurationMinMinutes(), config.breakDurationMaxMinutes()));
                debug("Starting break for {} seconds", getBreakTimeRemainingSeconds());
            }

            if (breakActive && Microbot.isLoggedIn() && !breakLogoutRequested)
            {
                Rs2Player.logout();
                breakLogoutRequested = true;
                sleepUntil(() -> !Microbot.isLoggedIn(), 5_000);
            }

            if (breakActive && now >= breakEndsAtMillis)
            {
                breakActive = false;
                breakLogoutRequested = false;
                scheduleNextBreak();
                debug("Break completed, resuming tasks");

                if (!Microbot.isLoggedIn())
                {
                    attemptLoginAfterBreak();
                }
            }
        }

        if (singleSkillTaskForced)
        {
            pendingTask = null;
            awaitingNextActivityStart = false;
            awaitingActivitySwitchTimerStart = false;
            nextActivitySwitchAtMillis = -1L;
            return;
        }

        if (!isActivitySwitchRandomizationEnabled() || awaitingNextActivityStart || awaitingActivitySwitchTimerStart)
        {
            return;
        }

        if (pendingTask == null && now >= nextActivitySwitchAtMillis)
        {
            if (!ensureInventoryTabOpenForTaskSelection())
            {
                return;
            }

            pendingTask = getRandomTaskExcluding(currentTask);
            taskStarted = false;
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            debug("Preparing activity switch: {} -> {}", currentTask, pendingTask);
        }

        if (pendingTask != null && switchTask(pendingTask))
        {
            pendingTask = null;
            awaitingNextActivityStart = true;
            awaitingActivitySwitchTimerStart = true;
            nextActivitySwitchAtMillis = -1L;
        }
    }

    private void runAccountBuilderCycle()
    {
        applySingleSkillOverride();

        if (!isSingleSkillTaskForced())
        {
            handleTutorialIslandPriority();
        }

        if (!isSingleSkillTaskForced() && !hasResourcesForTask(currentTask))
        {
            if (!switchToTaskWithResources())
            {
                stopCurrentTaskScript();
                taskStarted = false;
                return;
            }
        }

        if (taskStarted)
        {
            if (!isSingleSkillTaskForced()
                    && currentTask == BuilderTask.TUTORIAL_ISLAND
                    && tutorialIslandScript.isComplete())
            {
                tutorialIslandScript.shutdown();
                taskStarted = false;
                currentTask = getRandomTaskWithResourcesExcluding(BuilderTask.TUTORIAL_ISLAND);
                if (currentTask == null)
                {
                    currentTask = getRandomTaskExcluding(BuilderTask.TUTORIAL_ISLAND);
                }
                return;
            }

            if (!isSingleSkillTaskForced() && currentTask == BuilderTask.GE_BUY && buyScript.isComplete())
            {
                if (!switchToTaskWithResources())
                {
                    stopCurrentTaskScript();
                    taskStarted = false;
                }
                return;
            }
            if (!isSingleSkillTaskForced() && currentTask == BuilderTask.GE_SELL && sellScript.isComplete())
            {
                if (!switchToTaskWithResources())
                {
                    stopCurrentTaskScript();
                    taskStarted = false;
                }
                return;
            }
            maybeStartActivitySwitchTimer();
            return;
        }

        if (currentTask == BuilderTask.TUTORIAL_ISLAND)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            taskStarted = tutorialIslandScript.run();
        }
        else if (currentTask == BuilderTask.MINING)
        {
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            miningScript.setProgressiveMining(isMiningProgressionEnabled());
            taskStarted = miningScript.run(resolveMiningStartArea());
        }
        else if (currentTask == BuilderTask.WOODCUTTING)
        {
            miningScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = woodCuttingScript.run(resolveWoodcuttingStartArea());
        }
        else if (currentTask == BuilderTask.FIREMAKING)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = fireMakingScript.run(FireArea.FM_AREA_DRAYNOR_BANK);
        }
        else if (currentTask == BuilderTask.MELEE)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = meleeScript.run();
        }
        else if (currentTask == BuilderTask.GE_SELL)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = sellScript.run(GEArea.GRAND_EXCHANGE);
        }
        else if (currentTask == BuilderTask.GE_BUY)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = buyScript.run(GEArea.GRAND_EXCHANGE);
        }
        else if (currentTask == BuilderTask.SMITHING)
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smeltScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = smithScript.run(SmithArea.SMITH_AREA_VARROCK_WEST_ANVIL);
        }
        else
        {
            miningScript.shutdown();
            woodCuttingScript.shutdown();
            fireMakingScript.shutdown();
            meleeScript.shutdown();
            buyScript.shutdown();
            sellScript.shutdown();
            smithScript.shutdown();
            tutorialIslandScript.shutdown();
            taskStarted = smeltScript.run(SmeltArea.SMELT_AREA_EDGEVILLE_FURNACE, resolveSmeltingFallbackBar());
        }

        if (taskStarted && awaitingNextActivityStart)
        {
            awaitingNextActivityStart = false;
            debug("Activity switch completed; waiting to reach task area before starting next switch timer");
        }

        maybeStartActivitySwitchTimer();
    }

    private void handleTutorialIslandPriority()
    {
        if (!TutorialIslandScript.isOnTutorialIsland())
        {
            return;
        }

        if (currentTask != BuilderTask.TUTORIAL_ISLAND)
        {
            stopCurrentTaskScript();
            currentTask = BuilderTask.TUTORIAL_ISLAND;
            taskStarted = false;
            pendingTask = null;
            awaitingNextActivityStart = false;
            awaitingActivitySwitchTimerStart = false;
            nextActivitySwitchAtMillis = -1L;
            debug("Detected Tutorial Island; switching to tutorial task");
        }

    }

    private BuilderTask resolveStartingTask()
    {
        if (TutorialIslandScript.isOnTutorialIsland())
        {
            return BuilderTask.TUTORIAL_ISLAND;
        }

        BuilderTask singleSkillTask = resolveSingleSkillTask();
        return singleSkillTask != null ? singleSkillTask : getRandomTaskExcluding(null);
    }

    private void applySingleSkillOverride()
    {
        if (!isSingleSkillTaskForced())
        {
            return;
        }

        BuilderTask singleSkillTask = resolveSingleSkillTask();
        if (singleSkillTask == null || currentTask == singleSkillTask)
        {
            return;
        }

        stopCurrentTaskScript();
        taskStarted = false;

        if (!prepareForTaskSwitchAtBank())
        {
            debug("Waiting to apply single skill override; still preparing bank handoff to {}", singleSkillTask);
            return;
        }

        if (!ensureInventoryTabOpenForTaskSelection())
        {
            debug("Waiting to apply single skill override; inventory tab is not open");
            return;
        }

        pendingTask = null;
        awaitingNextActivityStart = false;
        awaitingActivitySwitchTimerStart = false;
        nextActivitySwitchAtMillis = -1L;
        currentTask = singleSkillTask;
        debug("Single skill override switched task to {}", currentTask);
    }

    private BuilderTask resolveSingleSkillTask()
    {
        if (config == null || !config.trainSingleSkill())
        {
            return null;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.TUTORIAL_ISLAND)
        {
            return BuilderTask.TUTORIAL_ISLAND;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.MINING)
        {
            return BuilderTask.MINING;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.WOODCUTTING)
        {
            return BuilderTask.WOODCUTTING;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.FIREMAKING)
        {
            return BuilderTask.FIREMAKING;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.GE_SELL)
        {
            return BuilderTask.GE_SELL;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.MELEE)
        {
            return BuilderTask.MELEE;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.GE_BUY)
        {
            return BuilderTask.GE_BUY;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.SMELTING)
        {
            return BuilderTask.SMELTING;
        }

        if (config.singleSkillTask() == KspTrainSingleSkillTask.SMITHING)
        {
            return BuilderTask.SMITHING;
        }

        return null;
    }

    private boolean isSingleSkillTaskForced()
    {
        return resolveSingleSkillTask() != null;
    }

    private boolean isActivitySwitchRandomizationEnabled()
    {
        return config != null
                && config.enableActivitySwitchRandomization()
                && !isSingleSkillTaskForced();
    }

    private boolean isMiningProgressionEnabled()
    {
        return !isSingleSkillTaskForced()
                || config.singleSkillTask() != KspTrainSingleSkillTask.MINING
                || config.singleSkillProgressive();
    }

    private Areas resolveMiningStartArea()
    {
        return Areas.TIN_COPPER_VARROCK_EAST;
    }

    private TreeAreas resolveWoodcuttingStartArea()
    {
        return TreeAreas.REGULAR_TREE_VARROCK_WEST;
    }

    private BarLevels resolveSmeltingFallbackBar()
    {
        return BarLevels.BRONZE;
    }

    private boolean hasResourcesForTask(BuilderTask task)
    {
        if (task == BuilderTask.TUTORIAL_ISLAND)
        {
            return TutorialIslandScript.isOnTutorialIsland();
        }

        if (task == BuilderTask.MINING)
        {
            return hasAnyToolAvailable(Buy.PICKAXE_NAMES);
        }

        if (task == BuilderTask.WOODCUTTING)
        {
            return hasAnyToolAvailable(Buy.AXE_NAMES);
        }

        if (task == BuilderTask.FIREMAKING)
        {
            return hasAnyFiremakingResourcesAvailable();
        }

        if (task == BuilderTask.MELEE)
        {
            return hasAnyMeleeResourcesAvailable();
        }

        if (task == BuilderTask.GE_SELL)
        {
            return hasAnyGeSellResourcesAvailable();
        }

        if (task == BuilderTask.GE_BUY)
        {
            return hasAnyGeBuyResourcesAvailable();
        }

        if (task == BuilderTask.SMITHING)
        {
            return hasAnySmithingResourcesAvailable();
        }

        return hasAnySmeltingResourcesAvailable();
    }

    private boolean hasAnyToolAvailable(String[] toolNames)
    {
        for (String toolName : toolNames)
        {
            if (Rs2Equipment.isWearing(toolName) || Rs2Inventory.hasItem(toolName) || Rs2Bank.count(toolName) > 0)
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyMeleeResourcesAvailable()
    {
        if (hasAnyMeleeWeaponAvailable())
        {
            return true;
        }

        return Rs2Inventory.count(Buy.COINS_NAME) > 0
                || Rs2Inventory.count(Buy.COINS_NAME, true) > 0
                || Rs2Bank.count(Buy.COINS_NAME) > 0;
    }

    private boolean hasAnyMeleeWeaponAvailable()
    {
        for (Weapons weapon : Weapons.values())
        {
            String weaponName = weapon.getDisplayName();
            if (Rs2Equipment.isWearing(weaponName)
                    || Rs2Inventory.hasItem(weaponName)
                    || Rs2Inventory.hasItem(weaponName, true)
                    || Rs2Bank.count(weaponName) > 0)
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnySmeltingResourcesAvailable()
    {
        int smithingLevel = Microbot.getClient().getRealSkillLevel(Skill.SMITHING);
        for (int i = BarLevels.values().length - 1; i >= 0; i--)
        {
            BarLevels bar = BarLevels.values()[i];
            if (smithingLevel < bar.getRequiredSmithingLevel())
            {
                continue;
            }

            ReqOres req = ReqOres.valueOf(bar.name());
            int primaryAvailable = Rs2Inventory.count(req.getPrimaryOreName()) + Math.max(0, Rs2Bank.count(req.getPrimaryOreName()));
            if (primaryAvailable < req.getPrimaryOreAmount())
            {
                continue;
            }

            if (!req.hasSecondaryOre())
            {
                return true;
            }

            int secondaryAvailable = Rs2Inventory.count(req.getSecondaryOreName()) + Math.max(0, Rs2Bank.count(req.getSecondaryOreName()));
            if (secondaryAvailable >= req.getSecondaryOreAmount())
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyFiremakingResourcesAvailable()
    {
        if (!hasTinderboxAnywhere())
        {
            return false;
        }

        int firemakingLevel = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
        for (int i = LogsLvl.values().length - 1; i >= 0; i--)
        {
            LogsLvl logsLvl = LogsLvl.values()[i];
            if (firemakingLevel < logsLvl.getRequiredLevel())
            {
                continue;
            }

            int availableLogs = Rs2Inventory.count(logsLvl.getDisplayName()) + Math.max(0, Rs2Bank.count(logsLvl.getDisplayName()));
            if (availableLogs > 0)
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnySmithingResourcesAvailable()
    {
        if (!hasHammerWithRequiredIdAnywhere())
        {
            return false;
        }

        int smithingLevel = Microbot.getClient().getRealSkillLevel(Skill.SMITHING);
        for (int i = SmithLevels.values().length - 1; i >= 0; i--)
        {
            SmithLevels level = SmithLevels.values()[i];
            if (smithingLevel < level.getRequiredLevel())
            {
                continue;
            }

            String barName = level.getDisplayName().split(" ")[0] + " bar";
            int barsAvailable = Rs2Inventory.count(barName) + Math.max(0, Rs2Bank.count(barName));
            if (barsAvailable > 0)
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyGeSellResourcesAvailable()
    {
        if (hasAnySellListItemsAvailable())
        {
            return true;
        }

        if (hasAnyOutdatedToolAvailable())
        {
            return true;
        }

        return false;
    }

    private boolean hasAnyGeBuyResourcesAvailable()
    {
        return Buy.hasAnyGeBuyRequirementMissing();
    }

    private boolean hasAnySellListItemsAvailable()
    {
        int firemakingLevel = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
        for (SellList sellList : SellList.values())
        {
            if (sellList == SellList.LOGS && firemakingLevel < 15)
            {
                continue;
            }

            if (sellList == SellList.OAK_LOGS && firemakingLevel < 30)
            {
                continue;
            }

            int available = Rs2Inventory.count(sellList.getDisplayName())
                    + Rs2Inventory.count(sellList.getDisplayName(), true)
                    + Math.max(0, Rs2Bank.count(sellList.getDisplayName()));
            if (available > 0)
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyOutdatedToolAvailable()
    {
        String desiredPickaxe = resolveDesiredPickaxeForSellingTask();
        String desiredAxe = resolveDesiredAxeForSellingTask();

        for (String pickaxeName : Buy.PICKAXE_NAMES)
        {
            if (pickaxeName.equalsIgnoreCase(desiredPickaxe))
            {
                continue;
            }

            if (Rs2Equipment.isWearing(pickaxeName)
                    || Rs2Inventory.hasItem(pickaxeName)
                    || Rs2Inventory.hasItem(pickaxeName, true)
                    || Rs2Bank.count(pickaxeName) > 0)
            {
                return true;
            }
        }

        for (String axeName : Buy.AXE_NAMES)
        {
            if (axeName.equalsIgnoreCase(desiredAxe))
            {
                continue;
            }

            if (Rs2Equipment.isWearing(axeName)
                    || Rs2Inventory.hasItem(axeName)
                    || Rs2Inventory.hasItem(axeName, true)
                    || Rs2Bank.count(axeName) > 0)
            {
                return true;
            }
        }

        return false;
    }

    private String resolveDesiredPickaxeForSellingTask()
    {
        return Buy.resolveDesiredPickaxeNameForGear();
    }

    private String resolveDesiredAxeForSellingTask()
    {
        return Buy.resolveDesiredAxeNameForGear();
    }

    private boolean hasHammerWithRequiredIdAnywhere()
    {
        return Buy.hasHammerAnywhere();
    }

    private boolean hasTinderboxAnywhere()
    {
        return Buy.hasTinderboxAnywhere();
    }

    private boolean switchToTaskWithResources()
    {
        stopCurrentTaskScript();
        taskStarted = false;

        if (!prepareForTaskSwitchAtBank())
        {
            debug("Waiting to switch task; still preparing bank handoff before selecting next task");
            return false;
        }

        if (!ensureInventoryTabOpenForTaskSelection())
        {
            debug("Waiting to switch task; inventory tab is not open before selecting next task");
            return false;
        }

        BuilderTask nextTask = getRandomTaskWithResourcesExcluding(currentTask);
        if (nextTask == null)
        {
            debug("Current task {} has no resources and no alternative task is available", currentTask);
            return false;
        }

        pendingTask = null;
        awaitingNextActivityStart = false;
        awaitingActivitySwitchTimerStart = isActivitySwitchRandomizationEnabled();
        nextActivitySwitchAtMillis = -1L;

        debug("Switching task from {} to {} because resources are unavailable", currentTask, nextTask);
        currentTask = nextTask;
        return true;
    }

    private BuilderTask getRandomTaskWithResourcesExcluding(BuilderTask excludedTask)
    {
        if (TutorialIslandScript.isOnTutorialIsland())
        {
            return excludedTask == BuilderTask.TUTORIAL_ISLAND ? null : BuilderTask.TUTORIAL_ISLAND;
        }

        BuilderTask[] candidates = new BuilderTask[BuilderTask.values().length];
        int candidateCount = 0;

        for (BuilderTask task : BuilderTask.values())
        {
            if (task == excludedTask || task == BuilderTask.TUTORIAL_ISLAND)
            {
                continue;
            }

            if (hasResourcesForTask(task))
            {
                candidates[candidateCount++] = task;
            }
        }

        if (candidateCount == 0)
        {
            return null;
        }

        return candidates[ThreadLocalRandom.current().nextInt(candidateCount)];
    }

    private void stopCurrentTaskScript()
    {
        if (currentTask == BuilderTask.TUTORIAL_ISLAND)
        {
            tutorialIslandScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.MINING)
        {
            miningScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.WOODCUTTING)
        {
            woodCuttingScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.FIREMAKING)
        {
            fireMakingScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.MELEE)
        {
            meleeScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.GE_SELL || currentTask == BuilderTask.GE_BUY)
        {
            buyScript.shutdown();
            sellScript.shutdown();
            return;
        }

        if (currentTask == BuilderTask.SMITHING)
        {
            smithScript.shutdown();
            return;
        }

        smeltScript.shutdown();
        tutorialIslandScript.shutdown();
    }

    private boolean switchTask(BuilderTask nextTask)
    {
        if (!prepareForTaskSwitchAtBank())
        {
            debug("Waiting to switch task; still preparing bank/gear handoff to {}", nextTask);
            return false;
        }

        taskStarted = false;
        if (currentTask == BuilderTask.TUTORIAL_ISLAND)
        {
            tutorialIslandScript.shutdown();
        }
        else if (currentTask == BuilderTask.MINING)
        {
            miningScript.shutdown();
        }
        else if (currentTask == BuilderTask.WOODCUTTING)
        {
            woodCuttingScript.shutdown();
        }
        else if (currentTask == BuilderTask.FIREMAKING)
        {
            fireMakingScript.shutdown();
        }
        else if (currentTask == BuilderTask.MELEE)
        {
            meleeScript.shutdown();
        }
        else if (currentTask == BuilderTask.GE_SELL || currentTask == BuilderTask.GE_BUY)
        {
            buyScript.shutdown();
            sellScript.shutdown();
        }
        else if (currentTask == BuilderTask.SMITHING)
        {
            smithScript.shutdown();
        }
        else
        {
            smeltScript.shutdown();
        }

        currentTask = nextTask;
        debug("Switching task to {}", currentTask);
        return true;
    }

    private BuilderTask getRandomTaskExcluding(BuilderTask excludedTask)
    {
        if (TutorialIslandScript.isOnTutorialIsland())
        {
            return BuilderTask.TUTORIAL_ISLAND;
        }

        BuilderTask[] tasks = BuilderTask.values();
        BuilderTask nextTask = tasks[ThreadLocalRandom.current().nextInt(tasks.length)];

        int safety = 0;
        while ((nextTask == excludedTask || nextTask == BuilderTask.TUTORIAL_ISLAND) && safety < 20)
        {
            nextTask = tasks[ThreadLocalRandom.current().nextInt(tasks.length)];
            safety++;
        }

        if (nextTask == BuilderTask.TUTORIAL_ISLAND)
        {
            return BuilderTask.MINING;
        }

        return nextTask;
    }

    private boolean ensureInventoryTabOpenForTaskSelection()
    {
        if (!Microbot.isLoggedIn())
        {
            return false;
        }

        if (Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY)
        {
            return true;
        }

        Rs2Tab.switchTo(InterfaceTab.INVENTORY);
        sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY, 1_500);

        return Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY;
    }

    private boolean prepareForTaskSwitchAtBank()
    {
        if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Bank.openBank())
        {
            return false;
        }

        if (!Rs2Bank.isOpen())
        {
            return false;
        }

        sleep(300);

        if (!depositInventoryForTaskSwitch())
        {
            return false;
        }

        if (!unequipGatheringTools())
        {
            return false;
        }

        if (!Rs2Bank.isOpen() && !Rs2Bank.openBank())
        {
            return false;
        }

        depositGatheringToolsInInventory();

        return depositInventoryForTaskSwitch();
    }

    private boolean depositInventoryForTaskSwitch()
    {
        if (!Rs2Bank.isOpen())
        {
            return false;
        }

        if (!Rs2Inventory.isEmpty())
        {
            Rs2Bank.depositAll();
            sleepUntil(Rs2Inventory::isEmpty, 1_500);
            sleep(200);
        }

        return Rs2Bank.isOpen();
    }

    private boolean unequipGatheringTools()
    {
        boolean hasPickaxeEquipped = Rs2Equipment.isWearing("pickaxe", false) || Rs2Equipment.isWearing("pickaxe");
        boolean hasAxeEquipped = Rs2Equipment.isWearing("axe", false) || Rs2Equipment.isWearing("axe");
        if (!hasPickaxeEquipped && !hasAxeEquipped)
        {
            return true;
        }

        if (Rs2Bank.isOpen())
        {
            Rs2Bank.depositEquipment();
            sleepUntil(() -> !isGatheringToolEquipped(), 1_500);
            return !isGatheringToolEquipped();
        }

        return false;
    }

    private boolean isGatheringToolEquipped()
    {
        return Rs2Equipment.isWearing("pickaxe", false) || Rs2Equipment.isWearing("pickaxe")
                || Rs2Equipment.isWearing("axe", false) || Rs2Equipment.isWearing("axe");
    }

    private void depositGatheringToolsInInventory()
    {
        for (String pickaxeName : Buy.PICKAXE_NAMES)
        {
            if (Rs2Inventory.hasItem(pickaxeName))
            {
                Rs2Bank.depositAll(pickaxeName);
                sleep(100);
            }
        }

        for (String axeName : Buy.AXE_NAMES)
        {
            if (Rs2Inventory.hasItem(axeName))
            {
                Rs2Bank.depositAll(axeName);
                sleep(100);
            }
        }
    }

    private void scheduleNextBreak()
    {
        if (!config.doBreaks())
        {
            nextBreakAtMillis = -1L;
            breakEndsAtMillis = -1L;
            return;
        }

        long delayMinutes = randomMinutes(config.breakAfterMinMinutes(), config.breakAfterMaxMinutes());
        nextBreakAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes);
    }

    private void scheduleNextActivitySwitch()
    {
        if (!isActivitySwitchRandomizationEnabled())
        {
            nextActivitySwitchAtMillis = -1L;
            return;
        }

        long delayMinutes = randomMinutes(config.activitySwitchMinMinutes(), config.activitySwitchMaxMinutes());
        nextActivitySwitchAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes);
    }

    private void maybeStartActivitySwitchTimer()
    {
        if (!isActivitySwitchRandomizationEnabled() || !awaitingActivitySwitchTimerStart || !taskStarted)
        {
            return;
        }

        if (!isInCurrentTaskArea())
        {
            return;
        }

        awaitingActivitySwitchTimerStart = false;
        scheduleNextActivitySwitch();
        debug("Started activity switch timer for {} after reaching task area", currentTask);
    }

    private boolean isInCurrentTaskArea()
    {
        if (Rs2Player.getWorldLocation() == null)
        {
            return false;
        }

        if (currentTask == BuilderTask.TUTORIAL_ISLAND)
        {
            return TutorialIslandScript.isOnTutorialIsland();
        }

        if (currentTask == BuilderTask.MINING)
        {
            return miningScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.WOODCUTTING)
        {
            return woodCuttingScript.getTargetArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.FIREMAKING)
        {
            return fireMakingScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.MELEE)
        {
            return meleeScript.getTargetArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.GE_SELL)
        {
            return sellScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.GE_BUY)
        {
            return buyScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
        }

        if (currentTask == BuilderTask.SMITHING)
        {
            return smithScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
        }

        return smeltScript.getTargetArea().toWorldArea().contains(Rs2Player.getWorldLocation());
    }

    private int randomMinutes(int min, int max)
    {
        int safeMin = Math.min(min, max);
        int safeMax = Math.max(min, max);
        return ThreadLocalRandom.current().nextInt(safeMin, safeMax + 1);
    }


    private void applyAntibanSettings()
    {
        Rs2Antiban.resetAntibanSettings();

        if (!config.useAntiban())
        {
            return;
        }

        Rs2Antiban.antibanSetupTemplates.applyUniversalAntibanSetup();
        for (PlayStyle playStyle : PlayStyle.values())
        {
            playStyle.resetPlayStyle();
        }
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.universalAntiban = true;
        Rs2AntibanSettings.nonLinearIntervals = false;
        Rs2AntibanSettings.actionCooldownChance = 0.20;
    }

    private void debug(String message, Object... args)
    {
        if (debugEnabled)
        {
            KspTaskDebug.info(log, true, "Builder", message, args);
        }
    }

    private void maybeLogStatus()
    {
        long now = System.currentTimeMillis();
        if (now - lastStatusLogAt >= 10_000)
        {
            debug("active task | currentTask={} pendingTask={} breakActive={} player={} moving={} animating={} interacting={} bankOpen={} taskStarted={}",
                    currentTask,
                    pendingTask,
                    breakActive,
                    Rs2Player.getWorldLocation(),
                    Rs2Player.isMoving(),
                    Rs2Player.isAnimating(),
                    Rs2Player.isInteracting(),
                    Rs2Bank.isOpen(),
                    taskStarted);
            lastStatusLogAt = now;
        }
    }

    private void captureOriginalWindowTitle()
    {
        try
        {
            String currentTitle = ClientUI.getFrame().getTitle();
            if (currentTitle != null && !currentTitle.isEmpty())
            {
                originalWindowTitle = currentTitle;
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private void updateWindowTitle()
    {
        try
        {
            if (!breakActive)
            {
                ClientUI.getFrame().setTitle(originalWindowTitle);
                return;
            }

            long breakRemaining = getBreakTimeRemainingSeconds();
            String title = originalWindowTitle + " - Breaking for: " + formatBreakDuration(breakRemaining);
            ClientUI.getFrame().setTitle(title);
        }
        catch (Exception ignored)
        {
        }
    }

    private String formatBreakDuration(long totalSeconds)
    {
        if (totalSeconds < 0)
        {
            return "--:--";
        }

        Duration duration = Duration.ofSeconds(totalSeconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        if (hours > 0)
        {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%02d:%02d", minutes, seconds);
    }

    private void attemptLoginAfterBreak()
    {
        long now = System.currentTimeMillis();
        if ((now - lastBreakLoginAttemptAt) < TimeUnit.SECONDS.toMillis(10))
        {
            return;
        }

        lastBreakLoginAttemptAt = now;

        if (attemptAutoLogin())
        {
            debug("Triggered AutoLogin after break");
            return;
        }

        if (LoginManager.getActiveProfile() == null)
        {
            debug("Unable to log back in after break; no active login profile is configured");
            return;
        }

        try
        {
            boolean isMemberProfile = LoginManager.getActiveProfile().isMember();
            int world = isMemberProfile
                    ? Login.getRandomWorld(true)
                    : SAFE_F2P_LOGIN_WORLDS[ThreadLocalRandom.current().nextInt(SAFE_F2P_LOGIN_WORLDS.length)];

            new Login(world);
            debug("Triggered manual login after break");
        }
        catch (Exception ex)
        {
            debug("Failed to trigger login after break: {}", ex.getMessage());
        }
    }

    private boolean attemptAutoLogin()
    {
        try
        {
            AutoLoginPlugin autoLoginPlugin = (AutoLoginPlugin) Microbot.getPlugin(AutoLoginPlugin.class.getName());
            if (autoLoginPlugin == null)
            {
                return false;
            }

            if (!Microbot.isPluginEnabled(autoLoginPlugin.getClass()))
            {
                Microbot.getClientThread().runOnSeperateThread(() ->
                {
                    Microbot.startPlugin(autoLoginPlugin);
                    return true;
                });
            }
            return true;
        }
        catch (Exception ex)
        {
            debug("Failed to trigger AutoLogin after break: {}", ex.getMessage());
            return false;
        }
    }

    public String getCurrentTaskName()
    {
        if (pendingTask != null)
        {
            return "SWITCHING_TO_" + pendingTask.name();
        }

        return currentTask.name();
    }

    public long getRuntimeSeconds()
    {
        if (startedAtMillis <= 0L)
        {
            return 0L;
        }
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startedAtMillis);
    }

    public long getTimeUntilBreakSeconds()
    {
        if (!config.doBreaks() || breakActive || nextBreakAtMillis <= 0L)
        {
            return -1L;
        }
        return Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(nextBreakAtMillis - System.currentTimeMillis()));
    }

    public long getBreakTimeRemainingSeconds()
    {
        if (!breakActive || breakEndsAtMillis <= 0L)
        {
            return -1L;
        }
        return Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(breakEndsAtMillis - System.currentTimeMillis()));
    }

    public long getTimeUntilActivitySwitchSeconds()
    {
        if (isSingleSkillTaskForced())
        {
            return -1L;
        }

        if (!isActivitySwitchRandomizationEnabled() || nextActivitySwitchAtMillis <= 0L || pendingTask != null || awaitingNextActivityStart)
        {
            return -1L;
        }
        return Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(nextActivitySwitchAtMillis - System.currentTimeMillis()));
    }

    @Override
    public void shutdown()
    {
        taskStarted = false;
        breakActive = false;
        startedAtMillis = 0L;
        nextBreakAtMillis = -1L;
        breakEndsAtMillis = -1L;
        nextActivitySwitchAtMillis = -1L;
        debugEnabled = false;
        pendingTask = null;
        awaitingNextActivityStart = false;
        awaitingActivitySwitchTimerStart = false;
        breakLogoutRequested = false;
        lastBreakLoginAttemptAt = 0L;
        updateWindowTitle();

        if (miningScript != null)
        {
            miningScript.shutdown();
        }

        if (woodCuttingScript != null)
        {
            woodCuttingScript.shutdown();
        }

        if (fireMakingScript != null)
        {
            fireMakingScript.shutdown();
        }

        if (meleeScript != null)
        {
            meleeScript.shutdown();
        }

        if (buyScript != null)
        {
            buyScript.shutdown();
        }

        if (sellScript != null)
        {
            sellScript.shutdown();
        }

        if (smithScript != null)
        {
            smithScript.shutdown();
        }

        if (smeltScript != null)
        {
            smeltScript.shutdown();
        }

        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
    }
}
