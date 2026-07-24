package de.omegazirkel.risingworld.bosses;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;

import net.risingworld.api.Plugin;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.objects.Npc;

/** File-backed NPC type to name/loot-table mapping from groups.json. */
public final class BossGroupCatalog {
    private List<Definition> groups = new ArrayList<>();

    public void load(Plugin plugin) {
        Path path = Path.of(plugin.getPath(), "groups.json");
        try {
            BossUtils.copyRuntimeConfig(plugin, path, "groups.default.json");
            BossGroupCatalog parsed = new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), BossGroupCatalog.class);
            groups = parsed == null || parsed.groups == null ? new ArrayList<>() : validated(parsed.groups);
        } catch (Exception ex) {
            BossUtils.logger().error("Cannot load boss groups: " + ex.getMessage());
            groups = new ArrayList<>();
        }
    }

    public List<SpawnDefinition> spawnDefinitions() {
        List<SpawnDefinition> result = new ArrayList<>();
        for (Definition group : groups) {
            var bossNpc = npcDefinition(group.npc);
            if (bossNpc == null) {
                BossUtils.logger().warn("Unknown boss NPC type in groups.json: " + group.npc);
                continue;
            }
            var followerNpc = blank(group.followerNpc) ? bossNpc : npcDefinition(group.followerNpc);
            if (followerNpc == null) {
                BossUtils.logger().warn("Unknown follower NPC type in groups.json for " + group.key + ": "
                        + group.followerNpc + "; using boss NPC type");
                followerNpc = bossNpc;
            }
            result.add(new SpawnDefinition(group.key, blank(group.name) ? bossNpc.name : group.name.trim(),
                    bossNpc.id, followerNpc.id, group.weight == null ? 1 : group.weight,
                    group.nameType, group.lootTable, group.bossBaseHealth,
                    group.bossHealthPerLevel, group.followerBaseHealth, group.followerHealthPerLevel,
                    group.minSpawnDistance));
        }
        return result;
    }

    public SpawnDefinition spawnDefinition(String key) {
        if (blank(key))
            return null;
        return spawnDefinitions().stream().filter(group -> group.key().equalsIgnoreCase(key)).findFirst().orElse(null);
    }

    public String definitionKey(Npc npc) {
        Definition group = find(npc);
        return group == null ? null : group.key;
    }

    public String nameType(Npc npc) { Definition group = find(npc); return group == null || blank(group.nameType) ? null : group.nameType; }
    public String lootTable(Npc npc, String fallback) { Definition group = find(npc); return group == null || blank(group.lootTable) ? fallback : group.lootTable; }
    private Definition find(Npc npc) {
        if (npc == null || npc.getDefinition() == null || npc.getDefinition().name == null) return null;
        return groups.stream().filter(group -> sameNpc(group.npc, npc)).findFirst().orElse(null);
    }

    private List<Definition> validated(List<Definition> parsed) {
        Map<String, Definition> unique = new LinkedHashMap<>();
        for (Definition group : parsed) {
            if (group == null || blank(group.npc)) {
                BossUtils.logger().warn("Ignoring group without an NPC type in groups.json");
                continue;
            }
            group.key = blank(group.key) ? group.npc.trim().toLowerCase(Locale.ROOT) : group.key.trim();
            String normalizedKey = group.key.toLowerCase(Locale.ROOT);
            if (unique.containsKey(normalizedKey)) {
                BossUtils.logger().warn("Ignoring duplicate group key in groups.json: " + group.key);
                continue;
            }
            group.weight = nonNegative(group.weight, group.key, "weight");
            group.bossBaseHealth = positive(group.bossBaseHealth, group.key, "bossBaseHealth");
            group.bossHealthPerLevel = nonNegative(group.bossHealthPerLevel, group.key, "bossHealthPerLevel");
            group.followerBaseHealth = positive(group.followerBaseHealth, group.key, "followerBaseHealth");
            group.followerHealthPerLevel = nonNegative(group.followerHealthPerLevel, group.key,
                    "followerHealthPerLevel");
            group.minSpawnDistance = nonNegative(group.minSpawnDistance, group.key, "minSpawnDistance");
            unique.put(normalizedKey, group);
        }
        return new ArrayList<>(unique.values());
    }

    private Integer positive(Integer value, String key, String field) {
        if (value == null || value > 0)
            return value;
        BossUtils.logger().warn("Ignoring non-positive " + field + " override for group " + key);
        return null;
    }

    private Integer nonNegative(Integer value, String key, String field) {
        if (value == null || value >= 0)
            return value;
        BossUtils.logger().warn("Ignoring negative " + field + " override for group " + key);
        return null;
    }

    private boolean sameNpc(String configured, Npc npc) {
        var definition = npcDefinition(configured);
        return definition != null && npc.getDefinition() != null && definition.id == npc.getDefinition().id;
    }

    private net.risingworld.api.definitions.Npcs.NpcDefinition npcDefinition(String configured) {
        if (blank(configured))
            return null;
        var definition = Definitions.getNpcDefinition(configured.trim());
        if (definition == null)
            try {
                definition = Definitions.getNpcDefinition(Short.parseShort(configured.trim()));
            } catch (NumberFormatException ignored) {
            }
        return definition;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record SpawnDefinition(String key, String displayName, short bossNpcType, short followerNpcType,
            int weight, String nameType, String lootTable, Integer bossBaseHealth, Integer bossHealthPerLevel,
            Integer followerBaseHealth, Integer followerHealthPerLevel, Integer minSpawnDistance) {
    }

    private static final class Definition {
        String key, name, npc, followerNpc, nameType, lootTable;
        Integer weight, bossBaseHealth, bossHealthPerLevel, followerBaseHealth, followerHealthPerLevel,
                minSpawnDistance;
    }
}
