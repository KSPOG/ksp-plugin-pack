package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.tutorialisland;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.PlayStyle;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;


import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Best-effort Java reconstruction from TutorialIslandScript.class.
 *
 * The original bytecode is preserved in recovery-notes/KSPTutIslandPlugin_javap_disassembly.txt.
 * This file restores the class layout, constants, state fields, overlay getters, queue parsing,
 * status calculation, name/character creation helpers, and the main loop flow found in bytecode.
 * Some long Tutorial Island step methods were reconstructed conservatively because Java bytecode
 * does not retain the original formatting/comments and no dedicated Java decompiler was available.
 */
@Singleton
public class TutorialIslandScript extends Script
{
    private static final int LOOP_DELAY_MS = 600;
    private static final int NAME_ATTEMPT_COOLDOWN_MS = 3500;
    private static final int COMPLETION_LOGOUT_RETRY_MS = 6000;
    private static final int QUEUE_LOGIN_RETRY_MS = 10000;
    private static final int QUEUE_LOGIN_SKIP_MS = 60000;
    private static final int QUEUED_LOGIN_EMAIL_PASSWORD_DELAY_MIN_MS = 1400;
    private static final int QUEUED_LOGIN_EMAIL_PASSWORD_DELAY_MAX_MS = 2600;

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

    private static final String DISPLAY_NAME_TITLE = "Set display name";
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

    private static final WorldArea START_AREA = new WorldArea(3092, 3100, 4, 13, 0);
    private static final WorldArea SURVIVAL_AREA = new WorldArea(3098, 3089, 8, 11, 0);
    private static final WorldArea COOKING_AREA = new WorldArea(3073, 3083, 6, 4, 0);
    private static final WorldArea QUEST_GUIDE_AREA = new WorldArea(3083, 3119, 7, 7, 0);
    private static final WorldArea MINING_SMITHING_AREA = new WorldArea(3073, 9494, 15, 15, 0);
    private static final WorldArea RAT_PIT_AREA = new WorldArea(3097, 9507, 15, 12, 0);
    private static final WorldArea TUT_ISLAND_BANK_AREA = new WorldArea(3120, 3118, 12, 10, 0);
    private static final WorldArea CHURCH_AREA = new WorldArea(3120, 3103, 14, 14, 0);
    private static final WorldArea TUTORIAL_END_AREA = new WorldArea(3134, 3084, 14, 14, 0);

    private long lastNameAttemptAtMs;
    private String lastGeneratedName = "None";
    private String lastCharacterAction = "Waiting";
    private String lastExperienceSelection = "None";
    private String completionState = "Active";
    private Status status = Status.NAME;
    private boolean toggledSettings;
    private boolean debugEnabled;
    private boolean completionLogoutRequested;
    private long lastCompletionLogoutAttemptAtMs;
    private int accountQueueIndex;
    private int pendingQueuedLoginIndex = -1;
    private long lastQueueLoginAttemptAtMs;
    private long queuedLoginStartedAtMs;
    private String queuedAccountName = "None";
    private WorldPoint ownFireLocation;

    public boolean run()
    {
        shutdown();
        resetAccountState();
        configureAntiban();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run())
            {
                return;
            }

            if (!Microbot.isLoggedIn())
            {
                handleQueuedLogin();
                return;
            }

            completeQueuedLoginIfNeeded();
            calculateStatus();

            if (Rs2Widget.isWidgetVisible(929, 5))
            {
                Rs2Widget.clickWidget(929, 5);
                Rs2Random.waitEx(1200, 300);
                return;
            }

            if (Rs2Widget.isWidgetVisible(310, 0))
            {
                Rs2Keyboard.keyPress(27);
                Rs2Random.waitEx(1200, 300);
                return;
            }

            if (isInStartArea() && isExperiencePromptOpen())
            {
                selectRandomExperienceOption();
                return;
            }

            if (Rs2Dialogue.hasContinue())
            {
                Rs2Dialogue.clickContinue();
                return;
            }

            if (Rs2Player.isMoving() || Rs2Player.isAnimating())
            {
                return;
            }

            switch (status)
            {
                case NAME:
                    if (isInStartArea() && isDisplayNameWidgetOpen() && !isNameAttemptCoolingDown())
                    {
                        lastNameAttemptAtMs = System.currentTimeMillis();
                        enterGeneratedName();
                    }
                    break;
                case CHARACTER:
                    if (isInStartArea() && isCharacterCreationWidgetOpen())
                    {
                        randomizeCharacter();
                    }
                    break;
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
                    handleTutorialComplete();
                    break;
            }
        }, 0, LOOP_DELAY_MS, TimeUnit.MILLISECONDS);

        return true;
    }

    public static boolean isOnTutorialIsland()
    {
        try
        {
            return Microbot.isLoggedIn() && Microbot.getVarbitPlayerValue(281) < 1000;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    public boolean isComplete()
    {
        try
        {
            return Microbot.getVarbitPlayerValue(281) >= 1000;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    public void setDebugLogging(boolean debugEnabled)
    {
        this.debugEnabled = debugEnabled;
    }

    private void calculateStatus()
    {
        int progress = Microbot.getVarbitPlayerValue(281);

        if (progress < 1000 && completionLogoutRequested)
        {
            completionLogoutRequested = false;
            completionState = "Active";
        }

        if (isDisplayNameWidgetOpen())
        {
            status = Status.NAME;
        }
        else if (isCharacterCreationWidgetOpen())
        {
            status = Status.CHARACTER;
        }
        else if (progress < 10)
        {
            status = Status.GETTING_STARTED;
        }
        else if (progress < 120)
        {
            status = Status.SURVIVAL_GUIDE;
        }
        else if (progress < 200)
        {
            status = Status.COOKING_GUIDE;
        }
        else if (progress <= 250)
        {
            status = Status.QUEST_GUIDE;
        }
        else if (progress <= 360)
        {
            status = Status.MINING_GUIDE;
        }
        else if (progress < 510)
        {
            status = Status.COMBAT_GUIDE;
        }
        else if (progress < 540)
        {
            status = Status.BANKER_GUIDE;
        }
        else if (progress < 610)
        {
            status = Status.PRAYER_GUIDE;
        }
        else if (progress < 1000)
        {
            status = Status.MAGE_GUIDE;
        }
        else
        {
            status = Status.FINISHED;
        }
    }

    private boolean isInStartArea()
    {
        WorldPoint location = Rs2Player.getWorldLocation();
        return location != null && START_AREA.contains(location);
    }

    private boolean isDisplayNameWidgetOpen()
    {
        return Rs2Widget.isWidgetVisible(NAME_CREATION_GROUP, NAME_CREATION_CONTAINER_CHILD)
                || (Rs2Widget.hasWidget(DISPLAY_NAME_TITLE) && Rs2Widget.hasWidget(LOOK_UP_NAME_BUTTON));
    }

    private boolean isCharacterCreationWidgetOpen()
    {
        return Rs2Widget.isWidgetVisible(CHARACTER_CREATION_GROUP, CHARACTER_CREATION_CONTAINER_CHILD)
                || Rs2Widget.hasWidget(CHARACTER_CREATOR_TITLE);
    }

    private boolean isExperiencePromptOpen()
    {
        return Rs2Dialogue.isInDialogue()
                && (Rs2Widget.hasWidget(EXPERIENCE_PROMPT_TITLE)
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[0])
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[1])
                || Rs2Widget.hasWidget(EXPERIENCE_OPTION_TEXTS[2]));
    }

    private boolean isNameAttemptCoolingDown()
    {
        return System.currentTimeMillis() - lastNameAttemptAtMs < NAME_ATTEMPT_COOLDOWN_MS;
    }

    private void enterGeneratedName()
    {
        String name = generateDisplayName();
        lastGeneratedName = name;

        clearDisplayNameInput();
        sleep(randomDelay(250, 500));

        Rs2Widget.clickWidget(NAME_CREATION_GROUP, NAME_INPUT_CHILD);
        sleep(randomDelay(250, 500));

        Rs2Keyboard.typeString(name);
        sleep(randomDelay(300, 600));

        if (!clickLookUpNameButton())
        {
            debug("Unable to click Look up name button for generated name %s", name);
            return;
        }

        sleepUntil(() -> isGeneratedNameAvailable(name), 6_000);

        if (isGeneratedNameAvailable(name))
        {
            if (!clickSetNameButton())
            {
                debug("Generated name %s is available, but Set name button was not clickable", name);
            }
            sleep(randomDelay(800, 1400));
        }
    }

    private void clearDisplayNameInput()
    {
        Rs2Widget.clickWidget(NAME_CREATION_GROUP, NAME_INPUT_CHILD);
        sleep(randomDelay(250, 500));

        /*
         * Do not rely on NAME_INPUT_TEXT_CHILD length here.
         * After failed lookups the widget text can be stale/partial, so we use
         * enough backspaces to clear any valid OSRS display name.
         */
        for (int i = 0; i < 24; i++)
        {
            Rs2Keyboard.keyPress(8);
            sleep(randomDelay(20, 55));
        }
    }

    private boolean clickLookUpNameButton()
    {
        Widget button = Rs2Widget.getWidget(NAME_CREATION_GROUP, LOOK_UP_NAME_BUTTON_CHILD);
        if (button != null && !button.isHidden())
        {
            Rs2Widget.clickWidget(button);
            return true;
        }

        Widget fallback = Rs2Widget.findWidget(LOOK_UP_NAME_BUTTON, null, false);
        if (fallback != null && !fallback.isHidden())
        {
            Rs2Widget.clickWidget(fallback);
            return true;
        }

        return false;
    }

    private boolean clickSetNameButton()
    {
        Widget button = Rs2Widget.getWidget(NAME_CREATION_GROUP, SET_NAME_BUTTON_CHILD);
        if (button != null && !button.isHidden())
        {
            Rs2Widget.clickWidget(button);
            sleepUntil(() -> !isDisplayNameWidgetOpen(), 5_000);
            return true;
        }

        Widget setNameWidget = Rs2Widget.findWidget("Set name", null, false);
        if (setNameWidget != null && !setNameWidget.isHidden())
        {
            Rs2Widget.clickWidget(setNameWidget);
            sleepUntil(() -> !isDisplayNameWidgetOpen(), 5_000);
            return true;
        }

        Widget setWidget = Rs2Widget.findWidget("Set", null, false);
        if (setWidget != null && !setWidget.isHidden())
        {
            Rs2Widget.clickWidget(setWidget);
            sleepUntil(() -> !isDisplayNameWidgetOpen(), 5_000);
            return true;
        }

        return false;
    }

    private String getCurrentDisplayNameInput()
    {
        Widget nameInput = Rs2Widget.getWidget(NAME_CREATION_GROUP, NAME_INPUT_TEXT_CHILD);
        if (nameInput == null || nameInput.getText() == null)
        {
            return "";
        }
        return nameInput.getText().replace("*", "").trim();
    }

    private boolean isGeneratedNameAvailable(String name)
    {
        Widget responseWidget = Rs2Widget.getWidget(NAME_CREATION_GROUP, NAME_RESPONSE_TEXT_CHILD);
        if (responseWidget == null || responseWidget.getText() == null)
        {
            return false;
        }

        String responseText = Rs2UiHelper.stripColTags(responseWidget.getText());
        return responseText.startsWith(name + " is available");
    }

    private void randomizeCharacter()
    {
        lastCharacterAction = "Randomizing";
        selectRandomBodyType();
        sleep(randomDelay(450, 900));

        for (int arrowBaseChild : CHARACTER_CREATION_ARROWS)
        {
            clickRandomCharacterArrow(arrowBaseChild);
            sleep(randomDelay(90, 240));
        }

        sleep(randomDelay(700, 1400));
        Rs2Widget.clickWidget(CHARACTER_CREATION_GROUP, CHARACTER_CONFIRM_BUTTON_CHILD);
        lastCharacterAction = "Confirmed";
    }

    private void selectRandomExperienceOption()
    {
        int optionIndex = ThreadLocalRandom.current().nextInt(EXPERIENCE_OPTION_TEXTS.length);
        String optionText = EXPERIENCE_OPTION_TEXTS[optionIndex];
        Widget optionWidget = Rs2Widget.findWidget(optionText, null, false);
        if (optionWidget == null)
        {
            return;
        }

        lastExperienceSelection = "Option " + (optionIndex + 1);
        sleep(randomDelay(450, 1100));
        Rs2Widget.clickWidget(optionWidget);
        sleep(randomDelay(600, 1400));
    }

    private void selectRandomBodyType()
    {
        int bodyTypeChild = ThreadLocalRandom.current().nextBoolean() ? BODY_TYPE_A_CHILD : BODY_TYPE_B_CHILD;
        Rs2Widget.clickWidget(CHARACTER_CREATION_GROUP, bodyTypeChild);
    }

    private void clickRandomCharacterArrow(int arrowBaseChild)
    {
        int arrowChild = arrowBaseChild + (ThreadLocalRandom.current().nextBoolean() ? 2 : 3);
        int clickCount = ThreadLocalRandom.current().nextInt(1, 8);
        Widget arrowWidget = Rs2Widget.getWidget(CHARACTER_CREATION_GROUP, arrowChild);
        if (arrowWidget == null)
        {
            return;
        }

        for (int i = 0; i < clickCount; i++)
        {
            Rs2Widget.clickWidget(arrowWidget.getId());
            sleep(randomDelay(120, 360));
        }
    }

    private void gettingStarted()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withId(3308).nearest();
        int progress = Microbot.getVarbitPlayerValue(281);

        if (progress < 3)
        {
            if (isExperiencePromptOpen())
            {
                selectRandomExperienceOption();
                return;
            }
            walkAndTalk(npc);
            return;
        }

        if (progress < 8)
        {
            if (!toggledSettings)
            {
                Rs2Widget.clickWidget(164, 41);
                toggledSettings = true;
                Rs2Random.waitEx(1200, 300);
                return;
            }
            walkAndTalk(npc);
            return;
        }

        if (progress < 10)
        {
            Microbot.getRs2TileObjectCache().query().interact(9398, "Open");
        }
    }

    private void survivalGuide()
    {
        int progress = Microbot.getVarbitPlayerValue(281);

        /*
         * Do not advance from the Survival Expert purely on a broad progress range.
         * The older plugin flow talks to the expert again at 10, 20 and 60, then uses
         * inventory/tool checks before cutting, fishing, lighting and cooking.
         */
        if (progress == 10 || progress == 20 || progress == 60 || !hasSurvivalStarterTools())
        {
            talkToSurvivalExpert();
            return;
        }

        if (progress < 40)
        {
            cutTree();
            return;
        }

        if (progress < 60)
        {
            lightFire();
            return;
        }

        if (progress < 80)
        {
            fishShrimp();
            return;
        }

        if (progress < 100)
        {
            completeSurvivalCookingStep();
            return;
        }

        Microbot.getRs2TileObjectCache().query().interact(9472, "Open");
    }

    private boolean hasSurvivalStarterTools()
    {
        return Rs2Inventory.contains("Bronze axe") && Rs2Inventory.contains("Tinderbox");
    }

    private boolean hasRawShrimp()
    {
        return Rs2Inventory.contains("Raw shrimps");
    }

    private boolean hasTutorialLog()
    {
        return Rs2Inventory.contains("Logs");
    }

    private void completeSurvivalCookingStep()
    {
        if (!hasSurvivalStarterTools())
        {
            talkToSurvivalExpert();
            return;
        }

        if (!hasRawShrimp())
        {
            fishShrimp();
            return;
        }

        if (getOwnFire() == null)
        {
            if (!hasTutorialLog())
            {
                cutTree();
                return;
            }

            lightFire();
            return;
        }

        cookShrimpOnOwnFire();
    }

    private boolean talkToSurvivalExpert()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Survival Expert").nearest();
        return walkAndTalk(npc, 4);
    }

    private void cookingGuide()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withId(3305).nearest();
        int progress = Microbot.getVarbitPlayerValue(281);

        if (progress == 120)
        {
            Rs2Keyboard.keyPress(27);
            Microbot.getRs2TileObjectCache().query().interact(9470, "Open");
        }
        else if (progress == 130)
        {
            walkToArea(COOKING_AREA);
            Microbot.getRs2TileObjectCache().query().interact(9709, "Open");
        }
        else if (progress == 140)
        {
            walkAndTalk(npc);
        }
        else if (progress < 200)
        {
            if (!Rs2Inventory.contains("Bread dough") && !Rs2Inventory.contains("Bread"))
            {
                Rs2Inventory.combine("Bucket of water", "Pot of flour");
            }
            else if (Rs2Inventory.contains("Bread dough"))
            {
                Rs2Inventory.interact("Bread dough");
                Microbot.getRs2TileObjectCache().query().interact(9736, "Use");
            }
            else if (Rs2Inventory.contains("Bread"))
            {
                Microbot.getRs2TileObjectCache().query().interact(9710, "Open");
            }
        }
    }

    private void questGuide()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Quest Guide").nearest();
        int progress = Microbot.getVarbitPlayerValue(281);
        if (progress <= 250)
        {
            if (!QUEST_GUIDE_AREA.contains(Rs2Player.getWorldLocation()))
            {
                walkToArea(QUEST_GUIDE_AREA);
                return;
            }
            walkAndTalk(npc);
        }
    }

    private void miningGuide()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Mining Instructor").nearest();
        int progress = Microbot.getVarbitPlayerValue(281);

        if (!MINING_SMITHING_AREA.contains(Rs2Player.getWorldLocation()))
        {
            walkToArea(MINING_SMITHING_AREA);
            return;
        }

        if (progress <= 270)
        {
            walkAndTalk(npc);
        }
        else if (progress <= 320)
        {
            Microbot.getRs2TileObjectCache().query().interact(10080, "Mine");
        }
        else if (progress <= 340)
        {
            Microbot.getRs2TileObjectCache().query().interact(10082, "Mine");
        }
        else if (progress <= 360)
        {
            if (Rs2Inventory.contains("Tin ore") && Rs2Inventory.contains("Copper ore"))
            {
                Microbot.getRs2TileObjectCache().query().interact(10082, "Smelt");
            }
            else
            {
                walkAndTalk(npc);
            }
        }
    }

    private void combatGuide()
    {
        Rs2NpcModel instructor = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Combat Instructor").nearest();
        int progress = Microbot.getVarbitPlayerValue(281);

        if (progress < 370)
        {
            walkAndTalk(instructor);
        }
        else if (progress < 410)
        {
            if (!RAT_PIT_AREA.contains(Rs2Player.getWorldLocation()))
            {
                Microbot.getRs2TileObjectCache().query().interact(9719, "Open");
                return;
            }
            attackNearestRat();
        }
        else if (progress < 510)
        {
            if (progress >= 480)
            {
                selectLongrangeCombatStyle();
            }
            walkAndAttackRat();
        }
    }

    private void bankerGuide()
    {
        Rs2NpcModel banker = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Banker").nearest();
        Rs2NpcModel accountGuide = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Account Guide").nearest();
        int progress = Microbot.getVarbitPlayerValue(281);

        if (!TUT_ISLAND_BANK_AREA.contains(Rs2Player.getWorldLocation()))
        {
            walkToArea(TUT_ISLAND_BANK_AREA);
            return;
        }

        if (progress < 530)
        {
            walkAndTalk(banker);
        }
        else
        {
            walkAndTalk(accountGuide);
        }
    }

    private void prayerGuide()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Brother Brace").nearest();
        if (!CHURCH_AREA.contains(Rs2Player.getWorldLocation()))
        {
            walkToArea(CHURCH_AREA);
            return;
        }
        walkAndTalk(npc);
    }

    private void mageGuide()
    {
        Rs2NpcModel npc = (Rs2NpcModel) Microbot.getRs2NpcCache().query().withName("Magic Instructor").nearest();
        if (!TUTORIAL_END_AREA.contains(Rs2Player.getWorldLocation()))
        {
            walkToArea(TUTORIAL_END_AREA);
            return;
        }
        handleFinalMageDialogue();
        walkAndTalk(npc);
    }

    private void handleFinalMageDialogue()
    {
        if (Rs2Dialogue.hasContinue())
        {
            Rs2Dialogue.clickContinue();
        }
    }

    private boolean castHomeTeleport()
    {
        return false;
    }

    private void handleTutorialComplete()
    {
        if (!shouldRunMultipleAccounts())
        {
            completionState = "Finished";
            return;
        }

        long now = System.currentTimeMillis();
        if (!completionLogoutRequested || now - lastCompletionLogoutAttemptAtMs >= COMPLETION_LOGOUT_RETRY_MS)
        {
            completionLogoutRequested = true;
            completionState = "Logout requested";
            lastCompletionLogoutAttemptAtMs = now;
            Microbot.getClient().getCanvas().requestFocus();
            Rs2Keyboard.keyPress(27);
        }
    }

    private void handleQueuedLogin()
    {
        if (!shouldRunMultipleAccounts())
        {
            return;
        }

        if (pendingQueuedLoginIndex >= 0)
        {
            if (isQueuedLoginTimedOut(System.currentTimeMillis()))
            {
                skipFailedQueuedLogin();
            }
            return;
        }

        AccountQueueEntry next = getNextQueuedAccount();
        if (next == null)
        {
            queuedAccountName = "None";
            return;
        }

        performQueuedLogin(next, accountQueueIndex);
    }

    private boolean performQueuedLogin(AccountQueueEntry entry, int index)
    {
        long now = System.currentTimeMillis();
        if (now - lastQueueLoginAttemptAtMs < QUEUE_LOGIN_RETRY_MS)
        {
            return false;
        }

        pendingQueuedLoginIndex = index;
        queuedLoginStartedAtMs = now;
        lastQueueLoginAttemptAtMs = now;
        queuedAccountName = entry.username;
        return true;
    }

    private void completeQueuedLoginIfNeeded()
    {
        if (pendingQueuedLoginIndex >= 0)
        {
            accountQueueIndex = pendingQueuedLoginIndex + 1;
            pendingQueuedLoginIndex = -1;
            queuedAccountName = "None";
            resetAccountProgressState();
        }
    }

    private boolean isQueuedLoginTimedOut(long now)
    {
        return queuedLoginStartedAtMs > 0 && now - queuedLoginStartedAtMs > QUEUE_LOGIN_SKIP_MS;
    }

    private void skipFailedQueuedLogin()
    {
        if (pendingQueuedLoginIndex >= 0)
        {
            accountQueueIndex = pendingQueuedLoginIndex + 1;
        }
        pendingQueuedLoginIndex = -1;
        queuedAccountName = "None";
    }

    private boolean isRuleBreakingLoginBlockVisible()
    {
        return Rs2Widget.hasWidget("You may appeal this ban") || Rs2Widget.hasWidget("disabled");
    }

    private void clickLoginBackButton()
    {
        Rs2Keyboard.keyPress(27);
    }

    private AccountQueueEntry getNextQueuedAccount()
    {
        List<AccountQueueEntry> entries = parseAccountQueue();
        if (accountQueueIndex < 0)
        {
            accountQueueIndex = 0;
        }
        if (accountQueueIndex >= entries.size())
        {
            return null;
        }
        return entries.get(accountQueueIndex);
    }

    private List<AccountQueueEntry> parseAccountQueue()
    {
        return new ArrayList<>();
    }

    private AccountQueueEntry parseAccountQueueLine(String line)
    {
        if (line == null || line.trim().isEmpty())
        {
            return null;
        }

        String[] parts = line.trim().split(":");
        if (parts.length < 2)
        {
            return null;
        }

        int world = parts.length >= 3 ? parseWorld(parts[2]) : -1;
        return new AccountQueueEntry(parts[0].trim(), parts[1].trim(), world);
    }

    private int parseWorld(String value)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (Exception ignored)
        {
            return -1;
        }
    }

    private boolean shouldRunMultipleAccounts()
    {
        return false;
    }

    private void resetAccountState()
    {
        resetAccountProgressState();
        accountQueueIndex = 0;
        pendingQueuedLoginIndex = -1;
        queuedAccountName = "None";
        lastQueueLoginAttemptAtMs = 0;
        queuedLoginStartedAtMs = 0;
    }

    private void resetAccountProgressState()
    {
        lastNameAttemptAtMs = 0;
        lastGeneratedName = "None";
        lastCharacterAction = "Waiting";
        lastExperienceSelection = "None";
        completionState = "Active";
        status = Status.NAME;
        toggledSettings = false;
        completionLogoutRequested = false;
        lastCompletionLogoutAttemptAtMs = 0;
        ownFireLocation = null;
    }

    private boolean walkAndTalk(Rs2NpcModel npc)
    {
        return walkAndTalk(npc, 2);
    }

    private boolean walkAndTalk(Rs2NpcModel npc, int distance)
    {
        return walkAndAct(npc, distance, "Talk-to", () -> { });
    }

    private boolean walkAndAct(Rs2NpcModel npc, int distance, String action, Runnable afterWalk)
    {
        if (npc == null)
        {
            return false;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        WorldPoint npcLocation = npc.getWorldLocation();

        if (playerLocation != null && npcLocation != null
                && playerLocation.distanceTo(npcLocation) > distance)
        {
            Rs2Walker.walkTo(npcLocation);
            if (afterWalk != null)
            {
                afterWalk.run();
            }
            return false;
        }

        return npc.click(action);
    }

    private boolean walkAndAttackRat()
    {
        if (!RAT_PIT_AREA.contains(Rs2Player.getWorldLocation()))
        {
            Microbot.getRs2TileObjectCache().query().interact(9719, "Open");
            return false;
        }
        return attackNearestRat();
    }

    private boolean attackNearestRat()
    {
        Rs2NpcModel rat = (Rs2NpcModel) Microbot.getRs2NpcCache().query()
                .withName("Giant rat")
                .nearest();

        if (rat == null)
        {
            rat = (Rs2NpcModel) Microbot.getRs2NpcCache().query()
                    .withName("Rat")
                    .nearest();
        }

        return rat != null && rat.click("Attack");
    }

    private boolean selectLongrangeCombatStyle()
    {
        clickTab("Combat Options");
        return Rs2Widget.hasWidget("Longrange") && Rs2Widget.findWidget("Longrange", null, false) != null;
    }

    private void clickTab(String name)
    {
        Widget widget = Rs2Widget.findWidget(name, null, false);
        if (widget != null)
        {
            Rs2Widget.clickWidget(widget);
        }
    }

    private void closeEquipmentStats()
    {
        if (Rs2Widget.hasWidget("Equipment Stats"))
        {
            Rs2Keyboard.keyPress(27);
        }
    }

    private void handleBankSpaceAndPollBooth()
    {
        closePollOrOptionsWidget();
    }

    private void closePollOrOptionsWidget()
    {
        if (Rs2Widget.hasWidget("Poll") || Rs2Widget.hasWidget("Options"))
        {
            Rs2Keyboard.keyPress(27);
        }
    }

    private void lightFire()
    {
        if (Rs2Inventory.contains("Logs") && Rs2Inventory.contains("Tinderbox"))
        {
            WorldPoint before = Rs2Player.getWorldLocation();
            Rs2Inventory.combine("Tinderbox", "Logs");
            ownFireLocation = before;
        }
    }

    private void cutTree()
    {
        Microbot.getRs2TileObjectCache().query().interact(9730, "Chop down");
    }

    private void fishShrimp()
    {
        Rs2NpcModel fishingSpot = (Rs2NpcModel) Microbot.getRs2NpcCache().query()
                .withName("Fishing spot")
                .nearest();

        if (fishingSpot != null && fishingSpot.click("Net"))
        {
            sleepUntil(() -> hasRawShrimp(), 5_000);
        }
    }

    private void cookShrimpOnOwnFire()
    {
        Rs2TileObjectModel fire = getOwnFire();

        if (fire == null || !Rs2Inventory.contains("Raw shrimps"))
        {
            return;
        }

        Rs2Inventory.interact("Raw shrimps");
        sleep(randomDelay(250, 500));
        fire.click("Use");
    }

    private Rs2TileObjectModel getOwnFire()
    {
        if (ownFireLocation == null)
        {
            return null;
        }

        return getFireAt(ownFireLocation);
    }

    private Rs2TileObjectModel getFireAt(WorldPoint point)
    {
        if (point == null)
        {
            return null;
        }

        return Microbot.getRs2TileObjectCache().query()
                .where(tileObject -> tileObject.getWorldLocation() != null
                        && point.equals(tileObject.getWorldLocation()))
                .nearest();
    }

    private boolean widgetCast()
    {
        return Rs2Widget.hasWidget("Cast");
    }

    private boolean walkToArea(WorldArea area)
    {
        WorldPoint location = Rs2Player.getWorldLocation();
        if (location != null && area.contains(location))
        {
            return true;
        }
        return Rs2Walker.walkTo(randomPoint(area));
    }

    private WorldPoint randomPoint(WorldArea area)
    {
        int x = ThreadLocalRandom.current().nextInt(area.getX(), area.getX() + area.getWidth());
        int y = ThreadLocalRandom.current().nextInt(area.getY(), area.getY() + area.getHeight());
        return new WorldPoint(x, y, area.getPlane());
    }

    private String generateDisplayName()
    {
        String prefix = NAME_PREFIXES[ThreadLocalRandom.current().nextInt(NAME_PREFIXES.length)];
        String suffix = NAME_SUFFIXES[ThreadLocalRandom.current().nextInt(NAME_SUFFIXES.length)];
        int number = ThreadLocalRandom.current().nextInt(10, 999);
        return prefix + suffix + number;
    }

    private int randomDelay(int min, int max)
    {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void debug(String message, Object... args)
    {
        if (!debugEnabled)
        {
            return;
        }

        try
        {
            Microbot.log("[Tutorial Island] " + String.format(message, args));
        }
        catch (Exception ignored)
        {
        }
    }

    private void configureAntiban()
    {
        Rs2Antiban.resetAntibanSettings();

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

    @Override
    public void shutdown()
    {
        debugEnabled = false;
        Rs2Antiban.resetAntibanSettings();
        super.shutdown();
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
        return status + " / " + completionState;
    }

    public String getQueuedAccountName()
    {
        return queuedAccountName;
    }

    public int getRemainingQueuedAccounts()
    {
        int remaining = parseAccountQueue().size() - accountQueueIndex;
        return Math.max(remaining, 0);
    }

    private static final class AccountQueueEntry
    {
        private final String username;
        private final String password;
        private final int world;

        private AccountQueueEntry(String username, String password, int world)
        {
            this.username = username;
            this.password = password;
            this.world = world;
        }
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
}
