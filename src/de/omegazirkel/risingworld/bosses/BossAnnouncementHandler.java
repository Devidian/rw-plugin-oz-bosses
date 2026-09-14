package de.omegazirkel.risingworld.bosses;

import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.bosses.ui.BossPlayerPluginSettings;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/** Delivers localized boss announcements in game and to the optional Discord bridge. */
public final class BossAnnouncementHandler {
    private final I18n i18n;
    private final Supplier<PluginSettings> settings;
    private final DiscordBridge discord;
    private final PlayerSettings playerSettings;

    public BossAnnouncementHandler(I18n i18n, Supplier<PluginSettings> settings, DiscordBridge discord,
            PlayerSettings playerSettings) {
        this.i18n = i18n;
        this.settings = settings;
        this.discord = discord;
        this.playerSettings = playerSettings;
    }

    public void announce(String key, String... replacements) {
        announcePlayers(key, replacements);
        sendDiscord(key, replacements);
    }

    /** Broadcasts only in-game, for state updates that are not Discord events. */
    public void announcePlayers(String key, String... replacements) {
        for (Player player : Server.getAllPlayers()) {
            if (!enabled(player, key))
                continue;
            String message = BossUtils.message(i18n, key, player, replacements);
            player.sendTextMessage(message);
            player.sendYellMessage(message, 8f, true);
        }
    }

    private boolean enabled(Player player, String announcementKey) {
        String preference = switch (announcementKey) {
            case "tc.bosses.announce.spawn" -> BossPlayerPluginSettings.ANNOUNCE_SPAWN;
            case "tc.bosses.announce.level" -> BossPlayerPluginSettings.ANNOUNCE_LEVEL;
            case "tc.bosses.announce.defeat" -> BossPlayerPluginSettings.ANNOUNCE_DEFEAT;
            case "tc.bosses.announce.followers.remaining" -> BossPlayerPluginSettings.ANNOUNCE_FOLLOWERS;
            default -> null;
        };
        return preference == null || playerSettings == null
                || playerSettings.getBoolean(player.getDbID(), preference).orElse(true);
    }

    private void sendDiscord(String key, String... replacements) {
        if (discord == null || settings.get().discordChannel <= 0)
            return;
        String message = BossUtils.replace(i18n.get(key, "de"), replacements);
        discord.sendTextMessage(message.replaceAll("<[^>]+>", "").replaceFirst("^\\[[^]]+\\]\\s*", ""),
                settings.get().discordChannel);
    }
}
