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
    public void load(Plugin plugin) {
        Path path = Path.of(plugin.getPath(), "names.json");
        try { BossUtils.copyRuntimeConfig(plugin, path, "names.default.json"); BossNameCatalog parsed = new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), BossNameCatalog.class); types = parsed == null || parsed.types == null ? new LinkedHashMap<>() : parsed.types; }
        catch (Exception ex) { BossUtils.logger().error("Cannot load boss names: " + ex.getMessage()); types = new LinkedHashMap<>(); }
    }
    public String boss(String type, String gender) { NameSet set = set(type, gender, "boss"); String prefix = set.prefixes.isEmpty() ? "Boss" : random(set.prefixes); String name = set.names.isEmpty() ? "" : random(set.names); return (prefix + " " + name).trim(); }
    public List<String> followers(String type, String gender) { List<String> names = set(type, gender, "follower").names; return names.isEmpty() ? List.of("Follower") : names; }
    public boolean isBossOrFollowerName(String type, String gender, String name) { return name != null && !name.isBlank() && (isBossName(type, gender, name) || followers(type, gender).contains(name)); }
    public boolean isBossName(String type, String gender, String name) { return name != null && set(type, gender, "boss").prefixes.stream().anyMatch(prefix -> name.startsWith(prefix + " ")); }
    private NameSet set(String type, String gender, String role) { Map<String, Map<String, NameSet>> byGender = types.getOrDefault(type, types.get("animal")); if (byGender == null || byGender.isEmpty()) return new NameSet(); Map<String, NameSet> roles = byGender.get(gender); if (roles == null) roles = byGender.get("any"); if (roles == null) roles = byGender.values().iterator().next(); return roles.getOrDefault(role, new NameSet()); }
    private String random(List<String> values) { return values.get(new Random().nextInt(values.size())); }
    private static final class NameSet { List<String> prefixes = new ArrayList<>(), names = new ArrayList<>(); }
}
