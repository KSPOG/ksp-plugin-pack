/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.inject.Singleton
 *  net.runelite.api.MenuAction
 *  net.runelite.api.Skill
 *  net.runelite.api.coords.WorldPoint
 *  net.runelite.api.widgets.Widget
 *  net.runelite.client.plugins.Plugin
 *  net.runelite.client.plugins.microbot.Microbot
 *  net.runelite.client.plugins.microbot.Script
 *  net.runelite.client.plugins.microbot.util.bank.Rs2Bank
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
 *  net.runelite.client.plugins.microbot.util.walker.Rs2Walker
 *  net.runelite.client.plugins.microbot.util.widget.Rs2Widget
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.sellscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.kspaccountbuilder.KspTaskDebug;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.mining.levelreqmining.MiningReq;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.gearea.GEArea;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.sell.SellList;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.selling.sellscript.SellState;
import net.runelite.client.plugins.microbot.kspaccountbuilder.tasks.skilling.woodcutting.levelreqwc.WoodCuttingReq;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeAction;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeRequest;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SellScript
extends Script {
    private static final Logger log = LoggerFactory.getLogger(SellScript.class);
    private static final int LOOP_DELAY_MS = 600;
    private static final int WEB_WALK_COOLDOWN_MS = 3000;
    private static final int ACTION_COOLDOWN_MS = 1200;
    private static final int GE_OFFER_INPUT_DELAY_MS = 900;
    private static final int INVENTORY_WAIT_TIMEOUT_MS = 3000;
    private static final int OFFER_SCREEN_WAIT_TIMEOUT_MS = 3000;
    private static final int TRADE_RESTRICTION_CACHE_MS = 300000;
    private static final int TRADE_RESTRICTION_MIN_TOTAL_LEVEL = 100;
    private static final int TRADE_RESTRICTION_MIN_QUEST_POINTS = 10;
    private static final int TRADE_RESTRICTION_MIN_HOURS_PLAYED = 20;
    private static final int SIDE_JOURNAL_TAB_CONTAINER_WIDGET_ID = 41222157;
    private static final int ACCOUNT_SUMMARY_WIDGET_ID = 46661633;
    private static final int TIME_PLAYED_WIDGET_ID = 46661634;
    private static final String[] PICKAXE_NAMES = new String[]{"Bronze pickaxe", "Iron pickaxe", "Steel pickaxe", "Black pickaxe", "Mithril pickaxe", "Adamant pickaxe", "Rune pickaxe"};
    private static final String[] AXE_NAMES = new String[]{"Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe", "Adamant axe", "Rune axe"};
    private static final Set<String> TRADE_RESTRICTED_ITEM_NAMES = new HashSet<String>(Arrays.asList("rune essence", "pure essence", "air rune", "water rune", "earth rune", "fire rune", "mind rune", "chaos rune", "nature rune", "law rune", "death rune", "blood rune", "feather", "cowhide", "leather", "green dragonhide", "copper ore", "tin ore", "iron ore", "coal", "gold ore", "mithril ore", "adamantite ore", "runite ore", "iron bar", "steel bar", "mithril bar", "adamantite bar", "runite bar", "logs", "oak logs", "willow logs", "maple logs", "yew logs", "magic logs", "raw shrimp", "shrimp", "raw anchovies", "anchovies", "raw trout", "trout", "raw salmon", "salmon", "raw tuna", "tuna", "raw lobster", "lobster", "raw swordfish", "swordfish", "raw shark", "shark", "sapphire", "emerald", "ruby", "diamond", "bow string", "bronze arrow", "iron arrow", "steel arrow", "mithril arrow", "adamant arrow", "rune arrow", "bronze bolts", "iron bolts", "steel bolts", "bones", "big bones", "wine of zamorak"));
    private GEArea targetArea = GEArea.GRAND_EXCHANGE;
    private boolean debugLogging;
    private long lastWebWalkAtMs;
    private long lastActionAtMs;
    private final Set<String> blockedSellItems = new HashSet<String>();
    private Boolean tradeRestrictionUnlockedCache;
    private Integer cachedHoursPlayed;
    private long lastTradeRestrictionCheckAtMs;
    private boolean attemptedHoursPlayedLookup;
    private SellState state = SellState.GOING_TO_GE;
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
            if (this.complete) {
                Microbot.status = "GE Sell Complete";
                return;
            }
            this.updateState();
            KspTaskDebug.throttled(log, this.debugLogging, "GE Sell", "loop", 5_000L,
                    "loop | state={} complete={} player={} moving={} interacting={} bankOpen={} geOpen={} offerScreen={} slots={} hasInvSellable={} hasBankSellable={} blockedItems={}",
                    this.state,
                    this.complete,
                    Rs2Player.getWorldLocation(),
                    Rs2Player.isMoving(),
                    Rs2Player.isInteracting(),
                    Rs2Bank.isOpen(),
                    Rs2GrandExchange.isOpen(),
                    Rs2GrandExchange.isOfferScreenOpen(),
                    Rs2GrandExchange.isOpen() ? Rs2GrandExchange.getAvailableSlotsCount() : -1,
                    this.hasSellableInventoryItems(),
                    this.hasSellableBankItems(),
                    this.blockedSellItems);
            switch (this.state) {
                case GOING_TO_GE: {
                    if (!this.ensureInTargetArea()) {
                        return;
                    }
                    this.state = SellState.RESTOCKING_FROM_BANK;
                    return;
                }
                case RESTOCKING_FROM_BANK: {
                    if (!this.hasSellableInventoryItems()) {
                        this.prepareSellInventoryFromBank();
                        return;
                    }
                    this.state = SellState.SELLING_ITEMS;
                    return;
                }
                case SELLING_ITEMS: {
                    this.handleSellingItems();
                    return;
                }
            }
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
        this.debug("Walking to GE for sell task | player={} target={} area={}", Rs2Player.getWorldLocation(), walkTarget, this.targetArea.getDisplayName());
        Rs2Walker.walkTo(walkTarget, 2);
        return false;
    }

    private void prepareSellInventoryFromBank() {
        if (!Rs2Bank.isOpen()) {
            if (Rs2GrandExchange.isOpen()) {
                Microbot.status = "Closing GE";
                Rs2GrandExchange.closeExchange();
                SellScript.sleepUntil(() -> !Rs2GrandExchange.isOpen(), (int)2000);
                return;
            }
            if (this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
                Microbot.status = "Using GE Bank";
                Rs2Bank.walkToBankAndUseBank();
                SellScript.sleepUntil(Rs2Bank::isOpen, (int)3000);
                return;
            }
            Microbot.status = "Opening Bank";
            if (Rs2Bank.openBank()) {
                SellScript.sleepUntil(Rs2Bank::isOpen, (int)3000);
            } else {
                Microbot.status = "Walking to Bank";
                Rs2Bank.walkToBankAndUseBank();
            }
            return;
        }
        Microbot.status = "Withdrawing Sell Items";
        this.debug("Preparing sell inventory | bankOpen={} withdrawAsNote={} sellableBankItems={} inventoryEmpty={}",
                Rs2Bank.isOpen(),
                Rs2Bank.hasWithdrawAsNote(),
                this.hasSellableBankItems(),
                Rs2Inventory.isEmpty());
        if (!Rs2Inventory.isEmpty()) {
            Rs2Bank.depositAll();
            SellScript.sleepUntil(Rs2Inventory::isEmpty, (int)3000);
        }
        if (!Rs2Bank.hasWithdrawAsNote()) {
            Rs2Bank.setWithdrawAsNote();
            SellScript.sleepUntil(Rs2Bank::hasWithdrawAsNote, (int)2000);
        }
        boolean withdrewAny = false;
        for (SellList sellList : SellList.values()) {
            if (!this.shouldSellEntry(sellList)) continue;
            if (Rs2Inventory.isFull()) break;
            if (Rs2Bank.count((String)sellList.getDisplayName()) <= 0 || this.isBlockedSellItem(sellList.getDisplayName())) continue;
            Rs2Bank.withdrawAll((String)sellList.getDisplayName(), (boolean)true);
            boolean withdrew = SellScript.sleepUntil(() -> Rs2Inventory.hasItem((String)sellList.getDisplayName(), (boolean)true), (int)3000);
            this.debug("Withdraw sell item | item={} withdrew={} invFull={}", sellList.getDisplayName(), withdrew, Rs2Inventory.isFull());
            withdrewAny = withdrewAny || withdrew;
        }
        withdrewAny = this.withdrawOutdatedToolsAsNotes() || withdrewAny;
        if (!withdrewAny) {
            if (!this.hasSellableBankItems()) {
                this.complete = true;
                Microbot.status = "GE Sell Complete";
            } else {
                Microbot.status = "Waiting for sell item withdraw";
            }
            return;
        }
        Rs2Bank.closeBank();
        SellScript.sleepUntil(() -> !Rs2Bank.isOpen(), (int)2000);
        Microbot.status = "Opening GE";
    }

    private void updateState() {
        if (!this.targetArea.toWorldArea().contains(Rs2Player.getWorldLocation())) {
            this.state = SellState.GOING_TO_GE;
            return;
        }
        if (!this.hasSellableInventoryItems()) {
            this.state = SellState.RESTOCKING_FROM_BANK;
            return;
        }
        this.state = SellState.SELLING_ITEMS;
    }

    private void handleSellingItems() {
        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) {
            Microbot.status = "Waiting at GE";
            return;
        }
        if (Rs2GrandExchange.hasSoldOffer() && this.ensureGrandExchangeOpen()) {
            Microbot.status = "Collecting Sold Items";
            Rs2GrandExchange.collectAllToBank();
            SellScript.sleepUntil(() -> !Rs2GrandExchange.hasSoldOffer(), (int)5000);
            return;
        }
        if (Rs2GrandExchange.isOfferScreenOpen()) {
            Microbot.status = "Opening GE Overview";
            this.returnToGrandExchangeOverview();
            return;
        }
        if (!this.ensureGrandExchangeOpen()) {
            return;
        }
        if (Rs2GrandExchange.getAvailableSlotsCount() <= 0) {
            if (Rs2GrandExchange.hasSoldOffer()) {
                Rs2GrandExchange.collectAllToBank();
                SellScript.sleepUntil(() -> !Rs2GrandExchange.hasSoldOffer(), (int)5000);
            }
            return;
        }
        Rs2ItemModel nextItem = this.getNextSellableInventoryItem();
        if (nextItem == null) {
            Microbot.status = "Waiting at GE";
            this.state = SellState.RESTOCKING_FROM_BANK;
            return;
        }
        this.placeFallbackSellOffer(nextItem);
    }

    private boolean shouldSellEntry(SellList sellList) {
        if (this.isTradeRestrictedItemName(sellList.getDisplayName())) {
            return false;
        }
        int firemakingLevel = Microbot.getClient().getRealSkillLevel(Skill.FIREMAKING);
        if (sellList == SellList.LOGS) {
            return firemakingLevel >= 15;
        }
        if (sellList == SellList.OAK_LOGS) {
            return firemakingLevel >= 30;
        }
        return true;
    }

    private boolean hasSellableInventoryItems() {
        return this.getNextSellableInventoryItem() != null;
    }

    private boolean hasSellableBankItems() {
        for (SellList sellList : SellList.values()) {
            if (!this.shouldSellEntry(sellList)
                    || this.isBlockedSellItem(sellList.getDisplayName())
                    || Rs2Bank.count((String)sellList.getDisplayName()) <= 0) continue;
            return true;
        }
        return this.hasOutdatedToolInBank();
    }

    private Rs2ItemModel getNextSellableInventoryItem() {
        List<String> sellNames = this.getAllowedSellNames();
        String desiredPickaxe = this.resolveDesiredPickaxeName();
        String desiredAxe = this.resolveDesiredAxeName();
        for (Rs2ItemModel item : Rs2Inventory.all()) {
            if (item == null || item.getName() == null || this.isBlockedSellItem(item.getName()) || !sellNames.stream().anyMatch(name -> name.equalsIgnoreCase(item.getName())) && !this.isOutdatedToolName(item.getName(), desiredPickaxe, desiredAxe)) continue;
            return item;
        }
        return null;
    }

    private List<String> getAllowedSellNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (SellList sellList : SellList.values()) {
            if (!this.shouldSellEntry(sellList) || this.isBlockedSellItem(sellList.getDisplayName())) continue;
            names.add(sellList.getDisplayName());
        }
        return names;
    }

    private boolean withdrawOutdatedToolsAsNotes() {
        boolean withdrew;
        boolean withdrewAny = false;
        String desiredPickaxe = this.resolveDesiredPickaxeName();
        String desiredAxe = this.resolveDesiredAxeName();
        for (String pickaxeName : PICKAXE_NAMES) {
            if (pickaxeName.equalsIgnoreCase(desiredPickaxe) || Rs2Inventory.isFull() || Rs2Bank.count((String)pickaxeName) <= 0) continue;
            Rs2Bank.withdrawAll((String)pickaxeName, (boolean)true);
            withdrew = SellScript.sleepUntil(() -> Rs2Inventory.hasItem((String)pickaxeName, (boolean)true), (int)3000);
            withdrewAny = withdrewAny || withdrew;
        }
        for (String axeName : AXE_NAMES) {
            if (axeName.equalsIgnoreCase(desiredAxe) || Rs2Inventory.isFull() || Rs2Bank.count((String)axeName) <= 0) continue;
            Rs2Bank.withdrawAll((String)axeName, (boolean)true);
            withdrew = SellScript.sleepUntil(() -> Rs2Inventory.hasItem((String)axeName, (boolean)true), (int)3000);
            withdrewAny = withdrewAny || withdrew;
        }
        return withdrewAny;
    }

    private boolean hasOutdatedToolInBank() {
        String desiredPickaxe = this.resolveDesiredPickaxeName();
        String desiredAxe = this.resolveDesiredAxeName();
        for (String pickaxeName : PICKAXE_NAMES) {
            if (pickaxeName.equalsIgnoreCase(desiredPickaxe) || Rs2Bank.count((String)pickaxeName) <= 0) continue;
            return true;
        }
        for (String axeName : AXE_NAMES) {
            if (axeName.equalsIgnoreCase(desiredAxe) || Rs2Bank.count((String)axeName) <= 0) continue;
            return true;
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

    private boolean isBlockedSellItem(String itemName) {
        return itemName != null && this.blockedSellItems.contains(itemName.toLowerCase(Locale.ENGLISH));
    }

    private boolean isTradeRestrictedItemName(String itemName) {
        if (itemName == null || !TRADE_RESTRICTED_ITEM_NAMES.contains(itemName.toLowerCase(Locale.ENGLISH))) {
            return false;
        }
        return !this.hasUnlockedTradeRestrictedItems();
    }

    private boolean hasUnlockedTradeRestrictedItems() {
        long now = System.currentTimeMillis();
        if (this.tradeRestrictionUnlockedCache != null && now - this.lastTradeRestrictionCheckAtMs < 300000L) {
            return this.tradeRestrictionUnlockedCache;
        }
        boolean unlocked = false;
        if (this.getTotalLevel() >= 100 && this.getQuestPoints() >= 10) {
            unlocked = this.getHoursPlayed() >= 20;
        }
        this.tradeRestrictionUnlockedCache = unlocked;
        this.lastTradeRestrictionCheckAtMs = now;
        return unlocked;
    }

    private int getTotalLevel() {
        int total = 0;
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;
            total += Microbot.getClient().getRealSkillLevel(skill);
        }
        return total;
    }

    private int getQuestPoints() {
        return Microbot.getVarbitPlayerValue((int)101);
    }

    private int getHoursPlayed() {
        String[] parts;
        if (this.cachedHoursPlayed != null) {
            return this.cachedHoursPlayed;
        }
        String timePlayedText = this.getTimePlayedText();
        if (timePlayedText == null || timePlayedText.isEmpty()) {
            return 0;
        }
        int totalHours = 0;
        for (String rawPart : parts = timePlayedText.toLowerCase(Locale.ENGLISH).split(",")) {
            String part = rawPart.trim();
            if (part.endsWith("days") || part.endsWith("day")) {
                totalHours += this.extractLeadingNumber(part) * 24;
                continue;
            }
            if (part.endsWith("hours") || part.endsWith("hour")) {
                totalHours += this.extractLeadingNumber(part);
                continue;
            }
            if (!part.endsWith("minutes") && !part.endsWith("minute")) continue;
            totalHours += this.extractLeadingNumber(part) / 60;
        }
        this.cachedHoursPlayed = totalHours;
        return totalHours;
    }

    private int extractLeadingNumber(String text) {
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        }
        catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String getTimePlayedText() {
        Widget timePlayedWidget = Rs2Widget.getWidget((int)46661634);
        if (timePlayedWidget == null || !Rs2Widget.isWidgetVisible((int)timePlayedWidget.getId()) || timePlayedWidget.getText() == null || timePlayedWidget.getText().isEmpty()) {
            if (this.attemptedHoursPlayedLookup) {
                return null;
            }
            if (!this.ensureTimePlayedPanelVisible()) {
                return null;
            }
            timePlayedWidget = Rs2Widget.getWidget((int)46661634);
            if (timePlayedWidget == null || timePlayedWidget.getText() == null || timePlayedWidget.getText().isEmpty()) {
                return null;
            }
        }
        return this.stripWidgetTags(timePlayedWidget.getText());
    }

    private boolean ensureTimePlayedPanelVisible() {
        Widget accountSummaryWidget;
        this.attemptedHoursPlayedLookup = true;
        Widget timePlayedWidget = Rs2Widget.getWidget((int)46661634);
        if (timePlayedWidget != null && Rs2Widget.isWidgetVisible((int)timePlayedWidget.getId())) {
            return true;
        }
        if (Rs2GrandExchange.isOpen() || Rs2Bank.isOpen() || Rs2Player.isMoving() || Rs2Player.isInteracting()) {
            return false;
        }
        Widget sideJournalTabContainer = Rs2Widget.getWidget((int)41222157);
        if (sideJournalTabContainer != null) {
            Rs2Widget.clickWidget((Widget)sideJournalTabContainer);
            SellScript.sleep((int)200, (int)350);
        }
        if ((accountSummaryWidget = Rs2Widget.getWidget((int)46661633)) != null) {
            Rs2Widget.clickWidget((Widget)accountSummaryWidget);
            SellScript.sleepUntil(() -> {
                Widget widget = Rs2Widget.getWidget((int)46661634);
                return widget != null && widget.getText() != null && !widget.getText().isEmpty();
            }, (int)2000);
        }
        return (timePlayedWidget = Rs2Widget.getWidget((int)46661634)) != null && timePlayedWidget.getText() != null && !timePlayedWidget.getText().isEmpty();
    }

    private String stripWidgetTags(String text) {
        return text == null ? null : text.replaceAll("<[^>]+>", "").trim();
    }

    private boolean ensureGrandExchangeOpen() {
        if (Rs2GrandExchange.isOpen()) {
            return true;
        }
        if (Rs2Bank.isOpen()) {
            Microbot.status = "Closing Bank";
            Rs2Bank.closeBank();
            SellScript.sleepUntil(() -> !Rs2Bank.isOpen(), (int)2000);
            return false;
        }
        Microbot.status = "Opening GE";
        this.debug("Opening GE for sell task | player={} bankOpen={} geOpen={}", Rs2Player.getWorldLocation(), Rs2Bank.isOpen(), Rs2GrandExchange.isOpen());
        if (Rs2GrandExchange.openExchange()) {
            SellScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
            return Rs2GrandExchange.isOpen();
        }
        if (this.interactGrandExchangeClerk()) {
            SellScript.sleepUntil(Rs2GrandExchange::isOpen, (int)3000);
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
            this.debug("Grand Exchange clerk not found for sell task | player={} area={}", Rs2Player.getWorldLocation(), this.targetArea.getDisplayName());
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
        if (item == null || System.currentTimeMillis() - this.lastActionAtMs < 1200L) {
            return false;
        }
        Microbot.status = "Selling " + item.getName();
        this.waitForGrandExchangeOfferInput();
        if (offered = Rs2GrandExchange.processOffer((GrandExchangeRequest)(request = GrandExchangeRequest.builder().action(GrandExchangeAction.SELL).itemName(item.getName()).quantity(item.getQuantity()).percent(-10).closeAfterCompletion(false).build()))) {
            this.lastActionAtMs = System.currentTimeMillis();
            SellScript.sleepUntil(() -> !Rs2Inventory.hasItem((String)item.getName(), (boolean)true), (int)5000);
        }
        this.debug("GE sell offer | item={} qty={} percent=-10 offered={} slots={} offerScreen={}",
                item.getName(),
                item.getQuantity(),
                offered,
                Rs2GrandExchange.isOpen() ? Rs2GrandExchange.getAvailableSlotsCount() : -1,
                Rs2GrandExchange.isOfferScreenOpen());
        return offered;
    }

    private void waitForGrandExchangeOfferInput() {
        SellScript.sleep(GE_OFFER_INPUT_DELAY_MS);
    }

    private void returnToGrandExchangeOverview() {
        if (System.currentTimeMillis() - this.lastActionAtMs < 1200L) {
            return;
        }
        Rs2GrandExchange.backToOverview();
        this.lastActionAtMs = System.currentTimeMillis();
        SellScript.sleepUntil(() -> !Rs2GrandExchange.isOfferScreenOpen(), (int)2000);
    }

    private WorldPoint getAreaCenter() {
        int centerX = (this.targetArea.getSouthWest().getX() + this.targetArea.getNorthEast().getX()) / 2;
        int centerY = (this.targetArea.getSouthWest().getY() + this.targetArea.getNorthEast().getY()) / 2;
        int plane = this.targetArea.getSouthWest().getPlane();
        return new WorldPoint(centerX, centerY, plane);
    }

    private void debug(String message, Object ... args) {
        if (this.debugLogging) {
            KspTaskDebug.info(log, true, "GE Sell", message, args);
        }
    }

    public void shutdown() {
        this.state = SellState.GOING_TO_GE;
        this.lastWebWalkAtMs = 0L;
        this.lastActionAtMs = 0L;
        this.blockedSellItems.clear();
        this.tradeRestrictionUnlockedCache = null;
        this.cachedHoursPlayed = null;
        this.lastTradeRestrictionCheckAtMs = 0L;
        this.attemptedHoursPlayedLookup = false;
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

