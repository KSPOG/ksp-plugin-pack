package net.runelite.client.plugins.microbot.kspwalker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KspTeleportFileLoadResult
{
    private final Path file;
    private final int objectTeleportsLoaded;
    private final int itemTeleportsLoaded;
    private final int spellTeleportsLoaded;
    private final List<String> errors;

    public KspTeleportFileLoadResult(
        Path file,
        int objectTeleportsLoaded,
        int itemTeleportsLoaded,
        int spellTeleportsLoaded,
        List<String> errors
    )
    {
        this.file = file;
        this.objectTeleportsLoaded = objectTeleportsLoaded;
        this.itemTeleportsLoaded = itemTeleportsLoaded;
        this.spellTeleportsLoaded = spellTeleportsLoaded;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public Path getFile()
    {
        return file;
    }

    public int getObjectTeleportsLoaded()
    {
        return objectTeleportsLoaded;
    }

    public int getItemTeleportsLoaded()
    {
        return itemTeleportsLoaded;
    }

    public int getSpellTeleportsLoaded()
    {
        return spellTeleportsLoaded;
    }

    public int getTotalLoaded()
    {
        return objectTeleportsLoaded + itemTeleportsLoaded + spellTeleportsLoaded;
    }

    public List<String> getErrors()
    {
        return errors;
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }
}
