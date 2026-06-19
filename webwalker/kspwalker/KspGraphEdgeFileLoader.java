package net.runelite.client.plugins.microbot.kspwalker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KspGraphEdgeFileLoader
{
    private static final Logger log = LoggerFactory.getLogger(KspGraphEdgeFileLoader.class);

    public static final String DEFAULT_FILE_NAME = "kspwalker_edges.txt";
    public static final String EDGE_PREFIX = "edge-db:file:";

    private KspGraphEdgeFileLoader()
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

    public static KspGraphEdgeFileLoadResult reloadDefaultFile(KspWebGraph graph)
    {
        Path file = getDefaultFilePath();

        try
        {
            ensureDefaultFile(file);
            return reload(graph, file);
        }
        catch (IOException ex)
        {
            log.warn("Failed to load KSP walker edge database from {}", file, ex);
            List<String> errors = new ArrayList<>();
            errors.add("IO error: " + ex.getMessage());
            return new KspGraphEdgeFileLoadResult(file, 0, errors);
        }
    }

    public static KspGraphEdgeFileLoadResult reload(KspWebGraph graph, Path file) throws IOException
    {
        List<String> errors = new ArrayList<>();
        int loaded = 0;

        if (graph == null)
        {
            errors.add("Graph is null");
            return new KspGraphEdgeFileLoadResult(file, 0, errors);
        }

        removeExisting(graph);

        if (file == null)
        {
            errors.add("Edge database file is null");
            return new KspGraphEdgeFileLoadResult(null, 0, errors);
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

        return new KspGraphEdgeFileLoadResult(file, loaded, errors);
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

    private static void removeExisting(KspWebGraph graph)
    {
        for (KspWebEdge edge : graph.getEdges())
        {
            if (edge != null && edge.getId() != null && edge.getId().startsWith(EDGE_PREFIX))
            {
                graph.remove(edge.getId());
            }
        }
    }

    private static KspWebEdge parseLine(String line, int lineNumber)
    {
        String[] parts = splitPipe(line);

        if (parts.length < 4)
        {
            throw new IllegalArgumentException("Expected pipe format. Example: OBJECT_EDGE | id | start | end | object | action | cost");
        }

        String type = parts[0].trim().toUpperCase(Locale.ROOT);

        switch (type)
        {
            case "WALK_EDGE":
            case "REGION_WALK":
                return parseWalk(parts, lineNumber);

            case "OBJECT_EDGE":
                return parseObject(parts, lineNumber, false);

            case "OBJECT_ID_EDGE":
                return parseObject(parts, lineNumber, true);

            case "NPC_EDGE":
                return parseNpc(parts, lineNumber, false);

            case "NPC_ID_EDGE":
                return parseNpc(parts, lineNumber, true);

            case "DIALOGUE_EDGE":
                return parseDialogue(parts, lineNumber);

            default:
                throw new IllegalArgumentException(
                    "Unknown edge type '" + type + "'. Use WALK_EDGE, OBJECT_EDGE, OBJECT_ID_EDGE, NPC_EDGE, NPC_ID_EDGE, or DIALOGUE_EDGE"
                );
        }
    }

    private static KspWebEdge parseWalk(String[] parts, int lineNumber)
    {
        if (parts.length < 4)
        {
            throw new IllegalArgumentException("WALK_EDGE format: WALK_EDGE | id | start | end | optional cost | optional REQ ...");
        }

        String rawId = parts[1].trim();
        WorldPoint start = parseWorldPoint(parts[2], "start");
        WorldPoint end = parseWorldPoint(parts[3], "end");
        int cost = optionalCost(parts, 4, cost(start, end, 1));
        BooleanSupplier requirement = KspRequirementParser.parse(KspRequirementParser.extractRequirement(parts, 4));

        return KspWebEdge.builder(edgeId("walk", rawId, lineNumber), KspWebEdgeType.WALK, start, end)
            .cost(cost)
            .requirement(requirement)
            .build();
    }

    private static KspWebEdge parseObject(String[] parts, int lineNumber, boolean byId)
    {
        if (parts.length < 6)
        {
            throw new IllegalArgumentException(
                "OBJECT_EDGE format: OBJECT_EDGE | id | start | end | object name/id | action | optional cost | optional REQ ... | optional dialogue"
            );
        }

        String rawId = parts[1].trim();
        WorldPoint start = parseWorldPoint(parts[2], "start");
        WorldPoint end = parseWorldPoint(parts[3], "end");
        String object = parts[4].trim();
        String action = parts[5].trim();
        int cost = optionalCost(parts, 6, cost(start, end, 8));
        String[] extra = extraFields(parts, 6);
        String req = KspRequirementParser.extractRequirement(extra, 0);
        String[] dialogue = KspRequirementParser.nonRequirementFields(extra, 0);

        KspWebEdge.Builder builder = KspWebEdge.builder(edgeId(byId ? "object_id" : "object", rawId, lineNumber), KspWebEdgeType.OBJECT, start, end)
            .action(action)
            .cost(cost)
            .requirement(KspRequirementParser.parse(req));

        if (byId)
        {
            builder.objectId(parseInt(object, "object id"));
        }
        else
        {
            builder.objectName(object);
        }

        if (dialogue.length > 0)
        {
            builder.dialogueOptions(dialogue);
        }

        return builder.build();
    }

    private static KspWebEdge parseNpc(String[] parts, int lineNumber, boolean byId)
    {
        if (parts.length < 6)
        {
            throw new IllegalArgumentException(
                "NPC_EDGE format: NPC_EDGE | id | start | end | npc name/id | action | optional cost | optional REQ ... | optional dialogue"
            );
        }

        String rawId = parts[1].trim();
        WorldPoint start = parseWorldPoint(parts[2], "start");
        WorldPoint end = parseWorldPoint(parts[3], "end");
        String npc = parts[4].trim();
        String action = parts[5].trim();
        int cost = optionalCost(parts, 6, cost(start, end, 12));
        String[] extra = extraFields(parts, 6);
        String req = KspRequirementParser.extractRequirement(extra, 0);
        String[] dialogue = KspRequirementParser.nonRequirementFields(extra, 0);

        KspWebEdge.Builder builder = KspWebEdge.builder(edgeId(byId ? "npc_id" : "npc", rawId, lineNumber), KspWebEdgeType.NPC, start, end)
            .action(action)
            .cost(cost)
            .requirement(KspRequirementParser.parse(req));

        if (byId)
        {
            builder.npcId(parseInt(npc, "npc id"));
        }
        else
        {
            builder.npcName(npc);
        }

        if (dialogue.length > 0)
        {
            builder.dialogueOptions(dialogue);
        }

        return builder.build();
    }

    private static KspWebEdge parseDialogue(String[] parts, int lineNumber)
    {
        if (parts.length < 5)
        {
            throw new IllegalArgumentException(
                "DIALOGUE_EDGE format: DIALOGUE_EDGE | id | start | end | dialogue option | optional cost | optional REQ ..."
            );
        }

        String rawId = parts[1].trim();
        WorldPoint start = parseWorldPoint(parts[2], "start");
        WorldPoint end = parseWorldPoint(parts[3], "end");
        String option = parts[4].trim();
        int cost = optionalCost(parts, 5, cost(start, end, 4));
        String[] extra = extraFields(parts, 5);
        String req = KspRequirementParser.extractRequirement(extra, 0);

        return KspWebEdge.builder(edgeId("dialogue", rawId, lineNumber), KspWebEdgeType.DIALOGUE, start, end)
            .cost(cost)
            .dialogueOptions(option)
            .requirement(KspRequirementParser.parse(req))
            .build();
    }

    private static int optionalCost(String[] parts, int index, int fallback)
    {
        if (parts == null || index >= parts.length)
        {
            return fallback;
        }

        String candidate = parts[index] == null ? "" : parts[index].trim();

        if (candidate.isBlank() || KspRequirementParser.isRequirementField(candidate))
        {
            return fallback;
        }

        try
        {
            return Math.max(1, Integer.parseInt(candidate));
        }
        catch (NumberFormatException ex)
        {
            return fallback;
        }
    }

    private static String[] extraFields(String[] parts, int startIndex)
    {
        if (parts == null || startIndex >= parts.length)
        {
            return new String[0];
        }

        int start = startIndex;

        /*
         * Skip optional numeric cost field.
         */
        if (start < parts.length)
        {
            try
            {
                Integer.parseInt(parts[start].trim());
                start++;
            }
            catch (NumberFormatException ignored)
            {
                // Not a cost field.
            }
        }

        List<String> out = new ArrayList<>();

        for (int i = start; i < parts.length; i++)
        {
            String field = parts[i] == null ? "" : parts[i].trim();

            if (!field.isBlank())
            {
                out.add(field);
            }
        }

        return out.toArray(new String[0]);
    }

    private static String[] splitPipe(String line)
    {
        String[] parts = line.split("\\|");
        List<String> cleaned = new ArrayList<>();

        for (String part : parts)
        {
            String value = part == null ? "" : part.trim();

            if (!value.isBlank())
            {
                cleaned.add(value);
            }
        }

        return cleaned.toArray(new String[0]);
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

    private static WorldPoint parseWorldPoint(String raw, String label)
    {
        if (raw == null || raw.isBlank())
        {
            throw new IllegalArgumentException("Missing " + label + " coordinate");
        }

        String[] parts = raw.replace(" ", "").split(",");

        if (parts.length != 3)
        {
            throw new IllegalArgumentException(label + " coordinate must be x,y,plane");
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
            throw new IllegalArgumentException(label + " coordinate contains invalid number: " + raw);
        }
    }

    private static int parseInt(String raw, String label)
    {
        try
        {
            return Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(label + " must be a number: " + raw);
        }
    }

    private static int cost(WorldPoint start, WorldPoint end, int actionCost)
    {
        if (start != null && end != null && start.getPlane() == end.getPlane())
        {
            return Math.max(1, start.distanceTo2D(end)) + actionCost;
        }

        return actionCost + 25;
    }

    private static String edgeId(String type, String rawId, int lineNumber)
    {
        return EDGE_PREFIX + type + ":" + lineNumber + ":" + sanitize(rawId);
    }

    private static String sanitize(String raw)
    {
        if (raw == null)
        {
            return "edge";
        }

        String cleaned = raw.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("_+", "_");

        if (cleaned.startsWith("_"))
        {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.endsWith("_"))
        {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned.isBlank() ? "edge" : cleaned;
    }

    public static String defaultFileContent()
    {
        return String.join(
            System.lineSeparator(),
            "# KSP Walker explicit edge database",
            "# File: " + getDefaultFilePath(),
            "#",
            "# This file is for edges that generic obstacle handling cannot reliably infer.",
            "# Use it for region links, unusual object actions, stairs, ladders, caves, gates, doors, and plane transitions.",
            "#",
            "# WALK / REGION EDGES",
            "# Format:",
            "# WALK_EDGE | id | start x,y,p | end x,y,p | optional cost | optional REQ ...",
            "WALK_EDGE | varrock_square_to_west_bank | 3212,3429,0 | 3185,3436,0 | 30",
            "#",
            "# OBJECT EDGES",
            "# Format:",
            "# OBJECT_EDGE | id | start x,y,p | end x,y,p | object name | action | optional cost | optional REQ ... | optional dialogue",
            "# OBJECT_ID_EDGE | id | start x,y,p | end x,y,p | object id | action | optional cost | optional REQ ... | optional dialogue",
            "OBJECT_EDGE | example_stairs_up | 3204,3207,0 | 3204,3207,1 | Staircase | Climb-up | 8",
            "OBJECT_EDGE | example_stairs_down | 3204,3207,1 | 3204,3207,0 | Staircase | Climb-down | 8",
            "#",
            "# NPC EDGES",
            "# Format:",
            "# NPC_EDGE | id | start x,y,p | end x,y,p | npc name | action | optional cost | optional REQ ... | optional dialogue",
            "# NPC_ID_EDGE | id | start x,y,p | end x,y,p | npc id | action | optional cost | optional REQ ... | optional dialogue",
            "NPC_EDGE | karamja_ship_port_sarim | 3028,3217,0 | 2956,3146,0 | Seaman Lorris | Travel | 25 | REQ coins>=30 | Yes please. | Yes, please.",
            "#",
            "# REQUIREMENTS",
            "# Supported examples:",
            "# REQ coins>=30",
            "# REQ item:Amulet of glory>=1",
            "# REQ bank:Coins>=30",
            "# REQ skill:Agility>=5",
            "# REQ varp:123=1",
            "# REQ varbit:456=0",
            "#",
            "# Add more explicit world/region edges below:"
        ) + System.lineSeparator();
    }
}
