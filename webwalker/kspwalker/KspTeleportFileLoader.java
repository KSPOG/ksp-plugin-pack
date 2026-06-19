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

public final class KspTeleportFileLoader
{
    private static final Logger log = LoggerFactory.getLogger(KspTeleportFileLoader.class);

    public static final String DEFAULT_FILE_NAME = "kspwalker_teleports.txt";
    public static final String OBJECT_EDGE_PREFIX = "teleport-object:file:";
    public static final String TELEPORT_OPTION_PREFIX = "teleport:file:";

    private static final Pattern OBJECT_ANGLE_FORMAT = Pattern.compile(
        "^(OBJECT_TELEPORT|OBJECT_TELEPORT_ID|OBJECT|OBJECT_ID)\\s+(.+?)\\s*@\\s*([0-9, ]+)\\s*<([^>]+)>\\s*([0-9, ]+)(?:\\s*\\|\\s*(.*))?$",
        Pattern.CASE_INSENSITIVE
    );

    private KspTeleportFileLoader()
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

    public static KspTeleportFileLoadResult reloadDefaultFile(KspWebGraph graph, KspTeleportRegistry registry)
    {
        Path file = getDefaultFilePath();

        try
        {
            ensureDefaultFile(file);
            return reload(graph, registry, file);
        }
        catch (IOException ex)
        {
            log.warn("Failed to load KSP walker teleports from {}", file, ex);
            List<String> errors = new ArrayList<>();
            errors.add("IO error: " + ex.getMessage());
            return new KspTeleportFileLoadResult(file, 0, 0, 0, errors);
        }
    }

    public static KspTeleportFileLoadResult reload(
        KspWebGraph graph,
        KspTeleportRegistry registry,
        Path file
    ) throws IOException
    {
        List<String> errors = new ArrayList<>();
        int objectTeleports = 0;
        int itemTeleports = 0;
        int spellTeleports = 0;

        if (graph == null)
        {
            errors.add("Graph is null");
            return new KspTeleportFileLoadResult(file, 0, 0, 0, errors);
        }

        if (registry == null)
        {
            errors.add("Teleport registry is null");
            return new KspTeleportFileLoadResult(file, 0, 0, 0, errors);
        }

        removeExisting(graph, registry);

        if (file == null)
        {
            errors.add("Teleport file is null");
            return new KspTeleportFileLoadResult(null, 0, 0, 0, errors);
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
                String type = firstToken(line).toUpperCase(Locale.ROOT);

                if (type.equals("OBJECT_TELEPORT") || type.equals("OBJECT_TELEPORT_ID"))
                {
                    KspWebEdge edge = parseObjectTeleport(line, lineNumber);
                    graph.add(edge);
                    objectTeleports++;
                }
                else if (type.equals("ITEM_TELEPORT"))
                {
                    KspTeleportOption option = parseItemTeleport(line, lineNumber);
                    registry.add(option);
                    itemTeleports++;
                }
                else if (type.equals("SPELL_TELEPORT"))
                {
                    KspTeleportOption option = parseSpellTeleport(line, lineNumber);
                    registry.add(option);
                    spellTeleports++;
                }
                else
                {
                    throw new IllegalArgumentException(
                        "Unknown teleport type '" + type + "'. Use OBJECT_TELEPORT, ITEM_TELEPORT, or SPELL_TELEPORT"
                    );
                }
            }
            catch (IllegalArgumentException ex)
            {
                errors.add("line " + lineNumber + ": " + ex.getMessage() + " | raw=" + rawLine);
            }
        }

        return new KspTeleportFileLoadResult(file, objectTeleports, itemTeleports, spellTeleports, errors);
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

    private static void removeExisting(KspWebGraph graph, KspTeleportRegistry registry)
    {
        List<KspWebEdge> edges = graph.getEdges();

        for (KspWebEdge edge : edges)
        {
            if (edge != null && edge.getId() != null && edge.getId().startsWith(OBJECT_EDGE_PREFIX))
            {
                graph.remove(edge.getId());
            }
        }

        List<KspTeleportOption> options = registry.getOptions();

        for (KspTeleportOption option : options)
        {
            if (option != null && option.getId() != null && option.getId().startsWith(TELEPORT_OPTION_PREFIX))
            {
                registry.remove(option.getId());
            }
        }
    }

    private static KspWebEdge parseObjectTeleport(String line, int lineNumber)
    {
        Matcher matcher = OBJECT_ANGLE_FORMAT.matcher(line);

        if (!matcher.matches())
        {
            throw new IllegalArgumentException(
                "Object teleport format: OBJECT_TELEPORT Spirit tree @ 3184,3509,0 <Travel> 2461,3444,0 | Dialogue option"
            );
        }

        String type = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        String objectNameOrId = matcher.group(2).trim();
        WorldPoint start = parseWorldPoint(matcher.group(3), "start tile");
        String action = matcher.group(4).trim();
        WorldPoint end = parseWorldPoint(matcher.group(5), "end tile");
        String[] dialogue = splitOptions(matcher.group(6));

        String id = OBJECT_EDGE_PREFIX + lineNumber + ":" + sanitize(objectNameOrId);
        KspWebEdge.Builder builder = KspWebEdge.builder(id, KspWebEdgeType.OBJECT, start, end)
            .action(action)
            .cost(cost(start, end, 8));

        if (type.endsWith("_ID"))
        {
            builder.objectId(parseId(objectNameOrId, "object teleport ID"));
        }
        else
        {
            builder.objectName(objectNameOrId);
        }

        if (dialogue.length > 0)
        {
            builder.dialogueOptions(dialogue);
        }

        return builder.build();
    }

    private static KspTeleportOption parseItemTeleport(String line, int lineNumber)
    {
        String body = removeFirstToken(line);
        String[] parts = splitPipe(body);

        if (parts.length < 3)
        {
            throw new IllegalArgumentException(
                "Item teleport format: ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0"
            );
        }

        String itemName = parts[0].trim();
        String action = parts[1].trim().isEmpty() ? "Rub" : parts[1].trim();
        WorldPoint destination = findWorldPoint(parts, "item teleport destination");
        String[] dialogue = fieldsExceptCoordinates(parts, 2);
        String displayName = itemName + (dialogue.length > 0 ? " -> " + dialogue[0] : " -> " + format(destination));

        String id = TELEPORT_OPTION_PREFIX + "item:" + lineNumber + ":" + sanitize(itemName);

        return KspTeleportOption.item(
            id,
            displayName,
            destination,
            12,
            () -> KspTeleportActionExecutor.hasInventoryItem(itemName),
            () -> KspTeleportActionExecutor.itemTeleport(destination, itemName, action, dialogue)
        );
    }

    private static KspTeleportOption parseSpellTeleport(String line, int lineNumber)
    {
        String body = removeFirstToken(line);
        String[] parts = splitPipe(body);

        if (parts.length < 2)
        {
            throw new IllegalArgumentException(
                "Spell teleport format: SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1"
            );
        }

        String spellName = parts[0].trim();
        WorldPoint destination = findWorldPoint(parts, "spell teleport destination");
        List<RuneRequirement> runeRequirements = parseRuneRequirements(fieldsExceptCoordinatesAsText(parts, 1));
        String id = TELEPORT_OPTION_PREFIX + "spell:" + lineNumber + ":" + sanitize(spellName);

        return KspTeleportOption.spell(
            id,
            spellName,
            destination,
            35,
            () -> KspTeleportActionExecutor.hasSpellStringCasting() && hasRunes(runeRequirements),
            () -> KspTeleportActionExecutor.spellTeleport(destination, spellName)
        );
    }

    private static boolean hasRunes(List<RuneRequirement> requirements)
    {
        for (RuneRequirement requirement : requirements)
        {
            if (KspTeleportActionExecutor.inventoryQuantity(requirement.name) < requirement.amount)
            {
                return false;
            }
        }

        return true;
    }

    private static List<RuneRequirement> parseRuneRequirements(String text)
    {
        List<RuneRequirement> requirements = new ArrayList<>();

        if (text == null || text.isBlank())
        {
            return requirements;
        }

        String[] pieces = text.split(",");

        for (String piece : pieces)
        {
            String trimmed = piece.trim();

            if (trimmed.isBlank())
            {
                continue;
            }

            Matcher matcher = Pattern.compile("^(.*?)(?:\\s+x?<?(\\d+)>?)?$", Pattern.CASE_INSENSITIVE)
                .matcher(trimmed);

            if (!matcher.matches())
            {
                continue;
            }

            String name = matcher.group(1).trim();
            String amountText = matcher.group(2);
            int amount = amountText == null || amountText.isBlank() ? 1 : Integer.parseInt(amountText);

            if (!name.isBlank() && amount > 0)
            {
                requirements.add(new RuneRequirement(name, amount));
            }
        }

        return requirements;
    }

    private static WorldPoint findWorldPoint(String[] parts, String label)
    {
        for (String part : parts)
        {
            if (looksLikeWorldPoint(part))
            {
                return parseWorldPoint(part, label);
            }
        }

        throw new IllegalArgumentException("Missing " + label + " coordinate x,y,plane");
    }

    private static String[] fieldsExceptCoordinates(String[] parts, int startIndex)
    {
        List<String> fields = new ArrayList<>();

        for (int i = startIndex; i < parts.length; i++)
        {
            String field = parts[i].trim();

            if (field.isBlank() || looksLikeWorldPoint(field))
            {
                continue;
            }

            fields.add(field);
        }

        return fields.toArray(new String[0]);
    }

    private static String fieldsExceptCoordinatesAsText(String[] parts, int startIndex)
    {
        List<String> fields = new ArrayList<>();

        for (int i = startIndex; i < parts.length; i++)
        {
            String field = parts[i].trim();

            if (field.isBlank() || looksLikeWorldPoint(field))
            {
                continue;
            }

            fields.add(field);
        }

        return String.join(", ", fields);
    }

    private static boolean looksLikeWorldPoint(String raw)
    {
        if (raw == null)
        {
            return false;
        }

        return raw.trim().replace(" ", "").matches("\\d+,\\d+,\\d+");
    }

    private static WorldPoint parseWorldPoint(String raw, String label)
    {
        if (raw == null || raw.isBlank())
        {
            throw new IllegalArgumentException("Missing " + label);
        }

        String[] parts = raw.trim().replace(" ", "").split(",");

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

    private static String[] splitPipe(String raw)
    {
        String[] parts = raw.split("\\|");
        List<String> cleaned = new ArrayList<>();

        for (String part : parts)
        {
            String trimmed = part.trim();

            if (!trimmed.isBlank())
            {
                cleaned.add(trimmed);
            }
        }

        return cleaned.toArray(new String[0]);
    }

    private static String[] splitOptions(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return new String[0];
        }

        return splitPipe(raw);
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

    private static String firstToken(String line)
    {
        String trimmed = line == null ? "" : line.trim();
        int index = trimmed.indexOf(' ');

        if (index < 0)
        {
            return trimmed;
        }

        return trimmed.substring(0, index);
    }

    private static String removeFirstToken(String line)
    {
        String trimmed = line == null ? "" : line.trim();
        int index = trimmed.indexOf(' ');

        if (index < 0)
        {
            return "";
        }

        return trimmed.substring(index + 1).trim();
    }

    private static int cost(WorldPoint start, WorldPoint end, int actionCost)
    {
        if (start != null && end != null && start.getPlane() == end.getPlane())
        {
            return Math.max(1, start.distanceTo2D(end)) + actionCost;
        }

        return actionCost + 25;
    }

    private static String format(WorldPoint point)
    {
        if (point == null)
        {
            return "-";
        }

        return point.getX() + "," + point.getY() + "," + point.getPlane();
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
            "# KSP Walker teleports",
            "# File: " + getDefaultFilePath(),
            "#",
            "# OBJECT TELEPORTS",
            "# Format:",
            "# OBJECT_TELEPORT Object name @ StartX,StartY,Plane <Menu entry> EndX,EndY,Plane | Optional dialogue",
            "# Example Spirit tree line. Replace end tile/dialogue with the tree destination you want:",
            "OBJECT_TELEPORT Spirit tree @ 3184,3509,0 <Travel> 2461,3444,0 | Tree Gnome Stronghold",
            "#",
            "# ITEM TELEPORTS",
            "# Format:",
            "# ITEM_TELEPORT Item name | item action | dialogue/destination option | EndX,EndY,Plane",
            "ITEM_TELEPORT Amulet of glory | Rub | Edgeville | 3087,3496,0",
            "ITEM_TELEPORT Amulet of glory | Rub | Draynor Village | 3105,3251,0",
            "#",
            "# SPELL TELEPORTS",
            "# Format:",
            "# SPELL_TELEPORT Spell name | EndX,EndY,Plane | Rune name amount, Rune name amount",
            "SPELL_TELEPORT Varrock Teleport | 3212,3424,0 | Law rune 1, Air rune 3, Fire rune 1",
            "#",
            "# Notes:",
            "# - Object teleports are graph transports.",
            "# - Item teleports are registered in the teleport planner.",
            "# - Spell teleports are registered only if this Microbot branch exposes a string-based Rs2Magic cast method.",
            "# - Add more teleport definitions below:"
        ) + System.lineSeparator();
    }

    private static final class RuneRequirement
    {
        private final String name;
        private final int amount;

        private RuneRequirement(String name, int amount)
        {
            this.name = name;
            this.amount = amount;
        }
    }
}
