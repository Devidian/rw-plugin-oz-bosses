package de.omegazirkel.risingworld.bosses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Items.Modifier;
import net.risingworld.api.definitions.Npcs.AttackReaction;
import net.risingworld.api.definitions.Npcs.Behaviour;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Storage;
import net.risingworld.api.objects.world.Chunk;
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.Vector2i;
import net.risingworld.api.utils.Vector3f;

/** Spawn scheduling, safe placement and NPC equipment for boss groups. */
public final class BossSpawnHandler {
    private final Plugin plugin;
    private final Map<Integer, BossGroup> activeGroups;
    private final Map<Long, BossGroup> npcGroups;
    private final Map<String, BossSector> sectors;
    private final Map<Integer, BossScore> scores;
    private final Supplier<PluginSettings> settings;
    private final BossGroupCatalog groups;
    private final BossNameCatalog names;
    private final BossGroupPersistence persistence;
    private final BossStateRepository stateRepository;
    private final BossAnnouncementHandler announcements;
    private final Random random = new Random();

    public BossSpawnHandler(Plugin plugin, Map<Integer, BossGroup> activeGroups, Map<Long, BossGroup> npcGroups,
            Map<String, BossSector> sectors, Map<Integer, BossScore> scores, Supplier<PluginSettings> settings,
            BossGroupCatalog groups,
            BossNameCatalog names, BossGroupPersistence persistence, BossStateRepository stateRepository,
            BossAnnouncementHandler announcements) {
        this.plugin = plugin;
        this.activeGroups = activeGroups;
        this.npcGroups = npcGroups;
        this.sectors = sectors;
        this.scores = scores;
        this.settings = settings;
        this.groups = groups;
        this.names = names;
        this.persistence = persistence;
        this.stateRepository = stateRepository;
        this.announcements = announcements;
    }

    public void schedule() {
        plugin.executeDelayed(settings.get().interval, () -> {
            if (Server.getAllPlayers().length > 0)
                for (BossSector sector : new ArrayList<>(sectors.values())) {
                    int chance = BossState.spawnChance(sector, settings.get());
                    if (sector.position != null && chance > 0 && random.nextInt(100) < chance)
                        spawn(sector, sector.position, null);
                }
            try {
                stateRepository.save(sectors, scores);
            } catch (java.sql.SQLException ex) {
                BossUtils.logger().error(ex.getMessage());
            }
            schedule();
        });
    }

    public void spawn(BossSector sector, Vector3f at, Short requestedType) {
        List<Short> configuredTypes = configuredSpawnTypes();
        if (configuredTypes.isEmpty()) {
            BossUtils.logger().warn("No NPC types configured in groups.json or boss.types");
            return;
        }
        short type = requestedType != null && configuredTypes.contains(requestedType) ? requestedType
                : configuredTypes.get(random.nextInt(configuredTypes.size()));
        int id = World.getNextNpcGroupID();
        BossGroup group = new BossGroup(id, sector, "Boss");
        Vector3f center = null;
        Npc boss = null;
        for (int attempt = 0; attempt < 16 && boss == null; attempt++) {
            center = spawnCenter(at);
            if (center != null)
                boss = npc(type, center, id, "Boss", settings.get().baseHealth);
        }
        if (boss == null || center == null) {
            BossUtils.logger().warn("No dry boss spawn location found for sector " + sector.key);
            return;
        }
        activeGroups.put(id, group);
        group.typeKey = BossUtils.enemyKey(boss, groups);
        group.lootKey = groups.lootTable(boss, group.typeKey);
        group.genderKey = BossUtils.genderKey(boss);
        group.name = names.boss(group.typeKey, group.genderKey);
        group.memberNames.add(group.name);
        boss.setName(group.name);
        Npc spawnedBoss = boss;
        plugin.executeDelayed(0.2f, () -> { if (!spawnedBoss.isDead()) spawnedBoss.setName(group.name); });
        group.boss = boss.getGlobalID();
        group.members.add(group.boss);
        npcGroups.put(group.boss, group);
        int followers = settings.get().initialFollowers
                + (int) Math.floor(Server.getAllPlayers().length * settings.get().followersPerOnlinePlayer);
        for (int i = 0; i < followers; i++)
            addFollower(group, type, center);
        sector.active++;
        persistence.save();
        announcements.announce("TC_BOSSES_ANNOUNCE_SPAWN", "PH_BOSS", group.name, "PH_SECTOR", sector.key);
    }

    public void addFollower(BossGroup group, short type, Vector3f at) {
        Npc npc = npc(type, at, group.id, "Follower",
                settings.get().followerHealth + group.level * settings.get().healthPerLevel / 2);
        if (npc == null) return;
        String name = uniqueFollowerName(group, names.followers(group.typeKey, BossUtils.genderKey(npc)));
        npc.setName(name);
        plugin.executeDelayed(0.2f, () -> { if (!npc.isDead()) npc.setName(name); });
        group.members.add(npc.getGlobalID());
        npcGroups.put(npc.getGlobalID(), group);
    }

    public void enhanceWeaponDrop(long storageId) {
        plugin.executeDelayed(0.1f, () -> {
            Storage storage = World.getStorage(storageId);
            if (storage == null) return;
            for (var item : storage.getItems())
                if (item != null && item.getDefinition() != null) {
                    item.setModifier(Modifier.Legendary);
                    item.setDurability(item.getDefinition().durability);
                }
        });
    }

    private Npc npc(short type, Vector3f at, int groupId, String name, int health) {
        float angle = random.nextFloat() * (float) Math.PI * 2, distance = 8 + random.nextFloat() * 12;
        var definition = Definitions.getNpcDefinition(type);
        int variation = definition == null ? 0 : random.nextInt(Math.max(1, definition.variations));
        Npc npc = World.spawnNpc(type, variation,
                new Vector3f(at.x + (float) Math.cos(angle) * distance, at.y + 1,
                        at.z + (float) Math.sin(angle) * distance), new Quaternion());
        if (npc == null) return null;
        if (npc.isInWater() || npc.isUnderwater()) { npc.delete(); return null; }
        npc.setGroupID(groupId); npc.setName(name); npc.setBehaviour(Behaviour.Aggressive);
        npc.setAttackReaction(AttackReaction.Attack); npc.setAlerted(true); npc.setHealth(health);
        plugin.executeDelayed(0.1f, () -> {
            if (npc.isDead()) return;
            npc.setName(name); npc.setBehaviour(Behaviour.Aggressive); npc.setAttackReaction(AttackReaction.Attack);
            npc.setAlerted(true); improveWeapon(npc); equipDummyClothes(npc);
        });
        return npc;
    }

    private void improveWeapon(Npc npc) {
        if (npc.getEquippedItem() == null || npc.getEquippedItem().getDefinition() == null) return;
        npc.getEquippedItem().setModifier(Modifier.Legendary);
        npc.getEquippedItem().setDurability(npc.getEquippedItem().getDefinition().durability);
    }

    private void equipDummyClothes(Npc npc) {
        if (npc.getDefinition() == null || !"dummy".equalsIgnoreCase(npc.getDefinition().name)
                || !npc.getDefinition().hasclothes) return;
        String[] tops = { "ragshirt", "medievalshirt", "medievalshirt2", "medievalshirt3" };
        String[] legs = { "medievalpants", "mountiepants", "cargopants" };
        String[] feet = { "medievalfurboots", "mountieboots", "oldboot" };
        for (String name : List.of(random(tops), random(legs), random(feet))) {
            var clothing = Definitions.getClothingDefinition(name);
            if (clothing != null) npc.getClothes().add((short) clothing.id);
        }
    }

    private String uniqueFollowerName(BossGroup group, List<String> candidates) {
        for (String candidate : candidates)
            if (group.memberNames.add(candidate)) return candidate;
        String base = candidates.isEmpty() ? "Follower" : candidates.get(random.nextInt(candidates.size()));
        int number = 2;
        while (!group.memberNames.add(base + " " + number)) number++;
        return base + " " + number;
    }

    private Vector3f spawnCenter(Vector3f reference) {
        float requiredDistance = settings.get().minSpawnDistance + 24f;
        for (int attempt = 0; attempt < 16; attempt++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float distance = settings.get().minSpawnDistance
                    + random.nextFloat() * (sectorRadius(reference) - settings.get().minSpawnDistance);
            Vector3f candidate = new Vector3f(reference.x + (float) Math.cos(angle) * distance, reference.y,
                    reference.z + (float) Math.sin(angle) * distance);
            if ((int) Math.floor(candidate.x / 512f) != (int) Math.floor(reference.x / 512f)
                    || (int) Math.floor(candidate.z / 512f) != (int) Math.floor(reference.z / 512f)) continue;
            float ground = groundLevel(candidate);
            if (Float.isNaN(ground) || isWaterAt(candidate, ground)) continue;
            candidate.y = ground + 1f;
            boolean clear = true;
            for (Player player : Server.getAllPlayers())
                if (player.getPosition().distance(candidate) < requiredDistance) { clear = false; break; }
            if (clear) return candidate;
        }
        return null;
    }

    private float groundLevel(Vector3f position) {
        Chunk chunk = World.getChunk(Math.floorDiv((int) Math.floor(position.x), Chunk.SIZE_X),
                Math.floorDiv((int) Math.floor(position.z), Chunk.SIZE_Z));
        if (chunk == null || !chunk.isValid()) return Float.NaN;
        return chunk.getLODSurfaceLevel(Math.floorMod((int) Math.floor(position.x), Chunk.SIZE_X),
                Math.floorMod((int) Math.floor(position.z), Chunk.SIZE_Z), false);
    }

    private boolean isWaterAt(Vector3f position, float ground) {
        Chunk chunk = World.getChunk(Math.floorDiv((int) Math.floor(position.x), Chunk.SIZE_X),
                Math.floorDiv((int) Math.floor(position.z), Chunk.SIZE_Z));
        if (chunk == null || !chunk.isValid()) return true;
        return chunk.getLODSurfaceLevel(Math.floorMod((int) Math.floor(position.x), Chunk.SIZE_X),
                Math.floorMod((int) Math.floor(position.z), Chunk.SIZE_Z), true) > ground + .01f;
    }

    private float sectorRadius(Vector3f reference) {
        Vector2i sector = new Vector2i((int) Math.floor(reference.x / 512f), (int) Math.floor(reference.z / 512f));
        float minX = sector.x * 512f, minZ = sector.y * 512f;
        return Math.max(settings.get().minSpawnDistance,
                Math.min(Math.min(reference.x - minX, minX + 512f - reference.x),
                        Math.min(reference.z - minZ, minZ + 512f - reference.z)) - 24f);
    }

    private String random(String[] values) { return values[random.nextInt(values.length)]; }

    private List<Short> configuredSpawnTypes() {
        List<Short> types = groups.spawnTypes();
        return types.isEmpty() ? settings.get().types : types;
    }
}
