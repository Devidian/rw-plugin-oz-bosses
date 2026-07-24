package de.omegazirkel.risingworld.bosses;

import java.util.HashSet;
import java.util.Set;

import de.omegazirkel.risingworld.bosses.ui.BossPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/** Localized administrator-only runtime diagnostics. */
public final class BossDebugService {
    private final I18n i18n;
    private final PlayerSettings playerSettings;
    private final Set<Integer> enabledPlayers = new HashSet<>();

    public BossDebugService(I18n i18n, PlayerSettings playerSettings) {
        this.i18n = i18n;
        this.playerSettings = playerSettings;
    }

    public void debug(Player player, String key, String... replacements) {
        if (!enabled(player))
            return;
        String prefix = i18n.get("TC_BOSSES_DEBUG_PREFIX", player);
        player.sendTextMessage("<color=#A0C8FF>" + prefix + "</color> "
                + BossUtils.message(i18n, key, player, replacements));
    }

    public void debugAdmins(String key, String... replacements) {
        for (Player player : Server.getAllPlayers())
            debug(player, key, replacements);
    }

    public void setEnabled(Player player, boolean enabled) {
        if (player == null || !player.isAdmin())
            return;
        if (enabled)
            enabledPlayers.add(player.getDbID());
        else
            enabledPlayers.remove(player.getDbID());
        playerSettings.setBoolean(player.getDbID(), BossPlayerPluginSettings.DEBUG_EVENTS, enabled);
        String prefix = i18n.get("TC_BOSSES_DEBUG_PREFIX", player);
        player.sendTextMessage("<color=#A0C8FF>" + prefix + "</color> "
                + i18n.get(enabled ? "TC_BOSSES_DEBUG_ENABLED" : "TC_BOSSES_DEBUG_DISABLED", player));
    }

    public boolean enabled(Player player) {
        return player != null && player.isAdmin()
                && (enabledPlayers.contains(player.getDbID())
                        || playerSettings.getBoolean(player.getDbID(), BossPlayerPluginSettings.DEBUG_EVENTS)
                                .orElse(false));
    }
}
