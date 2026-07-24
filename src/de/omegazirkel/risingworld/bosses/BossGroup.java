package de.omegazirkel.risingworld.bosses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mutable runtime and persisted state for one boss group. */
public final class BossGroup {
    public final int id;
    public final BossSector sector;
    public String name;
    public String definitionKey = "";
    public String typeKey = "default";
    public String lootKey = "default";
    public String genderKey = "any";
    public final List<Long> members = new ArrayList<>();
    public final Map<Integer, Long> damage = new HashMap<>();
    public final Set<String> memberNames = new HashSet<>();
    public long boss;
    public int level = 1;
    public boolean finished;
    public boolean bossDefeated;
    public boolean invalid;
    public String killerName = "Unknown";

    public BossGroup(int id, BossSector sector, String name) {
        this.id = id;
        this.sector = sector;
        this.name = name;
    }
}
