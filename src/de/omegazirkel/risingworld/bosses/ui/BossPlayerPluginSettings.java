package de.omegazirkel.risingworld.bosses.ui;

import de.omegazirkel.risingworld.bosses.BossDebugService;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;

/** Per-player chat visibility for boss combat messages. */
public final class BossPlayerPluginSettings extends PlayerPluginSettings {
    public static final String OWN_OUTGOING_DAMAGE = "oz.bosses.damage.ownOutgoing";
    public static final String OTHER_OUTGOING_DAMAGE = "oz.bosses.damage.otherOutgoing";
    public static final String OWN_INCOMING_DAMAGE = "oz.bosses.damage.ownIncoming";
    public static final String OTHER_INCOMING_DAMAGE = "oz.bosses.damage.otherIncoming";
    public static final String DEBUG_EVENTS = "oz.bosses.debug.events";
    private final I18n i18n;
    private final PlayerSettings playerSettings;
    private final BossDebugService debug;

    public BossPlayerPluginSettings(String pluginName, String version, I18n i18n,
            PlayerSettings playerSettings, BossDebugService debug) {
        pluginLabel = pluginName;
        pluginVersion = version;
        this.i18n = i18n;
        this.playerSettings = playerSettings;
        this.debug = debug;
    }

    @Override public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player player) {
        return new BasePlayerPluginSettingsPanel(player, pluginLabel) {
            @Override protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(setting(player, OWN_OUTGOING_DAMAGE, "tc.bosses.setting.own.outgoing", true));
                flexWrapper.addChild(setting(player, OTHER_OUTGOING_DAMAGE, "tc.bosses.setting.other.outgoing", false));
                flexWrapper.addChild(setting(player, OWN_INCOMING_DAMAGE, "tc.bosses.setting.own.incoming", true));
                flexWrapper.addChild(setting(player, OTHER_INCOMING_DAMAGE, "tc.bosses.setting.other.incoming", false));
                if (player.isAdmin()) flexWrapper.addChild(setting(player, DEBUG_EVENTS, "tc.bosses.setting.debug.events", false));
            }

            private OZUIElement setting(Player player, String key, String labelKey, boolean defaultValue) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(i18n.get(labelKey, player)));
                boolean value = playerSettings.getBoolean(player.getDbID(), key).orElse(defaultValue);
                element.addChild(switchButtons(player, value, event -> {
                    if (DEBUG_EVENTS.equals(key))
                        debug.setEnabled(player, !value);
                    else
                        playerSettings.setBoolean(player.getDbID(), key, !value);
                    redrawContent();
                }, i18n.get("tc.btn.off", player), i18n.get("tc.btn.on", player)));
                return element;
            }
        };
    }
}
