package de.omegazirkel.risingworld.bosses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.risingworld.api.World;
import net.risingworld.api.objects.Npc;

/** Reattaches persisted boss groups to live NPCs after a plugin reload. */
public final class BossGroupRehydrationHandler {
    private final Map<Integer, BossGroup> activeGroups;
    private final Map<Long, BossGroup> npcGroups;
    private final Map<String, BossSector> sectors;
    private final BossGroupPersistence persistence;
    private final BossGroupCatalog groups;
    private final BossNameCatalog names;

    public BossGroupRehydrationHandler(Map<Integer, BossGroup> activeGroups, Map<Long, BossGroup> npcGroups,
            Map<String, BossSector> sectors, BossGroupPersistence persistence, BossGroupCatalog groups,
            BossNameCatalog names) {
        this.activeGroups = activeGroups;
        this.npcGroups = npcGroups;
        this.sectors = sectors;
        this.persistence = persistence;
        this.groups = groups;
        this.names = names;
    }

    public void rehydrate() {
        npcGroups.clear(); activeGroups.clear();
        for (BossSector sector : sectors.values()) sector.active = 0;
        Map<Integer, BossGroup> restored = persistence.load(sectors);
        for (var recovered : restorePersistentGroups().entrySet())
            restored.putIfAbsent(recovered.getKey(), recovered.getValue());
        for (BossGroup group : restored.values()) {
            activeGroups.put(group.id, group);
            for (long memberId : new ArrayList<>(group.members)) {
                Npc npc = World.getNpc(memberId);
                if (npc == null || npc.isDead()) { group.members.remove(memberId); continue; }
                refreshRestoredGroupMember(group, npc);
                group.memberNames.add(npc.getName());
                npcGroups.put(memberId, group);
            }
            if (group.members.isEmpty()) { activeGroups.remove(group.id); continue; }
            group.sector.active++;
        }
        persistence.save();
        long count = restored.values().stream().filter(group -> !group.members.isEmpty()).count();
        BossUtils.logger().info("Restored " + count + " active boss groups.");
    }

    private Map<Integer, BossGroup> restorePersistentGroups() {
        Map<Integer, BossGroup> restored = new HashMap<>();
        for (Npc npc : World.getAllNpcs()) {
            if (npc == null || npc.isDead() || npc.getGroupID() <= 0)
                continue;
            String type = groups.nameType(npc);
            String gender = BossUtils.genderKey(npc);
            if (type == null || !names.isBossName(type, gender, npc.getName()))
                continue;
            BossGroup group = new BossGroup(npc.getGroupID(), sectorAt(npc.getPosition()), npc.getName());
            group.definitionKey = valueOrDefault(groups.definitionKey(npc), "");
            group.typeKey = type;
            group.lootKey = groups.lootTable(npc, type);
            group.genderKey = gender;
            group.boss = npc.getGlobalID();
            restored.putIfAbsent(group.id, group);
        }
        for (Npc npc : World.getAllNpcs()) {
            if (npc == null || npc.isDead())
                continue;
            BossGroup group = restored.get(npc.getGroupID());
            if (group != null)
                group.members.add(npc.getGlobalID());
        }
        if (!restored.isEmpty())
            BossUtils.logger().info("Recovered " + restored.size() + " boss groups from live NPC group IDs.");
        return restored;
    }

    private void refreshRestoredGroupMember(BossGroup group, Npc npc) {
        String type = groups.nameType(npc);
        if (type == null)
            return;
        if (group.definitionKey == null || group.definitionKey.isBlank())
            group.definitionKey = valueOrDefault(groups.definitionKey(npc), "");
        if ("default".equals(group.typeKey)) {
            group.typeKey = type;
            group.lootKey = groups.lootTable(npc, type);
            group.genderKey = BossUtils.genderKey(npc);
        }
    }

    private BossSector sectorAt(net.risingworld.api.utils.Vector3f position) {
        var sector = BossUtils.sectorPosition(position);
        int x = sector.x;
        int z = sector.y;
        String key = x + "," + z;
        return sectors.computeIfAbsent(key, ignored -> new BossSector(key, x, z));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
