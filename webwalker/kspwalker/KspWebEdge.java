package net.runelite.client.plugins.microbot.kspwalker;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.coords.WorldPoint;

public final class KspWebEdge
{
    private final String id;
    private final KspWebEdgeType type;
    private final WorldPoint start;
    private final WorldPoint end;
    private final int cost;
    private final int objectId;
    private final String objectName;
    private final int npcId;
    private final String npcName;
    private final String action;
    private final String[] dialogueOptions;
    private final BooleanSupplier requirement;
    private final BooleanSupplier completion;
    private final Supplier<KspWalkResult> customAction;

    private KspWebEdge(Builder builder)
    {
        this.id = builder.id;
        this.type = builder.type;
        this.start = builder.start;
        this.end = builder.end;
        this.cost = builder.cost;
        this.objectId = builder.objectId;
        this.objectName = builder.objectName;
        this.npcId = builder.npcId;
        this.npcName = builder.npcName;
        this.action = builder.action;
        this.dialogueOptions = builder.dialogueOptions == null ? new String[0] : builder.dialogueOptions.clone();
        this.requirement = builder.requirement == null ? () -> true : builder.requirement;
        this.completion = builder.completion == null ? () -> false : builder.completion;
        this.customAction = builder.customAction;
    }

    public static Builder builder(String id, KspWebEdgeType type, WorldPoint start, WorldPoint end)
    {
        return new Builder(id, type, start, end);
    }

    public static KspWebEdge walk(String id, WorldPoint start, WorldPoint end)
    {
        int cost = distanceOrDefault(start, end, 1);
        return builder(id, KspWebEdgeType.WALK, start, end)
            .cost(cost)
            .build();
    }

    public static KspWebEdge object(String id, WorldPoint start, WorldPoint end, int objectId, String action)
    {
        return builder(id, KspWebEdgeType.OBJECT, start, end)
            .objectId(objectId)
            .action(action)
            .cost(distanceOrDefault(start, end, 5) + 8)
            .build();
    }

    public static KspWebEdge objectName(String id, WorldPoint start, WorldPoint end, String objectName, String action)
    {
        return builder(id, KspWebEdgeType.OBJECT, start, end)
            .objectName(objectName)
            .action(action)
            .cost(distanceOrDefault(start, end, 5) + 8)
            .build();
    }

    public static KspWebEdge npc(String id, WorldPoint start, WorldPoint end, int npcId, String action)
    {
        return builder(id, KspWebEdgeType.NPC, start, end)
            .npcId(npcId)
            .action(action)
            .cost(distanceOrDefault(start, end, 5) + 12)
            .build();
    }

    public static KspWebEdge npcName(String id, WorldPoint start, WorldPoint end, String npcName, String action)
    {
        return builder(id, KspWebEdgeType.NPC, start, end)
            .npcName(npcName)
            .action(action)
            .cost(distanceOrDefault(start, end, 5) + 12)
            .build();
    }

    public static KspWebEdge custom(
        String id,
        WorldPoint start,
        WorldPoint end,
        int cost,
        Supplier<KspWalkResult> customAction
    )
    {
        return builder(id, KspWebEdgeType.CUSTOM, start, end)
            .cost(cost)
            .customAction(customAction)
            .build();
    }

    private static int distanceOrDefault(WorldPoint a, WorldPoint b, int fallback)
    {
        if (a == null || b == null || a.getPlane() != b.getPlane())
        {
            return fallback;
        }

        return Math.max(1, a.distanceTo2D(b));
    }

    public String getId()
    {
        return id;
    }

    public KspWebEdgeType getType()
    {
        return type;
    }

    public WorldPoint getStart()
    {
        return start;
    }

    public WorldPoint getEnd()
    {
        return end;
    }

    public int getCost()
    {
        return cost;
    }

    public int getObjectId()
    {
        return objectId;
    }

    public String getObjectName()
    {
        return objectName;
    }

    public int getNpcId()
    {
        return npcId;
    }

    public String getNpcName()
    {
        return npcName;
    }

    public String getAction()
    {
        return action;
    }

    public String[] getDialogueOptions()
    {
        return dialogueOptions.clone();
    }

    public BooleanSupplier getRequirement()
    {
        return requirement;
    }

    public BooleanSupplier getCompletion()
    {
        return completion;
    }

    public Supplier<KspWalkResult> getCustomAction()
    {
        return customAction;
    }

    public boolean isEnabled()
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

    public boolean isComplete()
    {
        if (end != null)
        {
            KspPlayerState state = KspPlayerState.capture();
            if (state.isNear(end, 2))
            {
                return true;
            }
        }

        try
        {
            return completion.getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    public boolean isWalkingEdge()
    {
        return type == KspWebEdgeType.WALK;
    }

    public boolean isActionEdge()
    {
        return type != KspWebEdgeType.WALK;
    }

    @Override
    public String toString()
    {
        return "KspWebEdge{" +
            "id='" + id + '\'' +
            ", type=" + type +
            ", start=" + start +
            ", end=" + end +
            ", cost=" + cost +
            ", objectId=" + objectId +
            ", objectName='" + objectName + '\'' +
            ", npcId=" + npcId +
            ", npcName='" + npcName + '\'' +
            ", action='" + action + '\'' +
            ", dialogueOptions=" + Arrays.toString(dialogueOptions) +
            '}';
    }

    public static final class Builder
    {
        private final String id;
        private final KspWebEdgeType type;
        private final WorldPoint start;
        private final WorldPoint end;

        private int cost = 1;
        private int objectId = -1;
        private String objectName;
        private int npcId = -1;
        private String npcName;
        private String action = "";
        private String[] dialogueOptions = new String[0];
        private BooleanSupplier requirement = () -> true;
        private BooleanSupplier completion = () -> false;
        private Supplier<KspWalkResult> customAction;

        private Builder(String id, KspWebEdgeType type, WorldPoint start, WorldPoint end)
        {
            this.id = Objects.requireNonNull(id, "id");
            this.type = Objects.requireNonNull(type, "type");
            this.start = start;
            this.end = end;
        }

        public Builder cost(int cost)
        {
            this.cost = Math.max(1, cost);
            return this;
        }

        public Builder objectId(int objectId)
        {
            this.objectId = objectId;
            return this;
        }

        public Builder objectName(String objectName)
        {
            this.objectName = objectName;
            return this;
        }

        public Builder npcId(int npcId)
        {
            this.npcId = npcId;
            return this;
        }

        public Builder npcName(String npcName)
        {
            this.npcName = npcName;
            return this;
        }

        public Builder action(String action)
        {
            this.action = action == null ? "" : action;
            return this;
        }

        public Builder dialogueOptions(String... dialogueOptions)
        {
            this.dialogueOptions = dialogueOptions == null ? new String[0] : dialogueOptions.clone();
            return this;
        }

        public Builder requirement(BooleanSupplier requirement)
        {
            this.requirement = requirement;
            return this;
        }

        public Builder completion(BooleanSupplier completion)
        {
            this.completion = completion;
            return this;
        }

        public Builder customAction(Supplier<KspWalkResult> customAction)
        {
            this.customAction = customAction;
            return this;
        }

        public KspWebEdge build()
        {
            return new KspWebEdge(this);
        }
    }
}
