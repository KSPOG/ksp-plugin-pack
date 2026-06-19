package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.coords.WorldPoint;

public final class KspTeleportOption
{
    private final String id;
    private final String name;
    private final KspTeleportType type;
    private final WorldPoint destination;
    private final int cost;
    private final BooleanSupplier requirement;
    private final Supplier<KspWalkResult> action;

    private KspTeleportOption(Builder builder)
    {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = builder.name == null || builder.name.isBlank() ? builder.id : builder.name;
        this.type = Objects.requireNonNull(builder.type, "type");
        this.destination = Objects.requireNonNull(builder.destination, "destination");
        this.cost = Math.max(0, builder.cost);
        this.requirement = builder.requirement == null ? () -> true : builder.requirement;
        this.action = builder.action;
    }

    public static Builder builder(String id, KspTeleportType type, WorldPoint destination)
    {
        return new Builder(id, type, destination);
    }

    public static KspTeleportOption spell(
        String id,
        String name,
        WorldPoint destination,
        int cost,
        BooleanSupplier requirement,
        Supplier<KspWalkResult> action
    )
    {
        return builder(id, KspTeleportType.SPELL, destination)
            .name(name)
            .cost(cost)
            .requirement(requirement)
            .action(action)
            .build();
    }

    public static KspTeleportOption item(
        String id,
        String name,
        WorldPoint destination,
        int cost,
        BooleanSupplier requirement,
        Supplier<KspWalkResult> action
    )
    {
        return builder(id, KspTeleportType.ITEM, destination)
            .name(name)
            .cost(cost)
            .requirement(requirement)
            .action(action)
            .build();
    }

    public static KspTeleportOption custom(
        String id,
        String name,
        WorldPoint destination,
        int cost,
        BooleanSupplier requirement,
        Supplier<KspWalkResult> action
    )
    {
        return builder(id, KspTeleportType.CUSTOM, destination)
            .name(name)
            .cost(cost)
            .requirement(requirement)
            .action(action)
            .build();
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public KspTeleportType getType()
    {
        return type;
    }

    public WorldPoint getDestination()
    {
        return destination;
    }

    public int getCost()
    {
        return cost;
    }

    public boolean canUse()
    {
        try
        {
            return requirement.getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    public KspWalkResult execute(WorldPoint finalTarget)
    {
        if (action == null)
        {
            return KspWalkResult.failed(finalTarget, "Teleport has no action: " + id);
        }

        KspWalkResult result = action.get();

        if (result == null)
        {
            return KspWalkResult.waiting(finalTarget, "Teleport action returned null: " + id);
        }

        return result;
    }

    @Override
    public String toString()
    {
        return "KspTeleportOption{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", destination=" + destination +
            ", cost=" + cost +
            '}';
    }

    public static final class Builder
    {
        private final String id;
        private final KspTeleportType type;
        private final WorldPoint destination;

        private String name;
        private int cost = 30;
        private BooleanSupplier requirement = () -> true;
        private Supplier<KspWalkResult> action;

        private Builder(String id, KspTeleportType type, WorldPoint destination)
        {
            this.id = id;
            this.type = type;
            this.destination = destination;
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Builder cost(int cost)
        {
            this.cost = Math.max(0, cost);
            return this;
        }

        public Builder requirement(BooleanSupplier requirement)
        {
            this.requirement = requirement;
            return this;
        }

        public Builder action(Supplier<KspWalkResult> action)
        {
            this.action = action;
            return this;
        }

        public KspTeleportOption build()
        {
            return new KspTeleportOption(this);
        }
    }
}
