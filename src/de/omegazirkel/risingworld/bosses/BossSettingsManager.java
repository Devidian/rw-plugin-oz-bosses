package de.omegazirkel.risingworld.bosses;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;
import net.risingworld.api.Plugin;

/** Loads runtime settings and exposes their Tools admin metadata. */
public final class BossSettingsManager {
    private final Plugin plugin;
    private final I18n i18n;
    private PluginSettings current;

    public BossSettingsManager(Plugin plugin, I18n i18n) {
        this.plugin = plugin;
        this.i18n = i18n;
        reload();
    }

    public PluginSettings current() {
        return current;
    }

    public void reload() {
        current = PluginSettings.load(settingsFile());
    }

    public List<AdminSettingsEntry> adminEntries() {
        Path file = settingsFile();
        return List.of(
                group("threat"), setting(file, "threat.threshold", AdminSettingsType.INTEGER),
                setting(file, "threat.checkIntervalMinutes", AdminSettingsType.INTEGER),
                setting(file, "threat.npcKill", AdminSettingsType.INTEGER),
                setting(file, "threat.pickaxe", AdminSettingsType.INTEGER),
                setting(file, "threat.hoe", AdminSettingsType.INTEGER),
                setting(file, "threat.sledgehammer", AdminSettingsType.INTEGER),
                setting(file, "threat.vegetation", AdminSettingsType.INTEGER),
                setting(file, "threat.objectDestroy", AdminSettingsType.INTEGER),
                group("boss"), setting(file, "boss.baseHealth", AdminSettingsType.INTEGER),
                setting(file, "boss.healthPerLevel", AdminSettingsType.INTEGER),
                setting(file, "boss.initialFollowers", AdminSettingsType.INTEGER),
                setting(file, "boss.followersPerOnlinePlayer", AdminSettingsType.DECIMAL),
                setting(file, "boss.minSpawnDistance", AdminSettingsType.INTEGER),
                setting(file, "boss.spawnChance", AdminSettingsType.INTEGER),
                group("integrations"), setting(file, "wallet.enabled", AdminSettingsType.BOOLEAN),
                setting(file, "wallet.bountyPercent", AdminSettingsType.DECIMAL),
                setting(file, "discord.channelId", AdminSettingsType.STRING));
    }

    private AdminSettingsEntry group(String key) {
        String base = "TC_SETTING_" + key.toUpperCase(Locale.ROOT);
        return AdminSettingsEntry.group(key, i18n.get(base + "_LABEL"), i18n.get(base + "_DESC"));
    }

    private AdminSettingsEntry setting(Path file, String key, AdminSettingsType type) {
        Path defaults = Path.of(plugin.getPath(), "settings.default.properties");
        String defaultValue = PluginSettings.read(defaults, key, "");
        String value = PluginSettings.read(file, key, defaultValue);
        String base = "TC_SETTING_" + key.toUpperCase(Locale.ROOT).replace(".", "");
        return new AdminSettingsEntry(key, i18n.get(base + "_LABEL"), i18n.get(base + "_DESC"), value, defaultValue,
                type, false, next -> SettingsFileEditor.writeValue(file, key, next));
    }

    private Path settingsFile() {
        return Path.of(plugin.getPath(), "settings.properties");
    }
}
