package net.runelite.client.plugins.microbot.kspwalker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public final class KspRequirementParser
{
    private static final Pattern COMPARISON = Pattern.compile(
        "^(.*?)(>=|<=|==|=|>|<)(.*?)$"
    );

    private KspRequirementParser()
    {
    }

    public static BooleanSupplier always()
    {
        return () -> true;
    }

    public static BooleanSupplier parse(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return always();
        }

        String cleaned = cleanPrefix(raw);

        if (cleaned.isBlank() || cleaned.equalsIgnoreCase("none") || cleaned.equalsIgnoreCase("true"))
        {
            return always();
        }

        String[] pieces = cleaned.split("[;]");
        List<BooleanSupplier> checks = new ArrayList<>();

        for (String piece : pieces)
        {
            String part = piece == null ? "" : piece.trim();

            if (part.isBlank())
            {
                continue;
            }

            checks.add(parseSingle(part));
        }

        if (checks.isEmpty())
        {
            return always();
        }

        return () ->
        {
            for (BooleanSupplier check : checks)
            {
                if (!safe(check))
                {
                    return false;
                }
            }

            return true;
        };
    }

    public static String extractRequirement(String[] fields, int startIndex)
    {
        if (fields == null || startIndex >= fields.length)
        {
            return "";
        }

        List<String> requirements = new ArrayList<>();

        for (int i = Math.max(0, startIndex); i < fields.length; i++)
        {
            String field = fields[i] == null ? "" : fields[i].trim();

            if (isRequirementField(field))
            {
                requirements.add(cleanPrefix(field));
            }
        }

        return String.join("; ", requirements);
    }

    public static String[] nonRequirementFields(String[] fields, int startIndex)
    {
        if (fields == null || startIndex >= fields.length)
        {
            return new String[0];
        }

        List<String> output = new ArrayList<>();

        for (int i = Math.max(0, startIndex); i < fields.length; i++)
        {
            String field = fields[i] == null ? "" : fields[i].trim();

            if (field.isBlank() || isRequirementField(field))
            {
                continue;
            }

            output.add(field);
        }

        return output.toArray(new String[0]);
    }

    public static boolean isRequirementField(String field)
    {
        if (field == null)
        {
            return false;
        }

        String lower = field.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("req ")
            || lower.startsWith("req:")
            || lower.startsWith("requirement ")
            || lower.startsWith("requirements ");
    }

    private static BooleanSupplier parseSingle(String raw)
    {
        String lower = raw.toLowerCase(Locale.ROOT);

        if (lower.startsWith("coins"))
        {
            return parseInventoryQuantity("Coins", raw.substring("coins".length()).trim());
        }

        if (lower.startsWith("item:"))
        {
            return parseInventoryQuantity(raw.substring("item:".length()).trim());
        }

        if (lower.startsWith("inv:"))
        {
            return parseInventoryQuantity(raw.substring("inv:".length()).trim());
        }

        if (lower.startsWith("inventory:"))
        {
            return parseInventoryQuantity(raw.substring("inventory:".length()).trim());
        }

        if (lower.startsWith("bank:"))
        {
            return parseBankQuantity(raw.substring("bank:".length()).trim());
        }

        if (lower.startsWith("bankitem:"))
        {
            return parseBankQuantity(raw.substring("bankitem:".length()).trim());
        }

        if (lower.startsWith("skill:"))
        {
            return parseSkill(raw.substring("skill:".length()).trim());
        }

        if (lower.startsWith("varp:"))
        {
            return parseVarp(raw.substring("varp:".length()).trim());
        }

        if (lower.startsWith("varbit:"))
        {
            return parseVarbit(raw.substring("varbit:".length()).trim());
        }

        /*
         * Unknown requirement types fail closed so the walker does not take
         * transports it cannot verify.
         */
        return () -> false;
    }

    private static BooleanSupplier parseInventoryQuantity(String raw)
    {
        Matcher matcher = COMPARISON.matcher(raw);

        if (!matcher.matches())
        {
            String itemName = raw.trim();
            return () -> hasInventoryItem(itemName, 1);
        }

        String itemName = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        int amount = parseIntSafe(matcher.group(3).trim(), 1);

        return () -> compare(inventoryQuantity(itemName), operator, amount);
    }

    private static BooleanSupplier parseInventoryQuantity(String forcedItemName, String comparison)
    {
        Matcher matcher = COMPARISON.matcher(comparison == null ? "" : comparison.trim());

        if (!matcher.matches())
        {
            return () -> hasInventoryItem(forcedItemName, 1);
        }

        String operator = matcher.group(2).trim();
        int amount = parseIntSafe(matcher.group(3).trim(), 1);
        return () -> compare(inventoryQuantity(forcedItemName), operator, amount);
    }

    private static BooleanSupplier parseBankQuantity(String raw)
    {
        Matcher matcher = COMPARISON.matcher(raw);

        if (!matcher.matches())
        {
            String itemName = raw.trim();
            return () -> bankQuantity(itemName) >= 1;
        }

        String itemName = matcher.group(1).trim();
        String operator = matcher.group(2).trim();
        int amount = parseIntSafe(matcher.group(3).trim(), 1);

        return () -> compare(bankQuantity(itemName), operator, amount);
    }

    private static BooleanSupplier parseSkill(String raw)
    {
        Matcher matcher = COMPARISON.matcher(raw);

        if (!matcher.matches())
        {
            return () -> false;
        }

        String skillName = normalizeSkillName(matcher.group(1).trim());
        String operator = matcher.group(2).trim();
        int requiredLevel = parseIntSafe(matcher.group(3).trim(), 1);

        return () -> compare(skillLevel(skillName), operator, requiredLevel);
    }

    private static BooleanSupplier parseVarp(String raw)
    {
        Matcher matcher = COMPARISON.matcher(raw);

        if (!matcher.matches())
        {
            return () -> false;
        }

        int id = parseIntSafe(matcher.group(1).trim(), -1);
        String operator = matcher.group(2).trim();
        int value = parseIntSafe(matcher.group(3).trim(), 0);

        return () -> id >= 0 && compare(Microbot.getClient().getVarpValue(id), operator, value);
    }

    private static BooleanSupplier parseVarbit(String raw)
    {
        Matcher matcher = COMPARISON.matcher(raw);

        if (!matcher.matches())
        {
            return () -> false;
        }

        int id = parseIntSafe(matcher.group(1).trim(), -1);
        String operator = matcher.group(2).trim();
        int value = parseIntSafe(matcher.group(3).trim(), 0);

        return () -> id >= 0 && compare(Microbot.getClient().getVarbitValue(id), operator, value);
    }

    private static boolean hasInventoryItem(String itemName, int amount)
    {
        return inventoryQuantity(itemName) >= amount;
    }

    private static int inventoryQuantity(String itemName)
    {
        if (itemName == null || itemName.isBlank())
        {
            return 0;
        }

        try
        {
            return Rs2Inventory.itemQuantity(itemName, true);
        }
        catch (RuntimeException ex)
        {
            try
            {
                return Rs2Inventory.count(itemName, true);
            }
            catch (RuntimeException ignored)
            {
                return 0;
            }
        }
    }

    private static int bankQuantity(String itemName)
    {
        if (itemName == null || itemName.isBlank())
        {
            return 0;
        }

        try
        {
            return Rs2Bank.count(itemName, true);
        }
        catch (RuntimeException ex)
        {
            return 0;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int skillLevel(String skillName)
    {
        try
        {
            Class<?> skillClass = Class.forName("net.runelite.api.Skill");
            Object skill = Enum.valueOf((Class<Enum>) skillClass.asSubclass(Enum.class), skillName);
            Method method = Microbot.getClient().getClass().getMethod("getRealSkillLevel", skillClass);
            Object value = method.invoke(Microbot.getClient(), skill);

            if (value instanceof Number)
            {
                return ((Number) value).intValue();
            }
        }
        catch (ReflectiveOperationException | IllegalArgumentException ignored)
        {
            return 0;
        }

        return 0;
    }

    private static boolean compare(int actual, String operator, int expected)
    {
        switch (operator)
        {
            case ">":
                return actual > expected;
            case ">=":
                return actual >= expected;
            case "<":
                return actual < expected;
            case "<=":
                return actual <= expected;
            case "=":
            case "==":
                return actual == expected;
            default:
                return false;
        }
    }

    private static boolean safe(BooleanSupplier supplier)
    {
        try
        {
            return supplier != null && supplier.getAsBoolean();
        }
        catch (RuntimeException ex)
        {
            return false;
        }
    }

    private static int parseIntSafe(String raw, int fallback)
    {
        try
        {
            String cleaned = raw == null ? "" : raw.replace("<", "").replace(">", "").trim();
            return Integer.parseInt(cleaned);
        }
        catch (NumberFormatException ex)
        {
            return fallback;
        }
    }

    private static String cleanPrefix(String raw)
    {
        String cleaned = raw == null ? "" : raw.trim();

        String lower = cleaned.toLowerCase(Locale.ROOT);

        if (lower.startsWith("requirements "))
        {
            return cleaned.substring("requirements ".length()).trim();
        }

        if (lower.startsWith("requirement "))
        {
            return cleaned.substring("requirement ".length()).trim();
        }

        if (lower.startsWith("req:"))
        {
            return cleaned.substring("req:".length()).trim();
        }

        if (lower.startsWith("req "))
        {
            return cleaned.substring("req ".length()).trim();
        }

        return cleaned;
    }

    private static String normalizeSkillName(String skill)
    {
        return skill == null
            ? ""
            : skill.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
