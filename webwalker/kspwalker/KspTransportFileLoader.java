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

public final class KspTransportFileLoader
{
    private static final Logger log = LoggerFactory.getLogger(KspTransportFileLoader.class);

    public static final String TRANSPORT_EDGE_PREFIX = "transport:file:";
    public static final String DEFAULT_FILE_NAME = "kspwalker_transports.txt";

    private static final Pattern ANGLE_FORMAT = Pattern.compile(
        "^(NPC|NPC_ID|OBJECT|OBJECT_ID)\\s+(.+?)\\s*@\\s*([0-9, ]+)\\s*<([^>]+)>\\s*([0-9, ]+)(?:\\s*\\|\\s*(.*))?$",
        Pattern.CASE_INSENSITIVE
    );

    private KspTransportFileLoader()
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

    public static KspTransportLoadResult reloadDefaultFile(KspWebGraph graph)
    {
        Path file = getDefaultFilePath();

        try
        {
            ensureDefaultFile(file);
            return reload(graph, file);
        }
        catch (IOException ex)
        {
            log.warn("Failed to load KSP walker transports from {}", file, ex);
            List<String> errors = new ArrayList<>();
            errors.add("IO error: " + ex.getMessage());
            return new KspTransportLoadResult(file, 0, errors);
        }
    }

    public static KspTransportLoadResult reload(KspWebGraph graph, Path file) throws IOException
    {
        if (graph == null)
        {
            List<String> errors = new ArrayList<>();
            errors.add("Graph is null");
            return new KspTransportLoadResult(file, 0, errors);
        }

        removeExistingTransportEdges(graph);

        if (file == null)
        {
            List<String> errors = new ArrayList<>();
            errors.add("Transport file is null");
            return new KspTransportLoadResult(null, 0, errors);
        }

        if (!Files.exists(file))
        {
            ensureDefaultFile(file);
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> errors = new ArrayList<>();
        int loaded = 0;

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
                KspWebEdge edge = parseLine(line, lineNumber);

                if (edge != null)
                {
                    graph.add(edge);
                    loaded++;
                }
            }
            catch (IllegalArgumentException ex)
            {
                errors.add("line " + lineNumber + ": " + ex.getMessage() + " | raw=" + rawLine);
            }
        }

        return new KspTransportLoadResult(file, loaded, errors);
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

    private static void removeExistingTransportEdges(KspWebGraph graph)
    {
        List<KspWebEdge> existing = graph.getEdges();

        for (KspWebEdge edge : existing)
        {
            if (edge != null && edge.getId() != null && edge.getId().startsWith(TRANSPORT_EDGE_PREFIX))
            {
                graph.remove(edge.getId());
            }
        }
    }

    private static KspWebEdge parseLine(String line, int lineNumber)
    {
        if (line.contains("|"))
        {
            return parsePipeFormat(line, lineNumber);
        }

        Matcher matcher = ANGLE_FORMAT.matcher(line);

        if (matcher.matches())
        {
            String type = matcher.group(1);
            String nameOrId = matcher.group(2).trim();
            WorldPoint start = parseWorldPoint(matcher.group(3), "start tile");
            String action = matcher.group(4).trim();
            WorldPoint end = parseWorldPoint(matcher.group(5), "end tile");
            String dialogueRaw = matcher.group(6);
            String[] dialogue = parseDialogue(dialogueRaw);

            return buildEdge(type, nameOrId, start, action, end, dialogue, lineNumber);
        }

        throw new IllegalArgumentException("Invalid transport format");
    }

    private static KspWebEdge parsePipeFormat(String line, int lineNumber)
    {
        String[] parts = line.split("\\|");

        if (parts.length < 5)
        {
            throw new IllegalArgumentException("Pipe format requires at least 5 fields: TYPE|NAME_OR_ID|START|ACTION|END");
        }

        String type = parts[0].trim();
        String nameOrId = parts[1].trim();
        WorldPoint start = parseWorldPoint(parts[2], "start tile");
        String action = parts[3].trim();
        WorldPoint end = parseWorldPoint(parts[4], "end tile");

        String[] dialogue = new String[0];

        if (parts.length > 5)
        {
            dialogue = new String[parts.length - 5];

            for (int i = 5; i < parts.length; i++)
            {
                dialogue[i - 5] = parts[i].trim();
            }
        }

        return buildEdge(type, nameOrId, start, action, end, dialogue, lineNumber);
    }

    private static KspWebEdge buildEdge(
        String rawType,
        String nameOrId,
        WorldPoint start,
        String action,
        WorldPoint end,
        String[] dialogue,
        int lineNumber
    )
    {
        if (rawType == null || rawType.isBlank())
        {
            throw new IllegalArgumentException("Missing type");
        }

        if (nameOrId == null || nameOrId.isBlank())
        {
            throw new IllegalArgumentException("Missing NPC/object name or ID");
        }

        if (action == null || action.isBlank())
        {
            throw new IllegalArgumentException("Missing menu action");
        }

        action = normalizeAction(action);

        String type = rawType.trim().toUpperCase(Locale.ROOT);
        String id = TRANSPORT_EDGE_PREFIX + sanitize(type) + ":" + lineNumber + ":" + sanitize(nameOrId);

        KspWebEdge.Builder builder;

        switch (type)
        {
            case "NPC":
                builder = KspWebEdge.builder(id, KspWebEdgeType.NPC, start, end)
                    .npcName(nameOrId)
                    .action(action)
                    .cost(cost(start, end, 12));
                break;

            case "NPC_ID":
                builder = KspWebEdge.builder(id, KspWebEdgeType.NPC, start, end)
                    .npcId(parseId(nameOrId, "NPC_ID"))
                    .action(action)
                    .cost(cost(start, end, 12));
                break;

            case "OBJECT":
                builder = KspWebEdge.builder(id, KspWebEdgeType.OBJECT, start, end)
                    .objectName(nameOrId)
                    .action(action)
                    .cost(cost(start, end, 8));
                break;

            case "OBJECT_ID":
                builder = KspWebEdge.builder(id, KspWebEdgeType.OBJECT, start, end)
                    .objectId(parseId(nameOrId, "OBJECT_ID"))
                    .action(action)
                    .cost(cost(start, end, 8));
                break;

            default:
                throw new IllegalArgumentException("Unknown type '" + rawType + "'. Use NPC, NPC_ID, OBJECT, or OBJECT_ID");
        }

        if (dialogue != null && dialogue.length > 0)
        {
            builder.dialogueOptions(dialogue);
        }

        return builder.build();
    }

    private static String normalizeAction(String action)
    {
        if (action == null)
        {
            return "";
        }

        String trimmed = action.trim();

        if (trimmed.equalsIgnoreCase("Pay-fare")
            || trimmed.equalsIgnoreCase("Pay-Fare")
            || trimmed.equalsIgnoreCase("Pay fare"))
        {
            return "Travel";
        }

        return trimmed;
    }

    private static int cost(WorldPoint start, WorldPoint end, int actionCost)
    {
        if (start != null && end != null && start.getPlane() == end.getPlane())
        {
            return Math.max(1, start.distanceTo2D(end)) + actionCost;
        }

        return actionCost + 25;
    }

    private static WorldPoint parseWorldPoint(String raw, String label)
    {
        if (raw == null || raw.isBlank())
        {
            throw new IllegalArgumentException("Missing " + label);
        }

        String cleaned = raw.trim()
            .replace(" ", "");

        String[] parts = cleaned.split(",");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException(label + " must be x,y,plane");
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
            throw new IllegalArgumentException(label + " contains invalid number: " + raw);
        }
    }

    private static int parseId(String value, String label)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(label + " must be a number: " + value);
        }
    }

    private static String[] parseDialogue(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return new String[0];
        }

        String[] parts = raw.split("\\|");
        List<String> options = new ArrayList<>();

        for (String part : parts)
        {
            String trimmed = part.trim();

            if (!trimmed.isBlank())
            {
                options.add(trimmed);
            }
        }

        return options.toArray(new String[0]);
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

    private static String sanitize(String value)
    {
        if (value == null)
        {
            return "unknown";
        }

        String sanitized = value.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("_+", "_");

        if (sanitized.startsWith("_"))
        {
            sanitized = sanitized.substring(1);
        }

        if (sanitized.endsWith("_"))
        {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    public static String defaultFileContent()
    {
        return String.join(
            System.lineSeparator(),
            "# KSP Walker transports",
            "# File: " + getDefaultFilePath(),
            "#",
            "# Simple format:",
            "# NPC Name @ StartX,StartY,Plane <Menu entry> EndX,EndY,Plane | Optional dialogue",
            "#",
            "# Port Sarim -> Karamja presets:",
            "NPC Captain Tobias @ 3029,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.",
            "NPC Seaman Lorris @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.",
            "NPC Seaman Thresnor @ 3027,3217,0 <Travel> 2956,3146,0 | Yes please. | Yes, please.",
            "#",
            "# More examples:",
            "# OBJECT Door @ 3234,3383,0 <Open> 3235,3383,0",
            "# OBJECT_ID 1530 @ 3234,3383,0 <Open> 3235,3383,0",
            "# NPC_ID 3645 @ 3028,3217,0 <Travel> 2956,3146,0 | Yes please.",
            "#",
            "# Add more transports below:"
        ) + System.lineSeparator();
    }
}
