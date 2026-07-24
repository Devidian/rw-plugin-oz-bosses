package de.omegazirkel.risingworld.bosses;

import java.util.Map;
import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.objects.Player;

/** Completes boss groups, grants loot and distributes damage-based bounties. */
public final class BossRewardHandler {
    private final Map<Integer, BossGroup> activeGroups;
    private final WalletBridge wallet;
    private final BossLootHandler loot;
    private final Supplier<PluginSettings> settings;
    private final BossAnnouncementHandler announcements;
    private final BossGroupPersistence persistence;
    private final I18n i18n;

    public BossRewardHandler(Map<Integer, BossGroup> activeGroups, WalletBridge wallet, BossLootHandler loot,
            Supplier<PluginSettings> settings, BossAnnouncementHandler announcements,
            BossGroupPersistence persistence, I18n i18n) {
        this.activeGroups = activeGroups;
        this.wallet = wallet;
        this.loot = loot;
        this.settings = settings;
        this.announcements = announcements;
        this.persistence = persistence;
        this.i18n = i18n;
    }

    public void finish(BossGroup group, NpcDeathEvent event) {
        group.finished = true;
        activeGroups.remove(group.id);
        group.sector.active = Math.max(0, group.sector.active - 1);
        group.sector.threat = Math.max(0, group.sector.threat - settings.get().threshold);
        announcements.announce("TC_BOSSES_ANNOUNCE_DEFEAT", "PH_PLAYER", group.killerName, "PH_BOSS", group.name);
        loot.createLootSack(event.getDeathPosition(), group.name, group.lootKey, group.level,
                Math.max(1, group.damage.size()));
        long total = group.damage.values().stream().mapToLong(Long::longValue).sum();
        if (settings.get().wallet && total > 0 && wallet.isAvailable())
            for (var entry : group.damage.entrySet()) {
                long bounty = Math.round(entry.getValue() * settings.get().bountyPercent / 100d);
                if (bounty > 0 && wallet.depositDefault(entry.getKey(), bounty, "Boss bounty: " + group.name,
                        "oz-bosses").success()) {
                    Player player = Server.getPlayerByDbID(entry.getKey());
                    if (player != null)
                        player.sendTextMessage(BossUtils.message(i18n, "TC_BOSSES_BOUNTY_RECEIVED", player,
                                "PH_AMOUNT", Long.toString(bounty)));
                }
            }
        persistence.save();
    }

    public void abandon(BossGroup group) {
        group.finished = true;
        activeGroups.remove(group.id);
        group.sector.active = Math.max(0, group.sector.active - 1);
        BossUtils.logger().info("Boss group " + group.name + " ended without a player victory.");
        persistence.save();
    }
}
