package net.runelite.client.plugins.microbot.kspwalker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KspBankFileLoader
{
    private static final Logger log = LoggerFactory.getLogger(KspBankFileLoader.class);

    public static final String DEFAULT_FILE_NAME = "bank.txt";

    private static final Pattern PIPE_FORMAT = Pattern.compile(
        "^(.+?)\\s*\\|\\s*([0-9]+\\s*,\\s*[0-9]+\\s*,\\s*[0-9]+)(?:\\s*\\|\\s*(yes|no|y|n|true|false|member|members|p2p|free|f2p))?\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SPACE_FORMAT = Pattern.compile(
        "^(.+?)\\s+([0-9]+\\s*,\\s*[0-9]+\\s*,\\s*[0-9]+)(?:\\s+(yes|no|y|n|true|false|member|members|p2p|free|f2p))?\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private KspBankFileLoader()
    {
    }

    public static Path getDefaultFilePath()
    {
        return Path.of(
            System.getProperty("user.home"),
            ".kspwalker",
            DEFAULT_FILE_NAME
        );
    }

    public static KspBankFileLoadResult reloadDefaultFile()
    {
        Path file = getDefaultFilePath();

        try
        {
            ensureDefaultFile(file);
            return reload(file);
        }
        catch (IOException ex)
        {
            log.warn("Failed to load KSP walker bank file from {}", file, ex);
            List<String> errors = new ArrayList<>();
            errors.add("IO error: " + ex.getMessage());
            return new KspBankFileLoadResult(file, 0, errors);
        }
    }

    public static KspBankFileLoadResult reload(Path file) throws IOException
    {
        List<String> errors = new ArrayList<>();
        List<KspWalkerDestination> banks = new ArrayList<>();

        if (file == null)
        {
            errors.add("Bank file is null");
            KspBankTargetRegistry.replaceFileBanks(banks);
            return new KspBankFileLoadResult(null, 0, errors);
        }

        if (!Files.exists(file))
        {
            ensureDefaultFile(file);
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++)
        {
            String rawLine = lines.get(i);
            int lineNumber = i + 1;
            String line = stripComment(rawLine).trim();

            if (line.isEmpty())
            {
                continue;
            }

            try
            {
                banks.add(parseLine(line));
            }
            catch (IllegalArgumentException ex)
            {
                errors.add("line " + lineNumber + ": " + ex.getMessage() + " | raw=" + rawLine);
            }
        }

        KspBankTargetRegistry.replaceFileBanks(banks);
        return new KspBankFileLoadResult(file, banks.size(), errors);
    }

    public static void ensureDefaultFile(Path file) throws IOException
    {
        if (file == null)
        {
            return;
        }

        Path parent = file.getParent();

        if (parent != null && !Files.exists(parent))
        {
            Files.createDirectories(parent);
        }

        if (Files.exists(file))
        {
            return;
        }

        Files.writeString(file, defaultFileContent(), StandardCharsets.UTF_8);
    }

    private static KspWalkerDestination parseLine(String line)
    {
        Matcher pipe = PIPE_FORMAT.matcher(line);

        if (pipe.matches())
        {
            return createBank(pipe.group(1), pipe.group(2), pipe.group(3));
        }

        Matcher space = SPACE_FORMAT.matcher(line);

        if (space.matches())
        {
            return createBank(space.group(1), space.group(2), space.group(3));
        }

        throw new IllegalArgumentException("Expected: Bank name | x,y,plane | yes/no");
    }

    private static KspWalkerDestination createBank(String name, String coords, String membersText)
    {
        String bankName = name == null ? "" : name.trim();

        if (bankName.isBlank())
        {
            throw new IllegalArgumentException("Bank name is blank");
        }

        return new KspWalkerDestination(
            bankName,
            KspWalkerDestinationType.NEAREST_BANK,
            parseWorldPoint(coords),
            parseMembersFlag(membersText)
        );
    }

    private static boolean parseMembersFlag(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            /*
             * Backwards compatibility: old two-field lines are assumed F2P unless
             * explicitly marked as members-only.
             */
            return false;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);

        return value.equals("yes")
            || value.equals("y")
            || value.equals("true")
            || value.equals("member")
            || value.equals("members")
            || value.equals("p2p");
    }

    private static WorldPoint parseWorldPoint(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            throw new IllegalArgumentException("Missing coords");
        }

        String[] parts = raw.replace(" ", "").split(",");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException("Coords must be x,y,plane");
        }

        try
        {
            return new WorldPoint(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("Coords contain invalid number: " + raw);
        }
    }

    private static String stripComment(String line)
    {
        if (line == null)
        {
            return "";
        }

        int hash = line.indexOf('#');

        if (hash >= 0)
        {
            return line.substring(0, hash);
        }

        return line;
    }

    public static String defaultFileContent()
    {
        return String.join(
            System.lineSeparator(),
            "# KSP Walker bank list",
            "# File: " + getDefaultFilePath(),
            "#",
            "# Format:",
            "# BankName | x,y,plane | members yes/no",
            "#",
            "# members=no  -> free-to-play bank",
            "# members=yes -> members-only bank",
            "#",
            "Varrock East | 3251,3420,0 | no",
            "Varrock West | 3182,3440,0 | no",
            "Grand Exchange | 3164,3486,0 | no",
            "Edgeville | 3094,3491,0 | no",
            "Draynor | 3092,3243,0 | no",
            "Al Kharid | 3269,3167,0 | no",
            "Falador East | 3013,3355,0 | no",
            "Falador West | 2946,3368,0 | no",
            "Lumbridge Castle | 3208,3220,2 | no",
            "Port Sarim Deposit Box | 3045,3235,0 | no",
            "Ferox Enclave | 3130,3631,0 | yes",
            "Catherby | 2809,3441,0 | yes",
            "Seers' Village | 2725,3492,0 | yes",
            "Ardougne North | 2616,3332,0 | yes",
            "Ardougne South | 2655,3283,0 | yes",
            "Yanille | 2612,3093,0 | yes",
            "#",
            "# Add more banks below:"
        ) + System.lineSeparator();
    }
}
