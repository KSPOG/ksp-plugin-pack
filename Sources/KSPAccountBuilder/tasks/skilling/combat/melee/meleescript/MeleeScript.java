/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Singleton
 *  net.runelite.api.Actor
 *  net.runelite.api.Player
 *  net.runelite.api.Quest
 *  net.runelite.api.QuestState
 *  net.runelite.api.Skill
 *  net.runelite.api.coords.WorldPoint
 *  net.runelite.api.widgets.WidgetInfo
 *  net.runelite.client.plugins.grounditems.GroundItem
 *  net.runelite.client.plugins.microbot.Microbot
 *  net.runelite.client.plugins.microbot.Script
 *  net.runelite.client.plugins.microbot.api.npc.Rs2NpcQueryable
 *  net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel
 *  net.runelite.client.plugins.microbot.globval.enums.InterfaceTab
 *  net.runelite.client.plugins.microbot.util.bank.Rs2Bank
 *  net.runelite.client.plugins.microbot.util.combat.Rs2Combat
 *  net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest
 *  net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange
 *  net.runelite.client.plugins.microbot.util.grounditem.LootingParameters
 *  net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem
 *  net.runelite.client.plugins.microbot.util.grounditem.Rs2LootEngine
 *  net.runelite.client.plugins.microbot.util.grounditem.Rs2LootEngine$Builder
 *  net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory
 *  net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel
 *  net.runelite.client.plugins.microbot.util.player.Rs2Player
 *  net.runelite.client.plugins.microbot.util.tabs.Rs2Tab
 *  net.runelite.client.plugins.microbot.util.walker.Rs2Walker
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.meleescript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspTaskDebug;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.areas.CombatAreas;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.equipment.amulet.Amulet;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.equipment.armour.Armour;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.equipment.cape.Capes;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.equipment.weapon.Weapons;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.food.Food;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.loot.alkharidwarriotloot.WarriorLoot;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.loot.cowloot.CowLoot;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.loot.hillgiantloot.HillGiantLoot;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.loot.mossgiantloot.MossGiantLoot;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.meleescript.CombatState;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.combat.melee.npc.NPC;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grandexchange.models.GrandExchangeOfferDetails;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MeleeScript
extends Script {
    private static final Logger log = LoggerFactory.getLogger(MeleeScript.class);
    private static final int LOOP_DELAY_MS = 600;
    private static final int WEB_WALK_COOLDOWN_MS = 3000;
    private static final int BUY_WAIT_TIMEOUT_MS = 20000;
    private static final int GE_OFFER_INPUT_DELAY_MS = 900;
    private static final int TARGET_FOOD_COUNT = 5;
    private static final int FOOD_PURCHASE_QUANTITY = 20;
    private static final int LOOT_RADIUS = 12;
    private static final String COINS_NAME = "Coins";
    private String status = "Idle";
    private CombatState state = CombatState.PREPARING;
    private boolean debugLogging;
    private long lastWebWalkAtMs;
    private WorldPoint lastWalkTarget;
    private final List<PurchaseRequest> pendingPurchases = new CopyOnWriteArrayList<PurchaseRequest>();
    private final List<PurchaseRequest> activePurchases = new CopyOnWriteArrayList<PurchaseRequest>();

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public boolean run() {
        this.shutdown();
        this.status = "Starting melee training";
        this.state = CombatState.PREPARING;
        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run() || !Microbot.isLoggedIn()) {
                    return;
                }
                TrainingStage stage = this.resolveTrainingStage();
                KspTaskDebug.throttled(log, this.debugLogging, "Melee", "loop", 5_000L,
                        "loop | state={} status={} area={} npc={} player={} moving={} animating={} interacting={} inCombat={} hp={}/{} pendingBuys={} activeBuys={} bankOpen={} geOpen={}",
                        this.state,
                        this.status,
                        stage.area.getDisplayName(),
                        stage.primaryNpc.getDisplayName(),
                        Rs2Player.getWorldLocation(),
                        Rs2Player.isMoving(),
                        Rs2Player.isAnimating(),
                        Rs2Player.isInteracting(),
                        Rs2Combat.inCombat(),
                        Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS),
                        Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS),
                        this.pendingPurchases.size(),
                        this.activePurchases.size(),
                        Rs2Bank.isOpen(),
                        Rs2GrandExchange.isOpen());
                if (this.handlePendingGrandExchangePurchases(stage)) {
                    this.state = CombatState.BUYING_UPGRADES;
                    return;
                }
                if (this.handleHealing()) {
                    this.state = CombatState.PREPARING;
                    return;
                }
                if (this.lootOwnDrops(stage)) {
                    this.state = CombatState.LOOTING;
                    return;
                }
                if (this.buryBonesInInventory()) {
                    this.state = CombatState.PREPARING;
                    return;
                }
                if (this.shouldBank(stage)) {
                    this.state = CombatState.BANKING;
                    this.handleBanking(stage);
                    return;
                }
                if (this.equipInventoryUpgrades()) {
                    this.state = CombatState.EQUIPPING;
                    return;
                }
                if (!this.hasCurrentTaskWeaponEquippedOrInInventory()) {
                    this.state = CombatState.PREPARING;
                    this.status = "Waiting for melee weapon";
                    return;
                }
                if (!this.ensureInTargetArea(stage.area)) {
                    this.state = CombatState.WALKING_TO_AREA;
                    return;
                }
                if (this.hasLootNearby(stage)) {
                    this.state = CombatState.LOOTING;
                    this.lootOwnDrops(stage);
                    return;
                }
                if (Rs2Player.isMoving()) {
                    this.state = CombatState.WALKING_TO_AREA;
                    this.status = "Moving to " + stage.primaryNpc.getDisplayName();
                    return;
                }
                if (Rs2Player.isAnimating() || Rs2Combat.inCombat()) {
                    this.state = CombatState.FIGHTING;
                    this.handleHealing();
                    this.ensureBalancedAttackStyle();
                    if (Rs2Combat.inCombat() || Rs2Player.isAnimating()) {
                        this.status = "Fighting " + stage.primaryNpc.getDisplayName();
                    }
                    return;
                }
                if (this.ensureBalancedAttackStyle()) {
                    this.state = CombatState.PREPARING;
                    return;
                }
                this.state = CombatState.FIGHTING;
                this.attackTarget(stage);
            }
            catch (Exception ex) {
                Microbot.logStackTrace((String)((Object)((Object)this)).getClass().getSimpleName(), (Exception)ex);
            }
        }, 0L, 600L, TimeUnit.MILLISECONDS);
        return true;
    }

    private TrainingStage resolveTrainingStage() {
        int attackLevel = this.getSkillLevel(Skill.ATTACK);
        int strengthLevel = this.getSkillLevel(Skill.STRENGTH);
        int defenceLevel = this.getSkillLevel(Skill.DEFENCE);
        if (attackLevel < 20 || strengthLevel < 20 || defenceLevel < 20) {
            return new TrainingStage(CombatAreas.COWPEN, NPC.COW, NPC.COW_CALF, (String[])Arrays.stream(CowLoot.values()).map(CowLoot::getDisplayName).toArray(String[]::new));
        }
        if (attackLevel < 30 || strengthLevel < 30 || defenceLevel < 30) {
            return new TrainingStage(CombatAreas.AL_KHARID_WARRIOR, NPC.AL_KHARID_WARRIOR, null, (String[])Arrays.stream(WarriorLoot.values()).map(WarriorLoot::getDisplayName).toArray(String[]::new));
        }
        if (attackLevel < 40 || strengthLevel < 40 || defenceLevel < 40) {
            return new TrainingStage(CombatAreas.HILL_GIANTS, NPC.HILL_GIANT, null, (String[])Arrays.stream(HillGiantLoot.values()).map(HillGiantLoot::getDisplayName).toArray(String[]::new));
        }
        return new TrainingStage(CombatAreas.MOSS_GIANTS, NPC.MOSS_GIANT, null, (String[])Arrays.stream(MossGiantLoot.values()).map(MossGiantLoot::getDisplayName).toArray(String[]::new));
    }

    private boolean handleHealing() {
        int maxHp;
        Food bestInventoryFood = this.getBestFoodInInventory();
        if (bestInventoryFood == null) {
            return false;
        }
        int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        boolean shouldEat = this.shouldHealNow(currentHp, maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS));
        if (!shouldEat) {
            return false;
        }
        this.status = "Eating " + bestInventoryFood.getDisplayName() + " at " + currentHp + "/" + maxHp + " hp";
        if (Rs2Inventory.interact((String)bestInventoryFood.getDisplayName(), (String)"Eat")) {
            MeleeScript.sleepUntil(() -> Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) > currentHp, (int)1800);
            return true;
        }
        return false;
    }

    private boolean buryBonesInInventory() {
        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) {
            return false;
        }
        List<Rs2ItemModel> bones = Rs2Inventory.getBones();
        if (bones == null || bones.isEmpty()) {
            return false;
        }
        this.status = "Burying bones";
        for (Rs2ItemModel bone : bones) {
            if (bone == null || bone.getName() == null || !Rs2Inventory.interact((Rs2ItemModel)bone, (String)"Bury")) continue;
            MeleeScript.sleep((int)250, (int)400);
            return true;
        }
        return false;
    }

    private boolean shouldBank(TrainingStage stage) {
        if (!this.pendingPurchases.isEmpty() || !this.activePurchases.isEmpty()) {
            return false;
        }
        if (this.hasInventoryEquipmentToEquip()) {
            return false;
        }
        int foodCount = this.getFoodCountInInventory();
        if (foodCount <= 0) {
            return this.shouldBankForNoFood(stage);
        }
        if (Rs2Inventory.isFull() && this.projectedFreeSlotsAfterBury() <= 0) {
            return true;
        }
        GearPlan gearPlan = this.buildGearPlan();
        return !gearPlan.missingItems.isEmpty();
    }

    private void handleBanking(TrainingStage stage) {
        this.status = "Banking for " + stage.primaryNpc.getDisplayName();
        if (!Rs2Bank.isOpen()) {
            if (!Rs2Bank.walkToBankAndUseBank() && !Rs2Bank.openBank()) {
                return;
            }
            MeleeScript.sleepUntil(Rs2Bank::isOpen, (int)3000);
            return;
        }
        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            MeleeScript.sleepUntil(Rs2Inventory::isEmpty, (int)3000);
        }
        GearPlan gearPlan = this.buildGearPlan();
        ArrayList<PurchaseRequest> purchases = new ArrayList<PurchaseRequest>();
        for (String desiredItem : gearPlan.desiredItems) {
            if (desiredItem == null || Rs2Equipment.isWearing((String[])new String[]{desiredItem})) continue;
            if (Rs2Bank.count((String)desiredItem) > 0) {
                Rs2Bank.withdrawX((String)desiredItem, (int)1);
                MeleeScript.sleepUntil(() -> Rs2Inventory.hasItem((String[])new String[]{desiredItem}), (int)2000);
                continue;
            }
            if (this.hasItemAnywhere(desiredItem)) continue;
            purchases.add(new PurchaseRequest(desiredItem, 1));
        }
        Food bankFood = this.getBestFoodAvailableInBank();
        if (bankFood != null) {
            Rs2Bank.withdrawX((String)bankFood.getDisplayName(), (int)5);
            MeleeScript.sleepUntil(() -> Rs2Inventory.count((String)bankFood.getDisplayName()) >= 1, (int)2000);
        } else {
            purchases.add(new PurchaseRequest(this.getBestOverallFood().getDisplayName(), 20));
        }
        if (!purchases.isEmpty() && Rs2Inventory.count((String)COINS_NAME) <= 0 && Rs2Bank.count((String)COINS_NAME) > 0) {
            Rs2Bank.withdrawAll((String)COINS_NAME);
            MeleeScript.sleepUntil(() -> Rs2Inventory.itemQuantity((int)995) > 0, (int)2000);
        }
        Rs2Bank.closeBank();
        MeleeScript.sleepUntil(() -> !Rs2Bank.isOpen(), (int)2000);
        this.pendingPurchases.clear();
        this.pendingPurchases.addAll(purchases.stream().filter(p -> p != null && !this.hasItemAnywhere(p.itemName)).collect(Collectors.toList()));
    }

    private boolean equipInventoryUpgrades() {
        GearPlan gearPlan = this.buildGearPlan();
        for (String desiredItem : gearPlan.desiredItems) {
            if (desiredItem == null || Rs2Equipment.isWearing((String[])new String[]{desiredItem}) || !Rs2Inventory.hasItem((String[])new String[]{desiredItem})) continue;
            this.status = "Equipping " + desiredItem;
            Rs2Inventory.wield((String[])new String[]{desiredItem});
            MeleeScript.sleepUntil(() -> Rs2Equipment.isWearing((String[])new String[]{desiredItem}), (int)2000);
            return true;
        }
        return false;
    }

    private boolean handlePendingGrandExchangePurchases(TrainingStage stage) {
        this.clearOwnedPurchases();
        if (this.pendingPurchases.isEmpty() && this.activePurchases.isEmpty()) {
            return false;
        }
        this.status = "Buying upgrades for " + stage.primaryNpc.getDisplayName();
        if (!Rs2GrandExchange.walkToGrandExchange()) {
            this.debug("Walking to Grand Exchange for melee purchases | pending={} active={} player={}",
                    this.pendingPurchases.size(),
                    this.activePurchases.size(),
                    Rs2Player.getWorldLocation());
            return true;
        }
        if (!Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.openExchange();
            MeleeScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
            return true;
        }
        this.syncActivePurchasesFromOpenOffers();
        if (Rs2GrandExchange.hasBoughtOffer() || Rs2GrandExchange.hasSoldOffer()) {
            this.markCompletedActivePurchases();
            Rs2GrandExchange.collectAllToBank();
            MeleeScript.sleepUntil(() -> !Rs2GrandExchange.hasBoughtOffer() && !Rs2GrandExchange.hasSoldOffer(), (int)5000);
            this.clearOwnedPurchases();
            this.closeGrandExchangeIfPurchasesComplete();
            return true;
        }
        this.processAvailableGrandExchangeSlots();
        this.closeGrandExchangeIfPurchasesComplete();
        return true;
    }

    private void processAvailableGrandExchangeSlots() {
        int availableSlots = Rs2GrandExchange.getAvailableSlotsCount();
        if (availableSlots <= 0) {
            this.status = "Waiting for melee GE offers";
            this.debug("No GE slots available for melee purchases | pending={} active={}", this.pendingPurchases.size(), this.activePurchases.size());
            return;
        }
        this.status = "Placing melee GE offers";
        while (availableSlots > 0 && !this.pendingPurchases.isEmpty()) {
            PurchaseRequest purchase = this.pendingPurchases.get(0);
            if (this.hasItemAnywhere(purchase.itemName)) {
                this.pendingPurchases.remove(purchase);
                continue;
            }
            if (this.isPurchaseActive(purchase.itemName)) {
                this.pendingPurchases.remove(purchase);
                continue;
            }
            if (!this.placeGrandExchangeBuyOffer(purchase)) {
                break;
            }
            this.pendingPurchases.remove(purchase);
            this.activePurchases.add(purchase);
            availableSlots = Rs2GrandExchange.getAvailableSlotsCount();
        }
        if (!this.pendingPurchases.isEmpty() || !this.activePurchases.isEmpty()) {
            this.status = "Waiting for melee GE offers";
        }
    }

    private boolean placeGrandExchangeBuyOffer(PurchaseRequest purchase) {
        if (purchase == null || purchase.itemName == null) {
            return false;
        }
        GrandExchangeRequest request = GrandExchangeRequest.builder()
                .action(GrandExchangeAction.BUY)
                .itemName(purchase.itemName)
                .quantity(purchase.quantity)
                .percent(10)
                .closeAfterCompletion(false)
                .build();
        this.waitForGrandExchangeOfferInput();
        this.debug("Placing melee GE buy offer | item={} qty={} percent=10 availableSlots={}", purchase.itemName, purchase.quantity, Rs2GrandExchange.getAvailableSlotsCount());
        if (!Rs2GrandExchange.processOffer((GrandExchangeRequest)request)) {
            this.debug("Failed to place melee GE buy offer | item={} qty={}", purchase.itemName, purchase.quantity);
            return false;
        }
        MeleeScript.sleepUntil(() -> !Rs2GrandExchange.isOfferScreenOpen(), (int)2000);
        return true;
    }

    private void syncActivePurchasesFromOpenOffers() {
        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details == null || details.isSelling() || details.getItemName() == null) {
                continue;
            }
            PurchaseRequest pending = this.getPendingPurchase(details.getItemName());
            if (pending == null || this.isPurchaseActive(details.getItemName())) {
                continue;
            }
            this.pendingPurchases.remove(pending);
            this.activePurchases.add(pending);
        }
    }

    private void markCompletedActivePurchases() {
        boolean markedAny = false;
        for (GrandExchangeOfferDetails details : Rs2GrandExchange.getCompletedOffers().values()) {
            if (details == null || details.isSelling() || details.getItemName() == null) {
                continue;
            }
            markedAny = this.removeActivePurchase(details.getItemName()) || markedAny;
        }
        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details == null || details.isSelling() || !details.isCompleted() || details.getItemName() == null) {
                continue;
            }
            markedAny = this.removeActivePurchase(details.getItemName()) || markedAny;
        }
        if (!markedAny && Rs2GrandExchange.hasBoughtOffer() && this.activePurchases.size() == 1) {
            this.activePurchases.remove(0);
        }
    }

    private void clearOwnedPurchases() {
        for (PurchaseRequest purchase : this.pendingPurchases) {
            if (this.hasItemAnywhere(purchase.itemName)) {
                this.pendingPurchases.remove(purchase);
            }
        }
        for (PurchaseRequest purchase : this.activePurchases) {
            if (this.hasItemAnywhere(purchase.itemName)) {
                this.activePurchases.remove(purchase);
            }
        }
    }

    private void closeGrandExchangeIfPurchasesComplete() {
        if (!this.pendingPurchases.isEmpty() || !this.activePurchases.isEmpty()) {
            return;
        }
        if (Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.collectAllToBank();
            MeleeScript.sleepUntil(() -> !Rs2GrandExchange.hasBoughtOffer() && !Rs2GrandExchange.hasSoldOffer(), (int)5000);
            Rs2GrandExchange.closeExchange();
        }
    }

    private PurchaseRequest getPendingPurchase(String itemName) {
        if (itemName == null) {
            return null;
        }
        for (PurchaseRequest purchase : this.pendingPurchases) {
            if (purchase != null && itemName.equalsIgnoreCase(purchase.itemName)) {
                return purchase;
            }
        }
        return null;
    }

    private boolean isPurchaseActive(String itemName) {
        if (itemName == null) {
            return false;
        }
        for (PurchaseRequest purchase : this.activePurchases) {
            if (purchase != null && itemName.equalsIgnoreCase(purchase.itemName)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeActivePurchase(String itemName) {
        if (itemName == null) {
            return false;
        }
        for (PurchaseRequest purchase : this.activePurchases) {
            if (purchase == null || !itemName.equalsIgnoreCase(purchase.itemName)) {
                continue;
            }
            this.activePurchases.remove(purchase);
            return true;
        }
        return false;
    }

    private void waitForGrandExchangeOfferInput() {
        MeleeScript.sleep(GE_OFFER_INPUT_DELAY_MS);
    }

    private boolean lootOwnDrops(TrainingStage stage) {
        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) {
            return false;
        }
        if (this.projectedFreeSlotsAfterBury() <= 0) {
            return false;
        }
        Rs2TileItemModel loot = this.findNearestLoot(stage);
        if (loot == null) {
            return false;
        }

        this.status = "Looting " + loot.getName();
        boolean clicked = loot.pickup();
        this.debug("Loot pickup interaction | clicked={} item={} id={} qty={} loc={} player={}",
                clicked,
                loot.getName(),
                loot.getId(),
                loot.getQuantity(),
                loot.getWorldLocation(),
                Rs2Player.getWorldLocation());
        if (clicked) {
            MeleeScript.sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isInteracting() || this.findNearestLoot(stage) == null, 1_200);
        }
        return clicked;
    }

    private boolean hasLootNearby(TrainingStage stage) {
        return this.findNearestLoot(stage) != null;
    }

    private Rs2TileItemModel findNearestLoot(TrainingStage stage) {
        if (stage == null || stage.lootNames == null) {
            return null;
        }
        HashSet<String> lootNames = Arrays.stream(stage.lootNames)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ENGLISH))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        if (lootNames.isEmpty()) {
            return null;
        }

        return Microbot.getRs2TileItemCache().query()
                .fromWorldView()
                .within(12)
                .where(item -> item.getName() != null
                        && lootNames.contains(item.getName().trim().toLowerCase(Locale.ENGLISH))
                        && item.isLootAble())
                .nearestOnClientThread(12);
    }

    private boolean ensureInTargetArea(CombatAreas targetArea) {
        WorldPoint walkTarget;
        if (targetArea.contains(Rs2Player.getWorldLocation())) {
            return true;
        }
        if (Rs2Player.isMoving()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastWebWalkAtMs < 3000L) {
            return false;
        }
        this.lastWebWalkAtMs = now;
        this.status = "Walking to " + targetArea.getDisplayName();
        this.lastWalkTarget = walkTarget = targetArea.getRandomPoint();
        Rs2Walker.walkTo(walkTarget, 2);
        return false;
    }

    private void attackTarget(TrainingStage stage) {
        List<Rs2NpcModel> candidates;
        Rs2NpcModel target;
        Player localPlayer = Microbot.getClient().getLocalPlayer();
        Actor currentInteracting = Rs2Player.getInteracting();
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (stage == null || localPlayer == null || playerLocation == null) {
            return;
        }
        ArrayList<String> npcNames = new ArrayList<String>();
        npcNames.add(stage.primaryNpc.getDisplayName().toLowerCase(Locale.ENGLISH));
        if (stage.secondaryNpc != null) {
            npcNames.add(stage.secondaryNpc.getDisplayName().toLowerCase(Locale.ENGLISH));
        }
        candidates = Microbot.getRs2NpcCache().query()
                .fromWorldView()
                .where(npc -> npc.getCombatLevel() > 0 && !npc.isDead())
                .where(npc -> {
                    String name = npc.getName();
                    return name != null && npcNames.contains(name.toLowerCase(Locale.ENGLISH));
                })
                .where(npc -> this.isNpcInTargetArea(npc, stage))
                .toListOnClientThread()
                .stream()
                .filter(npc -> this.canAttackNpc(npc, localPlayer))
                .sorted(Comparator.comparingInt((Rs2NpcModel npc) -> npc.getInteracting() == null ? 0 : 1)
                        .thenComparingInt(npc -> npc.getWorldLocation().distanceTo(playerLocation)))
                .collect(Collectors.toList());
        target = candidates.isEmpty() ? null : candidates.get(0);
        if (target == null) {
            this.status = "Waiting for " + stage.primaryNpc.getDisplayName();
            KspTaskDebug.throttled(log, this.debugLogging, "Melee", "no-target", 3_000L,
                    "no attack target found | primary={} secondary={} player={} area={}",
                    stage.primaryNpc.getDisplayName(),
                    stage.secondaryNpc != null ? stage.secondaryNpc.getDisplayName() : "none",
                    playerLocation,
                    stage.area.getDisplayName());
            return;
        }
        if (Objects.equals(currentInteracting, target.getNpc())) {
            this.status = "Fighting " + target.getName();
            return;
        }
        this.status = "Attacking " + target.getName();
        this.debug("Attempting npc attack | target={} id={} loc={} combatLevel={} reachable={} player={} distance={} targetInteracting={}",
                target.getName(),
                target.getId(),
                target.getWorldLocation(),
                target.getCombatLevel(),
                target.isReachable(),
                playerLocation,
                playerLocation.distanceTo(target.getWorldLocation()),
                target.getInteracting());
        target.click("Attack");
        boolean activityStarted = MeleeScript.sleepUntil(() -> Rs2Player.isInteracting() || Rs2Player.isAnimating(), (int)2000);
        this.debug("Npc attack post-click wait | activityStarted={} target={} player={} moving={} animating={} interacting={} inCombat={}",
                activityStarted,
                target.getName(),
                Rs2Player.getWorldLocation(),
                Rs2Player.isMoving(),
                Rs2Player.isAnimating(),
                Rs2Player.isInteracting(),
                Rs2Combat.inCombat());
    }

    private boolean isNpcInTargetArea(Rs2NpcModel npc, TrainingStage stage) {
        return npc != null && npc.getWorldLocation() != null && stage != null && stage.area.contains(npc.getWorldLocation());
    }

    private boolean canAttackNpc(Rs2NpcModel npc, Player localPlayer) {
        if (npc == null) {
            return false;
        }
        Actor interacting = npc.getInteracting();
        return interacting == null || Objects.equals(interacting, localPlayer);
    }

    private boolean ensureBalancedAttackStyle() {
        int attack = this.getSkillLevel(Skill.ATTACK);
        int strength = this.getSkillLevel(Skill.STRENGTH);
        int defence = this.getSkillLevel(Skill.DEFENCE);
        Skill targetSkill = Skill.ATTACK;
        WidgetInfo targetWidget = WidgetInfo.COMBAT_STYLE_ONE;
        int targetStyleIndex = 0;
        if (strength <= attack && strength <= defence) {
            targetSkill = Skill.STRENGTH;
            targetWidget = WidgetInfo.COMBAT_STYLE_TWO;
            targetStyleIndex = 1;
        } else if (defence <= attack && defence <= strength) {
            targetSkill = Skill.DEFENCE;
            targetWidget = WidgetInfo.COMBAT_STYLE_FOUR;
            targetStyleIndex = 3;
        }
        if (Microbot.getVarbitPlayerValue((int)43) == targetStyleIndex) {
            this.status = "Training " + targetSkill.getName().toLowerCase(Locale.ENGLISH);
            return false;
        }
        if (Rs2Tab.getCurrentTab() != InterfaceTab.COMBAT) {
            Rs2Tab.switchToCombatOptionsTab();
            MeleeScript.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.COMBAT, (int)1500);
        }
        Rs2Combat.setAttackStyle((WidgetInfo)targetWidget);
        this.status = "Training " + targetSkill.getName().toLowerCase(Locale.ENGLISH);
        return true;
    }

    private GearPlan buildGearPlan() {
        int attackLevel = this.getSkillLevel(Skill.ATTACK);
        int defenceLevel = this.getSkillLevel(Skill.DEFENCE);
        ArrayList<String> desired = new ArrayList<String>();
        this.addIfPresent(desired, this.getBestWeapon(attackLevel));
        this.addIfPresent(desired, this.getBestArmour(defenceLevel, "full helm"));
        this.addIfPresent(desired, this.getBestArmour(defenceLevel, "kiteshield"));
        this.addIfPresent(desired, this.getBestArmour(defenceLevel, "platelegs"));
        this.addIfPresent(desired, this.getBestBodyArmour(defenceLevel));
        this.addIfPresent(desired, Amulet.AMULET_OF_POWER.getDisplayName());
        this.addIfPresent(desired, Capes.TEAM_1_CAPE.getDisplayName());
        List<String> missingItems = desired.stream().filter(item -> !this.hasItemAnywhere((String)item)).collect(Collectors.toList());
        return new GearPlan(desired, missingItems);
    }

    private String getBestWeapon(int attackLevel) {
        return Arrays.stream(Weapons.values()).filter(weapon -> attackLevel >= weapon.getRequiredAttackLevel()).max(Comparator.comparingInt(Weapons::getRequiredAttackLevel).thenComparingInt(Enum::ordinal)).map(Weapons::getDisplayName).orElse(null);
    }

    private String getBestOwnedWeaponUpToCurrentLevel() {
        int attackLevel = this.getSkillLevel(Skill.ATTACK);
        return Arrays.stream(Weapons.values()).filter(weapon -> attackLevel >= weapon.getRequiredAttackLevel()).sorted(Comparator.comparingInt(Weapons::getRequiredAttackLevel).thenComparingInt(Enum::ordinal).reversed()).map(Weapons::getDisplayName).filter(this::hasWeaponEquippedOrInInventory).findFirst().orElse(null);
    }

    private boolean hasCurrentTaskWeaponEquippedOrInInventory() {
        return this.getBestOwnedWeaponUpToCurrentLevel() != null;
    }

    private boolean hasWeaponEquippedOrInInventory(String itemName) {
        return itemName != null && (Rs2Equipment.isWearing((String[])new String[]{itemName}) || Rs2Inventory.hasItem((String[])new String[]{itemName}));
    }

    private String getBestArmour(int defenceLevel, String marker) {
        return Arrays.stream(Armour.values()).filter(armour -> defenceLevel >= armour.getRequiredDefenceLevel()).filter(armour -> armour.getDisplayName().toLowerCase(Locale.ENGLISH).contains(marker)).max(Comparator.comparingInt(Armour::getRequiredDefenceLevel).thenComparingInt(Enum::ordinal)).map(Armour::getDisplayName).orElse(null);
    }

    private String getBestBodyArmour(int defenceLevel) {
        if (defenceLevel >= Armour.RUNE_CHAINBODY.getRequiredDefenceLevel() && !this.isDragonSlayerCompleted()) {
            return Armour.RUNE_CHAINBODY.getDisplayName();
        }
        String platebody = this.getBestArmour(defenceLevel, "platebody");
        if (platebody != null) {
            return platebody;
        }
        return Arrays.stream(Armour.values()).filter(armour -> defenceLevel >= armour.getRequiredDefenceLevel()).filter(armour -> armour.getDisplayName().toLowerCase(Locale.ENGLISH).contains("chainbody")).max(Comparator.comparingInt(Armour::getRequiredDefenceLevel).thenComparingInt(Enum::ordinal)).map(Armour::getDisplayName).orElse(null);
    }

    private boolean isDragonSlayerCompleted() {
        return Rs2Player.getQuestState((Quest)Quest.DRAGON_SLAYER_I) == QuestState.FINISHED;
    }

    private boolean hasInventoryEquipmentToEquip() {
        return this.buildGearPlan().desiredItems.stream().anyMatch(item -> item != null && Rs2Inventory.hasItem((String[])new String[]{item}) && !Rs2Equipment.isWearing((String[])new String[]{item}));
    }

    private boolean hasItemAnywhere(String itemName) {
        return itemName != null && (Rs2Equipment.isWearing((String[])new String[]{itemName}) || Rs2Inventory.hasItem((String[])new String[]{itemName}) || Rs2Inventory.hasItem((String)itemName, (boolean)true) || Rs2Bank.count((String)itemName) > 0);
    }

    private Food getBestFoodInInventory() {
        return Arrays.stream(Food.values()).filter(food -> Rs2Inventory.count((String)food.getDisplayName()) > 0).max(Comparator.comparingInt(Food::getHealAmount)).orElse(null);
    }

    private Food getBestFoodAvailableInBank() {
        return Arrays.stream(Food.values()).filter(food -> Rs2Bank.count((String)food.getDisplayName()) > 0).max(Comparator.comparingInt(Food::getHealAmount)).orElse(null);
    }

    private Food getBestOverallFood() {
        return Arrays.stream(Food.values()).max(Comparator.comparingInt(Food::getHealAmount)).orElse(Food.SALMON);
    }

    private int getFoodCountInInventory() {
        return Arrays.stream(Food.values()).mapToInt(food -> Rs2Inventory.count((String)food.getDisplayName())).sum();
    }

    private boolean shouldBankForNoFood(TrainingStage stage) {
        if (!Rs2Inventory.isEmpty()) {
            return true;
        }
        if (stage != null && !stage.area.contains(Rs2Player.getWorldLocation())) {
            return true;
        }
        int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
        return this.shouldHealNow(currentHp, maxHp);
    }

    private boolean shouldHealNow(int currentHp, int maxHp) {
        int healThreshold = (int)Math.ceil((double)maxHp * 0.28);
        return currentHp <= healThreshold;
    }

    private int projectedFreeSlotsAfterBury() {
        int emptySlots = Rs2Inventory.emptySlotCount();
        List<Rs2ItemModel> bones = Rs2Inventory.getBones();
        return emptySlots + (bones == null ? 0 : bones.size());
    }

    private int getSkillLevel(Skill skill) {
        return Microbot.getClient().getRealSkillLevel(skill);
    }

    public CombatAreas getTargetArea() {
        return this.resolveTrainingStage().area;
    }

    private void addIfPresent(List<String> items2, String item) {
        if (item != null) {
            items2.add(item);
        }
    }

    public void shutdown() {
        this.lastWebWalkAtMs = 0L;
        this.lastWalkTarget = null;
        this.pendingPurchases.clear();
        this.activePurchases.clear();
        this.state = CombatState.PREPARING;
        this.status = "Idle";
        super.shutdown();
    }

    private void debug(String message, Object ... args) {
        if (this.debugLogging) {
            KspTaskDebug.info(log, true, "Melee", message, args);
        }
    }

    public String getStatus() {
        return this.status;
    }

    public CombatState getState() {
        return this.state;
    }

    private static class PurchaseRequest {
        private final String itemName;
        private final int quantity;

        public PurchaseRequest(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    private static class GearPlan {
        private final List<String> desiredItems;
        private final List<String> missingItems;

        public GearPlan(List<String> desiredItems, List<String> missingItems) {
            this.desiredItems = desiredItems;
            this.missingItems = missingItems;
        }
    }

    private static class TrainingStage {
        private final CombatAreas area;
        private final NPC primaryNpc;
        private final NPC secondaryNpc;
        private final String[] lootNames;

        public TrainingStage(CombatAreas area, NPC primaryNpc, NPC secondaryNpc, String[] lootNames) {
            this.area = area;
            this.primaryNpc = primaryNpc;
            this.secondaryNpc = secondaryNpc;
            this.lootNames = lootNames;
        }
    }
}
