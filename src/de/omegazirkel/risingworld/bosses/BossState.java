package de.omegazirkel.risingworld.bosses;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.risingworld.api.World;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector2i;

/** Owns all in-memory boss state and its lookup rules. */
public final class BossState {
    private final Map<Long, BossGroup> npcGroups = new HashMap<>();
    private final Map<Integer, BossGroup> activeGroups = new HashMap<>();
    private final Map<String, BossSector> sectors = new HashMap<>();
    private final Map<Integer, BossScore> scores = new HashMap<>();

    Map<Long, BossGroup> npcGroups() {
        return npcGroups;
    }

    Map<Integer, BossGroup> activeGroups() {
        return activeGroups;
    }

    Map<String, BossSector> sectors() {
        return sectors;
    }

    Map<Integer, BossScore> scores() {
        return scores;
    }

    BossSector sector(Player player) {
        Vector2i position = player.getSectorPosition();
        String key = position.x + "," + position.y;
        return sectors.computeIfAbsent(key, ignored -> new BossSector(key, position.x, position.y));
    }

    BossScore score(Player player) {
        return scores.computeIfAbsent(player.getDbID(), id -> new BossScore(id, player.getName()));
    }

    BossGroup group(Npc npc) {
        if (npc == null)
            return null;
        BossGroup group = npcGroups.get(npc.getGlobalID());
        if (group != null)
            return group;
        group = activeGroups.get(npc.getGroupID());
        if (group != null && !group.finished) {
            npcGroups.put(npc.getGlobalID(), group);
            if (!group.members.contains(npc.getGlobalID()))
                group.members.add(npc.getGlobalID());
        }
        return group;
    }

    BossGroup removeGroup(Npc npc) {
        BossGroup group = group(npc);
        if (npc != null)
            npcGroups.remove(npc.getGlobalID());
        return group;
    }

    BossGroup attacking(Player player) {
        for (Npc npc : World.getAllNpcsInRange(player.getPosition(), 24f)) {
            if (npc == null)
                continue;
            BossGroup group = group(npc);
            Player target = npc.getHostilePlayer();
            if (group != null && target != null && target.getDbID() == player.getDbID())
                return group;
        }
        return null;
    }

    List<BossViewService.RankingRow> ranking() {
        return scores.values().stream().sorted(Comparator.comparingLong(BossScore::value).reversed())
                .map(score -> new BossViewService.RankingRow(score.name, score.value(), score.bossKills,
                        score.followerKills, score.damage))
                .toList();
    }

    List<BossViewService.SectorRow> threatLevels(PluginSettings settings) {
        return sectors.values().stream().sorted(Comparator.comparingInt((BossSector sector) -> sector.threat).reversed())
                .map(sector -> new BossViewService.SectorRow(sector.key, sector.threat, sector.active,
                        spawnChance(sector, settings)))
                .toList();
    }

    static int spawnChance(BossSector sector, PluginSettings settings) {
        if (settings.maxBossesPerSector >= 0 && sector.active >= settings.maxBossesPerSector
                && (!settings.levelUpOnOverflow || settings.maxBossesPerSector == 0))
            return 0;
        if (settings.threshold <= 0)
            return 0;
        int availableThreat = Math.max(0, sector.threat - sector.active * settings.threshold);
        return Math.min(100, (availableThreat / settings.threshold) * settings.spawnChance);
    }
}
