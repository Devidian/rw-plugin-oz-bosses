package de.omegazirkel.risingworld.bosses;

import java.util.function.Supplier;

import de.omegazirkel.risingworld.bosses.ui.BossPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import net.risingworld.api.Server;
import net.risingworld.api.events.npc.NpcDamageEvent;
import net.risingworld.api.events.npc.NpcDamageEvent.Cause;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.player.PlayerDamageEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerHitNpcEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;

/** Combat, death and anti-exploit rules for active boss groups. */
public final class BossCombatHandler {
    private final BossState state;
    private final BossThreatService threat;
    private final BossDebugService debug;
    private final Supplier<PluginSettings> settings;
    private final BossSpawnHandler spawn;
    private final BossRewardHandler rewards;
    private final BossGroupPersistence persistence;
    private final PlayerSettings playerSettings;
    private final I18n i18n;

    public BossCombatHandler(BossState state, BossThreatService threat, BossDebugService debug,
            Supplier<PluginSettings> settings, BossSpawnHandler spawn, BossRewardHandler rewards,
            BossGroupPersistence persistence,
            PlayerSettings playerSettings, I18n i18n) {
        this.state = state;
        this.threat = threat;
        this.debug = debug;
        this.settings = settings;
        this.spawn = spawn;
        this.rewards = rewards;
        this.persistence = persistence;
        this.playerSettings = playerSettings;
        this.i18n = i18n;
    }

    public void hit(PlayerHitNpcEvent event) {
        Npc npc = event.getNpc();
        BossGroup group = state.group(npc);
        if (group == null || event.getDamage() <= 0) {
            if (npc != null)
                debug.debug(event.getPlayer(), "TC_BOSSES_DEBUG_NPC_HIT_NO_GROUP", "PH_NPC", npc.getName());
            return;
        }
        BossScore score = state.score(event.getPlayer());
        score.damage += event.getDamage();
        group.damage.merge(event.getPlayer().getDbID(), (long) event.getDamage(), Long::sum);
        debug.debug(event.getPlayer(), "TC_BOSSES_DEBUG_DAMAGE_RECORDED", "PH_DAMAGE",
                Short.toString(event.getDamage()), "PH_NPC", npc.getName());
        notifyOutgoingDamage(event.getPlayer(), event.getDamage(), npc);
    }

    public void damage(PlayerDamageEvent event) {
        if (event.getDamage() <= 0)
            return;
        BossGroup group = state.attacking(event.getPlayer());
        if (group != null)
            notifyIncomingDamage(event.getPlayer(), group, event.getDamage());
    }

    public void npcDeath(NpcDeathEvent event) {
        Npc npc = event.getNpc();
        BossGroup group = state.removeGroup(npc);
        if (group == null) {
            if (event.getKiller() instanceof Player player)
                threat.add(player, settings.get().npcKill, "TC_BOSSES_ACTION_NPC_KILLED");
            return;
        }
        if (event.getKiller() instanceof Player player)
            debug.debug(player, "TC_BOSSES_DEBUG_GROUP_NPC_DEATH");
        else
            group.invalid = true;
        group.members.remove(npc.getGlobalID());
        if (event.getKiller() instanceof Player player) {
            if (npc.getGlobalID() == group.boss)
                state.score(player).bossKills++;
            else
                state.score(player).followerKills++;
        }
        spawn.enhanceWeaponDrop(event.getStorageID());
        if (npc.getGlobalID() == group.boss && event.getKiller() instanceof Player player) {
            group.bossDefeated = true;
            group.killerName = player.getName();
        }
        if (group.members.isEmpty()) {
            if (group.bossDefeated && !group.invalid)
                rewards.finish(group, event);
            else
                rewards.abandon(group);
        } else {
            persistence.save();
        }
    }

    public void npcDamage(NpcDamageEvent event) {
        if (state.group(event.getNpc()) == null)
            return;
        Cause cause = event.getCause();
        if (cause != Cause.HitByPlayer && cause != Cause.ShotByPlayer)
            event.setCancelled(true);
    }

    public void playerDeath(PlayerDeathEvent event) {
        if (!(event.getKiller() instanceof Npc npc))
            return;
        BossGroup group = state.group(npc);
        if (group == null || group.finished || group.bossDefeated)
            return;
        spawn.levelUp(group, npc);
    }

    private void notifyOutgoingDamage(Player attacker, short damage, Npc target) {
        for (Player viewer : Server.getAllPlayers()) {
            boolean own = viewer.getDbID() == attacker.getDbID();
            if (!damageEnabled(viewer, own ? BossPlayerPluginSettings.OWN_OUTGOING_DAMAGE
                    : BossPlayerPluginSettings.OTHER_OUTGOING_DAMAGE, own))
                continue;
            String key = own ? "TC_BOSSES_DAMAGE_OWN_OUTGOING" : "TC_BOSSES_DAMAGE_OTHER_OUTGOING";
            viewer.sendTextMessage(BossUtils.message(i18n, key, viewer, "PH_PLAYER", attacker.getName(), "PH_DAMAGE",
                    Short.toString(damage), "PH_BOSS", target.getName()));
        }
    }

    private void notifyIncomingDamage(Player target, BossGroup group, short damage) {
        for (Player viewer : Server.getAllPlayers()) {
            boolean own = viewer.getDbID() == target.getDbID();
            if (!damageEnabled(viewer, own ? BossPlayerPluginSettings.OWN_INCOMING_DAMAGE
                    : BossPlayerPluginSettings.OTHER_INCOMING_DAMAGE, own))
                continue;
            String key = own ? "TC_BOSSES_DAMAGE_OWN_INCOMING" : "TC_BOSSES_DAMAGE_OTHER_INCOMING";
            viewer.sendTextMessage(BossUtils.message(i18n, key, viewer, "PH_PLAYER", target.getName(), "PH_DAMAGE",
                    Short.toString(damage), "PH_BOSS", group.name));
        }
    }

    private boolean damageEnabled(Player player, String key, boolean defaultValue) {
        return playerSettings.getBoolean(player.getDbID(), key).orElse(defaultValue);
    }
}
