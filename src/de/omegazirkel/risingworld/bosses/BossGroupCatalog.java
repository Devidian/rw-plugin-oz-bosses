package de.omegazirkel.risingworld.bosses;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            groups = parsed == null || parsed.groups == null ? new ArrayList<>() : parsed.groups;
        } catch (Exception ex) {
            BossUtils.logger().error("Cannot load boss groups: " + ex.getMessage());
            groups = new ArrayList<>();
        }
    }

    public List<Short> spawnTypes() {
        List<Short> result = new ArrayList<>();
        for (Definition group : groups) {
            if (group == null || group.npc == null || group.npc.isBlank()) continue;
            var definition = Definitions.getNpcDefinition(group.npc);
            if (definition == null) try { definition = Definitions.getNpcDefinition(Short.parseShort(group.npc)); }
            catch (NumberFormatException ignored) { }
            if (definition == null) { BossUtils.logger().warn("Unknown NPC type in groups.json: " + group.npc); continue; }
            if (!result.contains(definition.id)) result.add(definition.id);
        }
        return result;
    }

    public String nameType(Npc npc) { Definition group = find(npc); return group == null || blank(group.nameType) ? null : group.nameType; }
    public String lootTable(Npc npc, String fallback) { Definition group = find(npc); return group == null || blank(group.lootTable) ? fallback : group.lootTable; }
    private Definition find(Npc npc) {
        if (npc == null || npc.getDefinition() == null || npc.getDefinition().name == null) return null;
        return groups.stream().filter(group -> group != null && group.npc != null
                && group.npc.equalsIgnoreCase(npc.getDefinition().name)).findFirst().orElse(null);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private static final class Definition { String key, npc, nameType, lootTable; }
}
