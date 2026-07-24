package de.omegazirkel.risingworld.bosses;

import de.omegazirkel.risingworld.tools.ui.AssetManager;
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

    public BossPluginGUI(Plugin plugin, BossOverlayController overlays) {
        AssetManager.loadIconFromPlugin(plugin, "oz-bosses");
        pluginName = plugin.getDescription("name");
        this.overlays = overlays;
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
        menuItems.add(MenuItem.closeMenu(player));
        PluginMenuManager.showMenu(player, menuItems);
    }

    public void registerInventoryEntry() {
        InventoryOverlayButtons.registerButton(pluginName, "Bosses", "oz-bosses",
                event -> overlays.open(event.getPlayer()));
    }
}
