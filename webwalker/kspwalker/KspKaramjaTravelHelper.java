package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspKaramjaTravelHelper
{
    public static final int DEFAULT_KARAMJA_FARE_COINS = 30;

    private static final String COINS = "Coins";

    private KspKaramjaTravelHelper()
    {
    }

    public static KspWalkResult prepareFareIfNeeded(
        KspWebWalker walker,
        KspWalkerTesterConfig config,
        WorldPoint finalTarget
    )
    {
        if (walker == null || config == null || finalTarget == null)
        {
            return null;
        }

        if (!config.autoWithdrawKaramjaFare())
        {
            return null;
        }

        if (!isMusaPointOrKaramjaTarget(finalTarget))
        {
            return null;
        }

        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null)
        {
            return KspWalkResult.waiting(finalTarget, "Karamja fare check waiting: player location unavailable");
        }

        if (isMusaPointOrKaramjaTarget(player))
        {
            return null;
        }

        int requiredCoins = Math.max(0, config.karamjaFareCoins());
        int inventoryCoins = inventoryCoins();

        if (inventoryCoins >= requiredCoins)
        {
            if (Rs2Bank.isOpen())
            {
                Rs2Bank.closeBank();
                return KspWalkResult.waiting(finalTarget, "Karamja fare ready; closing bank");
            }

            return null;
        }

        int missingCoins = requiredCoins - inventoryCoins;

        if (Rs2Bank.isOpen())
        {
            if (Rs2Bank.count(COINS, true) < missingCoins)
            {
                return KspWalkResult.failed(
                    finalTarget,
                    "Karamja fare missing: need "
                        + missingCoins
                        + " more coins but bank has "
                        + Rs2Bank.count(COINS, true)
                );
            }

            boolean withdrew = Rs2Bank.withdrawX(COINS, missingCoins);

            return withdrew
                ? KspWalkResult.waiting(finalTarget, "Withdrawing Karamja fare coins: " + missingCoins)
                : KspWalkResult.waiting(finalTarget, "Trying to withdraw Karamja fare coins: " + missingCoins);
        }

        Optional<KspWalkerDestination> bank = KspBankTargetRegistry.nearestBank();

        if (bank.isEmpty())
        {
            return KspWalkResult.failed(finalTarget, "Karamja fare needed but nearest bank could not be resolved");
        }

        WorldPoint bankPoint = bank.get().getPoint();

        if (player.getPlane() == bankPoint.getPlane() && player.distanceTo2D(bankPoint) <= config.bankOpenDistance())
        {
            boolean opened = Rs2Bank.openBank();

            if (opened)
            {
                return KspWalkResult.waiting(finalTarget, "Opening bank to withdraw Karamja fare");
            }

            return KspWalkResult.waiting(finalTarget, "At bank but failed to open bank; retrying");
        }

        KspWalkResult walkToBank = walker.walkTo(bankPoint);

        return KspWalkResult.waiting(
            finalTarget,
            "Karamja fare needed. Walking to nearest bank: "
                + bank.get().getName()
                + " bankTarget="
                + format(bankPoint)
                + " walkStatus="
                + walkToBank.getStatus()
                + " walkMsg="
                + walkToBank.getMessage()
        );
    }

    public static boolean isMusaPointOrKaramjaTarget(WorldPoint point)
    {
        if (point == null || point.getPlane() != 0)
        {
            return false;
        }

        int x = point.getX();
        int y = point.getY();

        /*
         * Covers the common F2P Karamja/Musa Point region reached from Port Sarim:
         * dock, banana plantation, volcano/fishing area. This intentionally does not
         * try to classify every members-only Karamja subregion yet.
         */
        return x >= 2810 && x <= 2978 && y >= 3050 && y <= 3200;
    }

    private static int inventoryCoins()
    {
        try
        {
            return Rs2Inventory.itemQuantity(COINS, true);
        }
        catch (RuntimeException ex)
        {
            return Rs2Inventory.count(COINS, true);
        }
    }

    private static String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }
}
