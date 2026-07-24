package de.omegazirkel.risingworld.bosses.ui;

import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import net.risingworld.api.objects.Player;

/** Standard Tools data entry retained for player-visible boss statistics. */
public final class BossPlayerPluginData extends PlayerPluginData {
    public BossPlayerPluginData(String pluginName, String version) { pluginLabel = pluginName; pluginVersion = version; }
    @Override public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player player) {
        return new BasePlayerPluginDataPanel(player, pluginLabel) { @Override protected void redrawContent() { flexWrapper.removeAllChilds(); flexWrapper.addChild(defaultEmptyStateLabel()); } };
    }
}
