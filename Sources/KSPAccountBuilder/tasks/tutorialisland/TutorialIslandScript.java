package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.tutorialisland;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Singleton;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.clickContinue;
import static net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.hasContinue;
import static net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.hasSelectAnOption;
import static net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue.isInDialogue;

@Singleton
@Slf4j
public class TutorialIslandScript extends Script
{
    private static final int LOOP_DELAY_MS = 600;
    private static final int NAME_ATTEMPT_COOLDOWN_MS = 3500;
    private static final int NAME_CREATION_GROUP = 558;
    private static final int NAME_CREATION_CONTAINER_CHILD = 2;
    private static final int NAME_INPUT_CHILD = 7;
    private static final int NAME_INPUT_TEXT_CHILD = 12;
    private static final int NAME_RESPONSE_TEXT_CHILD = 13;
    private static final int LOOK_UP_NAME_BUTTON_CHILD = 18;
    private static final int SET_NAME_BUTTON_CHILD = 19;
    private static final int CHARACTER_CREATION_GROUP = 679;
    private static final int CHARACTER_CREATION_CONTAINER_CHILD = 4;
    private static final int BODY_TYPE_A_CHILD = 68;
    private static final int BODY_TYPE_B_CHILD = 69;
    private static final int CHARACTER_CONFIRM_BUTTON_CHILD = 74;
    private static final int INTRO_MIN_X = 3092;
    private static final int INTRO_MAX_X_EXCLUSIVE = 3096;
    private static final int INTRO_MIN_Y = 3100;
    private static final int INTRO_MAX_Y_EXCLUSIVE = 3113;
    private static final int TUTORIAL_CAVE_MIN_X = 3071;
    private static final int TUTORIAL_CAVE_MAX_X = 3123;
    private static final int TUTORIAL_CAVE_MIN_Y = 9493;
    private static final int TUTORIAL_CAVE_MAX_Y = 9537;
    private static final int TUTORIAL_OVERWORLD_MIN_X = 3055;
    private static final int TUTORIAL_OVERWORLD_MAX_X = 3153;
    private static final int TUTORIAL_OVERWORLD_MIN_Y = 3058;
    private static final int TUTORIAL_OVERWORLD_MAX_Y = 3136;
    private static final WorldArea SURVIVAL_AREA = new WorldArea(3098, 3089, 8, 11, 0);
    private static final WorldArea COOKING_AREA = new WorldArea(3073, 3083, 6, 4, 0);
    private static final WorldArea QUEST_GUIDE_AREA = new WorldArea(3083, 3119, 7, 7, 0);
    private static final WorldArea MINING_SMITHING_AREA = new WorldArea(3073, 9494, 15, 15, 0);
    private static final WorldArea COMBAT_INSTRUCTOR_AREA = new WorldArea(3103, 9507, 6, 3, 0);
    private static final WorldArea RAT_PIT_AREA = new WorldArea(3097, 9507, 15, 12, 0);
    private static final WorldArea RAT_GATE_EDGE_AREA = new WorldArea(3111, 9516, 1, 4, 0);
    private static final String DISPLAY_NAME_TITLE = "Set display name";
    private static final String LEARNING_THE_ROPES_QUEST_NAME = "Learning the Ropes";
    private static final String LOOK_UP_NAME_BUTTON = "Look up name";
    private static final String CHARACTER_CREATOR_TITLE = "Character Creator";
    private static final String EXPERIENCE_PROMPT_TITLE = "How familiar are you with Old School RuneScape?";
    private static final String[] EXPERIENCE_OPTION_TEXTS = {
            "I'm brand new! This is my first time here.",
            "I've played in the past, but not recently.",
            "I'm an experienced player."
    };
    private static final String[] NAME_PREFIXES = {
            "Ash", "Bryn", "Cora", "Dane", "Eli", "Faye", "Glen", "Hale", "Iris", "Joss",
            "Kian", "Lena", "Mira", "Nora", "Oren", "Perr", "Quin", "Rhea", "Sora", "Tavi"
    };
    private static final String[] NAME_SUFFIXES = {
            "ford", "vale", "mere", "wyn", "low", "den", "holt", "wick", "row", "lan",
            "well", "mont", "ley", "mar", "rin", "son", "len", "hart", "brook", "field"
    };
    private static final int[] CHARACTER_CREATION_ARROWS = {
            13, 17, 21, 25, 29, 33, 37, 44, 48, 52, 56, 60
    };

    private Status status = Status.NAME;
    private boolean debugLogging;
    private boolean toggledSettings;
    private boolean survivalLogCut;
    private boolean survivalFireLit;
    private long lastNameAttemptAtMs;
    private long lastNameDebugAtMs;
    private long lastClientReadyDebugAtMs;
    private long lastLoopDebugAtMs;
    private long lastMiningDebugAtMs;
    private long lastQuestSyncDebugAtMs;
    private long lastNpcWalkAttemptAtMs;
    private QuestState lastLearningTheRopesState;
    private String lastGeneratedName = "None";
    private String lastCharacterAction = "Waiting";
    private String lastExperienceSelection = "None";
    private String lastLearningTheRopesHint = "Unavailable";
    private String lastLearningTheRopesJournalText = "";
    private WorldPoint ownFireLocation;

    public boolean run()
    {
        shutdown();
        resetRuntimeState();
        configureAntiban();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::runTutorialLoop,
                0,
                LOOP_DELAY_MS,
                TimeUnit.MILLISECONDS);

        return true;
    }

    private void runTutorialLoop()
    {
        try {
            if (!super.run()) {
                return;
            }

            if (!Microbot.isLoggedIn()) {
                return;
            }

            if (!isTutorialClientReady()
                    && !isDisplayNameWidgetOpen()
                    && !isCharacterCreationWidgetOpen()) {
                return;
            }

            if (closeBlockingWidgetIfOpen()) {
                return;
            }

            if (isDisplayNameWidgetOpen()) {
                status = Status.NAME;
                handleDisplayNameWidget();
                return;
            }

            if (isCharacterCreationWidgetOpen()) {
                status = Status.CHARACTER;
                randomizeCharacter();
                return;
            }

            updateStatus();

            if (isExperiencePromptOpen()) {
                selectRandomExperienceOption();
                return;
            }

            if (continueDialogueIfOpen()) {
                return;
            }

            if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
                return;
            }

            runCurrentStatus();
        } catch (Exception ex) {
            log.warn("[KSP Tutorial] loop failed", ex);
        }
    }

    private void runCurrentStatus()
    {
        debugLoopStatus();

        switch (status) {
            case GETTING_STARTED:
                gettingStarted();
                break;
            case SURVIVAL_GUIDE:
                survivalGuide();
                break;
            case COOKING_GUIDE:
                cookingGuide();
                break;
            case QUEST_GUIDE:
                questGuide();
                break;
            case MINING_GUIDE:
                miningGuide();
                break;
            case COMBAT_GUIDE:
                combatGuide();
                break;
            case BANKER_GUIDE:
                bankerGuide();
                break;
            case PRAYER_GUIDE:
                prayerGuide();
                break;
            case MAGE_GUIDE:
                mageGuide();
                break;
            case FINISHED:
                shutdown();
                break;
            default:
                break;
        }
    }

    private void updateStatus()
    {
        int progress = getProgress();
        Status progressStatus = statusFromProgress(progress);
        QuestState questState = getLearningTheRopesQuestState();
        Status journalStatus = getLearningTheRopesJournalStatusIfOpen();

        lastLearningTheRopesState = questState;

        if (questState == QuestState.FINISHED) {
            status = Status.FINISHED;
        } else if (shouldUseQuestJournalStatus(progress, progressStatus, journalStatus)) {
            status = journalStatus;
        } else {
            status = progressStatus;
        }

        debugQuestSync(progress, questState, progressStatus, journalStatus);
    }

    private Status statusFromProgress(int progress)
    {
        if (progress < 10) {
            return Status.GETTING_STARTED;
        }

        if (progress < 120) {
            return Status.SURVIVAL_GUIDE;
        }

        if (progress < 200) {
            return Status.COOKING_GUIDE;
        }

        if (progress <= 250) {
            return Status.QUEST_GUIDE;
        }

        if (progress <= 360) {
            return Status.MINING_GUIDE;
        }

        if (progress < 510) {
            return Status.COMBAT_GUIDE;
        }

        if (progress < 540) {
            return Status.BANKER_GUIDE;
        }

        if (progress < 610) {
            return Status.PRAYER_GUIDE;
        }

        if (progress < 1000) {
            return Status.MAGE_GUIDE;
        }

        return Status.FINISHED;
    }

    private QuestState getLearningTheRopesQuestState()
    {
        try {
            return Rs2Player.getQuestState(Quest.LEARNING_THE_ROPES);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Status getLearningTheRopesJournalStatusIfOpen()
    {
        Widget title = Rs2Widget.getWidget(InterfaceID.Questjournal.TITLE);
        if (title == null) {
            lastLearningTheRopesHint = "Quest journal closed";
            lastLearningTheRopesJournalText = "";
            return null;
        }

        String titleText = cleanJournalText(title.getText());
        if (!titleText.toLowerCase(Locale.ENGLISH).contains(LEARNING_THE_ROPES_QUEST_NAME.toLowerCase(Locale.ENGLISH))) {
            lastLearningTheRopesHint = titleText.isEmpty() ? "Quest journal title empty" : "Quest journal on " + titleText;
            lastLearningTheRopesJournalText = "";
            return null;
        }

        Widget textLayer = Rs2Widget.getWidget(InterfaceID.Questjournal.TEXTLAYER);
        String journalText = getWidgetTreeText(textLayer);
        if (journalText.isEmpty()) {
            lastLearningTheRopesHint = "Learning the Ropes journal text empty";
            lastLearningTheRopesJournalText = "";
            return null;
        }

        lastLearningTheRopesJournalText = journalText;
        lastLearningTheRopesHint = summarizeJournalText(journalText);
        return inferStatusFromLearningTheRopesJournal(journalText);
    }

    private boolean shouldUseQuestJournalStatus(int progress, Status progressStatus, Status journalStatus)
    {
        if (journalStatus == null || journalStatus == Status.FINISHED) {
            return false;
        }

        if (progress <= 0) {
            return true;
        }

        return progressStatus == Status.GETTING_STARTED
                && statusRank(journalStatus) > statusRank(progressStatus);
    }

    private Status inferStatusFromLearningTheRopesJournal(String journalText)
    {
        String text = journalText.toLowerCase(Locale.ENGLISH);

        if (containsAny(text, "magic instructor", "wind strike", "mainland")) {
            return Status.MAGE_GUIDE;
        }

        if (containsAny(text, "brother brace", "prayer", "friends list", "ignore list")) {
            return Status.PRAYER_GUIDE;
        }

        if (containsAny(text, "account guide", "banker", "bank", "poll booth")) {
            return Status.BANKER_GUIDE;
        }

        if (containsAny(text, "combat instructor", "giant rat", "shortbow", "bronze arrow", "ladder")) {
            return Status.COMBAT_GUIDE;
        }

        if (containsAny(text, "mining instructor", "copper", "tin", "bronze bar", "bronze dagger", "anvil", "furnace")) {
            return Status.MINING_GUIDE;
        }

        if (containsAny(text, "quest guide", "quest list", "quest journal")) {
            return Status.QUEST_GUIDE;
        }

        if (containsAny(text, "master chef", "bread", "dough", "cook")) {
            return Status.COOKING_GUIDE;
        }

        if (containsAny(text, "survival expert", "shrimp", "tinderbox", "tree", "logs")) {
            return Status.SURVIVAL_GUIDE;
        }

        if (containsAny(text, "gielinor guide", "settings menu", "options menu")) {
            return Status.GETTING_STARTED;
        }

        return null;
    }

    private String getWidgetTreeText(Widget widget)
    {
        StringBuilder builder = new StringBuilder();
        appendWidgetText(widget, builder);
        return cleanJournalText(builder.toString());
    }

    private void appendWidgetText(Widget widget, StringBuilder builder)
    {
        if (widget == null || widget.isHidden()) {
            return;
        }

        String text = cleanJournalText(widget.getText());
        if (!text.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(text);
        }

        appendWidgetChildrenText(widget.getStaticChildren(), builder);
        appendWidgetChildrenText(widget.getDynamicChildren(), builder);
        appendWidgetChildrenText(widget.getNestedChildren(), builder);
    }

    private void appendWidgetChildrenText(Widget[] widgets, StringBuilder builder)
    {
        if (widgets == null) {
            return;
        }

        for (Widget widget : widgets) {
            appendWidgetText(widget, builder);
        }
    }

    private String cleanJournalText(String text)
    {
        if (text == null) {
            return "";
        }

        return Rs2UiHelper.stripTagsToSpace(Rs2UiHelper.stripColTags(text))
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String summarizeJournalText(String text)
    {
        if (text.length() <= 180) {
            return text;
        }

        return text.substring(0, 180).trim();
    }

    private boolean containsAny(String text, String... needles)
    {
        return Arrays.stream(needles).anyMatch(text::contains);
    }

    private int statusRank(Status candidate)
    {
        if (candidate == null) {
            return -1;
        }

        switch (candidate) {
            case GETTING_STARTED:
                return 1;
            case SURVIVAL_GUIDE:
                return 2;
            case COOKING_GUIDE:
                return 3;
            case QUEST_GUIDE:
                return 4;
            case MINING_GUIDE:
                return 5;
            case COMBAT_GUIDE:
                return 6;
            case BANKER_GUIDE:
                return 7;
            case PRAYER_GUIDE:
                return 8;
            case MAGE_GUIDE:
                return 9;
            case FINISHED:
                return 10;
            default:
                return 0;
        }
    }

    private void debugQuestSync(int progress, QuestState questState, Status progressStatus, Status journalStatus)
    {
        if (!debugLogging) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastQuestSyncDebugAtMs < 5000) {
            return;
        }

        lastQuestSyncDebugAtMs = now;
        debug("quest sync | quest={} progress={} progressStatus={} journalStatus={} selectedStatus={} journalHint={}",
                questState == null ? "Unknown" : questState,
                progress,
                progressStatus,
                journalStatus,
                status,
                lastLearningTheRopesHint);
    }

    private void gettingStarted()
    {
        Rs2NpcModel guide = npc(NpcID.GIELINOR_GUIDE, "Gielinor Guide");
        int progress = getProgress();

        if (progress < 3) {
            walkAndTalk(guide);
            return;
        }

        if (progress < 8) {
            if (!toggledSettings) {
                Rs2Widget.clickWidget(164, 41);
                toggledSettings = true;
                waitForWidgetAction();
                return;
            }

            Rs2Camera.setZoom(Rs2Random.between(400, 450));
            Rs2Random.waitEx(300, 100);
            Rs2Camera.setPitch(280);
            sleepUntil(() -> Rs2Camera.getPitch() > 250, 1500);
        }

        walkAndTalk(guide);
    }

    private void survivalGuide()
    {
        int progress = getProgress();

        if (!ensureStartDoorPassed()) {
            return;
        }

        if (progress == 10 || progress == 20 || progress == 60) {
            talkToSurvivalExpert();
        } else if (progress < 40) {
            clickTabFast("Inventory");
            sleepUntil(() -> getProgress() >= 40, 700);
        } else if (progress < 50) {
            fishShrimp();
        } else if (progress < 70) {
            clickTabFast("Skills");
            sleepUntil(() -> getProgress() >= 70, 700);
            talkToSurvivalExpert();
        } else if (progress <= 90) {
            completeSurvivalCookingStep();
        }
    }

    private void completeSurvivalCookingStep()
    {
        if (!Rs2Inventory.hasItem(ItemID.BRONZE_AXE) || !Rs2Inventory.hasItem(ItemID.TINDERBOX)) {
            talkToSurvivalExpert();
            return;
        }

        if (!hasRawShrimp()) {
            fishShrimp();
            if (!hasRawShrimp()) {
                return;
            }
        }

        TileObject ownFire = getOwnFire();
        if (ownFire != null) {
            cookShrimpOnOwnFire(ownFire);
            return;
        }

        if (!hasTutorialLog()) {
            if (!cutOneTutorialLog()) {
                debug("No tutorial log available after chop attempt | progress={} fireLit={} fireLocation={} player={}",
                        getProgress(),
                        survivalFireLit,
                        ownFireLocation,
                        Rs2Player.getWorldLocation());
                return;
            }
        }

        if (lightOneTutorialFire()) {
            ownFire = getOwnFire();
            if (ownFire != null) {
                cookShrimpOnOwnFire(ownFire);
                return;
            }
        }

        debug("Tutorial fire was not detected after lighting attempt; will retry by cutting/lighting again if needed | hasLog={} fireLit={} fireLocation={} progress={} player={}",
                hasTutorialLog(),
                survivalFireLit,
                ownFireLocation,
                getProgress(),
                Rs2Player.getWorldLocation());
    }

    private void cookingGuide()
    {
        Rs2NpcModel chef = npc(NpcID.MASTER_CHEF, "Master Chef");
        int progress = getProgress();

        if (progress == 120) {
            Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
            ensureSurvivalCookingGatePassed();
        } else if (progress == 130) {
            ensureKitchenAccess(chef);
        } else if (progress == 140) {
            if (!ensureKitchenAccess(chef)) {
                return;
            }
            walkAndTalk(chef);
        } else if (progress < 200) {
            if (!Rs2Inventory.contains("Bread") && !ensureKitchenAccess(chef)) {
                return;
            }
            makeBreadAndLeaveKitchen();
        }
    }

    private void makeBreadAndLeaveKitchen()
    {
        if (!hasBreadDough() && !Rs2Inventory.contains("Bread")) {
            Rs2Inventory.combine("Bucket of water", "Pot of flour");
            sleepUntil(this::hasBreadDough, 2500);
            return;
        }

        if (hasBreadDough()) {
            if (Rs2Inventory.interact("Bread dough")) {
                Microbot.getRs2TileObjectCache().query().interact(9736, "Use");
                sleepUntil(() -> Rs2Inventory.contains("Bread"), 5000);
            }
            return;
        }

        if (Rs2Inventory.contains("Bread")) {
            ensureKitchenExitPassed();
        }
    }

    private void questGuide()
    {
        Rs2NpcModel guide = npc(NpcID.QUEST_GUIDE, "Quest Guide");
        int progress = getProgress();

        if (progress == 200 || progress == 210) {
            ensureQuestGuideAccess(guide);
        } else if (progress == 220 || progress == 240) {
            if (!ensureQuestGuideAccess(guide)) {
                return;
            }
            walkAndTalk(guide);
        } else if (progress == 230) {
            clickTab("Quest List");
        } else {
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            Rs2Random.waitEx(600, 100);
            ensureQuestCaveLadderPassed();
        }
    }

    private void miningGuide()
    {
        debugMiningGuideState();

        if (getProgress() == 260) {
            if (!isTutorialUndergroundPoint(Rs2Player.getWorldLocation()) && !ensureQuestCaveLadderPassed()) {
                return;
            }
            walkAndTalk(npc(NpcID.MINING_INSTRUCTOR, "Mining Instructor"));
            return;
        }

        if (Rs2Inventory.contains("Bronze dagger")) {
            ensureMiningCombatGatePassed();
            return;
        }

        if (Rs2Inventory.contains("Bronze bar") && Rs2Inventory.contains("Hammer")) {
            smithBronzeDagger();
            return;
        }

        if (Rs2Inventory.contains("Bronze bar")) {
            walkAndTalk(npc(NpcID.MINING_INSTRUCTOR, "Mining Instructor"));
            return;
        }

        if (Rs2Inventory.contains("Copper ore") && Rs2Inventory.contains("Tin ore")) {
            smeltBronzeBar();
            return;
        }

        if (Rs2Inventory.contains("Bronze pickaxe")) {
            mineMissingTutorialOre();
            return;
        }

        walkAndTalk(npc(NpcID.MINING_INSTRUCTOR, "Mining Instructor"));
    }

    private void combatGuide()
    {
        Rs2NpcModel instructor = npc(NpcID.COMBAT_INSTRUCTOR, "Combat Instructor");
        int progress = getProgress();
        WorldPoint playerLocation = Rs2Player.getWorldLocation();

        if (progress <= 370) {
            if (isInArea(MINING_SMITHING_AREA, playerLocation) && !ensureMiningCombatGatePassed()) {
                return;
            }

            if (isInRatPit(playerLocation) && !leaveRatPenThroughGate("Combat Instructor interaction")) {
                return;
            }

            walkAndTalk(instructor);
            return;
        }

        if (progress <= 410) {
            if (isInArea(MINING_SMITHING_AREA, playerLocation) && !ensureMiningCombatGatePassed()) {
                return;
            }

            if (isInRatPit(playerLocation) && !leaveRatPenThroughGate("equipment lesson")) {
                return;
            }

            handleEquipmentLesson(instructor);
            return;
        }

        if (progress == 420) {
            if (isInRatPit(playerLocation) && !leaveRatPenThroughGate("equip sword and shield")) {
                return;
            }

            ensureMeleeEquipmentEquipped();
            sleepUntil(() -> getProgress() >= 430, 1800);
            return;
        }

        if (progress == 430) {
            if (isInRatPit(playerLocation) && !leaveRatPenThroughGate("combat options tab")) {
                return;
            }

            clickTab("Combat Options");
            sleepUntil(() -> getProgress() >= 440, 1200);
            return;
        }

        if (progress >= 440 && progress < 470) {
            doMeleeRatStep();
            return;
        }

        if (progress == 470) {
            if (isInRatPit(playerLocation)) {
                leaveRatPenThroughGate("melee rat complete");
                return;
            }

            walkAndTalk(instructor);
            return;
        }

        if (progress == 480 || progress == 490) {
            doRangedRatStep();
            return;
        }

        if (progress == 500) {
            if (isInRatPit(playerLocation) && !leaveRatPenThroughGate("ladder exit")) {
                return;
            }

            climbTutorialLadder();
        }
    }

    private void bankerGuide()
    {
        Rs2NpcModel guide = npc(NpcID.ACCOUNT_GUIDE, "Account Guide");
        int progress = getProgress();

        if (progress == 510) {
            openObject(ObjectID.BANK_BOOTH_10083, null, () -> getProgress() != 510);
        } else if (progress == 520) {
            handleBankSpaceAndPollBooth();
        } else if (progress == 525 || progress == 530) {
            closePollOrOptionsWidget();
            walkAndTalk(guide);
        } else if (progress == 531) {
            clickTab("Account Management");
        } else if (progress == 532) {
            walkAndTalk(guide);
        }
    }

    private void prayerGuide()
    {
        Rs2NpcModel guide = npc(NpcID.BROTHER_BRACE, "Brother Brace");
        int progress = getProgress();

        if (progress == 540 || progress == 550) {
            walkAndTalk(guide);
        } else if (progress == 560) {
            clickTab("Prayer");
        } else if (progress == 570) {
            walkAndTalk(guide);
        } else if (progress == 580) {
            clickTab("Friends list");
        } else if (progress == 600) {
            walkAndTalk(guide);
        }
    }

    private void mageGuide()
    {
        Rs2NpcModel instructor = npc(NpcID.MAGIC_INSTRUCTOR, "Magic Instructor");
        int progress = getProgress();

        if (progress == 610 || progress == 620) {
            walkAndTalk(instructor);
        } else if (progress == 630) {
            clickTab("Magic");
        } else if (progress == 640) {
            walkAndTalk(instructor);
        } else if (progress == 650) {
            widgetCast();
        } else if (progress >= 660) {
            handleFinalMageStep(instructor);
        }
    }

    private void handleFinalMageStep(Rs2NpcModel instructor)
    {
        if (hasSelectAnOption()) {
            if (Rs2Dialogue.keyPressForDialogueOption("Yes, I'd like to go to the mainland")) return;
            if (Rs2Dialogue.keyPressForDialogueOption("Yes, send me to the mainland")) return;
            if (Rs2Dialogue.keyPressForDialogueOption("Yes")) return;
            Rs2Dialogue.keyPressForDialogueOption(1);
            return;
        }

        if (isInDialogue()) {
            Rs2Dialogue.clickContinue();
            return;
        }

        if (isInTutorialIslandBounds(Rs2Player.getWorldLocation())) {
            walkAndTalk(instructor);
        }
    }

    private void handleEquipmentLesson(Rs2NpcModel instructor)
    {
        clickTab("Worn Equipment");
        Rs2Widget.clickWidget(387, 1);
        sleepUntil(() -> Rs2Widget.getWidget(84, 1) != null, 2500);
        waitForWidgetAction();

        Rs2Widget.clickWidget("Bronze dagger");
        Rs2Random.waitEx(1200, 300);

        closeEquipmentStats();
        walkAndTalk(instructor);
    }

    private void doMeleeRatStep()
    {
        if (Rs2Player.getInteracting() != null || Rs2Player.isAnimating()) {
            return;
        }

        if (!ensureMeleeEquipmentEquipped()) {
            walkAndTalk(npc(NpcID.COMBAT_INSTRUCTOR, "Combat Instructor"));
            return;
        }

        if (!isInRatPit(Rs2Player.getWorldLocation())) {
            enterRatPenThroughGate();
            return;
        }

        attackNearestRat();
    }

    private void doRangedRatStep()
    {
        Actor interacting = Rs2Player.getInteracting();
        if (interacting != null && interacting.getName() != null && interacting.getName().equalsIgnoreCase("giant rat")) {
            return;
        }

        if (Rs2Inventory.hasItem("Shortbow")) {
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            Rs2Random.waitEx(300, 75);
            Rs2Inventory.wield("Shortbow");
            sleepUntil(() -> Rs2Equipment.isWearing("Shortbow"), 2000);
        }

        if (Rs2Inventory.hasItem("Bronze arrow")) {
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            Rs2Random.waitEx(300, 75);
            Rs2Inventory.wield("Bronze arrow");
            Rs2Random.waitEx(600, 100);
        }

        selectLongrangeCombatStyle();

        /*
         * Ranged tutorial step:
         * After receiving the shortbow and bronze arrows, do NOT enter the rat pen again.
         * The player should stay outside the fence/gate and attack a giant rat with ranged.
         */
        if (isInRatPit(Rs2Player.getWorldLocation())) {
            leaveRatPenThroughGate("ranged rat step");
            return;
        }

        attackNearestRatFromOutsidePen();
    }

    private boolean attackNearestRatFromOutsidePen()
    {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return false;
        }

        if (isInRatPit(playerLocation)) {
            debug("Skipping ranged rat attack because player is still inside the rat pen | player={} progress={}",
                    playerLocation,
                    getProgress());
            return false;
        }

        Rs2NpcModel rat = Microbot.getRs2NpcCache()
                .query()
                .withName("Giant rat")
                .nearest();

        if (rat == null || rat.getWorldLocation() == null || !isInRatPit(rat.getWorldLocation())) {
            debug("No tutorial rat found inside pen for ranged attack | player={} rat={} progress={}",
                    playerLocation,
                    rat == null ? null : rat.getWorldLocation(),
                    getProgress());
            return false;
        }

        /*
         * Do not use nearestReachable() here.
         * From outside the pen, path/reachability checks can fail because the fence blocks walking,
         * while ranged combat can still target the rat.
         */
        boolean clicked = rat.click("Attack");

        debug("ranged rat attack | clicked={} player={} rat={} progress={}",
                clicked,
                playerLocation,
                rat.getWorldLocation(),
                getProgress());

        if (clicked) {
            sleepUntil(() ->
                            Rs2Player.getInteracting() != null
                                    || Rs2Player.isAnimating()
                                    || getProgress() >= 500,
                    2500);
        }

        return clicked;
    }

    private boolean ensureMeleeEquipmentEquipped()
    {
        boolean wearingSword = Rs2Equipment.isWearing("Bronze sword");
        boolean wearingShield = Rs2Equipment.isWearing("Wooden shield");

        if (wearingSword && wearingShield) {
            return true;
        }

        if (!wearingSword && !Rs2Inventory.hasItem("Bronze sword")) {
            return false;
        }

        if (!wearingShield && !Rs2Inventory.hasItem("Wooden shield")) {
            return false;
        }

        Rs2Tab.switchTo(InterfaceTab.INVENTORY);
        Rs2Random.waitEx(450, 100);

        if (!wearingSword) {
            Rs2Inventory.wield("Bronze sword");
            sleepUntil(() -> Rs2Equipment.isWearing("Bronze sword"), 2000);
        }

        if (!wearingShield) {
            Rs2Inventory.wield("Wooden shield");
            sleepUntil(() -> Rs2Equipment.isWearing("Wooden shield"), 2000);
        }

        return Rs2Equipment.isWearing("Bronze sword") && Rs2Equipment.isWearing("Wooden shield");
    }

    private void smithBronzeDagger()
    {
        if (Rs2Inventory.contains("Bronze dagger")) {
            return;
        }

        if (!Rs2Inventory.contains("Bronze bar") || !Rs2Inventory.contains("Hammer")) {
            debug("Cannot smith bronze dagger yet | progress={} bar={} hammer={} player={}",
                    getProgress(),
                    Rs2Inventory.contains("Bronze bar"),
                    Rs2Inventory.contains("Hammer"),
                    Rs2Player.getWorldLocation());
            return;
        }

        if (!Rs2Widget.isSmithingWidgetOpen()) {
            debug("Opening tutorial anvil | progress={} player={}", getProgress(), Rs2Player.getWorldLocation());
            boolean clicked = Microbot.getRs2TileObjectCache().query().withName("Anvil").interact("Smith");
            if (!clicked) {
                clicked = Microbot.getRs2TileObjectCache().query().withName("Anvil").interact("Use");
            }

            debug("Tutorial anvil click | clicked={} progress={} player={}",
                    clicked,
                    getProgress(),
                    Rs2Player.getWorldLocation());

            if (clicked) {
                sleepUntil(Rs2Widget::isSmithingWidgetOpen, 1200);
            }
        }

        if (!Rs2Widget.isSmithingWidgetOpen()) {
            debug("Smithing widget did not open | progress={} player={} bar={} hammer={}",
                    getProgress(),
                    Rs2Player.getWorldLocation(),
                    Rs2Inventory.contains("Bronze bar"),
                    Rs2Inventory.contains("Hammer"));
            return;
        }

        debug("Selecting bronze dagger in smithing widget | progress={} player={}", getProgress(), Rs2Player.getWorldLocation());
        Rs2Widget.clickWidget(312, 9);
        sleepUntil(() -> Rs2Inventory.contains("Bronze dagger"), 3500);
    }

    private void smeltBronzeBar()
    {
        List<Integer> ores = Arrays.asList(ItemID.TIN_ORE, ItemID.COPPER_ORE);
        Collections.shuffle(ores);
        Rs2Inventory.useItemOnObject(ores.get(0), ObjectID.FURNACE_10082);
        sleepUntil(() -> Rs2Inventory.contains("Bronze bar"), 3500);
    }

    private void mineMissingTutorialOre()
    {
        int rockId = Rs2Inventory.contains("Copper ore") ? ObjectID.TIN_ROCKS : ObjectID.COPPER_ROCKS;
        boolean clicked = Microbot.getRs2TileObjectCache().query().interact(rockId, "Mine");

        if (!clicked) {
            return;
        }

        sleepUntil(() -> rockId == ObjectID.COPPER_ROCKS
                        ? Rs2Inventory.contains("Copper ore")
                        : Rs2Inventory.contains("Tin ore"),
                3500);
    }

    private boolean enterRatPenThroughGate()
    {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (isInRatPit(playerLocation)) {
            return true;
        }

        boolean opened = openRatGate(() -> isInRatPit(Rs2Player.getWorldLocation()));
        boolean inside = isInRatPit(Rs2Player.getWorldLocation());
        debug("rat gate enter | opened={} inside={} atGateEdge={} player={} progress={}",
                opened,
                inside,
                isAtRatGateEdge(Rs2Player.getWorldLocation()),
                Rs2Player.getWorldLocation(),
                getProgress());

        return inside;
    }

    private boolean leaveRatPenThroughGate(String reason)
    {
        if (!isInRatPit(Rs2Player.getWorldLocation())) {
            return true;
        }

        boolean opened = openRatGate(() -> !isInRatPit(Rs2Player.getWorldLocation()));
        debug("rat gate leave | reason={} opened={} inside={} atGateEdge={} player={} progress={}",
                reason,
                opened,
                isInRatPit(Rs2Player.getWorldLocation()),
                isAtRatGateEdge(Rs2Player.getWorldLocation()),
                Rs2Player.getWorldLocation(),
                getProgress());
        return !isInRatPit(Rs2Player.getWorldLocation());
    }

    private boolean openRatGate(Condition done)
    {
        if (done != null && done.matches()) {
            return true;
        }

        return ensureTransition("rat pen gate", ObjectID.GATE_9719, "Open", done);
    }

    private boolean attackNearestRat()
    {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!isInRatPit(playerLocation)) {
            debug("Skipping rat attack because player is not inside the rat pen | player={} progress={}",
                    playerLocation,
                    getProgress());
            return false;
        }

        Rs2NpcModel rat = Microbot.getRs2NpcCache().query().withName("Giant rat").nearestReachable();
        if (rat == null || !isInRatPit(rat.getWorldLocation()) || !isNpcReachable(rat, 8)) {
            debug("No reachable tutorial rat inside pen to attack | player={} rat={} progress={}",
                    playerLocation,
                    rat == null ? null : rat.getWorldLocation(),
                    getProgress());
            return false;
        }

        return rat.click("Attack");
    }

    private void climbTutorialLadder()
    {
        debug("climbing combat ladder | progress={} player={} inRatPit={} atGateEdge={}",
                getProgress(),
                Rs2Player.getWorldLocation(),
                isInRatPit(Rs2Player.getWorldLocation()),
                isAtRatGateEdge(Rs2Player.getWorldLocation()));

        boolean clicked = Microbot.getRs2TileObjectCache().query().withName("Ladder").interact("Climb-up");
        if (!clicked) {
            clicked = Microbot.getRs2TileObjectCache().query().interact(9726, "Climb-up");
        }

        debug("combat ladder click | clicked={} progress={} player={}",
                clicked,
                getProgress(),
                Rs2Player.getWorldLocation());

        if (clicked) {
            sleepUntil(() -> getProgress() != 500 || !isTutorialUndergroundPoint(Rs2Player.getWorldLocation()), 4000);
        }
    }

    private boolean selectLongrangeCombatStyle()
    {
        clickTab("Combat Options");

        Widget longrange = Rs2Widget.findWidget("Longrange", true);
        if (longrange == null) {
            longrange = Rs2Widget.findWidget("Long range", true);
        }

        if (longrange == null) {
            return false;
        }

        Rs2Widget.clickWidget(longrange);
        Rs2Random.waitEx(600, 100);
        return true;
    }

    private boolean widgetCast()
    {
        if (Rs2Player.isAnimating() || Rs2Player.getInteracting() != null) {
            return true;
        }

        Widget windStrike = Rs2Widget.findWidget("Wind Strike", null, true);
        if (windStrike == null) {
            windStrike = Rs2Widget.getWidget(218, 11);
        }
        if (windStrike == null) {
            return false;
        }

        if (isHidden(windStrike)) {
            clickTab("Magic");
            windStrike = Rs2Widget.getWidget(218, 8);
            if (windStrike == null) {
                windStrike = Rs2Widget.findWidget("Wind Strike", null, true);
            }
            if (windStrike == null || isHidden(windStrike)) {
                return false;
            }
        }

        Rs2Widget.clickWidget(windStrike);
        Rs2Random.waitEx(150, 50);

        Rs2NpcModel chicken = Microbot.getRs2NpcCache().query().withName("chicken").nearestReachable();
        if (chicken == null || !isNpcReachable(chicken, 10)) {
            debug("No reachable tutorial chicken for Wind Strike | player={} progress={}", Rs2Player.getWorldLocation(), getProgress());
            return false;
        }

        if (!chicken.click("Cast")) {
            chicken.click("Cast");
        }

        sleepUntil(() -> Rs2Player.isAnimating() || getProgress() != 650, 2000);
        return true;
    }

    private void handleBankSpaceAndPollBooth()
    {
        if (clickWidgetIfVisible(928, 4)) {
            return;
        }

        if (Rs2Widget.isWidgetVisible(289, 5)) {
            Widget widgetOptions = Rs2Widget.getWidget(289, 4);

            if (widgetOptions != null && widgetOptions.getDynamicChildren() != null) {
                for (Widget dynamicWidgetOption : widgetOptions.getDynamicChildren()) {
                    String widgetText = dynamicWidgetOption.getText();

                    if (widgetText != null && widgetText.equalsIgnoreCase("Want more bank space?")) {
                        Rs2Widget.clickWidget(289, 7);
                        waitForWidgetAction();
                        break;
                    }
                }
            }
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 2500);
        openObject(26815, null, () -> getProgress() != 520 || Rs2Widget.isWidgetVisible(928, 4));
    }

    private void closePollOrOptionsWidget()
    {
        if (clickWidgetIfVisible(928, 4)) {
            return;
        }

        clickFirstChildWithAction(310, 2, "close");
    }

    private boolean talkToSurvivalExpert()
    {
        return walkAndTalk(npc(NpcID.SURVIVAL_EXPERT, "Survival Expert"), 4);
    }

    private boolean ensureStartDoorPassed()
    {
        if (!isInStartArea()) {
            return true;
        }

        return ensureTransition("start-area door", ObjectID.DOOR_9398, "Open", () -> !isInStartArea());
    }

    private boolean ensureSurvivalCookingGatePassed()
    {
        if (getProgress() != 120 || isInArea(COOKING_AREA)) {
            return true;
        }

        return ensureTransition(
                "survival-to-cooking gate",
                ObjectID.GATE_9470,
                "Open",
                () -> getProgress() != 120 || isInArea(COOKING_AREA) || !isInArea(SURVIVAL_AREA));
    }

    private boolean ensureKitchenAccess(Rs2NpcModel chef)
    {
        if (isInArea(COOKING_AREA) || isNpcReachable(chef, 2)) {
            return true;
        }

        return ensureTransition(
                "kitchen door",
                ObjectID.DOOR_9709,
                "Open",
                () -> isInArea(COOKING_AREA) || isNpcReachable(chef, 2));
    }

    private boolean ensureKitchenExitPassed()
    {
        if (!isInArea(COOKING_AREA)) {
            return true;
        }

        return ensureTransition(
                "kitchen exit door",
                9710,
                "Open",
                () -> !isInArea(COOKING_AREA) || getProgress() >= 200);
    }

    private boolean ensureQuestGuideAccess(Rs2NpcModel guide)
    {
        if (isInArea(QUEST_GUIDE_AREA) || isNpcReachable(guide, 2)) {
            return true;
        }

        return ensureTransition(
                "quest guide door",
                9716,
                "Open",
                () -> isInArea(QUEST_GUIDE_AREA) || isNpcReachable(guide, 2));
    }

    private boolean ensureQuestCaveLadderPassed()
    {
        if (isTutorialUndergroundPoint(Rs2Player.getWorldLocation())) {
            return true;
        }

        return ensureTransition(
                "quest cave ladder",
                9726,
                "Climb-down",
                () -> isTutorialUndergroundPoint(Rs2Player.getWorldLocation()));
    }

    private boolean ensureMiningCombatGatePassed()
    {
        if (!isInArea(MINING_SMITHING_AREA)) {
            return true;
        }

        return ensureTransition(
                "mining-to-combat gate",
                ObjectID.GATE_9718,
                "Open",
                () -> !isInArea(MINING_SMITHING_AREA) || isInArea(COMBAT_INSTRUCTOR_AREA) || getProgress() > 360);
    }

    private boolean ensureTransition(String label, int objectId, String action, Condition done)
    {
        if (done != null && done.matches()) {
            return true;
        }

        boolean clicked = interactTransitionObject(objectId, action);
        if (clicked && done != null) {
            sleepUntil(done::matches, 1800);
        }

        boolean complete = done == null ? clicked : done.matches();
        if (!complete) {
            debug("transition not complete | label={} objectId={} action={} clicked={} player={} progress={}",
                    label,
                    objectId,
                    action,
                    clicked,
                    Rs2Player.getWorldLocation(),
                    getProgress());
            Rs2Random.waitEx(250, 75);
        }

        return complete;
    }

    private boolean interactTransitionObject(int objectId, String action)
    {
        if (action == null) {
            return Microbot.getRs2TileObjectCache().query().interact(objectId);
        }

        boolean clicked = Microbot.getRs2TileObjectCache().query().interact(objectId, action);
        if (!clicked) {
            clicked = Microbot.getRs2TileObjectCache().query().withId(objectId).interact(action);
        }

        return clicked;
    }

    private boolean walkAndTalk(Rs2NpcModel npc)
    {
        return walkAndTalk(npc, 2);
    }

    private boolean walkAndTalk(Rs2NpcModel npc, int reach)
    {
        return walkAndAct(npc, reach, "Talk-to", () -> sleepUntil(Rs2Dialogue::isInDialogue, 5000));
    }

    private boolean walkAndAct(Rs2NpcModel npc, int reach, String action, Runnable afterClick)
    {
        if (npc == null || npc.getWorldLocation() == null || Rs2Player.getWorldLocation() == null) {
            return false;
        }

        if (!isNpcReachable(npc, reach)) {
            debug("Tutorial NPC is not in interaction range yet; walking closer | action={} npc={} npcLoc={} player={} distance={} reach={} progress={}",
                    action,
                    npc.getName(),
                    npc.getWorldLocation(),
                    Rs2Player.getWorldLocation(),
                    getNpcDistance(npc),
                    reach,
                    getProgress());
            walkCloserToNpc(npc, reach);
            return false;
        }

        boolean clicked = npc.click(action);
        if (!clicked) {
            return false;
        }

        if (afterClick != null) {
            afterClick.run();
        }

        return true;
    }

    private boolean walkCloserToNpc(Rs2NpcModel npc, int reach)
    {
        if (npc == null || npc.getWorldLocation() == null || Rs2Player.getWorldLocation() == null) {
            return false;
        }

        if (Rs2Player.isMoving()) {
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - lastNpcWalkAttemptAtMs < 1800) {
            return false;
        }
        lastNpcWalkAttemptAtMs = now;

        int walkDistance = Math.max(1, reach);
        WorldPoint destination = npc.getWorldLocation();
        boolean walking = Rs2Walker.walkTo(destination, walkDistance);

        debug("walk closer to tutorial NPC | npc={} destination={} walkDistance={} result={} player={} progress={}",
                safeNpcName(npc),
                destination,
                walkDistance,
                walking,
                Rs2Player.getWorldLocation(),
                getProgress());

        if (walking) {
            sleepUntil(() -> Rs2Player.isMoving() || isNpcReachable(npc, reach), 1200);
        }

        return walking;
    }

    private Rs2NpcModel npc(int id, String name)
    {
        Rs2NpcModel npc = Microbot.getRs2NpcCache().query().withId(id).nearestReachable();
        if (npc != null) {
            return npc;
        }

        npc = Microbot.getRs2NpcCache().query().withName(name).nearestReachable();
        if (npc != null) {
            return npc;
        }

        Rs2NpcModel nearest = Microbot.getRs2NpcCache().query().withId(id).nearest();
        if (nearest == null) {
            nearest = Microbot.getRs2NpcCache().query().withName(name).nearest();
        }

        if (nearest != null) {
            debug("Nearest tutorial NPC is not reachable | id={} name={} npcLoc={} player={} distance={} progress={}",
                    id,
                    name,
                    nearest.getWorldLocation(),
                    Rs2Player.getWorldLocation(),
                    getNpcDistance(nearest),
                    getProgress());
        }

        return null;
    }

    private boolean isNpcReachable(Rs2NpcModel npc, int reach)
    {
        if (npc == null || Microbot.getClientThread() == null) {
            return false;
        }

        AtomicBoolean reachable = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Microbot.getClientThread().invoke(() ->
            {
                try {
                    reachable.set(isNpcReachableOnClientThread(npc, reach));
                } catch (Exception ex) {
                    debug("NPC reachability check failed on client thread | npc={} reach={} error={}",
                            safeNpcName(npc),
                            reach,
                            ex.getMessage());
                    reachable.set(false);
                } finally {
                    latch.countDown();
                }
            });

            if (!latch.await(750, TimeUnit.MILLISECONDS)) {
                debug("NPC reachability check timed out | npc={} reach={} progress={}",
                        safeNpcName(npc),
                        reach,
                        getProgress());
                return false;
            }

            return reachable.get();
        } catch (Exception ex) {
            debug("NPC reachability check failed safely | npc={} reach={} error={}",
                    safeNpcName(npc),
                    reach,
                    ex.getMessage());
            return false;
        }
    }

    private boolean isNpcReachableOnClientThread(Rs2NpcModel npc, int reach)
    {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        WorldPoint npcLocation = npc == null ? null : npc.getWorldLocation();
        if (playerLocation == null || npcLocation == null || playerLocation.getPlane() != npcLocation.getPlane()) {
            return false;
        }

        WorldArea playerArea = new WorldArea(playerLocation, 1, 1);
        WorldArea npcArea = npc.getWorldArea() == null ? new WorldArea(npcLocation, 1, 1) : npc.getWorldArea();
        int distance = playerArea.distanceTo(npcArea);
        if (distance > Math.max(1, reach)) {
            return false;
        }

        if (Microbot.getClient() == null) {
            return false;
        }

        WorldView worldView = Microbot.getClient().findWorldViewFromWorldPoint(playerLocation);
        if (worldView == null) {
            worldView = Microbot.getClient().getTopLevelWorldView();
        }

        if (worldView != null && !playerArea.hasLineOfSightTo(worldView, npcArea)) {
            return false;
        }

        return hasReachableInteractionTile(npcArea, playerLocation);
    }

    private boolean hasReachableInteractionTile(WorldArea npcArea, WorldPoint playerLocation)
    {
        if (npcArea == null || playerLocation == null) {
            return false;
        }

        if (npcArea.distanceTo(playerLocation) == 0) {
            return true;
        }

        for (WorldPoint npcTile : npcArea.toWorldPointList()) {
            if (isReachableTile(npcTile)) {
                return true;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    WorldPoint adjacentTile = new WorldPoint(
                            npcTile.getX() + dx,
                            npcTile.getY() + dy,
                            npcTile.getPlane());
                    if (isReachableTile(adjacentTile)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isReachableTile(WorldPoint tile)
    {
        try {
            return tile != null && Rs2Tile.isTileReachable(tile);
        } catch (Exception ignored) {
            return false;
        }
    }

    private int getNpcDistance(Rs2NpcModel npc)
    {
        if (npc == null || Microbot.getClientThread() == null) {
            return Integer.MAX_VALUE;
        }

        AtomicInteger distance = new AtomicInteger(Integer.MAX_VALUE);
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Microbot.getClientThread().invoke(() ->
            {
                try {
                    WorldPoint playerLocation = Rs2Player.getWorldLocation();
                    WorldPoint npcLocation = npc.getWorldLocation();
                    if (playerLocation == null || npcLocation == null || playerLocation.getPlane() != npcLocation.getPlane()) {
                        distance.set(Integer.MAX_VALUE);
                        return;
                    }

                    WorldArea npcArea = npc.getWorldArea() == null ? new WorldArea(npcLocation, 1, 1) : npc.getWorldArea();
                    distance.set(npcArea.distanceTo(playerLocation));
                } catch (Exception ignored) {
                    distance.set(Integer.MAX_VALUE);
                } finally {
                    latch.countDown();
                }
            });

            if (!latch.await(750, TimeUnit.MILLISECONDS)) {
                return Integer.MAX_VALUE;
            }

            return distance.get();
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private String safeNpcName(Rs2NpcModel npc)
    {
        try {
            return npc == null ? "null" : npc.getName();
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private boolean openObject(int objectId, String action, Condition done)
    {
        return ensureTransition("object", objectId, action, done);
    }

    private void fishShrimp()
    {
        boolean clicked = Microbot.getRs2NpcCache().query().withId(NpcID.FISHING_SPOT_3317).interact("Net");
        if (clicked) {
            sleepUntil(this::hasRawShrimp, 5000);
        }
    }

    private boolean cutOneTutorialLog()
    {
        if (hasTutorialLog()) {
            survivalLogCut = true;
            return true;
        }

        debug("Cutting tutorial log | progress={} player={}", getProgress(), Rs2Player.getWorldLocation());
        boolean clicked = Microbot.getRs2TileObjectCache().query().withName("Tree").interact("Chop down");
        if (!clicked) {
            clicked = Microbot.getRs2TileObjectCache().query().withName("Tree").interact("Chop");
        }
        if (!clicked) {
            debug("Failed to click tutorial tree | progress={} player={}", getProgress(), Rs2Player.getWorldLocation());
            return false;
        }

        sleepUntil(this::hasTutorialLog, 5000);
        survivalLogCut = hasTutorialLog();
        return survivalLogCut;
    }

    private boolean lightOneTutorialFire()
    {
        if (!hasTutorialLog()) {
            survivalFireLit = getOwnFire() != null;
            return survivalFireLit;
        }

        WorldPoint expectedFireLocation = Rs2Player.getWorldLocation();
        debug("Lighting tutorial fire | expectedFireLocation={} progress={} player={}",
                expectedFireLocation,
                getProgress(),
                Rs2Player.getWorldLocation());

        boolean clicked = Rs2Inventory.combine(ItemID.TINDERBOX, ItemID.LOGS_2511);
        if (!clicked) {
            clicked = Rs2Inventory.combine(ItemID.TINDERBOX, ItemID.LOGS);
        }

        if (!clicked) {
            debug("Failed to combine tinderbox with tutorial logs | progress={} player={}",
                    getProgress(),
                    Rs2Player.getWorldLocation());
            return false;
        }

        ownFireLocation = expectedFireLocation;
        sleepUntil(() -> getFireAt(expectedFireLocation) != null, 3500);

        survivalFireLit = getFireAt(expectedFireLocation) != null;
        if (!survivalFireLit && !hasTutorialLog()) {
            debug("Log was consumed but own fire was not found at expected location | expectedFireLocation={} player={} progress={}",
                    expectedFireLocation,
                    Rs2Player.getWorldLocation(),
                    getProgress());
        }

        return survivalFireLit;
    }

    private void cookShrimpOnOwnFire(TileObject ownFire)
    {
        if (!Rs2Inventory.interact(ItemID.RAW_SHRIMPS_2514, "Use")
                && !Rs2Inventory.interact(ItemID.RAW_SHRIMPS, "Use")) {
            return;
        }

        Rs2Random.waitEx(90, 30);
        Rs2GameObject.interact(ownFire, "Use");
        sleepUntil(() -> !hasRawShrimp() || getProgress() > 90, 3500);
    }

    private TileObject getOwnFire()
    {
        if (ownFireLocation == null) {
            return null;
        }

        return getFireAt(ownFireLocation);
    }

    private TileObject getFireAt(WorldPoint location)
    {
        if (location == null) {
            return null;
        }

        TileObject tileObject = Rs2GameObject.findGameObjectByLocation(location);
        return tileObject != null && tileObject.getId() == ObjectID.FIRE_26185 ? tileObject : null;
    }

    private boolean hasTutorialLog()
    {
        return Rs2Inventory.hasItem(ItemID.LOGS) || Rs2Inventory.hasItem(ItemID.LOGS_2511);
    }

    private boolean hasRawShrimp()
    {
        return Rs2Inventory.hasItem(ItemID.RAW_SHRIMPS) || Rs2Inventory.hasItem(ItemID.RAW_SHRIMPS_2514);
    }

    private boolean hasBreadDough()
    {
        return Rs2Inventory.contains("Bread dough") || Rs2Inventory.contains("Dough");
    }

    private void handleDisplayNameWidget()
    {
        String currentInput = getCurrentDisplayNameInput();
        String responseText = getDisplayNameResponseText();
        debugDisplayNameState(currentInput, responseText);

        if (isGeneratedNameAvailable(lastGeneratedName)) {
            clickSetDisplayNameAndWait();
            return;
        }

        if (System.currentTimeMillis() - lastNameAttemptAtMs < NAME_ATTEMPT_COOLDOWN_MS) {
            return;
        }

        lastNameAttemptAtMs = System.currentTimeMillis();
        enterGeneratedName();
    }

    private void enterGeneratedName()
    {
        String name = generateDisplayName();
        lastGeneratedName = name;

        clearDisplayNameInput();
        sleep(randomDelay(180, 420));
        focusDisplayNameInput();
        Rs2Keyboard.typeString(name);
        sleep(randomDelay(220, 520));
        Rs2Widget.clickWidget(NAME_CREATION_GROUP, LOOK_UP_NAME_BUTTON_CHILD);
        sleep(randomDelay(4200, 5600));

        if (isGeneratedNameAvailable(name)) {
            clickSetDisplayNameAndWait();
        }
    }

    private void clearDisplayNameInput()
    {
        String currentInput = getCurrentDisplayNameInput();
        if (currentInput.isEmpty()) {
            return;
        }

        focusDisplayNameInput();
        for (int i = 0; i < currentInput.length(); i++) {
            Rs2Keyboard.keyPress(KeyEvent.VK_BACK_SPACE);
            sleep(randomDelay(18, 55));
        }
    }

    private void focusDisplayNameInput()
    {
        Rs2Widget.clickWidget(NAME_CREATION_GROUP, NAME_INPUT_CHILD);
        sleep(randomDelay(240, 520));
    }

    private void clickSetDisplayNameAndWait()
    {
        Rs2Widget.clickWidget(NAME_CREATION_GROUP, SET_NAME_BUTTON_CHILD);
        sleepUntil(() -> !isDisplayNameWidgetOpen(), 5000);
    }

    private String getCurrentDisplayNameInput()
    {
        Widget nameInput = Rs2Widget.getWidget(NAME_CREATION_GROUP, NAME_INPUT_TEXT_CHILD);
        if (nameInput == null || nameInput.getText() == null) {
            return "";
        }

        return nameInput.getText().replace("*", "").trim();
    }

    private String getDisplayNameResponseText()
    {
        Widget responseWidget = Rs2Widget.getWidget(NAME_CREATION_GROUP, NAME_RESPONSE_TEXT_CHILD);
        if (responseWidget == null || responseWidget.getText() == null) {
            return "";
        }

        return Rs2UiHelper.stripColTags(responseWidget.getText()).trim();
    }

    private boolean isGeneratedNameAvailable(String name)
    {
        if (name == null || name.isEmpty() || "None".equals(name)) {
            return false;
        }

        String responseText = getDisplayNameResponseText();
        if (responseText.isEmpty()) {
            return false;
        }

        String normalizedResponse = responseText.toLowerCase(Locale.ENGLISH);
        if (normalizedResponse.contains("not available")
                || normalizedResponse.contains("unavailable")
                || normalizedResponse.contains("already taken")) {
            return false;
        }

        return normalizedResponse.contains(name.toLowerCase(Locale.ENGLISH))
                && normalizedResponse.contains("available");
    }

    private void randomizeCharacter()
    {
        lastCharacterAction = "Randomizing";
        Rs2Widget.clickWidget(CHARACTER_CREATION_GROUP,
                ThreadLocalRandom.current().nextBoolean() ? BODY_TYPE_A_CHILD : BODY_TYPE_B_CHILD);
        sleep(randomDelay(450, 900));

        for (int arrowBaseChild : CHARACTER_CREATION_ARROWS) {
            clickRandomCharacterArrow(arrowBaseChild);
            sleep(randomDelay(90, 240));
        }

        sleep(randomDelay(700, 1400));
        Rs2Widget.clickWidget(CHARACTER_CREATION_GROUP, CHARACTER_CONFIRM_BUTTON_CHILD);
        lastCharacterAction = "Confirmed";
        sleepUntil(() -> !isCharacterCreationWidgetOpen(), 5000);
    }

    private void clickRandomCharacterArrow(int arrowBaseChild)
    {
        int arrowChild = arrowBaseChild + (ThreadLocalRandom.current().nextBoolean() ? 2 : 3);
        int clickCount = ThreadLocalRandom.current().nextInt(1, 8);
        Widget arrowWidget = Rs2Widget.getWidget(CHARACTER_CREATION_GROUP, arrowChild);

        if (arrowWidget == null) {
            return;
        }

        for (int i = 0; i < clickCount; i++) {
            Rs2Widget.clickWidget(arrowWidget.getId());
            sleep(randomDelay(120, 360));
        }
    }

    private void selectRandomExperienceOption()
    {
        int optionIndex = ThreadLocalRandom.current().nextInt(EXPERIENCE_OPTION_TEXTS.length);
        String optionText = EXPERIENCE_OPTION_TEXTS[optionIndex];
        lastExperienceSelection = optionText;

        if (Rs2Dialogue.keyPressForDialogueOption(optionText)) {
            return;
        }

        Widget optionWidget = Rs2Widget.findWidget(optionText, null, false);
        if (optionWidget != null) {
            Rs2Widget.clickWidget(optionWidget);
            waitForWidgetAction();
        }
    }

    private boolean closeBlockingWidgetIfOpen()
    {
        if (clickWidgetIfVisible(929, 5)) {
            return true;
        }

        if (Rs2Widget.isWidgetVisible(310, 0)) {
            Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
            waitForWidgetAction();
            return true;
        }

        return false;
    }

    private boolean continueDialogueIfOpen()
    {
        if (hasSelectAnOption()) {
            return false;
        }

        if (!hasContinue()) {
            return false;
        }

        clickContinue();
        return true;
    }

    private boolean clickTab(String tabName)
    {
        Widget widget = Rs2Widget.findWidget(tabName, true);
        if (widget == null) {
            return false;
        }

        Rs2Widget.clickWidget(widget);
        waitForWidgetAction();
        return true;
    }

    private boolean clickTabFast(String tabName)
    {
        Widget widget = Rs2Widget.findWidget(tabName, true);
        if (widget == null) {
            return false;
        }

        Rs2Widget.clickWidget(widget);
        Rs2Random.waitEx(250, 75);
        return true;
    }

    private void closeEquipmentStats()
    {
        clickFirstChildWithAction(84, 3, "close");
    }

    private boolean clickWidgetIfVisible(int groupId, int childId)
    {
        if (!Rs2Widget.isWidgetVisible(groupId, childId)) {
            return false;
        }

        Rs2Widget.clickWidget(groupId, childId);
        waitForWidgetAction();
        return true;
    }

    private boolean clickFirstChildWithAction(int groupId, int childId, String action)
    {
        Widget widgetOptions = Rs2Widget.getWidget(groupId, childId);
        if (widgetOptions == null || widgetOptions.getDynamicChildren() == null) {
            return false;
        }

        return Arrays.stream(widgetOptions.getDynamicChildren())
                .filter(widget -> hasAction(widget, action))
                .findFirst()
                .map(widget -> {
                    Rs2Widget.clickWidget(widget);
                    waitForWidgetAction();
                    return true;
                })
                .orElse(false);
    }

    private boolean hasAction(Widget widget, String action)
    {
        String[] actionsText = widget.getActions();
        return actionsText != null && Arrays.stream(actionsText).anyMatch(action::equalsIgnoreCase);
    }

    private boolean isTutorialClientReady()
    {
        if (Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) {
            debugClientReadyWait("local-player-null");
            return false;
        }

        if (Rs2Player.getWorldLocation() == null) {
            debugClientReadyWait("world-location-null");
            return false;
        }

        return true;
    }

    private void debugClientReadyWait(String reason)
    {
        long now = System.currentTimeMillis();
        if (now - lastClientReadyDebugAtMs < 3000) {
            return;
        }

        lastClientReadyDebugAtMs = now;
        debug("waiting for client | reason={} progress={} status={}", reason, getProgress(), status);
    }

    private void debugLoopStatus()
    {
        if (!debugLogging) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastLoopDebugAtMs < 2000) {
            return;
        }

        lastLoopDebugAtMs = now;
        debug("loop | status={} progress={} quest={} questHint={} player={} inventoryTab={} dialogue={}",
                status,
                getProgress(),
                lastLearningTheRopesState == null ? "Unknown" : lastLearningTheRopesState,
                lastLearningTheRopesHint,
                Rs2Player.getWorldLocation(),
                Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY,
                isInDialogue());
    }

    private void debugMiningGuideState()
    {
        if (!debugLogging) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastMiningDebugAtMs < 1500) {
            return;
        }

        lastMiningDebugAtMs = now;
        debug("mining guide | progress={} bar={} hammer={} dagger={} pickaxe={} copper={} tin={} smithWidget={} player={}",
                getProgress(),
                Rs2Inventory.contains("Bronze bar"),
                Rs2Inventory.contains("Hammer"),
                Rs2Inventory.contains("Bronze dagger"),
                Rs2Inventory.contains("Bronze pickaxe"),
                Rs2Inventory.contains("Copper ore"),
                Rs2Inventory.contains("Tin ore"),
                Rs2Widget.isSmithingWidgetOpen(),
                Rs2Player.getWorldLocation());
    }

    private void debugDisplayNameState(String currentInput, String responseText)
    {
        if (!debugLogging) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastNameDebugAtMs < 2000) {
            return;
        }

        lastNameDebugAtMs = now;
        debug("display name | input={} lastGenerated={} response={} player={} progress={}",
                currentInput,
                lastGeneratedName,
                responseText,
                Rs2Player.getWorldLocation(),
                getProgress());
    }

    private boolean isHidden(Widget widget)
    {
        try {
            return Rs2Widget.isHidden(widget.getId());
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean isExperiencePromptOpen()
    {
        return isInDialogue()
                && (Rs2Widget.hasWidget(EXPERIENCE_PROMPT_TITLE)
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[0])
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[1])
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[2]));
    }

    private boolean isDisplayNameWidgetOpen()
    {
        return isDisplayNameWidgetOpenStatic();
    }

    private boolean isCharacterCreationWidgetOpen()
    {
        return isCharacterCreationWidgetOpenStatic();
    }

    private static boolean isDisplayNameWidgetOpenStatic()
    {
        return Rs2Widget.isWidgetVisible(NAME_CREATION_GROUP, NAME_CREATION_CONTAINER_CHILD)
                || (Rs2Widget.hasWidget(DISPLAY_NAME_TITLE) && Rs2Widget.hasWidget(LOOK_UP_NAME_BUTTON));
    }

    private static boolean isCharacterCreationWidgetOpenStatic()
    {
        return Rs2Widget.isWidgetVisible(CHARACTER_CREATION_GROUP, CHARACTER_CREATION_CONTAINER_CHILD)
                || Rs2Widget.hasWidget(CHARACTER_CREATOR_TITLE);
    }

    private boolean isInStartArea()
    {
        return isStartAreaPoint(Rs2Player.getWorldLocation());
    }

    private static boolean isStartAreaPoint(WorldPoint location)
    {
        return location != null
                && location.getPlane() == 0
                && location.getX() >= INTRO_MIN_X
                && location.getX() < INTRO_MAX_X_EXCLUSIVE
                && location.getY() >= INTRO_MIN_Y
                && location.getY() < INTRO_MAX_Y_EXCLUSIVE;
    }

    private boolean isInRatPit(WorldPoint location)
    {
        return location != null
                && RAT_PIT_AREA.contains(location)
                && !COMBAT_INSTRUCTOR_AREA.contains(location)
                && !RAT_GATE_EDGE_AREA.contains(location);
    }

    private boolean isInArea(WorldArea area)
    {
        return isInArea(area, Rs2Player.getWorldLocation());
    }

    private boolean isInArea(WorldArea area, WorldPoint location)
    {
        return area != null && location != null && area.contains(location);
    }

    private boolean isAtRatGateEdge(WorldPoint location)
    {
        return location != null && RAT_GATE_EDGE_AREA.contains(location);
    }

    private boolean isTutorialUndergroundPoint(WorldPoint location)
    {
        return location != null && location.getY() > 9000;
    }

    private static boolean isInTutorialIslandBounds(WorldPoint location)
    {
        return isWithinInclusiveArea(
                location,
                TUTORIAL_CAVE_MIN_X,
                TUTORIAL_CAVE_MAX_X,
                TUTORIAL_CAVE_MIN_Y,
                TUTORIAL_CAVE_MAX_Y)
                || isWithinInclusiveArea(
                location,
                TUTORIAL_OVERWORLD_MIN_X,
                TUTORIAL_OVERWORLD_MAX_X,
                TUTORIAL_OVERWORLD_MIN_Y,
                TUTORIAL_OVERWORLD_MAX_Y);
    }

    private static boolean isWithinInclusiveArea(WorldPoint location, int minX, int maxX, int minY, int maxY)
    {
        return location != null
                && location.getPlane() == 0
                && location.getX() >= minX
                && location.getX() <= maxX
                && location.getY() >= minY
                && location.getY() <= maxY;
    }

    private int getProgress()
    {
        try {
            return Microbot.getVarbitPlayerValue(281);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String generateDisplayName()
    {
        String name = NAME_PREFIXES[randomIndex(NAME_PREFIXES.length)]
                + NAME_SUFFIXES[randomIndex(NAME_SUFFIXES.length)];

        return name.length() <= 12 ? name : name.substring(0, 12);
    }

    private int randomDelay(int minInclusive, int maxInclusive)
    {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private int randomIndex(int upperExclusive)
    {
        return ThreadLocalRandom.current().nextInt(upperExclusive);
    }

    private void waitForWidgetAction()
    {
        Rs2Random.waitEx(1200, 300);
    }

    private void configureAntiban()
    {
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.simulateMistakes = true;
    }

    private void resetRuntimeState()
    {
        status = Status.NAME;
        toggledSettings = false;
        survivalLogCut = false;
        survivalFireLit = false;
        lastNameAttemptAtMs = 0L;
        lastNameDebugAtMs = 0L;
        lastClientReadyDebugAtMs = 0L;
        lastLoopDebugAtMs = 0L;
        lastMiningDebugAtMs = 0L;
        lastQuestSyncDebugAtMs = 0L;
        lastNpcWalkAttemptAtMs = 0L;
        lastLearningTheRopesState = null;
        lastGeneratedName = "None";
        lastCharacterAction = "Waiting";
        lastExperienceSelection = "None";
        lastLearningTheRopesHint = "Unavailable";
        lastLearningTheRopesJournalText = "";
        ownFireLocation = null;
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }

    public void setDebugLogging(boolean debugLogging)
    {
        this.debugLogging = debugLogging;
    }

    private void debug(String message, Object... args)
    {
        if (debugLogging) {
            log.info("[KSP Tutorial] " + message, args);
        }
    }

    public String getLastGeneratedName()
    {
        return lastGeneratedName;
    }

    public boolean isPlayerInStartArea()
    {
        return isInStartArea();
    }

    public boolean isNameCreationOpen()
    {
        return isDisplayNameWidgetOpen();
    }

    public boolean isCharacterCreationOpen()
    {
        return isCharacterCreationWidgetOpen();
    }

    public String getLastCharacterAction()
    {
        return lastCharacterAction;
    }

    public boolean isExperiencePromptVisible()
    {
        return isExperiencePromptOpen();
    }

    public String getLastExperienceSelection()
    {
        return lastExperienceSelection;
    }

    public String getStatus()
    {
        return status == null ? "Unknown" : status.name();
    }

    public String getLearningTheRopesQuestStateName()
    {
        return lastLearningTheRopesState == null ? "Unknown" : lastLearningTheRopesState.name();
    }

    public String getLearningTheRopesHint()
    {
        return lastLearningTheRopesHint;
    }

    public String getLearningTheRopesJournalText()
    {
        return lastLearningTheRopesJournalText;
    }

    public TutState getTutState()
    {
        if (!Microbot.isLoggedIn()) {
            return TutState.LOGIN;
        }

        try {
            return TutState.valueOf(getStatus());
        } catch (IllegalArgumentException ignored) {
            return isOnTutorialIsland() ? TutState.RUNNING : TutState.UNKNOWN;
        }
    }

    public boolean isComplete()
    {
        return !isOnTutorialIsland();
    }

    public static boolean isOnTutorialIsland()
    {
        if (!Microbot.isLoggedIn()) {
            return false;
        }

        if (isDisplayNameWidgetOpenStatic() || isCharacterCreationWidgetOpenStatic()) {
            return true;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (isStartAreaPoint(playerLocation) || isInTutorialIslandBounds(playerLocation)) {
            return true;
        }

        return false;
    }

    public String getQueuedAccountName()
    {
        return "None";
    }

    public int getRemainingQueuedAccounts()
    {
        return 0;
    }

    private enum Status
    {
        NAME,
        CHARACTER,
        GETTING_STARTED,
        SURVIVAL_GUIDE,
        COOKING_GUIDE,
        QUEST_GUIDE,
        MINING_GUIDE,
        COMBAT_GUIDE,
        BANKER_GUIDE,
        PRAYER_GUIDE,
        MAGE_GUIDE,
        FINISHED
    }

    private interface Condition
    {
        boolean matches();
    }
}
