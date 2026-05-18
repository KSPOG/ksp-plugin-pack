/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Singleton
 *  net.runelite.api.EquipmentInventorySlot
 *  net.runelite.api.MenuAction
 *  net.runelite.api.Skill
 *  net.runelite.api.coords.WorldPoint
 *  net.runelite.api.widgets.Widget
 *  net.runelite.client.plugins.Plugin
 *  net.runelite.client.plugins.microbot.Microbot
 *  net.runelite.client.plugins.microbot.Script
 *  net.runelite.client.plugins.microbot.globval.enums.InterfaceTab
 *  net.runelite.client.plugins.microbot.util.bank.Rs2Bank
 *  net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction
 *  net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest
 *  net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange
 *  net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory
 *  net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel
 *  net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard
 *  net.runelite.client.plugins.microbot.util.math.Rs2Random
 *  net.runelite.client.plugins.microbot.util.menu.NewMenuEntry
 *  net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper
 *  net.runelite.client.plugins.microbot.util.npc.Rs2Npc
 *  net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel
 *  net.runelite.client.plugins.microbot.util.player.Rs2Player
 *  net.runelite.client.plugins.microbot.util.tabs.Rs2Tab
 *  net.runelite.client.plugins.microbot.util.walker.Rs2Walker
 *  net.runelite.client.plugins.microbot.util.widget.Rs2Widget
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.buyscript;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspTaskDebug;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.mining.levelreqmining.MiningReq;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.gearea.GEArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.smithing.tool.SmithTool;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.levelreqwc.WoodCuttingReq;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
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
public class BuyScript
extends Script {
    private static final Logger log = LoggerFactory.getLogger(BuyScript.class);
    private static final int LOOP_DELAY_MS = 600;
    private static final int WEB_WALK_COOLDOWN_MS = 3000;
    private static final int ACTION_COOLDOWN_MS = 1200;
    private static final int BANK_WAIT_TIMEOUT_MS = 3000;
    private static final int GE_OFFER_INPUT_DELAY_MS = 900;
    private static final String[] PICKAXE_NAMES = new String[]{"Bronze pickaxe", "Iron pickaxe", "Steel pickaxe", "Black pickaxe", "Mithril pickaxe", "Adamant pickaxe", "Rune pickaxe"};
    private static final String[] AXE_NAMES = new String[]{"Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe", "Adamant axe", "Rune axe"};
    private static final String HAMMER_NAME = SmithTool.HAMMER.getDisplayName();
    private static final int HAMMER_ITEM_ID = SmithTool.HAMMER.getItemId();
    private static final String TINDERBOX_NAME = "Tinderbox";
    private GEArea targetArea = GEArea.GRAND_EXCHANGE;
    private boolean debugLogging;
    private long lastWebWalkAtMs;
    private long lastActionAtMs;
    private boolean bankToolsAudited;
    private boolean bankHasDesiredPickaxe;
    private boolean bankHasDesiredAxe;
    private boolean bankHasHammer;
    private boolean bankHasTinderbox;
    private String lastDesiredPickaxe;
    private String lastDesiredAxe;
    private final List<String> pendingMissingToolBuys = new ArrayList<>();
    private boolean complete;

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public boolean run(GEArea area) {
        this.shutdown();
        this.targetArea = area;
        this.complete = false;
        Microbot.status = "Walking to GE";
        this.mainScheduledFuture = this.scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run() || !Microbot.isLoggedIn()) {
                return;
            }
            if (!this.ensureInTargetArea()) {
                return;
            }
            if (Rs2Player.isMoving()) {
                Microbot.status = "Waiting at GE";
                return;
            }
            if (Rs2Player.isInteracting() && !Rs2GrandExchange.isOpen() && !Rs2Bank.isOpen() && !this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
                Microbot.status = "Waiting for GE interaction";
                return;
            }
            String desiredPickaxe = this.resolveDesiredPickaxeName();
            String desiredAxe = this.resolveDesiredAxeName();
            KspTaskDebug.throttled(log, this.debugLogging, "GE Buy", "loop", 5_000L,
                    "loop | desiredPickaxe={} desiredAxe={} audited={} bankHasPickaxe={} bankHasAxe={} bankHasHammer={} bankHasTinderbox={} pendingBuys={} complete={} player={} bankOpen={} geOpen={} offerScreen={} slots={} coins={}",
                    desiredPickaxe,
                    desiredAxe,
                    this.bankToolsAudited,
                    this.bankHasDesiredPickaxe,
                    this.bankHasDesiredAxe,
                    this.bankHasHammer,
                    this.bankHasTinderbox,
                    this.pendingMissingToolBuys,
                    this.complete,
                    Rs2Player.getWorldLocation(),
                    Rs2Bank.isOpen(),
                    Rs2GrandExchange.isOpen(),
                    Rs2GrandExchange.isOfferScreenOpen(),
                    Rs2GrandExchange.isOpen() ? Rs2GrandExchange.getAvailableSlotsCount() : -1,
                    Rs2Inventory.itemQuantity((int)995));
            if (desiredPickaxe == null || desiredAxe == null) {
                return;
            }
            this.updateDesiredToolTracking(desiredPickaxe, desiredAxe);
            if (this.hasOutdatedToolEquipped(desiredPickaxe, desiredAxe) && !this.unequipOutdatedTools()) {
                return;
            }
            if (!this.bankToolsAudited || Rs2Bank.isOpen()) {
                this.prepareExchangeInventory(desiredPickaxe, desiredAxe, true);
                return;
            }
            if (this.handlePendingMissingToolBuys(desiredPickaxe, desiredAxe)) {
                return;
            }
            Rs2ItemModel outdatedInventoryTool = this.getNextOutdatedInventoryTool(desiredPickaxe, desiredAxe);
            String missingTool = this.getNextMissingToolToBuy(desiredPickaxe, desiredAxe);
            if (outdatedInventoryTool == null && missingTool == null && this.pendingMissingToolBuys.isEmpty()) {
                Microbot.status = "GE Buy Complete";
                this.complete = true;
                return;
            }
            if (Rs2GrandExchange.isOfferScreenOpen()) {
                Microbot.status = "Opening GE Overview";
                this.returnToGrandExchangeOverview();
                return;
            }
            if (this.shouldPrepareExchangeInventory(outdatedInventoryTool, missingTool, desiredPickaxe, desiredAxe)) {
                this.prepareExchangeInventory(desiredPickaxe, desiredAxe, missingTool != null);
                return;
            }
            if (!this.ensureGrandExchangeOpen()) {
                return;
            }
            this.syncPendingMissingToolBuysFromActiveOffers(desiredPickaxe, desiredAxe);
            this.processAvailableExchangeSlots(desiredPickaxe, desiredAxe);
        }, 0L, 600L, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean ensureInTargetArea() {
        if (this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
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
        Microbot.status = "Walking to GE";
        WorldPoint walkTarget = this.targetArea.getRandomPoint();
        this.debug("Walking to GE for buy task | player={} target={} area={}", Rs2Player.getWorldLocation(), walkTarget, this.targetArea.getDisplayName());
        Rs2Walker.walkTo(walkTarget, 2);
        return false;
    }

    private boolean shouldPrepareExchangeInventory(Rs2ItemModel outdatedInventoryTool, String missingTool, String desiredPickaxe, String desiredAxe) {
        if (Rs2Bank.isOpen()) {
            return true;
        }
        if (outdatedInventoryTool != null) {
            return false;
        }
        if (missingTool != null && Rs2Inventory.itemQuantity((int)995) > 0) {
            return false;
        }
        if (this.hasOutdatedToolInBank(desiredPickaxe, desiredAxe)) {
            return true;
        }
        return missingTool != null;
    }

    private void prepareExchangeInventory(String desiredPickaxe, String desiredAxe, boolean needCoinsForBuying) {
        if (!Rs2Bank.isOpen()) {
            if (Rs2GrandExchange.isOpen()) {
                Microbot.status = "Closing GE";
                Rs2GrandExchange.closeExchange();
                BuyScript.sleepUntil(() -> !Rs2GrandExchange.isOpen(), (int)2000);
                return;
            }
            if (this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
                Microbot.status = "Using GE Bank";
                if (!Rs2Bank.openBank()) {
                    Rs2Bank.walkToBankAndUseBank();
                }
                BuyScript.sleepUntil(Rs2Bank::isOpen, (int)3000);
                return;
            }
            Microbot.status = "Opening Bank";
            if (Rs2Bank.openBank()) {
                BuyScript.sleepUntil(Rs2Bank::isOpen, (int)3000);
            } else {
                Microbot.status = "Walking to Bank";
                Rs2Bank.walkToBankAndUseBank();
            }
            return;
        }
        Microbot.status = "Preparing GE Items";
        this.bankHasDesiredPickaxe = Rs2Bank.count((String)desiredPickaxe) > 0;
        this.bankHasDesiredAxe = Rs2Bank.count((String)desiredAxe) > 0;
        this.debug("Preparing GE inventory | desiredPickaxe={} bankHasPickaxe={} desiredAxe={} bankHasAxe={} needCoins={} coinsInv={} coinsBank={}",
                desiredPickaxe,
                this.bankHasDesiredPickaxe,
                desiredAxe,
                this.bankHasDesiredAxe,
                needCoinsForBuying,
                Rs2Inventory.itemQuantity((int)995),
                Rs2Bank.count((String)"Coins"));
        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            BuyScript.sleepUntil(Rs2Inventory::isEmpty, (int)3000);
        }
        this.bankHasHammer = this.hasItemIdInBank(HAMMER_ITEM_ID);
        this.bankHasTinderbox = Rs2Bank.count(TINDERBOX_NAME) > 0;
        this.debug("Tool audit | hammerInBank={} tinderboxInBank={} pendingBefore={}", this.bankHasHammer, this.bankHasTinderbox, this.pendingMissingToolBuys);
        if (!Rs2Bank.hasWithdrawAsNote()) {
            Rs2Bank.setWithdrawAsNote();
            BuyScript.sleepUntil(Rs2Bank::hasWithdrawAsNote, (int)2000);
        }
        this.withdrawOutdatedToolsAsNotes(desiredPickaxe, desiredAxe);
        if (needCoinsForBuying && Rs2Inventory.itemQuantity((int)995) <= 0 && Rs2Bank.count((String)"Coins") > 0) {
            Rs2Bank.withdrawAll((String)"Coins");
            BuyScript.sleepUntil(() -> Rs2Inventory.itemQuantity((int)995) > 0, (int)3000);
        }
        Rs2Bank.closeBank();
        BuyScript.sleepUntil(() -> !Rs2Bank.isOpen(), (int)2000);
        this.bankToolsAudited = true;
        Microbot.status = "Opening GE";
    }

    private void updateDesiredToolTracking(String desiredPickaxe, String desiredAxe) {
        if (Objects.equals(this.lastDesiredPickaxe, desiredPickaxe) && Objects.equals(this.lastDesiredAxe, desiredAxe)) {
            return;
        }
        this.lastDesiredPickaxe = desiredPickaxe;
        this.lastDesiredAxe = desiredAxe;
        this.bankToolsAudited = false;
        this.bankHasDesiredPickaxe = false;
        this.bankHasDesiredAxe = false;
        this.bankHasHammer = false;
        this.bankHasTinderbox = false;
        this.complete = false;
        this.pendingMissingToolBuys.clear();
    }

    private void withdrawOutdatedToolsAsNotes(String desiredPickaxe, String desiredAxe) {
        for (String pickaxeName : PICKAXE_NAMES) {
            if (pickaxeName.equalsIgnoreCase(desiredPickaxe) || Rs2Bank.count((String)pickaxeName) <= 0 || Rs2Inventory.isFull()) continue;
            Rs2Bank.withdrawAll((String)pickaxeName, (boolean)true);
            BuyScript.sleepUntil(() -> Rs2Inventory.hasItem((String)pickaxeName, (boolean)true), (int)3000);
        }
        for (String axeName : AXE_NAMES) {
            if (axeName.equalsIgnoreCase(desiredAxe) || Rs2Bank.count((String)axeName) <= 0 || Rs2Inventory.isFull()) continue;
            Rs2Bank.withdrawAll((String)axeName, (boolean)true);
            BuyScript.sleepUntil(() -> Rs2Inventory.hasItem((String)axeName, (boolean)true), (int)3000);
        }
    }

    private boolean ensureGrandExchangeOpen() {
        if (Rs2GrandExchange.isOpen()) {
            return true;
        }
        if (Rs2Bank.isOpen()) {
            Microbot.status = "Closing Bank";
            Rs2Bank.closeBank();
            BuyScript.sleepUntil(() -> !Rs2Bank.isOpen(), (int)2000);
            return false;
        }
        Microbot.status = "Opening GE";
        this.debug("Opening GE | player={} bankOpen={} geOpen={}", Rs2Player.getWorldLocation(), Rs2Bank.isOpen(), Rs2GrandExchange.isOpen());
        if (Rs2GrandExchange.openExchange()) {
            BuyScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
            return Rs2GrandExchange.isOpen();
        }
        if (this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation()) && this.interactGrandExchangeClerk()) {
            BuyScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
            if (Rs2GrandExchange.isOpen()) {
                return true;
            }
        }
        if (this.interactGrandExchangeClerk()) {
            BuyScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
            return Rs2GrandExchange.isOpen();
        }
        return false;
    }

    private boolean interactGrandExchangeClerk() {
        Rs2NpcModel clerk = Microbot.getClientThread().invoke(() -> Microbot.getRs2NpcCache().query()
                .fromWorldView()
                .withName("Grand Exchange Clerk")
                .nearestReachable(15));
        if (clerk == null) {
            this.debug("Grand Exchange clerk not found | player={} area={}", Rs2Player.getWorldLocation(), this.targetArea.getDisplayName());
            return false;
        }

        boolean clicked = clerk.click("Exchange");
        if (!clicked) {
            clicked = clerk.click("Trade");
        }
        this.debug("Grand Exchange clerk interaction | clicked={} npc={} id={} loc={} player={} geOpen={}",
                clicked,
                clerk.getName(),
                clerk.getId(),
                clerk.getWorldLocation(),
                Rs2Player.getWorldLocation(),
                Rs2GrandExchange.isOpen());
        return clicked;
    }

    private boolean placeFallbackSellOffer(Rs2ItemModel item) {
        GrandExchangeRequest request;
        boolean offered;
        if (item == null) {
            return false;
        }
        this.waitForActionCooldown();
        Microbot.status = "Selling " + item.getName();
        this.waitForGrandExchangeOfferInput();
        if (offered = Rs2GrandExchange.processOffer((GrandExchangeRequest)(request = GrandExchangeRequest.builder().action(GrandExchangeAction.SELL).itemName(item.getName()).quantity(item.getQuantity()).percent(-10).closeAfterCompletion(false).build()))) {
            this.lastActionAtMs = System.currentTimeMillis();
            BuyScript.sleepUntil(() -> !Rs2Inventory.hasItem((String)item.getName(), (boolean)true), (int)5000);
        }
        this.debug("GE fallback sell offer | item={} qty={} percent=-10 offered={} slots={}",
                item.getName(),
                item.getQuantity(),
                offered,
                Rs2GrandExchange.isOpen() ? Rs2GrandExchange.getAvailableSlotsCount() : -1);
        return offered;
    }

    private boolean placeFallbackBuyOffer(String itemName) {
        if (itemName == null || this.isMissingToolBuyPending(itemName)) {
            return false;
        }
        this.waitForActionCooldown();
        Microbot.status = "Buying " + itemName;
        GrandExchangeRequest request = GrandExchangeRequest.builder().action(GrandExchangeAction.BUY).itemName(itemName).exact(true).quantity(1).percent(10).closeAfterCompletion(false).build();
        this.waitForGrandExchangeOfferInput();
        boolean offered = Rs2GrandExchange.processOffer((GrandExchangeRequest)request);
        this.debug("GE missing-tool buy offer | item={} qty=1 percent=10 offered={} slots={}",
                itemName,
                offered,
                Rs2GrandExchange.isOpen() ? Rs2GrandExchange.getAvailableSlotsCount() : -1);
        if (offered) {
            this.lastActionAtMs = System.currentTimeMillis();
            this.pendingMissingToolBuys.add(itemName);
            BuyScript.sleepUntil(() -> !Rs2GrandExchange.isOfferScreenOpen(), (int)2000);
        }
        return offered;
    }

    private boolean handlePendingMissingToolBuys(String desiredPickaxe, String desiredAxe) {
        this.clearOwnedPendingMissingToolBuys();
        if (!this.ensureGrandExchangeOpen()) {
            return false;
        }

        if (Rs2GrandExchange.isOfferScreenOpen()) {
            Microbot.status = "Opening GE Overview";
            this.returnToGrandExchangeOverview();
            return true;
        }

        this.syncPendingMissingToolBuysFromActiveOffers(desiredPickaxe, desiredAxe);
        if (Rs2GrandExchange.hasBoughtOffer() || Rs2GrandExchange.hasSoldOffer()) {
            Microbot.status = "Collecting GE Offers";
            this.markCompletedPendingToolBuys();
            Rs2GrandExchange.collectAllToBank();
            BuyScript.sleepUntil(() -> !Rs2GrandExchange.hasBoughtOffer() && !Rs2GrandExchange.hasSoldOffer(), (int)5000);
            this.clearOwnedPendingMissingToolBuys();
            return true;
        }

        return false;
    }

    private void processAvailableExchangeSlots(String desiredPickaxe, String desiredAxe) {
        int availableSlots = Rs2GrandExchange.getAvailableSlotsCount();
        this.debug("Processing GE slots | availableSlots={} pendingMissingToolBuys={} desiredPickaxe={} desiredAxe={}",
                availableSlots,
                this.pendingMissingToolBuys,
                desiredPickaxe,
                desiredAxe);
        if (availableSlots <= 0) {
            if (!this.pendingMissingToolBuys.isEmpty()) {
                Microbot.status = "Waiting for GE buys";
            }
            return;
        }

        boolean placedOffer = false;
        while (availableSlots > 0) {
            Rs2ItemModel outdatedInventoryTool = this.getNextOutdatedInventoryTool(desiredPickaxe, desiredAxe);
            if (outdatedInventoryTool != null) {
                if (!this.placeFallbackSellOffer(outdatedInventoryTool)) {
                    break;
                }
                placedOffer = true;
                availableSlots = Rs2GrandExchange.getAvailableSlotsCount();
                continue;
            }

            String missingTool = this.getNextMissingToolToBuy(desiredPickaxe, desiredAxe);
            if (missingTool == null) {
                break;
            }

            if (!this.placeFallbackBuyOffer(missingTool)) {
                break;
            }

            placedOffer = true;
            availableSlots = Rs2GrandExchange.getAvailableSlotsCount();
        }

        if (!placedOffer && !this.pendingMissingToolBuys.isEmpty()) {
            Microbot.status = "Waiting for GE buys";
        }
    }

    private void waitForActionCooldown() {
        long remaining = ACTION_COOLDOWN_MS - (System.currentTimeMillis() - this.lastActionAtMs);
        if (remaining > 0L) {
            BuyScript.sleep((int)Math.min(remaining, ACTION_COOLDOWN_MS));
        }
    }

    private void waitForGrandExchangeOfferInput() {
        BuyScript.sleep(GE_OFFER_INPUT_DELAY_MS);
    }

    private void syncPendingMissingToolBuysFromActiveOffers(String desiredPickaxe, String desiredAxe) {
        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details == null || details.isSelling() || details.getItemName() == null) {
                continue;
            }

            String itemName = details.getItemName();
            if (this.isExpectedMissingTool(itemName, desiredPickaxe, desiredAxe) && !this.isMissingToolBuyPending(itemName)) {
                this.pendingMissingToolBuys.add(itemName);
            }
        }
    }

    private void markCompletedPendingToolBuys() {
        boolean markedAny = false;
        for (GrandExchangeOfferDetails details : Rs2GrandExchange.getCompletedOffers().values()) {
            if (details == null || details.isSelling() || details.getItemName() == null) {
                continue;
            }

            if (this.markPendingToolBought(details.getItemName())) {
                markedAny = true;
            }
        }

        for (GrandExchangeSlots slot : Rs2GrandExchange.getActiveOfferSlots()) {
            GrandExchangeOfferDetails details = Rs2GrandExchange.getOfferDetails(slot);
            if (details == null || details.isSelling() || !details.isCompleted() || details.getItemName() == null) {
                continue;
            }

            if (this.markPendingToolBought(details.getItemName())) {
                markedAny = true;
            }
        }

        if (!markedAny && Rs2GrandExchange.hasBoughtOffer() && this.pendingMissingToolBuys.size() == 1) {
            String pendingTool = this.pendingMissingToolBuys.remove(0);
            this.markDesiredToolBought(pendingTool);
        }
    }

    private boolean markPendingToolBought(String itemName) {
        Iterator<String> iterator = this.pendingMissingToolBuys.iterator();
        while (iterator.hasNext()) {
            String pendingTool = iterator.next();
            if (!pendingTool.equalsIgnoreCase(itemName)) {
                continue;
            }

            this.markDesiredToolBought(pendingTool);
            iterator.remove();
            return true;
        }

        return false;
    }

    private void clearOwnedPendingMissingToolBuys() {
        Iterator<String> iterator = this.pendingMissingToolBuys.iterator();
        while (iterator.hasNext()) {
            String pendingTool = iterator.next();
            if (!this.hasToolAnywhere(pendingTool)) {
                continue;
            }

            this.markDesiredToolBought(pendingTool);
            iterator.remove();
        }
    }

    private void markDesiredToolBought(String toolName) {
        if (Objects.equals(toolName, this.lastDesiredPickaxe)) {
            this.bankHasDesiredPickaxe = true;
        }
        if (Objects.equals(toolName, this.lastDesiredAxe)) {
            this.bankHasDesiredAxe = true;
        }
        if (HAMMER_NAME.equalsIgnoreCase(toolName)) {
            this.bankHasHammer = true;
        }
        if (TINDERBOX_NAME.equalsIgnoreCase(toolName)) {
            this.bankHasTinderbox = true;
        }
        this.bankToolsAudited = true;
    }

    private void returnToGrandExchangeOverview() {
        if (System.currentTimeMillis() - this.lastActionAtMs < 1200L) {
            return;
        }
        Rs2GrandExchange.backToOverview();
        this.lastActionAtMs = System.currentTimeMillis();
        BuyScript.sleepUntil(() -> !Rs2GrandExchange.isOfferScreenOpen(), (int)2000);
    }

    private int getAdjustedSellPrice(Rs2ItemModel item) {
        int guidePrice = this.getGuidePrice(item);
        if (guidePrice <= 0) {
            return 1;
        }
        return Math.max(1, (int)((long)guidePrice * 90L / 100L));
    }

    private int getAdjustedBuyPrice(String itemName) {
        int guidePrice = this.getGuidePrice(itemName);
        if (guidePrice <= 0) {
            return 1;
        }
        return Math.max(1, (int)(((long)guidePrice * 110L + 99L) / 100L));
    }

    private int getGuidePrice(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return 1;
        }
        try {
            List<ItemPrice> matches = Microbot.getItemManager().search(itemName);
            if (matches == null || matches.isEmpty()) {
                return 1;
            }
            for (ItemPrice match : matches) {
                if (match != null && itemName.equalsIgnoreCase(match.getName())) {
                    return this.getGuidePrice(match);
                }
            }
            return this.getGuidePrice(matches.get(0));
        }
        catch (Exception ex) {
            this.debug("Unable to resolve guide price for {}: {}", itemName, ex.getMessage());
            return 1;
        }
    }

    private int getGuidePrice(ItemPrice itemPrice) {
        if (itemPrice == null) {
            return 1;
        }
        int gePrice = Rs2GrandExchange.getPrice(itemPrice.getId());
        if (gePrice > 0) {
            return gePrice;
        }
        return itemPrice.getPrice();
    }

    private int getGuidePrice(Rs2ItemModel item) {
        if (item == null) {
            return 1;
        }

        int unnotedId = item.getUnNotedId();
        int gePrice = unnotedId > 0 ? Rs2GrandExchange.getPrice(unnotedId) : 0;
        if (gePrice > 0) {
            return gePrice;
        }

        int linkedId = item.getLinkedId();
        gePrice = linkedId > 0 ? Rs2GrandExchange.getPrice(linkedId) : 0;
        if (gePrice > 0) {
            return gePrice;
        }

        gePrice = Rs2GrandExchange.getPrice(item.getId());
        if (gePrice > 0) {
            return gePrice;
        }

        int itemModelPrice = item.getPrice();
        if (itemModelPrice > 0) {
            return itemModelPrice;
        }

        return this.getGuidePrice(item.getName());
    }

    private Rs2ItemModel getNextOutdatedInventoryTool(String desiredPickaxe, String desiredAxe) {
        for (Rs2ItemModel item : Rs2Inventory.all()) {
            if (item == null || item.getName() == null || !this.isOutdatedToolName(item.getName(), desiredPickaxe, desiredAxe)) continue;
            return item;
        }
        return null;
    }

    private boolean hasOutdatedToolInBank(String desiredPickaxe, String desiredAxe) {
        for (String pickaxeName : PICKAXE_NAMES) {
            if (!this.isOutdatedToolName(pickaxeName, desiredPickaxe, desiredAxe) || Rs2Bank.count((String)pickaxeName) <= 0) continue;
            return true;
        }
        for (String axeName : AXE_NAMES) {
            if (!this.isOutdatedToolName(axeName, desiredPickaxe, desiredAxe) || Rs2Bank.count((String)axeName) <= 0) continue;
            return true;
        }
        return false;
    }

    private boolean hasOutdatedToolEquipped(String desiredPickaxe, String desiredAxe) {
        for (String pickaxeName : PICKAXE_NAMES) {
            if (!this.isOutdatedToolName(pickaxeName, desiredPickaxe, desiredAxe) || !Rs2Equipment.isWearing((String[])new String[]{pickaxeName})) continue;
            return true;
        }
        for (String axeName : AXE_NAMES) {
            if (!this.isOutdatedToolName(axeName, desiredPickaxe, desiredAxe) || !Rs2Equipment.isWearing((String[])new String[]{axeName})) continue;
            return true;
        }
        return false;
    }

    private boolean unequipOutdatedTools() {
        if (Rs2Tab.getCurrentTab() != InterfaceTab.EQUIPMENT) {
            Rs2Tab.switchTo((InterfaceTab)InterfaceTab.EQUIPMENT);
            BuyScript.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.EQUIPMENT, (int)1500);
        }
        Rs2Equipment.unEquip((EquipmentInventorySlot[])new EquipmentInventorySlot[]{EquipmentInventorySlot.WEAPON});
        BuyScript.sleep((int)250);
        if (Rs2Tab.getCurrentTab() != InterfaceTab.INVENTORY) {
            Rs2Tab.switchTo((InterfaceTab)InterfaceTab.INVENTORY);
            BuyScript.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY, (int)1500);
        }
        return !Rs2Equipment.isWearing((String)"pickaxe", (boolean)false) && !Rs2Equipment.isWearing((String[])new String[]{"pickaxe"}) && !Rs2Equipment.isWearing((String)"axe", (boolean)false) && !Rs2Equipment.isWearing((String[])new String[]{"axe"});
    }

    private String getNextMissingToolToBuy(String desiredPickaxe, String desiredAxe) {
        if (!this.hasToolAnywhere(desiredPickaxe) && !this.isMissingToolBuyPending(desiredPickaxe)) {
            return desiredPickaxe;
        }
        if (!this.hasToolAnywhere(desiredAxe) && !this.isMissingToolBuyPending(desiredAxe)) {
            return desiredAxe;
        }
        if (!this.hasHammerAnywhere() && !this.isMissingToolBuyPending(HAMMER_NAME)) {
            return HAMMER_NAME;
        }
        if (!this.hasTinderboxAnywhere() && !this.isMissingToolBuyPending(TINDERBOX_NAME)) {
            return TINDERBOX_NAME;
        }
        return null;
    }

    private boolean isMissingToolBuyPending(String toolName) {
        for (String pendingTool : this.pendingMissingToolBuys) {
            if (pendingTool.equalsIgnoreCase(toolName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isExpectedMissingTool(String itemName, String desiredPickaxe, String desiredAxe) {
        return itemName != null
                && (itemName.equalsIgnoreCase(desiredPickaxe)
                || itemName.equalsIgnoreCase(desiredAxe)
                || itemName.equalsIgnoreCase(HAMMER_NAME)
                || itemName.equalsIgnoreCase(TINDERBOX_NAME));
    }

    private boolean hasToolAnywhere(String toolName) {
        if (HAMMER_NAME.equalsIgnoreCase(toolName)) {
            return this.hasHammerAnywhere();
        }
        if (TINDERBOX_NAME.equalsIgnoreCase(toolName)) {
            return this.hasTinderboxAnywhere();
        }
        return Rs2Equipment.isWearing((String[])new String[]{toolName}) || Rs2Inventory.hasItem((String[])new String[]{toolName}) || Rs2Inventory.hasItem((String)toolName, (boolean)true) || Rs2Bank.isOpen() && Rs2Bank.count((String)toolName) > 0 || this.isToolCachedInBank(toolName);
    }

    private boolean hasHammerAnywhere() {
        return this.hasItemIdInInventory(HAMMER_ITEM_ID)
                || Rs2Bank.isOpen() && this.hasItemIdInBank(HAMMER_ITEM_ID)
                || this.isToolCachedInBank(HAMMER_NAME);
    }

    private boolean hasTinderboxAnywhere() {
        return Rs2Inventory.hasItem(TINDERBOX_NAME)
                || Rs2Inventory.hasItem(TINDERBOX_NAME, true)
                || Rs2Bank.isOpen() && Rs2Bank.count(TINDERBOX_NAME) > 0
                || this.isToolCachedInBank(TINDERBOX_NAME);
    }

    private boolean hasItemIdInInventory(int itemId) {
        return Rs2Inventory.all().stream()
                .anyMatch(item -> item != null && item.getId() == itemId);
    }

    private boolean hasItemIdInBank(int itemId) {
        return Rs2Bank.bankItems().stream()
                .anyMatch(item -> item != null && item.getId() == itemId);
    }

    private boolean isToolCachedInBank(String toolName) {
        if (!this.bankToolsAudited) {
            return false;
        }
        if (Objects.equals(toolName, this.lastDesiredPickaxe)) {
            return this.bankHasDesiredPickaxe;
        }
        if (Objects.equals(toolName, this.lastDesiredAxe)) {
            return this.bankHasDesiredAxe;
        }
        if (HAMMER_NAME.equalsIgnoreCase(toolName)) {
            return this.bankHasHammer;
        }
        if (TINDERBOX_NAME.equalsIgnoreCase(toolName)) {
            return this.bankHasTinderbox;
        }
        return false;
    }

    private boolean isOutdatedToolName(String itemName, String desiredPickaxe, String desiredAxe) {
        for (String pickaxeName : PICKAXE_NAMES) {
            if (!pickaxeName.equalsIgnoreCase(itemName)) continue;
            return !pickaxeName.equalsIgnoreCase(desiredPickaxe);
        }
        for (String axeName : AXE_NAMES) {
            if (!axeName.equalsIgnoreCase(itemName)) continue;
            return !axeName.equalsIgnoreCase(desiredAxe);
        }
        return false;
    }

    private String resolveDesiredPickaxeName() {
        MiningReq bestMiningReq = MiningReq.bestForMiningLevel(Microbot.getClient().getRealSkillLevel(Skill.MINING));
        return PICKAXE_NAMES[bestMiningReq.ordinal()];
    }

    private String resolveDesiredAxeName() {
        WoodCuttingReq bestWoodcuttingReq = WoodCuttingReq.bestForWoodcuttingLevel(Microbot.getClient().getRealSkillLevel(Skill.WOODCUTTING));
        return AXE_NAMES[bestWoodcuttingReq.ordinal()];
    }

    private WorldPoint getAreaCenter() {
        int centerX = (this.targetArea.getSouthWest().getX() + this.targetArea.getNorthEast().getX()) / 2;
        int centerY = (this.targetArea.getSouthWest().getY() + this.targetArea.getNorthEast().getY()) / 2;
        int plane = this.targetArea.getSouthWest().getPlane();
        return new WorldPoint(centerX, centerY, plane);
    }

    private void debug(String message, Object ... args) {
        if (this.debugLogging) {
            KspTaskDebug.info(log, true, "GE Buy", message, args);
        }
    }

    public void shutdown() {
        this.lastWebWalkAtMs = 0L;
        this.lastActionAtMs = 0L;
        this.bankToolsAudited = false;
        this.bankHasDesiredPickaxe = false;
        this.bankHasDesiredAxe = false;
        this.bankHasHammer = false;
        this.bankHasTinderbox = false;
        this.lastDesiredPickaxe = null;
        this.lastDesiredAxe = null;
        this.pendingMissingToolBuys.clear();
        this.complete = false;
        super.shutdown();
    }

    public boolean isComplete() {
        return this.complete;
    }

    public GEArea getTargetArea() {
        return this.targetArea;
    }
}

