package de.omegazirkel.risingworld.bosses;

import java.sql.Connection;
import java.sql.SQLException;

import de.omegazirkel.risingworld.bosses.ui.BossPlayerPluginData;
import de.omegazirkel.risingworld.bosses.ui.BossPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.InventoryOverlayButtons;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.Plugin;

/** Composes and owns the feature services used by the listener entry point. */
public final class BossRuntime {
    private final Plugin plugin;
    private final BossState state = new BossState();
    private final BossNameCatalog names = new BossNameCatalog();
    private final BossGroupCatalog groups = new BossGroupCatalog();
    private Connection db;
    private BossSettingsManager settings;
    private BossStateRepository stateRepository;
    private BossGroupPersistence groupPersistence;
    private BossLootHandler lootHandler;
    private BossPlayerActionHandler playerActions;
    private BossCombatHandler combat;
    private BossInteractionHandler interactions;

    public BossRuntime(Plugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        I18n i18n = I18n.getInstance(plugin);
        settings = new BossSettingsManager(plugin, i18n);
        names.load(plugin);
        groups.load(plugin);

        db = SQLiteConnectionFactory.open(plugin);
        stateRepository = new BossStateRepository(db);
        groupPersistence = new BossGroupPersistence(db, state.activeGroups());
        initializePersistence();

        PlayerSettings playerSettings = new PlayerSettings(db);
        BossDebugService debug = new BossDebugService(i18n, playerSettings);
        BossThreatService threat = new BossThreatService(state, debug, i18n, settings::current);
        DiscordBridge discord = new DiscordBridge(plugin);
        BossAnnouncementHandler announcements = new BossAnnouncementHandler(i18n, settings::current, discord);
        lootHandler = new BossLootHandler(plugin, debug, i18n);
        lootHandler.load();

        BossGroupRehydrationHandler rehydration = new BossGroupRehydrationHandler(state.activeGroups(),
                state.npcGroups(), state.sectors(), groupPersistence, groups, names);
        BossGroupAdminHandler groupAdmin = new BossGroupAdminHandler(state.activeGroups(), state.npcGroups(),
                groupPersistence);
        BossSpawnHandler spawn = new BossSpawnHandler(plugin, state.activeGroups(), state.npcGroups(),
                state.sectors(), state.scores(), settings::current, groups, names, groupPersistence,
                stateRepository, announcements);
        BossRewardHandler rewards = new BossRewardHandler(state.activeGroups(), new WalletBridge(plugin),
                lootHandler, settings::current, announcements, groupPersistence, i18n);
        BossViewService view = new BossViewService(state, groupAdmin, spawn, groups, settings::current, i18n);
        BossOverlayController overlays = new BossOverlayController(view);
        BossPluginGUI gui = new BossPluginGUI(plugin, overlays);

        playerActions = new BossPlayerActionHandler(threat, debug, settings::current);
        combat = new BossCombatHandler(state, threat, debug, settings::current, spawn, rewards,
                groupPersistence, announcements, playerSettings, i18n);
        interactions = new BossInteractionHandler(view, overlays, lootHandler);

        plugin.executeDelayed(2f, rehydration::rehydrate);
        spawn.schedule();
        registerTools(gui, playerSettings, debug, i18n);
    }

    public void disable() {
        saveRuntimeState();
        if (groupPersistence != null)
            groupPersistence.save();
        String pluginName = plugin.getDescription("name");
        PluginShortcutVisibility.unregister(pluginName);
        InventoryOverlayButtons.unregisterButtons(pluginName);
        PluginInfoStatusProviders.unregisterProvider(pluginName);
        try {
            if (db != null)
                db.close();
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot close boss database: " + ex.getMessage());
        }
    }

    public void reloadSettings() {
        settings.reload();
        names.load(plugin);
        groups.load(plugin);
        lootHandler.load();
    }

    public BossPlayerActionHandler playerActions() {
        return playerActions;
    }

    public BossCombatHandler combat() {
        return combat;
    }

    public BossInteractionHandler interactions() {
        return interactions;
    }

    private void initializePersistence() {
        try {
            stateRepository.initialize();
            stateRepository.load(state.sectors(), state.scores());
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot initialize boss persistence", ex);
        }
    }

    private void saveRuntimeState() {
        if (stateRepository == null)
            return;
        try {
            stateRepository.save(state.sectors(), state.scores());
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot save boss state: " + ex.getMessage());
        }
    }

    private void registerTools(BossPluginGUI gui, PlayerSettings playerSettings, BossDebugService debug, I18n i18n) {
        String pluginName = plugin.getDescription("name");
        String version = plugin.getDescription("version");
        PluginMenuManager.registerPluginMenu(
                new MenuItem(pluginName, "oz-bosses", "Bosses", gui::openMainMenu));
        PluginShortcutVisibility.register(pluginName, player -> true);
        gui.registerInventoryEntry();
        PlayerPluginSettingsOverlay.registerPlayerPluginSettings(
                new BossPlayerPluginSettings(pluginName, version, i18n, playerSettings, debug));
        PlayerPluginSettingsOverlay.registerPlayerPluginData(new BossPlayerPluginData(pluginName, version));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(new PlayerPluginAdminSettings(
                pluginName, version, settings::adminEntries, this::reloadSettings));
        PluginInfoStatusProviders.registerProvider(new BossInfoStatusProvider(pluginName, version));
    }
}
