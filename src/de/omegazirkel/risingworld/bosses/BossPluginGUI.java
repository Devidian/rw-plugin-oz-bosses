package de.omegazirkel.risingworld.bosses;

import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.InventoryOverlayButtons;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;

/** Registers and opens the plugin-owned menu entry points. */
public final class BossPluginGUI {
    private final String pluginName;
    private final BossOverlayController overlays;
    private final BossInformantService informants;
    private final I18n i18n;

    public BossPluginGUI(Plugin plugin, BossOverlayController overlays, BossInformantService informants) {
        AssetManager.loadIconFromPlugin(plugin, "oz-bosses");
        pluginName = plugin.getDescription("name");
        this.overlays = overlays;
        this.informants = informants;
        this.i18n = I18n.getInstance(plugin);
    }

    public void openMainMenu(Player player) {
        java.util.List<MenuItem> menuItems = new java.util.ArrayList<>();
        menuItems.add(new MenuItem(pluginName, "oz-bosses", "Bosses", selected -> {
            selected.hideRadialMenu(true);
            overlays.open(selected);
        }));
        menuItems.add(new MenuItem(pluginName, "info-status", "Info / Status", selected -> {
            selected.hideRadialMenu(true);
            PluginInfoStatusProviders.show(selected, pluginName);
        }));
        if (player.isAdmin()) {
            menuItems.add(new MenuItem(pluginName, "oz-bosses", i18n.get("tc.bosses.informant.menu.male", player), selected -> {
                selected.hideRadialMenu(true); informants.create(selected, true);
            }));
            menuItems.add(new MenuItem(pluginName, "oz-bosses", i18n.get("tc.bosses.informant.menu.female", player), selected -> {
                selected.hideRadialMenu(true); informants.create(selected, false);
            }));
        }
        menuItems.add(MenuItem.closeMenu(player));
        PluginMenuManager.showMenu(player, menuItems);
    }

    public void registerInventoryEntry() {
        InventoryOverlayButtons.registerButton(pluginName, "Bosses", "oz-bosses",
                event -> overlays.open(event.getPlayer()));
    }
}
