package de.omegazirkel.risingworld.bosses;

import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerObjectInteractionEvent;

/** Commands owned by the boss plugin. */
public final class BossInteractionHandler {
    private final BossViewService view;
    private final BossOverlayController overlays;
    private final BossLootHandler lootHandler;

    public BossInteractionHandler(BossViewService view, BossOverlayController overlays,
            BossLootHandler lootHandler) {
        this.view = view;
        this.overlays = overlays;
        this.lootHandler = lootHandler;
    }
    public void command(PlayerCommandEvent event) {
        String[] parts = event.getCommand().trim().split("\\s+");
        if (!parts[0].equalsIgnoreCase("/ozboss")) return;
        if (parts.length > 1 && parts[1].equalsIgnoreCase("spawn")) {
            view.spawnForAdmin(event.getPlayer(), parts.length > 2 ? parts[2] : null);
            return;
        }
        overlays.open(event.getPlayer());
    }

    public void openLootSack(PlayerObjectInteractionEvent event) {
        lootHandler.openLootSack(event);
    }
}
