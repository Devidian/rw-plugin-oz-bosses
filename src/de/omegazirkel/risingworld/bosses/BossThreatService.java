package de.omegazirkel.risingworld.bosses;

import java.util.Locale;
import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

/** Threat calculation and sector mutation rules. */
public final class BossThreatService {
    private final BossState state;
    private final BossDebugService debug;
    private final I18n i18n;
    private final Supplier<PluginSettings> settings;

    public BossThreatService(BossState state, BossDebugService debug, I18n i18n,
            Supplier<PluginSettings> settings) {
        this.state = state;
        this.debug = debug;
        this.i18n = i18n;
        this.settings = settings;
    }

    public void add(Player player, int amount, String actionKey, String... actionReplacements) {
        if (player == null)
            return;
        if (amount <= 0) {
            debug.debug(player, "TC_BOSSES_DEBUG_THREAT_IRRELEVANT", "PH_ACTION",
                    BossUtils.message(i18n, actionKey, player, actionReplacements));
            return;
        }
        BossSector sector = state.sector(player);
        sector.threat += amount;
        sector.position = new Vector3f(player.getPosition());
        debug.debug(player, "TC_BOSSES_DEBUG_THREAT_INCREASED", "PH_AMOUNT", Integer.toString(amount),
                "PH_ACTION", BossUtils.message(i18n, actionKey, player, actionReplacements));
    }

    public int terrain(Player player) {
        PluginSettings current = settings.get();
        if (player == null || player.getEquippedItem() == null || player.getEquippedItem().getDefinition() == null)
            return current.terrain;
        String normalized = equippedItemSearchText(player);
        if (isMiningTool(normalized))
            return current.pickaxe;
        if (normalized.contains("hoe") || normalized.contains("rake") || normalized.contains("harke"))
            return current.hoe;
        if (normalized.contains("sledgehammer") || normalized.contains("vorschlaghammer"))
            return current.sledgehammer;
        return current.terrain;
    }

    public void addForNonMiningTerrain(Player player, String actionKey) {
        String item = equippedItemName(player);
        if (isMiningTool(equippedItemSearchText(player)))
            return;
        add(player, terrain(player), actionKey, "PH_ITEM", item);
    }

    private static String equippedItemSearchText(Player player) {
        if (player == null || player.getEquippedItem() == null)
            return "";
        String definitionName = player.getEquippedItem().getDefinition() == null
                ? ""
                : player.getEquippedItem().getDefinition().name;
        return ((definitionName == null ? "" : definitionName) + " " + equippedItemName(player))
                .toLowerCase(Locale.ROOT);
    }

    static boolean isMiningTool(String normalized) {
        return normalized.contains("pickaxe")
                || normalized.contains("spitzhacke")
                || normalized.contains("miningdrill")
                || normalized.contains("mining drill")
                || normalized.contains("mining_drill");
    }

    public static String equippedItemName(Player player) {
        return player == null || player.getEquippedItem() == null ? "-" : player.getEquippedItem().getName();
    }
}
