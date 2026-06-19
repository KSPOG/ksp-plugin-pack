package net.runelite.client.plugins.microbot.kspwalker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

public final class KspTeleportActionExecutor
{
    private static final String RS2_MAGIC_CLASS = "net.runelite.client.plugins.microbot.util.magic.Rs2Magic";

    private KspTeleportActionExecutor()
    {
    }

    public static boolean hasInventoryItem(String itemName)
    {
        return itemName != null && !itemName.isBlank() && Rs2Inventory.hasItem(itemName, false);
    }

    public static int inventoryQuantity(String itemName)
    {
        if (itemName == null || itemName.isBlank())
        {
            return 0;
        }

        return Rs2Inventory.itemQuantity(itemName, false);
    }

    public static KspWalkResult itemTeleport(
        WorldPoint destination,
        String itemName,
        String action,
        String[] dialogueOptions
    )
    {
        if (itemName == null || itemName.isBlank())
        {
            return KspWalkResult.failed(destination, "Item teleport missing item name");
        }

        String safeAction = action == null || action.isBlank() ? "Rub" : action.trim();

        if (!hasInventoryItem(itemName))
        {
            return KspWalkResult.failed(destination, "Missing teleport item: " + itemName);
        }

        boolean interacted = Rs2Inventory.interact(itemName, safeAction);

        if (!interacted)
        {
            return KspWalkResult.failed(destination, "Failed item teleport interaction: " + itemName + " action=" + safeAction);
        }

        handleDialogue(dialogueOptions, 1_600L);

        return KspWalkResult.waiting(
            destination,
            "Used item teleport: " + itemName + " action=" + safeAction
        );
    }

    public static boolean hasSpellStringCasting()
    {
        try
        {
            Class<?> magic = Class.forName(RS2_MAGIC_CLASS);

            for (Method method : magic.getMethods())
            {
                if (!Modifier.isStatic(method.getModifiers()))
                {
                    continue;
                }

                if (!method.getName().toLowerCase(Locale.ROOT).contains("cast"))
                {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length == 1 && parameterTypes[0] == String.class)
                {
                    return true;
                }
            }
        }
        catch (ReflectiveOperationException ignored)
        {
            return false;
        }

        return false;
    }

    public static KspWalkResult spellTeleport(WorldPoint destination, String spellName)
    {
        if (spellName == null || spellName.isBlank())
        {
            return KspWalkResult.failed(destination, "Spell teleport missing spell name");
        }

        try
        {
            Class<?> magic = Class.forName(RS2_MAGIC_CLASS);

            for (Method method : magic.getMethods())
            {
                if (!Modifier.isStatic(method.getModifiers()))
                {
                    continue;
                }

                if (!method.getName().toLowerCase(Locale.ROOT).contains("cast"))
                {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length == 1 && parameterTypes[0] == String.class)
                {
                    Object result = method.invoke(null, spellName);

                    if (result instanceof Boolean)
                    {
                        return (Boolean) result
                            ? KspWalkResult.waiting(destination, "Casting spell teleport: " + spellName)
                            : KspWalkResult.failed(destination, "Spell cast returned false: " + spellName);
                    }

                    return KspWalkResult.waiting(destination, "Casting spell teleport: " + spellName);
                }
            }

            return KspWalkResult.failed(
                destination,
                "No string-based Rs2Magic cast method found for spell teleport: " + spellName
            );
        }
        catch (ReflectiveOperationException ex)
        {
            return KspWalkResult.failed(
                destination,
                "Spell teleport reflection failed: " + spellName + " reason=" + ex.getClass().getSimpleName()
            );
        }
    }

    private static void handleDialogue(String[] dialogueOptions, long timeoutMs)
    {
        if (dialogueOptions == null || dialogueOptions.length == 0)
        {
            return;
        }

        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);

        while (System.currentTimeMillis() < deadline && !Thread.currentThread().isInterrupted())
        {
            if (Rs2Dialogue.hasContinue())
            {
                Rs2Dialogue.clickContinue();
                sleep(160L);
                continue;
            }

            for (String option : dialogueOptions)
            {
                if (option == null || option.isBlank())
                {
                    continue;
                }

                if (Rs2Dialogue.hasDialogueOption(option) && Rs2Dialogue.clickOption(option))
                {
                    sleep(240L);
                    return;
                }
            }

            sleep(120L);
        }
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(Math.max(1L, ms));
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
}
