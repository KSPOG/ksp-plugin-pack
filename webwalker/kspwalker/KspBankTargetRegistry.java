package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

public final class KspBankTargetRegistry
{
    private static final WorldPoint DEFAULT_GRAND_EXCHANGE = new WorldPoint(3164, 3487, 0);

    private static final List<KspWalkerDestination> DEFAULT_BANKS = new ArrayList<>();
    private static final List<KspWalkerDestination> FILE_BANKS = new ArrayList<>();

    static
    {
        addDefault("Grand Exchange", 3164, 3487, 0, false);
        addDefault("Varrock West Bank", 3185, 3436, 0, false);
        addDefault("Varrock East Bank", 3253, 3420, 0, false);
        addDefault("Edgeville Bank", 3094, 3491, 0, false);
        addDefault("Falador West Bank", 2946, 3368, 0, false);
        addDefault("Falador East Bank", 3013, 3355, 0, false);
        addDefault("Draynor Bank", 3092, 3243, 0, false);
        addDefault("Lumbridge Castle Bank", 3208, 3220, 2, false);
        addDefault("Al Kharid Bank", 3269, 3167, 0, false);
        addDefault("Port Sarim Deposit Box", 3045, 3235, 0, false);

        addDefault("Clan Hall Bank", 3204, 3478, 0, true);
        addDefault("Seers' Village Bank", 2725, 3492, 0, true);
        addDefault("Catherby Bank", 2809, 3441, 0, true);
        addDefault("Ardougne North Bank", 2616, 3332, 0, true);
        addDefault("Ardougne South Bank", 2655, 3283, 0, true);
        addDefault("Yanille Bank", 2612, 3093, 0, true);
        addDefault("Castle Wars Bank", 2443, 3083, 0, true);
        addDefault("Duel Arena Bank", 3381, 3268, 0, true);
        addDefault("Canifis Bank", 3512, 3480, 0, true);
        addDefault("Ferox Enclave Bank", 3130, 3631, 0, true);
        addDefault("Shayzien Bank", 1504, 3622, 0, true);
        addDefault("Farming Guild Bank", 1250, 3748, 0, true);
    }

    private KspBankTargetRegistry()
    {
    }

    public static synchronized KspWalkerDestination grandExchange()
    {
        return findBankByName("Grand Exchange")
            .orElse(new KspWalkerDestination("Grand Exchange", KspWalkerDestinationType.GRAND_EXCHANGE, DEFAULT_GRAND_EXCHANGE, false));
    }

    public static synchronized Optional<KspWalkerDestination> nearestBank()
    {
        WorldPoint player = Rs2Player.getWorldLocation();

        if (player == null)
        {
            return Optional.empty();
        }

        boolean members = KspMembershipDetector.isMembers();

        return getBanks().stream()
            .filter(bank -> isUsableBank(bank, members))
            .min(Comparator.comparingInt(destination -> distanceScore(player, destination.getPoint())));
    }

    public static synchronized Optional<KspWalkerDestination> findBankByName(String name)
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }

        String wanted = normalizeName(name);
        boolean members = KspMembershipDetector.isMembers();

        /*
         * File banks win over defaults, so bank.txt can correct old hardcoded coords.
         */
        Optional<KspWalkerDestination> fileMatch = FILE_BANKS.stream()
            .filter(bank -> isUsableBank(bank, members))
            .filter(bank -> normalizeName(bank.getName()).equals(wanted))
            .findFirst();

        if (fileMatch.isPresent())
        {
            return fileMatch;
        }

        return DEFAULT_BANKS.stream()
            .filter(bank -> isUsableBank(bank, members))
            .filter(bank -> normalizeName(bank.getName()).equals(wanted))
            .findFirst();
    }

    public static synchronized List<KspWalkerDestination> getBanks()
    {
        List<KspWalkerDestination> banks = new ArrayList<>(DEFAULT_BANKS);
        banks.addAll(FILE_BANKS);
        return banks;
    }

    public static synchronized List<KspWalkerDestination> getUsableBanks()
    {
        boolean members = KspMembershipDetector.isMembers();
        List<KspWalkerDestination> banks = new ArrayList<>();

        for (KspWalkerDestination bank : getBanks())
        {
            if (isUsableBank(bank, members))
            {
                banks.add(bank);
            }
        }

        return banks;
    }

    public static synchronized List<KspWalkerDestination> getFileBanks()
    {
        return new ArrayList<>(FILE_BANKS);
    }

    public static synchronized void replaceFileBanks(List<KspWalkerDestination> banks)
    {
        FILE_BANKS.clear();

        if (banks == null)
        {
            return;
        }

        for (KspWalkerDestination bank : banks)
        {
            if (bank == null || bank.getPoint() == null)
            {
                continue;
            }

            FILE_BANKS.add(new KspWalkerDestination(
                bank.getName(),
                KspWalkerDestinationType.NEAREST_BANK,
                bank.getPoint(),
                bank.isMembersOnly()
            ));
        }
    }

    public static synchronized void clearFileBanks()
    {
        FILE_BANKS.clear();
    }

    private static boolean isUsableBank(KspWalkerDestination bank, boolean members)
    {
        return bank != null && bank.getPoint() != null && (!bank.isMembersOnly() || members);
    }

    private static void addDefault(String name, int x, int y, int plane, boolean members)
    {
        DEFAULT_BANKS.add(new KspWalkerDestination(
            name,
            KspWalkerDestinationType.NEAREST_BANK,
            new WorldPoint(x, y, plane),
            members
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

    private static String normalizeName(String name)
    {
        return name == null
            ? ""
            : name.trim().toLowerCase().replace(" bank", "").replaceAll("\\s+", " ");
    }
}
