package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.bosses.BossRuntime;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcDamageEvent;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerCreateBlueprintEvent;
import net.risingworld.api.events.player.PlayerDamageEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerGameObjectHitEvent;
import net.risingworld.api.events.player.PlayerHitNpcEvent;
import net.risingworld.api.events.player.PlayerObjectInteractionEvent;
import net.risingworld.api.events.player.world.PlayerDestroyObjectEvent;
import net.risingworld.api.events.player.world.PlayerDestroyTerrainEvent;
import net.risingworld.api.events.player.world.PlayerDestroyVegetationEvent;
import net.risingworld.api.events.player.world.PlayerHitObjectEvent;
import net.risingworld.api.events.player.world.PlayerHitTerrainEvent;
import net.risingworld.api.events.player.world.PlayerHitVegetationEvent;
import net.risingworld.api.events.player.world.PlayerPlaceConstructionEvent;
import net.risingworld.api.events.player.world.PlayerPlaceTerrainEvent;

/** Rising World listener entry point; all feature behavior is delegated. */
public final class Bosses extends Plugin implements Listener, FileChangeListener {
    private BossRuntime runtime;

    @Override
    public void onEnable() {
        runtime = new BossRuntime(this);
        runtime.enable();
        registerEventListener(this);
    }

    @Override
    public void onDisable() {
        unregisterEventListener(this);
        if (runtime != null)
            runtime.disable();
    }

    @Override
    public void onSettingsChanged(Path ignored) {
        if (runtime != null)
            runtime.reloadSettings();
    }

    @EventMethod
    public void terrain(PlayerHitTerrainEvent event) {
        runtime.playerActions().terrain(event);
    }

    @EventMethod
    public void placeTerrain(PlayerPlaceTerrainEvent event) {
        runtime.playerActions().placeTerrain(event);
    }

    @EventMethod
    public void destroyTerrain(PlayerDestroyTerrainEvent event) {
        runtime.playerActions().destroyTerrain(event);
    }

    @EventMethod
    public void destroyObject(PlayerDestroyObjectEvent event) {
        runtime.playerActions().destroyObject(event);
    }

    @EventMethod
    public void hitVegetation(PlayerHitVegetationEvent event) {
        runtime.playerActions().hitVegetation(event);
    }

    @EventMethod
    public void hitObject(PlayerHitObjectEvent event) {
        runtime.playerActions().hitObject(event);
    }

    @EventMethod
    public void hitGameObject(PlayerGameObjectHitEvent event) {
        runtime.playerActions().hitGameObject(event);
    }

    @EventMethod
    public void destroyVegetation(PlayerDestroyVegetationEvent event) {
        runtime.playerActions().destroyVegetation(event);
    }

    @EventMethod
    public void construction(PlayerPlaceConstructionEvent event) {
        runtime.playerActions().construction(event);
    }

    @EventMethod
    public void blueprint(PlayerCreateBlueprintEvent event) {
        runtime.playerActions().blueprint(event);
    }

    @EventMethod
    public void hit(PlayerHitNpcEvent event) {
        runtime.combat().hit(event);
    }

    @EventMethod
    public void damage(PlayerDamageEvent event) {
        runtime.combat().damage(event);
    }

    @EventMethod
    public void npcDeath(NpcDeathEvent event) {
        runtime.combat().npcDeath(event);
    }

    @EventMethod
    public void npcDamage(NpcDamageEvent event) {
        runtime.combat().npcDamage(event);
    }

    @EventMethod
    public void playerDeath(PlayerDeathEvent event) {
        runtime.combat().playerDeath(event);
    }

    @EventMethod
    public void command(PlayerCommandEvent event) {
        runtime.interactions().command(event);
    }

    @EventMethod
    public void openLootSack(PlayerObjectInteractionEvent event) {
        runtime.interactions().openLootSack(event);
    }
}
