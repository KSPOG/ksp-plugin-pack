package net.runelite.client.plugins.microbot.kspwalker;

import java.lang.reflect.Method;
import java.util.Collection;
import net.runelite.client.plugins.microbot.Microbot;

public final class KspMembershipDetector
{
    private KspMembershipDetector()
    {
    }

    public static boolean isMembers()
    {
        return isMembersWorld()
            || isMembersWorldViaWorldTypes()
            || isMembersAccountHint();
    }

    public static boolean isMembersWorld()
    {
        try
        {
            Method method = Microbot.getClient().getClass().getMethod("isMembersWorld");
            Object result = method.invoke(Microbot.getClient());

            if (result instanceof Boolean)
            {
                return (Boolean) result;
            }
        }
        catch (ReflectiveOperationException | RuntimeException ignored)
        {
            // Branch does not expose this method or client is unavailable.
        }

        return false;
    }

    private static boolean isMembersWorldViaWorldTypes()
    {
        try
        {
            Method method = Microbot.getClient().getClass().getMethod("getWorldType");
            Object result = method.invoke(Microbot.getClient());

            if (result == null)
            {
                return false;
            }

            if (result instanceof Collection)
            {
                for (Object value : (Collection<?>) result)
                {
                    if (looksMembers(value))
                    {
                        return true;
                    }
                }

                return false;
            }

            return looksMembers(result);
        }
        catch (ReflectiveOperationException | RuntimeException ignored)
        {
            return false;
        }
    }

    private static boolean isMembersAccountHint()
    {
        /*
         * Best effort only. Current-world membership remains the primary gate because
         * members-only banks/areas are only usable on members worlds.
         */
        String[] methodNames =
        {
            "isMember",
            "isMembers",
            "isAccountMember",
            "isAccountMembers",
            "isPlayerMember",
            "isPlayerMembers"
        };

        for (String methodName : methodNames)
        {
            try
            {
                Method method = Microbot.getClient().getClass().getMethod(methodName);
                Object result = method.invoke(Microbot.getClient());

                if (result instanceof Boolean && (Boolean) result)
                {
                    return true;
                }
            }
            catch (ReflectiveOperationException | RuntimeException ignored)
            {
                // Try next possible helper.
            }
        }

        return false;
    }

    private static boolean looksMembers(Object value)
    {
        if (value == null)
        {
            return false;
        }

        String text = value.toString().toUpperCase();

        return text.contains("MEMBER")
            || text.contains("P2P");
    }
}
