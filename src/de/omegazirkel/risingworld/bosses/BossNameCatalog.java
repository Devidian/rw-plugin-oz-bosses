package de.omegazirkel.risingworld.bosses;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;

import net.risingworld.api.Plugin;

/** File-backed boss and follower names from names.json. */
public final class BossNameCatalog {
    private Map<String, Map<String, Map<String, NameSet>>> types = new LinkedHashMap<>();
    private final transient Random random = new Random();

    public void load(Plugin plugin) {
        Path path = Path.of(plugin.getPath(), "names.json");
        try { BossUtils.copyRuntimeConfig(plugin, path, "names.default.json"); BossNameCatalog parsed = new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), BossNameCatalog.class); types = parsed == null || parsed.types == null ? new LinkedHashMap<>() : parsed.types; }
        catch (Exception ex) { BossUtils.logger().error("Cannot load boss names: " + ex.getMessage()); types = new LinkedHashMap<>(); }
    }
    public String boss(String type, String gender) { NameSet set = randomSet(type, gender, "boss"); String prefix = set.prefixes.isEmpty() ? "Boss" : random(set.prefixes); String name = set.names.isEmpty() ? "" : random(set.names); return (prefix + " " + name).trim(); }
    public List<String> followers(String type, String gender) { List<String> names = sets(type, gender, "follower").stream().flatMap(set -> set.names.stream()).distinct().toList(); return names.isEmpty() ? List.of("Follower") : names; }
    public boolean isBossOrFollowerName(String type, String gender, String name) { return name != null && !name.isBlank() && (isBossName(type, gender, name) || followers(type, gender).contains(name)); }
    public boolean isBossName(String type, String gender, String name) { return name != null && sets(type, gender, "boss").stream().flatMap(set -> set.prefixes.stream()).anyMatch(prefix -> name.startsWith(prefix + " ")); }
    private NameSet randomSet(String type, String gender, String role) { List<NameSet> sets = sets(type, gender, role); return sets.isEmpty() ? new NameSet() : sets.get(random.nextInt(sets.size())); }
    private List<NameSet> sets(String type, String gender, String role) {
        Map<String, Map<String, NameSet>> byGender = types.getOrDefault(type, types.get("dummy"));
        if (byGender == null || byGender.isEmpty()) return List.of();
        Map<String, NameSet> exact = byGender.get(gender);
        if (exact != null) return List.of(exact.getOrDefault(role, new NameSet()));
        Map<String, NameSet> any = byGender.get("any");
        if (any != null) return List.of(any.getOrDefault(role, new NameSet()));
        if ("any".equals(gender))
            return byGender.values().stream().map(roles -> roles.get(role)).filter(java.util.Objects::nonNull).toList();
        Map<String, NameSet> fallback = byGender.values().iterator().next();
        return List.of(fallback.getOrDefault(role, new NameSet()));
    }
    private String random(List<String> values) { return values.get(random.nextInt(values.size())); }
    private static final class NameSet { List<String> prefixes = new ArrayList<>(), names = new ArrayList<>(); }
}
