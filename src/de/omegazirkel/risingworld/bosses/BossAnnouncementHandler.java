package de.omegazirkel.risingworld.bosses;

import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/** Delivers localized boss announcements in game and to the optional Discord bridge. */
public final class BossAnnouncementHandler {
    private final I18n i18n;
    private final Supplier<PluginSettings> settings;
    private final DiscordBridge discord;

    public BossAnnouncementHandler(I18n i18n, Supplier<PluginSettings> settings, DiscordBridge discord) {
        this.i18n = i18n;
        this.settings = settings;
        this.discord = discord;
    }

    public void announce(String key, String... replacements) {
        for (Player player : Server.getAllPlayers()) {
            String message = BossUtils.message(i18n, key, player, replacements);
            player.sendTextMessage(message);
            player.sendYellMessage(message, 8f, true);
        }
        sendDiscord(key, replacements);
    }

    private void sendDiscord(String key, String... replacements) {
        if (discord == null || settings.get().discordChannel <= 0)
            return;
        String message = BossUtils.replace(i18n.get(key, "de"), replacements);
        discord.sendTextMessage(message.replaceAll("<[^>]+>", "").replaceFirst("^\\[[^]]+\\]\\s*", ""),
                settings.get().discordChannel);
    }
}
