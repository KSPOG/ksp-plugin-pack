/*
 * Updated MeleeScript.java
 * GE offer timeout escalation uses Rs2GrandExchange.abortOffer(itemName, true)
 * and Rs2GrandExchange.findSlotForItem(itemName, false).
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
 *  net.runelite.client.plugins.microbot.Microbot
 *  net.runelite.client.plugins.microbot.Script
 *  net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel
 *  net.runelite.client.plugins.microbot.globval.enums.InterfaceTab
 *  net.runelite.client.plugins.microbot.util.bank.Rs2Bank
 *  net.runelite.client.plugins.microbot.util.combat.Rs2Combat
 *  net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest
 *  net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange
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
import java.util.Collections;
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
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspBankMode;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspTaskDebug;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspWalkerGuard;
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
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.gearea.GEArea;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MeleeScript
        extends Script {
    private static final Logger log = LoggerFactory.getLogger(MeleeScript.class);
    private static final int LOOP_DELAY_MS = 600;
    private static final int WEB_WALK_COOLDOWN_MS = 3000;
    private static final int BUY_WAIT_TIMEOUT_MS = 60_000;
    private static final int GE_PRICE_INCREASE_STEP_PERCENT = 10;
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
                if (this.ensureBalancedAttackStyle()) {
                    this.state = CombatState.PREPARING;
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
        Food bestInventoryFood = this.getBestFoodInInventory();
        if (bestInventoryFood == null) {
            return false;
        }

        int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);

        if (!this.shouldHealNow(currentHp, maxHp)) {
            return false;
        }

        this.status = "Eating " + bestInventoryFood.getDisplayName() + " at " + currentHp + "/" + maxHp + " hp";

        if (Rs2Inventory.interact(bestInventoryFood.getItemId(), "Eat")) {
            MeleeScript.sleepUntil(() -> Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS) > currentHp, 1800);
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
            MeleeScript.sleepUntil(Rs2Bank::isOpen, 3000);
            return;
        }

        if (!KspBankMode.ensureWithdrawAsItem()) {
            this.debug("Waiting for withdraw-as-item mode before melee banking withdrawals");
            return;
        }

        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            MeleeScript.sleepUntil(Rs2Inventory::isEmpty, 3000);
        }

        GearPlan gearPlan = this.buildGearPlan();
        ArrayList<PurchaseRequest> purchases = new ArrayList<PurchaseRequest>();

        for (String desiredItem : gearPlan.desiredItems) {
            if (desiredItem == null || Rs2Equipment.isWearing((String[]) new String[]{desiredItem})) {
                continue;
            }

            if (Rs2Bank.count(desiredItem) > 0) {
                Rs2Bank.withdrawX(desiredItem, 1);
                MeleeScript.sleepUntil(() -> Rs2Inventory.hasItem((String[]) new String[]{desiredItem}), 2000);
                continue;
            }

            if (this.hasItemAnywhere(desiredItem)) {
                continue;
            }

            purchases.add(new PurchaseRequest(desiredItem, 1));
        }

        Food bankFood = this.getBestFoodAvailableInBank();

        if (bankFood != null) {
            this.debug("Withdrawing combat food by item id | food={} id={} amount={}",
                    bankFood.getDisplayName(),
                    bankFood.getItemId(),
                    TARGET_FOOD_COUNT);

            Rs2Bank.withdrawX(bankFood.getItemId(), TARGET_FOOD_COUNT);
            MeleeScript.sleepUntil(() -> Rs2Inventory.itemQuantity(bankFood.getItemId()) >= 1, 2000);
        } else {
            Food foodToBuy = this.getBestOverallFood();
            purchases.add(new PurchaseRequest(foodToBuy.getDisplayName(), FOOD_PURCHASE_QUANTITY));
        }

        if (!purchases.isEmpty() && Rs2Inventory.count(COINS_NAME) <= 0 && Rs2Bank.count(COINS_NAME) > 0) {
            Rs2Bank.withdrawAll(COINS_NAME);
            MeleeScript.sleepUntil(() -> Rs2Inventory.itemQuantity(995) > 0, 2000);
        }

        Rs2Bank.closeBank();
        MeleeScript.sleepUntil(() -> !Rs2Bank.isOpen(), 2000);

        this.pendingPurchases.clear();
        this.pendingPurchases.addAll(
                purchases.stream()
                        .filter(p -> p != null && !this.hasItemAnywhere(p.itemName))
                        .collect(Collectors.toList())
        );
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
        GEArea grandExchangeArea = GEArea.GRAND_EXCHANGE;
        if (!grandExchangeArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
            this.status = "Walking to Grand Exchange";
            if (!Rs2Player.isMoving()) {
                KspWalkerGuard.walkToDestination(
                        "Melee:grand-exchange",
                        grandExchangeArea::getRandomPoint,
                        grandExchangeArea.toWorldArea()::contains,
                        2,
                        WEB_WALK_COOLDOWN_MS);
            }

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

        if (this.modifyTimedOutGrandExchangeOffers()) {
            return true;
        }

        if (this.hasCollectableGrandExchangeOffer()) {
            this.markCompletedActivePurchases();
            this.collectMeleeGrandExchangeOffers("completed melee purchase");
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
                .percent(purchase.priceBoostPercent)
                .closeAfterCompletion(false)
                .build();

        this.waitForGrandExchangeOfferInput();

        this.debug(
                "Placing melee GE buy offer | item={} qty={} percent={} availableSlots={}",
                purchase.itemName,
                purchase.quantity,
                purchase.priceBoostPercent,
                Rs2GrandExchange.getAvailableSlotsCount()
        );

        if (!Rs2GrandExchange.processOffer(request)) {
            this.debug("Failed to place melee GE buy offer | item={} qty={}", purchase.itemName, purchase.quantity);
            return false;
        }

        purchase.markPlaced();

        MeleeScript.sleepUntil(() -> !Rs2GrandExchange.isOfferScreenOpen(), 2000);
        return true;
    }

    private boolean modifyTimedOutGrandExchangeOffers() {
        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);

            if (details == null || details.isSelling() || details.getItemName() == null) {
                continue;
            }

            PurchaseRequest purchase = this.getActivePurchase(details.getItemName());

            if (purchase == null) {
                continue;
            }

            if (this.isFinishedTradeOffer(details)) {
                continue;
            }

            if (this.isCancelledBuyOffer(details)) {
                this.status = "Collecting cancelled GE offer for " + purchase.itemName;
                this.debug("Cancelled melee GE buy offer still present | item={} slot={} state={}",
                        purchase.itemName,
                        slot,
                        details.getState());

                if (!this.collectMeleeGrandExchangeOffers("cancelled timed melee purchase")) {
                    purchase.markPlaced();
                    return true;
                }

                if (!this.waitForGrandExchangeSlotToClear(slot, purchase.itemName)) {
                    purchase.markPlaced();
                    this.debug("Cancelled melee GE slot did not clear yet | item={} slot={}", purchase.itemName, slot);
                    return true;
                }

                this.requeuePurchaseWithIncreasedPrice(purchase);
                return true;
            }

            if (purchase.offerPlacedAtMs <= 0L) {
                purchase.markPlaced();
                continue;
            }

            if (!purchase.shouldIncreasePrice()) {
                continue;
            }

            this.status = "Increasing GE offer for " + purchase.itemName;

            this.debug(
                    "Timed out GE buy offer | item={} currentPercent={} slot={} elapsedMs={}",
                    purchase.itemName,
                    purchase.priceBoostPercent,
                    slot,
                    System.currentTimeMillis() - purchase.offerPlacedAtMs
            );

            if (!this.abortGrandExchangePurchase(purchase, slot)) {
                this.debug(
                        "Failed to abort GE offer for timed purchase | item={} slot={}",
                        purchase.itemName,
                        slot
                );
                return true;
            }

            this.collectMeleeGrandExchangeOffers("aborted timed melee purchase");

            if (!this.waitForGrandExchangeSlotToClear(slot, purchase.itemName)) {
                purchase.markPlaced();
                this.debug("Waiting for aborted GE slot to clear before requeue | item={} slot={}", purchase.itemName, slot);
                return true;
            }

            this.requeuePurchaseWithIncreasedPrice(purchase);

            return true;
        }

        return false;
    }

    private boolean abortGrandExchangePurchase(PurchaseRequest purchase, GrandExchangeSlots slot) {
        if (purchase == null || purchase.itemName == null || purchase.itemName.isBlank() || slot == null) {
            return false;
        }

        try {
            List<?> cancelledOffers = Rs2GrandExchange.cancelSpecificOffers(Collections.singletonList(slot), false);
            boolean cancelled = !cancelledOffers.isEmpty()
                    || this.isCancelledBuyOffer(Rs2GrandExchange.getOfferDetails(slot))
                    || Rs2GrandExchange.findSlotForItem(purchase.itemName, false) == null;
            this.debug("Abort melee GE offer | item={} slot={} cancelled={} collectable={}",
                    purchase.itemName,
                    slot,
                    cancelled,
                    this.hasCollectableGrandExchangeOffer());
            return cancelled;
        } catch (Exception ex) {
            this.debug(
                    "Failed to abort melee GE offer | item={} slot={} error={}",
                    purchase.itemName,
                    slot,
                    ex.getMessage()
            );
            return false;
        }
    }

    private boolean waitForGrandExchangeSlotToClear(GrandExchangeSlots slot, String itemName) {
        return MeleeScript.sleepUntil(
                () -> Rs2GrandExchange.isSlotAvailable(slot)
                        || Rs2GrandExchange.findSlotForItem(itemName, false) == null,
                5_000
        );
    }

    private void requeuePurchaseWithIncreasedPrice(PurchaseRequest purchase) {
        purchase.increasePrice();
        this.activePurchases.remove(purchase);
        this.pendingPurchases.add(0, purchase);
        this.debug(
                "Re-queued GE buy offer with increased price | item={} newPercent={}",
                purchase.itemName,
                purchase.priceBoostPercent
        );
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

            pending.markPlaced();
            this.pendingPurchases.remove(pending);
            this.activePurchases.add(pending);
        }
    }

    private void markCompletedActivePurchases() {
        boolean markedAny = false;
        for (GrandExchangeOfferDetails details : Rs2GrandExchange.getCompletedOffers().values()) {
            if (details == null || details.isSelling() || details.getItemName() == null || !this.isFinishedTradeOffer(details)) {
                continue;
            }
            markedAny = this.removeActivePurchase(details.getItemName()) || markedAny;
        }
        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details == null || details.isSelling() || !this.isFinishedTradeOffer(details) || details.getItemName() == null) {
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
            if (!this.collectMeleeGrandExchangeOffers("closing completed melee purchases")) {
                return;
            }
            Rs2GrandExchange.closeExchange();
        }
    }

    private boolean collectMeleeGrandExchangeOffers(String reason) {
        if (!this.hasCollectableGrandExchangeOffer()) {
            return true;
        }

        boolean clicked = Rs2GrandExchange.collectAllToInventory();
        boolean cleared = MeleeScript.sleepUntil(() -> !this.hasCollectableGrandExchangeOffer(), 5_000);
        this.debug("Collect melee GE offers | reason={} clicked={} cleared={} bought={} sold={} activeSlots={}",
                reason,
                clicked,
                cleared,
                Rs2GrandExchange.hasBoughtOffer(),
                Rs2GrandExchange.hasSoldOffer(),
                Rs2GrandExchange.getActiveOfferSlots().length);
        return cleared;
    }

    private boolean hasCollectableGrandExchangeOffer() {
        if (Rs2GrandExchange.hasBoughtOffer() || Rs2GrandExchange.hasSoldOffer()) {
            return true;
        }

        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details != null && details.isCompleted()) {
                return true;
            }
        }

        return false;
    }

    private boolean isFinishedTradeOffer(GrandExchangeOfferDetails details) {
        if (details == null) {
            return false;
        }
        return details.getState() == GrandExchangeOfferState.BOUGHT
                || details.getState() == GrandExchangeOfferState.SOLD;
    }

    private boolean isCancelledBuyOffer(GrandExchangeOfferDetails details) {
        return details != null && details.getState() == GrandExchangeOfferState.CANCELLED_BUY;
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

    private PurchaseRequest getActivePurchase(String itemName) {
        if (itemName == null) {
            return null;
        }

        for (PurchaseRequest purchase : this.activePurchases) {
            if (purchase != null && itemName.equalsIgnoreCase(purchase.itemName)) {
                return purchase;
            }
        }

        return null;
    }

    private boolean isPurchaseActive(String itemName) {
        return this.getActivePurchase(itemName) != null;
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
        if (targetArea.contains(Rs2Player.getWorldLocation())) {
            KspWalkerGuard.clear("Melee:target-area");
            return true;
        }
        if (Rs2Player.isMoving()) {
            return false;
        }
        this.status = "Walking to " + targetArea.getDisplayName();
        if (KspWalkerGuard.walkToDestination(
                "Melee:target-area",
                targetArea::getRandomPoint,
                targetArea::contains,
                2,
                WEB_WALK_COOLDOWN_MS)) {
            this.lastWebWalkAtMs = System.currentTimeMillis();
            this.lastWalkTarget = null;
            this.debug("Requested melee area walk | player={} area={}",
                    Rs2Player.getWorldLocation(),
                    targetArea.getDisplayName());
        }
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

        Skill targetSkill;
        WidgetInfo targetWidget;
        int targetStyleIndex;

        if (attack <= strength && attack <= defence) {
            targetSkill = Skill.ATTACK;
            targetWidget = WidgetInfo.COMBAT_STYLE_ONE;
            targetStyleIndex = 0;
        } else if (strength <= attack && strength <= defence) {
            targetSkill = Skill.STRENGTH;
            targetWidget = WidgetInfo.COMBAT_STYLE_TWO;
            targetStyleIndex = 1;
        } else {
            targetSkill = Skill.DEFENCE;
            targetWidget = WidgetInfo.COMBAT_STYLE_FOUR;
            targetStyleIndex = 3;
        }

        int currentStyleIndex = Microbot.getVarbitPlayerValue(43);

        if (currentStyleIndex == targetStyleIndex) {
            this.status = "Training " + targetSkill.getName().toLowerCase(Locale.ENGLISH);
            return false;
        }

        if (Rs2Player.isMoving()) {
            this.status = "Waiting to switch combat style";
            return false;
        }

        if (Rs2Tab.getCurrentTab() != InterfaceTab.COMBAT) {
            Rs2Tab.switchToCombatOptionsTab();
            MeleeScript.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.COMBAT, 1500);
        }

        this.status = "Switching combat style to " + targetSkill.getName().toLowerCase(Locale.ENGLISH);

        Rs2Combat.setAttackStyle(targetWidget);

        boolean switched = MeleeScript.sleepUntil(
                () -> Microbot.getVarbitPlayerValue(43) == targetStyleIndex,
                2000
        );

        this.debug("Combat style switch | targetSkill={} targetIndex={} oldIndex={} newIndex={} switched={}",
                targetSkill.getName(),
                targetStyleIndex,
                currentStyleIndex,
                Microbot.getVarbitPlayerValue(43),
                switched);

        return switched;
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
        return Arrays.stream(Food.values())
                .filter(food -> Rs2Inventory.itemQuantity(food.getItemId()) > 0)
                .max(Comparator.comparingInt(Food::getHealAmount))
                .orElse(null);
    }

    private Food getBestFoodAvailableInBank() {
        return Arrays.stream(Food.values())
                .filter(food -> Rs2Bank.count(food.getItemId()) > 0)
                .max(Comparator.comparingInt(Food::getHealAmount))
                .orElse(null);
    }

    private Food getBestOverallFood() {
        return Arrays.stream(Food.values())
                .max(Comparator.comparingInt(Food::getHealAmount))
                .orElse(Food.SALMON);
    }

    private int getFoodCountInInventory() {
        return Arrays.stream(Food.values())
                .mapToInt(food -> Rs2Inventory.itemQuantity(food.getItemId()))
                .sum();
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
        KspWalkerGuard.clear("Melee:target-area");
        KspWalkerGuard.clear("Melee:grand-exchange");
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
        private int priceBoostPercent;
        private long offerPlacedAtMs;

        public PurchaseRequest(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.priceBoostPercent = 10;
            this.offerPlacedAtMs = 0L;
        }

        public void markPlaced() {
            this.offerPlacedAtMs = System.currentTimeMillis();
        }

        public boolean shouldIncreasePrice() {
            return this.offerPlacedAtMs > 0L
                    && System.currentTimeMillis() - this.offerPlacedAtMs >= BUY_WAIT_TIMEOUT_MS;
        }

        public void increasePrice() {
            this.priceBoostPercent += GE_PRICE_INCREASE_STEP_PERCENT;
            this.offerPlacedAtMs = 0L;
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
