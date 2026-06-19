package net.runelite.client.plugins.microbot.kspwalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KspTeleportRegistry
{
    private final CopyOnWriteArrayList<KspTeleportOption> options = new CopyOnWriteArrayList<>();

    public KspTeleportRegistry add(KspTeleportOption option)
    {
        if (option != null)
        {
            options.add(option);
        }

        return this;
    }

    public KspTeleportRegistry addAll(List<KspTeleportOption> newOptions)
    {
        if (newOptions != null)
        {
            newOptions.stream()
                .filter(option -> option != null)
                .forEach(options::add);
        }

        return this;
    }

    public KspTeleportRegistry remove(String id)
    {
        if (id != null)
        {
            options.removeIf(option -> id.equals(option.getId()));
        }

        return this;
    }

    public void clear()
    {
        options.clear();
    }

    public List<KspTeleportOption> getOptions()
    {
        return Collections.unmodifiableList(new ArrayList<>(options));
    }

    public List<KspTeleportOption> getUsableOptions()
    {
        List<KspTeleportOption> usable = new ArrayList<>();

        for (KspTeleportOption option : options)
        {
            if (option.canUse())
            {
                usable.add(option);
            }
        }

        return Collections.unmodifiableList(usable);
    }

    public Optional<KspTeleportOption> findById(String id)
    {
        if (id == null)
        {
            return Optional.empty();
        }

        return options.stream()
            .filter(option -> id.equals(option.getId()))
            .findFirst();
    }

    public boolean isEmpty()
    {
        return options.isEmpty();
    }
}
