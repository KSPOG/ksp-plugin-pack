package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspBankTargetRegistry
{
    private static final WorldPoint GRAND_EXCHANGE = new WorldPoint(3164, 3487, 0);

    private static final List<KspWalkerDestination> BANKS = new ArrayList<>();

    static
    {
        add("Grand Exchange", 3164, 3487, 0);
        add("Varrock West Bank", 3185, 3436, 0);
        add("Varrock East Bank", 3253, 3420, 0);
        add("Edgeville Bank", 3094, 3491, 0);
        add("Falador West Bank", 2946, 3368, 0);
        add("Falador East Bank", 3013, 3355, 0);
        add("Draynor Bank", 3092, 3243, 0);
        add("Lumbridge Castle Bank", 3208, 3220, 2);
        add("Al Kharid Bank", 3269, 3167, 0);
        add("Port Sarim Deposit Box", 3045, 3235, 0);
        add("Clan Hall Bank", 3204, 3478, 0);

        // Common member banks. Safe to keep here; nearest selection handles distance only.
        add("Seers' Village Bank", 2725, 3492, 0);
        add("Catherby Bank", 2809, 3441, 0);
        add("Ardougne North Bank", 2616, 3332, 0);
        add("Ardougne South Bank", 2655, 3283, 0);
        add("Yanille Bank", 2612, 3093, 0);
        add("Castle Wars Bank", 2443, 3083, 0);
        add("Duel Arena Bank", 3381, 3268, 0);
        add("Canifis Bank", 3512, 3480, 0);
        add("Ferox Enclave Bank", 3130, 3631, 0);
        add("Shayzien Bank", 1504, 3622, 0);
        add("Farming Guild Bank", 1250, 3748, 0);
    }

    private KspBankTargetRegistry()
    {
    }

    public static KspWalkerDestination grandExchange()
    {
        return new KspWalkerDestination("Grand Exchange", KspWalkerDestinationType.GRAND_EXCHANGE, GRAND_EXCHANGE);
    }

    public static Optional<KspWalkerDestination> nearestBank()
    {
        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null)
        {
            return Optional.empty();
        }

        return BANKS.stream()
            .min(Comparator.comparingInt(destination -> distanceScore(player, destination.getPoint())));
    }

    public static List<KspWalkerDestination> getBanks()
    {
        return new ArrayList<>(BANKS);
    }

    private static void add(String name, int x, int y, int plane)
    {
        BANKS.add(new KspWalkerDestination(
            name,
            KspWalkerDestinationType.NEAREST_BANK,
            new WorldPoint(x, y, plane)
        ));
    }

    private static int distanceScore(WorldPoint from, WorldPoint to)
    {
        if (from == null || to == null)
        {
            return Integer.MAX_VALUE;
        }

        int planePenalty = from.getPlane() == to.getPlane() ? 0 : 1000;
        return from.distanceTo2D(to) + planePenalty;
    }
}
