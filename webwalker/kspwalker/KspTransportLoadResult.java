package net.runelite.client.plugins.microbot.kspwalker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KspTransportLoadResult
{
    private final Path file;
    private final int loaded;
    private final List<String> errors;

    public KspTransportLoadResult(Path file, int loaded, List<String> errors)
    {
        this.file = file;
        this.loaded = loaded;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public Path getFile()
    {
        return file;
    }

    public int getLoaded()
    {
        return loaded;
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
