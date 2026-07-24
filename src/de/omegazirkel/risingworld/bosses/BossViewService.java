package de.omegazirkel.risingworld.bosses;

import java.util.List;
import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.objects.Player;

/** Read models and administrator actions consumed by the boss overlay. */
public final class BossViewService {
    private final BossState state;
    private final BossGroupAdminHandler admin;
    private final BossSpawnHandler spawn;
    private final BossGroupCatalog groups;
    private final Supplier<PluginSettings> settings;
    private final I18n i18n;

    public BossViewService(BossState state, BossGroupAdminHandler admin, BossSpawnHandler spawn,
            BossGroupCatalog groups, Supplier<PluginSettings> settings, I18n i18n) {
        this.state = state;
        this.admin = admin;
        this.spawn = spawn;
        this.groups = groups;
        this.settings = settings;
        this.i18n = i18n;
    }

    public I18n i18n() {
        return i18n;
    }

    public List<RankingRow> ranking() {
        return state.ranking();
    }

    public List<SectorRow> threatLevels() {
        return state.threatLevels(settings.get());
    }

    public boolean spawnInSector(String key, String requestedType) {
        BossSector sector = state.sectors().get(key);
        if (sector == null || sector.position == null)
            return false;
        spawn.spawn(sector, sector.position, spawnType(requestedType));
        return true;
    }

    public void spawnForAdmin(Player player, String requestedType) {
        if (player == null || !player.isAdmin()) {
            if (player != null)
                player.sendTextMessage(BossUtils.message(i18n, "TC_BOSSES_ADMIN_REQUIRED", player));
            return;
        }
        spawn.spawn(state.sector(player), player.getPosition(), spawnType(requestedType));
    }

    public int clearActiveGroups(String sectorKey) {
        return admin.clearSector(sectorKey);
    }

    public List<NamedNpcRow> namedNpcs() {
        return admin.namedNpcs();
    }

    public boolean teleportToNpc(Player player, long npcId) {
        return admin.teleport(player, npcId);
    }

    public int deleteNpc(long npcId, boolean wholeGroup) {
        return admin.delete(npcId, wholeGroup);
    }

    public List<SpawnType> spawnTypes() {
        List<Short> configuredTypes = groups.spawnTypes();
        if (configuredTypes.isEmpty())
            configuredTypes = settings.get().types;
        return configuredTypes.stream().map(id -> {
            var definition = Definitions.getNpcDefinition(id);
            return new SpawnType(Short.toString(id), definition == null ? Short.toString(id) : definition.name);
        }).toList();
    }

    private Short spawnType(String value) {
        if (value == null || value.isBlank())
            return null;
        for (SpawnType type : spawnTypes())
            if (type.id().equals(value) || type.label().equalsIgnoreCase(value))
                return Short.parseShort(type.id());
        return null;
    }

    public record RankingRow(String playerName, long score, int bossKills, int followerKills, long damage) {
    }

    public record SectorRow(String sector, int threat, int activeGroups, int spawnChance) {
    }

    public record SpawnType(String id, String label) {
    }

    public record NamedNpcRow(long id, String name, String type, int groupId, int health) {
    }
}
