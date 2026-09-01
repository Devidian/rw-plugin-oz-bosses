package de.omegazirkel.risingworld.bosses;

import java.util.function.Supplier;

import net.risingworld.api.definitions.Plants.Type;
import net.risingworld.api.events.player.PlayerCreateBlueprintEvent;
import net.risingworld.api.events.player.PlayerGameObjectHitEvent;
import net.risingworld.api.events.player.world.*;

/** Feature-specific threat rules for player world actions. */
public final class BossPlayerActionHandler {
    private final BossThreatService threat;
    private final BossDebugService debug;
    private final Supplier<PluginSettings> settings;

    public BossPlayerActionHandler(BossThreatService threat, BossDebugService debug,
            Supplier<PluginSettings> settings) {
        this.threat = threat;
        this.debug = debug;
        this.settings = settings;
    }

    public void terrain(PlayerHitTerrainEvent event) { debug.debug(event.getPlayer(), "tc.bosses.debug.terrain.hit", "PH_ITEM", BossThreatService.equippedItemName(event.getPlayer())); }
    public void placeTerrain(PlayerPlaceTerrainEvent event) { threat.addForNonMiningTerrain(event.getPlayer(), "tc.bosses.action.terrain.placed"); }
    public void destroyTerrain(PlayerDestroyTerrainEvent event) { threat.add(event.getPlayer(), threat.terrain(event.getPlayer()), "tc.bosses.action.terrain.changed", "PH_ITEM", BossThreatService.equippedItemName(event.getPlayer())); }
    public void destroyObject(PlayerDestroyObjectEvent event) { threat.add(event.getPlayer(), settings.get().objectDestroy, "tc.bosses.action.object.destroyed"); }
    public void hitVegetation(PlayerHitVegetationEvent event) { if (event.getPlantDefinition() != null && relevant(event.getPlantDefinition().type)) debug.debug(event.getPlayer(), "tc.bosses.debug.vegetation.hit"); }
    public void hitObject(PlayerHitObjectEvent event) { debug.debug(event.getPlayer(), "tc.bosses.debug.object.hit", "PH_OBJECT", event.getObjectDefinition() == null ? "-" : event.getObjectDefinition().name); }
    public void hitGameObject(PlayerGameObjectHitEvent event) { debug.debug(event.getPlayer(), "tc.bosses.debug.game.object.hit", "PH_OBJECT", event.getGameObject() == null ? "-" : event.getGameObject().getClass().getSimpleName()); }
    public void destroyVegetation(PlayerDestroyVegetationEvent event) { if (event.getPlantDefinition() == null) return; Type type = event.getPlantDefinition().type; if (type == Type.Rock) threat.add(event.getPlayer(), threat.terrain(event.getPlayer()), "tc.bosses.action.large.rock.mined"); else if (type == Type.Tree || type == Type.FruitTree || type == Type.Trunk) threat.add(event.getPlayer(), settings.get().vegetation, "tc.bosses.action.tree.felled"); }
    public void construction(PlayerPlaceConstructionEvent event) { threat.add(event.getPlayer(), settings.get().construction, "tc.bosses.action.construction"); }
    public void blueprint(PlayerCreateBlueprintEvent event) { threat.add(event.getPlayer(), settings.get().blueprint, "tc.bosses.action.blueprint"); }
    private boolean relevant(Type type) { return type == Type.Rock || type == Type.Tree || type == Type.FruitTree || type == Type.Trunk; }
}
