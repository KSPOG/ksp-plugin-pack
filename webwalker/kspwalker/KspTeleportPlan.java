package net.runelite.client.plugins.microbot.kspwalker;

public final class KspTeleportPlan
{
    private final KspTeleportOption option;
    private final int currentDistance;
    private final int postTeleportDistance;
    private final int savingDistance;
    private final int score;

    public KspTeleportPlan(
        KspTeleportOption option,
        int currentDistance,
        int postTeleportDistance,
        int savingDistance,
        int score
    )
    {
        this.option = option;
        this.currentDistance = currentDistance;
        this.postTeleportDistance = postTeleportDistance;
        this.savingDistance = savingDistance;
        this.score = score;
    }

    public KspTeleportOption getOption()
    {
        return option;
    }

    public int getCurrentDistance()
    {
        return currentDistance;
    }

    public int getPostTeleportDistance()
    {
        return postTeleportDistance;
    }

    public int getSavingDistance()
    {
        return savingDistance;
    }

    public int getScore()
    {
        return score;
    }

    @Override
    public String toString()
    {
        return "KspTeleportPlan{" +
            "option=" + option +
            ", currentDistance=" + currentDistance +
            ", postTeleportDistance=" + postTeleportDistance +
            ", savingDistance=" + savingDistance +
            ", score=" + score +
            '}';
    }
}
