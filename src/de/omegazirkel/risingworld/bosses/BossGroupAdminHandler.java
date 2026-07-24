package de.omegazirkel.risingworld.bosses;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.bosses.BossViewService.NamedNpcRow;
import net.risingworld.api.World;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;

/** Admin-facing inspection and cleanup of active boss groups. */
public final class BossGroupAdminHandler {
    private final Map<Integer, BossGroup> activeGroups;
    private final Map<Long, BossGroup> npcGroups;
    private final BossGroupPersistence persistence;

    public BossGroupAdminHandler(Map<Integer, BossGroup> activeGroups, Map<Long, BossGroup> npcGroups,
            BossGroupPersistence persistence) {
        this.activeGroups = activeGroups;
        this.npcGroups = npcGroups;
        this.persistence = persistence;
    }

    public int clearSector(String sectorKey) {
        List<BossGroup> targets = activeGroups.values().stream().filter(group -> group.sector.key.equals(sectorKey)).toList();
        List<Long> members = targets.stream().flatMap(group -> group.members.stream()).distinct().toList();
        for (BossGroup group : targets) {
            activeGroups.remove(group.id);
            for (long memberId : group.members)
                npcGroups.remove(memberId);
            group.sector.active = Math.max(0, group.sector.active - 1);
        }
        persistence.save();
        for (long memberId : members) {
            Npc npc = World.getNpc(memberId);
            if (npc != null && !npc.isDead())
                npc.delete();
        }
        return targets.size();
    }

    public List<NamedNpcRow> namedNpcs() {
        return java.util.Arrays.stream(World.getAllNpcs()).filter(npc -> npc != null && !npc.isDead())
                .filter(npc -> npc.getName() != null && !npc.getName().isBlank())
                .map(npc -> new NamedNpcRow(npc.getGlobalID(), npc.getName(),
                        npc.getDefinition() == null ? "-" : npc.getDefinition().name, npc.getGroupID(), npc.getHealth()))
                .sorted(Comparator.comparing(NamedNpcRow::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public boolean teleport(Player player, long npcId) {
        if (player == null || !player.isAdmin()) return false;
        Npc npc = World.getNpc(npcId);
        if (npc == null || npc.isDead()) return false;
        player.setPosition(npc.getPosition());
        return true;
    }

    public int delete(long npcId, boolean wholeGroup) {
        Npc selected = World.getNpc(npcId);
        if (selected == null) return 0;
        int groupId = selected.getGroupID();
        List<Npc> targets = wholeGroup && groupId > 0
                ? java.util.Arrays.stream(World.getAllNpcs()).filter(npc -> npc != null && npc.getGroupID() == groupId).toList()
                : List.of(selected);
        for (Npc npc : targets) {
            BossGroup group = npcGroups.remove(npc.getGlobalID());
            if (group == null) continue;
            group.members.remove(npc.getGlobalID());
            if (npc.getGlobalID() == group.boss) group.invalid = true;
            if (group.members.isEmpty()) {
                activeGroups.remove(group.id);
                group.sector.active = Math.max(0, group.sector.active - 1);
            }
        }
        persistence.save();
        for (Npc npc : targets)
            if (!npc.isDead()) npc.delete();
        return targets.size();
    }
}
