/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Singleton
 *  net.runelite.api.GameObject
 *  net.runelite.api.Skill
 *  net.runelite.api.coords.WorldPoint
 *  net.runelite.client.plugins.microbot.Microbot
 *  net.runelite.client.plugins.microbot.Script
 *  net.runelite.client.plugins.microbot.globval.enums.InterfaceTab
 *  net.runelite.client.plugins.microbot.util.bank.Rs2Bank
 *  net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment
 *  net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject
 *  net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory
 *  net.runelite.client.plugins.microbot.util.player.Rs2Player
 *  net.runelite.client.plugins.microbot.util.tabs.Rs2Tab
 *  net.runelite.client.plugins.microbot.util.walker.Rs2Walker
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.woodcuttingscript;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspTaskDebug;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.equiplevels.AxeEquip;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.levelreqwc.WoodCuttingReq;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.treeareas.TreeAreas;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.treelevel.TreeLevel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WoodCuttingScript
extends Script {
    private static final Logger log = LoggerFactory.getLogger(WoodCuttingScript.class);
    private static final int LOOP_DELAY_MS = 600;
    private static final int WEB_WALK_COOLDOWN_MS = 3000;
    private static final int OBJECT_INTERACTION_COOLDOWN_MS = 2_500;
    private static final int TREE_SEARCH_PADDING_TILES = 8;
    private static final int OUT_OF_AREA_TREE_FALLBACK_RADIUS = 4;
    private static final int MID_TIER_RANDOM_MAX_LEVEL = 60;
    private static final List<String> AXE_NAMES = Arrays.asList("Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe", "Adamant axe", "Rune axe");
    private TreeAreas targetArea = TreeAreas.REGULAR_TREE_VARROCK_WEST;
    private boolean startingTargetTreeInitialized;
    private TreeLevel randomMidTierTree;
    private boolean debugLogging;
    private long lastWebWalkAtMs;
    private long lastObjectInteractionAtMs;
    private boolean walkingToTargetArea;

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public boolean run(TreeAreas area) {
        this.shutdown();
        this.targetArea = area;
        this.startingTargetTreeInitialized = false;
        this.randomMidTierTree = null;
        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run() || !Microbot.isLoggedIn()) {
                return;
            }
            int woodcuttingLevel = Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING);
            int attackLevel = Microbot.getClient().getRealSkillLevel(Skill.ATTACK);
            this.initializeStartingTargetTree(woodcuttingLevel);
            TreeAreas desiredArea = this.resolveTargetArea(woodcuttingLevel);
            if (desiredArea != this.targetArea) {
                this.targetArea = desiredArea;
                this.debug("Switching woodcutting area to {} for woodcutting level {}", this.targetArea.getDisplayName(), woodcuttingLevel);
            }
            TreeLevel targetTree = this.getTargetTreeLevel(woodcuttingLevel);
            KspTaskDebug.throttled(log, this.debugLogging, "Woodcutting", "loop", 5_000L,
                    "loop | level={} attack={} area={} targetTree={} player={} moving={} animating={} interacting={} invFull={} bankOpen={}",
                    woodcuttingLevel,
                    attackLevel,
                    this.targetArea.getDisplayName(),
                    targetTree != null ? targetTree.getDisplayName() : "none",
                    Rs2Player.getWorldLocation(),
                    Rs2Player.isMoving(),
                    Rs2Player.isAnimating(),
                    Rs2Player.isInteracting(),
                    Rs2Inventory.isFull(),
                    Rs2Bank.isOpen());
            if (Rs2Inventory.isFull()) {
                this.debug("Inventory full; banking logs | player={} area={} equippedAxeInInv={}",
                        Rs2Player.getWorldLocation(),
                        this.targetArea.getDisplayName(),
                        this.resolveInventoryAxeToKeep(woodcuttingLevel));
                this.bankLogsOnly(woodcuttingLevel);
                return;
            }
            if (!this.upgradeAxe(woodcuttingLevel, attackLevel)) {
                return;
            }
            if (!this.hasAnyAxeEquippedOrInInventory()) {
                this.debug("No axe available in inventory or equipment, waiting before proceeding", new Object[0]);
                return;
            }
            if (!this.ensureInTargetArea()) {
                return;
            }
            if (!this.isIdleInTargetArea()) {
                return;
            }
            this.chopForCurrentLevel(woodcuttingLevel);
        }, 0L, 600L, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean upgradeAxe(int woodcuttingLevel, int attackLevel) {
        WoodCuttingReq activeAxeReq;
        boolean canEquipActiveAxe;
        WoodCuttingReq bestWoodcuttingReq = WoodCuttingReq.bestForWoodcuttingLevel(woodcuttingLevel);
        String targetAxeName = this.resolveDesiredAxe(bestWoodcuttingReq);
        if (targetAxeName == null) {
            return true;
        }
        String activeAxeName = this.resolveBestOwnedAxeName(targetAxeName);
        if (activeAxeName == null) {
            this.ensureInventoryTabOpen();
            if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Bank.openBank()) {
                return false;
            }
            activeAxeName = this.resolveBestOwnedAxeName(targetAxeName);
            if (activeAxeName == null) {
                this.debug("No eligible axe available up to target {}", targetAxeName);
                Rs2Bank.closeBank();
                return true;
            }
        }
        boolean bl = canEquipActiveAxe = (activeAxeReq = this.resolveWoodcuttingReq(activeAxeName)) != null && this.canEquipDesiredAxe(activeAxeReq, attackLevel);
        if (!Rs2Equipment.isWearing((String[])new String[]{activeAxeName}) && !Rs2Inventory.hasItem((String[])new String[]{activeAxeName})) {
            if (!Rs2Bank.isOpen()) {
                this.ensureInventoryTabOpen();
                if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Bank.openBank()) {
                    return false;
                }
            }
            if (Rs2Bank.isOpen() && Rs2Bank.count((String)activeAxeName) > 0) {
                String axeToWithdraw = activeAxeName;
                Rs2Bank.withdrawOne((String)activeAxeName);
                WoodCuttingScript.sleepUntil(() -> Rs2Inventory.hasItem((String[])new String[]{axeToWithdraw}), (int)3000);
            }
        }
        if (canEquipActiveAxe && Rs2Inventory.hasItem((String[])new String[]{activeAxeName}) && !Rs2Equipment.isWearing((String[])new String[]{activeAxeName})) {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
                return false;
            }
            String axeToWield = activeAxeName;
            Rs2Inventory.wield((String[])new String[]{activeAxeName});
            WoodCuttingScript.sleepUntil(() -> Rs2Equipment.isWearing((String[])new String[]{axeToWield}), (int)2000);
        }
        if (Rs2Bank.isOpen()) {
            this.depositOutdatedAxes(activeAxeName);
            if (!this.hasOutdatedAxeInInventory(activeAxeName)) {
                Rs2Bank.closeBank();
            }
            return false;
        }
        return Rs2Equipment.isWearing((String[])new String[]{activeAxeName}) || Rs2Inventory.hasItem((String[])new String[]{activeAxeName});
    }

    private void depositOutdatedAxes(String desiredAxeName) {
        for (String axeName : AXE_NAMES) {
            if (axeName.equalsIgnoreCase(desiredAxeName)) continue;
            if (!Rs2Inventory.hasItem((String[])new String[]{axeName})) continue;
            Rs2Bank.depositAll((String)axeName);
        }
    }

    private boolean hasOutdatedAxeInInventory(String desiredAxeName) {
        for (String axeName : AXE_NAMES) {
            if (axeName.equalsIgnoreCase(desiredAxeName) || !Rs2Inventory.hasItem((String[])new String[]{axeName})) continue;
            return true;
        }
        return false;
    }

    private String resolveDesiredAxe(WoodCuttingReq woodCuttingReq) {
        return woodCuttingReq.getDisplayName();
    }

    private String resolveBestOwnedAxeName(String targetAxeName) {
        int targetIndex = AXE_NAMES.indexOf(targetAxeName);
        if (targetIndex < 0) {
            return null;
        }
        for (int index = targetIndex; index >= 0; --index) {
            String axeName = AXE_NAMES.get(index);
            if (!Rs2Equipment.isWearing((String[])new String[]{axeName}) && !Rs2Inventory.hasItem((String[])new String[]{axeName}) && (!Rs2Bank.isOpen() || Rs2Bank.count((String)axeName) <= 0)) continue;
            return axeName;
        }
        return null;
    }

    private WoodCuttingReq resolveWoodcuttingReq(String axeName) {
        if (axeName == null) {
            return null;
        }
        return Arrays.stream(WoodCuttingReq.values()).filter(req -> axeName.equalsIgnoreCase(req.getDisplayName())).findFirst().orElse(null);
    }

    private boolean hasAnyAxeEquippedOrInInventory() {
        for (String axeName : AXE_NAMES) {
            if (!Rs2Equipment.isWearing((String[])new String[]{axeName}) && !Rs2Inventory.hasItem((String[])new String[]{axeName})) continue;
            return true;
        }
        return false;
    }

    private boolean canEquipDesiredAxe(WoodCuttingReq woodCuttingReq, int attackLevel) {
        AxeEquip equipRequirement = AxeEquip.valueOf(woodCuttingReq.name());
        return attackLevel >= equipRequirement.getRequiredAttackLevel();
    }

    private boolean ensureInTargetArea() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            this.debug("Cannot walk to woodcutting area; player location is null");
            return false;
        }
        if (this.targetArea.contains(playerLocation)) {
            this.clearTargetAreaWalkIfNeeded();
            return true;
        }
        if (Rs2Player.isMoving()) {
            KspTaskDebug.throttled(log, this.debugLogging, "Woodcutting", "walk-moving", 3_000L,
                    "waiting for walker | player={} targetArea={} walkerTarget={}",
                    playerLocation,
                    this.targetArea.getDisplayName(),
                    Rs2Walker.getCurrentTarget());
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastWebWalkAtMs < WEB_WALK_COOLDOWN_MS) {
            return false;
        }
        WorldPoint walkTarget = this.targetArea.getRandomPoint();
        this.lastWebWalkAtMs = now;
        this.walkingToTargetArea = true;
        Microbot.status = "Walking to woodcutting area";
        this.debug("Walking to woodcutting area | player={} target={} area={}", playerLocation, walkTarget, this.targetArea.getDisplayName());
        Rs2Walker.walkTo(walkTarget, 3);
        return false;
    }

    private void clearTargetAreaWalkIfNeeded() {
        WorldPoint walkerTarget = Rs2Walker.getCurrentTarget();
        if (!this.walkingToTargetArea && (walkerTarget == null || !this.targetArea.contains(walkerTarget))) {
            return;
        }
        Rs2Walker.clearWalkingRoute("ksp_account_builder_woodcutting_reached_area");
        this.walkingToTargetArea = false;
    }

    private WorldPoint getAreaCenter() {
        int centerX = (this.targetArea.getSouthWest().getX() + this.targetArea.getNorthEast().getX()) / 2;
        int centerY = (this.targetArea.getSouthWest().getY() + this.targetArea.getNorthEast().getY()) / 2;
        int plane = this.targetArea.getSouthWest().getPlane();
        return new WorldPoint(centerX, centerY, plane);
    }

    private void ensureInventoryTabOpen() {
        if (Rs2Tab.getCurrentTab() != InterfaceTab.INVENTORY) {
            Rs2Tab.switchTo((InterfaceTab)InterfaceTab.INVENTORY);
            WoodCuttingScript.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY, (int)1200);
        }
    }

    private void bankLogsOnly(int woodcuttingLevel) {
        this.ensureInventoryTabOpen();
        if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Bank.openBank()) {
            return;
        }
        if (Rs2Bank.isOpen()) {
            String axeToKeep = this.resolveInventoryAxeToKeep(woodcuttingLevel);
            if (axeToKeep != null) {
                Rs2Bank.depositAllExcept((String[])new String[]{axeToKeep});
            } else {
                Rs2Bank.depositAll();
            }
            WoodCuttingScript.sleep((int)300);
            Rs2Bank.closeBank();
        }
    }

    private String resolveInventoryAxeToKeep(int woodcuttingLevel) {
        String targetAxeName = this.resolveDesiredAxe(WoodCuttingReq.bestForWoodcuttingLevel(woodcuttingLevel));
        int targetIndex = AXE_NAMES.indexOf(targetAxeName);

        if (targetIndex < 0) {
            return null;
        }

        for (int index = targetIndex; index >= 0; index--) {
            String axeName = AXE_NAMES.get(index);
            if (Rs2Equipment.isWearing((String[])new String[]{axeName})) {
                return null;
            }
        }

        for (int index = targetIndex; index >= 0; index--) {
            String axeName = AXE_NAMES.get(index);
            if (Rs2Inventory.hasItem((String[])new String[]{axeName})) {
                return axeName;
            }
        }

        return null;
    }

    private void chopForCurrentLevel(int woodcuttingLevel) {
        if (!this.isIdleInTargetArea()) {
            KspTaskDebug.throttled(log, this.debugLogging, "Woodcutting", "not-idle", 2_000L,
                    "waiting for idle before chopping | player={} moving={} animating={} interacting={} area={}",
                    Rs2Player.getWorldLocation(),
                    Rs2Player.isMoving(),
                    Rs2Player.isAnimating(),
                    Rs2Player.isInteracting(),
                    this.targetArea.getDisplayName());
            return;
        }
        if (System.currentTimeMillis() - this.lastObjectInteractionAtMs < OBJECT_INTERACTION_COOLDOWN_MS) {
            KspTaskDebug.throttled(log, this.debugLogging, "Woodcutting", "interaction-cooldown", 2_000L,
                    "interaction cooldown active | elapsed={}ms cooldown={}ms",
                    System.currentTimeMillis() - this.lastObjectInteractionAtMs,
                    OBJECT_INTERACTION_COOLDOWN_MS);
            return;
        }
        Rs2TileObjectModel targetTree = this.findNearestTreeInTargetArea(woodcuttingLevel);
        if (targetTree == null) {
            Microbot.status = "No reachable tree found";
            return;
        }
        if (!this.isIdleInTargetArea()) {
            this.debug("Tree candidate found but player stopped being idle | tree={} id={} loc={} moving={} animating={} interacting={}",
                    targetTree.getName(),
                    targetTree.getId(),
                    targetTree.getWorldLocation(),
                    Rs2Player.isMoving(),
                    Rs2Player.isAnimating(),
                    Rs2Player.isInteracting());
            return;
        }
        this.lastObjectInteractionAtMs = System.currentTimeMillis();
        this.debug("Attempting tree interaction | expected={} objectName={} id={} loc={} reachable={} player={} distance={} hasChopAction={} insideArea={}",
                this.getTargetTreeLevel(woodcuttingLevel).getDisplayName(),
                targetTree.getName(),
                targetTree.getId(),
                targetTree.getWorldLocation(),
                targetTree.isReachable(),
                Rs2Player.getWorldLocation(),
                Rs2Player.getWorldLocation() != null ? Rs2Player.getWorldLocation().distanceTo(targetTree.getWorldLocation()) : -1,
                hasObjectAction(targetTree, "Chop down"),
                this.targetArea.contains(targetTree.getWorldLocation()));
        boolean interactionStarted = targetTree.click("Chop down");
        this.debug("Tree interaction result | clicked={} objectName={} id={} loc={} player={} moving={} animating={} interacting={}",
                interactionStarted,
                targetTree.getName(),
                targetTree.getId(),
                targetTree.getWorldLocation(),
                Rs2Player.getWorldLocation(),
                Rs2Player.isMoving(),
                Rs2Player.isAnimating(),
                Rs2Player.isInteracting());
        if (interactionStarted) {
            Microbot.status = "Chopping " + targetTree.getName();
            boolean activityStarted = WoodCuttingScript.sleepUntil(() -> Rs2Player.isAnimating() || Rs2Player.isInteracting(), (int)1200);
            this.debug("Tree post-click wait | activityStarted={} moving={} animating={} interacting={} player={}",
                    activityStarted,
                    Rs2Player.isMoving(),
                    Rs2Player.isAnimating(),
                    Rs2Player.isInteracting(),
                    Rs2Player.getWorldLocation());
        }
        else
        {
            this.debug("Tree interaction was not accepted by tile object API | objectName={} id={} loc={}",
                    targetTree.getName(),
                    targetTree.getId(),
                    targetTree.getWorldLocation());
        }
    }

    private boolean isIdleInTargetArea() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        return playerLocation != null
                && this.targetArea.contains(playerLocation)
                && !Rs2Player.isMoving()
                && !Rs2Player.isAnimating()
                && !Rs2Player.isInteracting();
    }

    private Rs2TileObjectModel findNearestTreeInTargetArea(int woodcuttingLevel) {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        WorldPoint searchCenter = this.getAreaCenter();
        if (playerLocation == null || searchCenter == null) {
            return null;
        }

        TreeLevel treeLevel = this.getTargetTreeLevel(woodcuttingLevel);
        int searchRadius = this.getAreaSearchRadius() + TREE_SEARCH_PADDING_TILES;
        Rs2TileObjectModel tree = this.findMatchingTree(searchCenter, searchRadius, treeLevel, true);
        if (tree != null) {
            this.debug("Tree candidate selected | expected={} objectName={} id={} loc={} reachable={} insideArea={} searchCenter={} radius={}",
                    treeLevel.getDisplayName(),
                    tree.getName(),
                    tree.getId(),
                    tree.getWorldLocation(),
                    tree.isReachable(),
                    this.targetArea.contains(tree.getWorldLocation()),
                    searchCenter,
                    searchRadius);
            return tree;
        }

        tree = this.findMatchingTree(searchCenter, searchRadius, treeLevel, false);

        if (tree == null) {
            this.debug("No reachable {} found in or near {}", treeLevel.getObjectCompositionName(), this.targetArea.getDisplayName());
        }

        return tree;
    }

    private Rs2TileObjectModel findMatchingTree(WorldPoint searchCenter, int searchRadius, TreeLevel treeLevel, boolean mustBeInsideArea) {
        return Microbot.getRs2TileObjectCache().query()
                .fromWorldView()
                .within(searchCenter, searchRadius)
                .where(candidate -> candidate != null
                        && candidate.getWorldLocation() != null
                        && (mustBeInsideArea
                                ? this.targetArea.contains(candidate.getWorldLocation())
                                : this.isNearTargetArea(candidate.getWorldLocation(), OUT_OF_AREA_TREE_FALLBACK_RADIUS))
                        && this.isTargetTree(candidate, treeLevel)
                        && candidate.isReachable()
                        && hasObjectAction(candidate, "Chop down"))
                .nearestOnClientThread();
    }

    private boolean isNearTargetArea(WorldPoint point, int radius) {
        if (point == null || point.getPlane() != this.targetArea.getSouthWest().getPlane()) {
            return false;
        }
        if (this.targetArea.contains(point)) {
            return true;
        }
        int minX = this.targetArea.getSouthWest().getX() - radius;
        int maxX = this.targetArea.getNorthEast().getX() + radius;
        int minY = this.targetArea.getSouthWest().getY() - radius;
        int maxY = this.targetArea.getNorthEast().getY() + radius;
        return point.getX() >= minX
                && point.getX() <= maxX
                && point.getY() >= minY
                && point.getY() <= maxY;
    }

    private boolean isTargetTree(Rs2TileObjectModel tree, TreeLevel treeLevel) {
        if (tree == null || treeLevel == null || tree.getName() == null) {
            return false;
        }

        String treeName = tree.getName().toLowerCase(Locale.ENGLISH);
        String objectName = treeLevel.getObjectCompositionName().toLowerCase(Locale.ENGLISH);
        String displayName = treeLevel.getDisplayName().toLowerCase(Locale.ENGLISH);

        if (treeLevel == TreeLevel.TREE) {
            return treeName.equals(objectName) || treeName.equals(displayName);
        }

        return treeName.equals(objectName)
                || treeName.equals(displayName)
                || treeName.equals(displayName + " tree");
    }

    private static boolean hasObjectAction(Rs2TileObjectModel object, String expectedAction) {
        if (object == null || expectedAction == null) {
            return false;
        }

        ObjectComposition composition = object.getObjectComposition();
        if (composition == null || composition.getActions() == null) {
            return false;
        }

        for (String rawAction : composition.getActions()) {
            if (rawAction == null) {
                continue;
            }
            String action = Rs2UiHelper.stripColTags(rawAction);
            if (expectedAction.equalsIgnoreCase(action)) {
                return true;
            }
        }

        return false;
    }

    private int getAreaSearchRadius() {
        int width = Math.abs(this.targetArea.getNorthEast().getX() - this.targetArea.getSouthWest().getX());
        int height = Math.abs(this.targetArea.getNorthEast().getY() - this.targetArea.getSouthWest().getY());
        return Math.max(width, height) + 2;
    }

    private TreeAreas resolveTargetArea(int woodcuttingLevel) {
        TreeLevel treeLevel = this.getTargetTreeLevel(woodcuttingLevel);
        if (treeLevel == TreeLevel.YEW) {
            return TreeAreas.YEW_TREE_VARROCK_PALACE;
        }
        if (treeLevel == TreeLevel.WILLOW) {
            return TreeAreas.WILLOW_TREES_DRAYNOR;
        }
        if (treeLevel == TreeLevel.OAK) {
            return TreeAreas.OAK_TREE_DRAYNOR;
        }
        return TreeAreas.REGULAR_TREE_VARROCK_WEST;
    }

    private TreeLevel getTargetTreeLevel(int woodcuttingLevel) {
        if (this.startingTargetTreeInitialized && this.randomMidTierTree != null) {
            return this.randomMidTierTree;
        }
        if (woodcuttingLevel >= TreeLevel.YEW.getRequiredWoodcuttingLevel()) {
            List<TreeLevel> levelSixtyOptions = Arrays.asList(TreeLevel.WILLOW, TreeLevel.YEW);
            return levelSixtyOptions.get(ThreadLocalRandom.current().nextInt(levelSixtyOptions.size()));
        }
        if (woodcuttingLevel >= TreeLevel.WILLOW.getRequiredWoodcuttingLevel()) {
            return TreeLevel.WILLOW;
        }
        if (woodcuttingLevel >= TreeLevel.OAK.getRequiredWoodcuttingLevel()) {
            return TreeLevel.OAK;
        }
        return TreeLevel.TREE;
    }

    private boolean shouldRandomizeMidTierTree(int woodcuttingLevel) {
        return woodcuttingLevel >= TreeLevel.WILLOW.getRequiredWoodcuttingLevel() && woodcuttingLevel < 60;
    }

    private void initializeStartingTargetTree(int woodcuttingLevel) {
        if (this.startingTargetTreeInitialized) {
            return;
        }
        if (this.shouldRandomizeMidTierTree(woodcuttingLevel)) {
            List<TreeLevel> randomOptions = Arrays.asList(TreeLevel.OAK, TreeLevel.WILLOW);
            this.randomMidTierTree = randomOptions.get(ThreadLocalRandom.current().nextInt(randomOptions.size()));
            this.debug("Selected starting woodcutting tree {} for woodcutting level {}", this.randomMidTierTree.getDisplayName(), woodcuttingLevel);
        } else {
            this.randomMidTierTree = null;
        }
        this.startingTargetTreeInitialized = true;
    }

    private void debug(String message, Object ... args) {
        if (this.debugLogging) {
            KspTaskDebug.info(log, true, "Woodcutting", message, args);
        }
    }

    public void shutdown() {
        this.startingTargetTreeInitialized = false;
        this.randomMidTierTree = null;
        this.lastWebWalkAtMs = 0L;
        this.lastObjectInteractionAtMs = 0L;
        this.walkingToTargetArea = false;
        super.shutdown();
    }

    public TreeAreas getTargetArea() {
        return this.targetArea;
    }
}
