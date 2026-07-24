package de.omegazirkel.risingworld.bosses;

/** Mutable persisted combat score for one player. */
public final class BossScore {
    public final int id;
    public final String name;
    public int bossKills;
    public int followerKills;
    public long damage;

    public BossScore(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public long value() {
        return bossKills * 1000L + followerKills * 100L + damage;
    }
}
