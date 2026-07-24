package de.omegazirkel.risingworld.bosses;

import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public final class BossInfoStatusProvider implements PluginInfoStatusProvider {
    private final String name, version;
    public BossInfoStatusProvider(String name, String version) { this.name=name; this.version=version; }
    @Override public String getPluginName() { return name; }
    @Override public String getInfo(Player player) { return "OZ Bosses " + version + "\nSector-driven boss events and rankings.\nCommand: /ozboss"; }
    @Override public String getStatus(Player player) { return "Boss events are active."; }
}
