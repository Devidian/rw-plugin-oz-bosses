package de.omegazirkel.risingworld.bosses;

import de.omegazirkel.risingworld.bosses.ui.BossOverlay;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UITarget;

/** Owns opening and lifetime guarding for the boss overlay. */
public final class BossOverlayController {
    private static final String OVERLAY_ATTRIBUTE = "oz.bosses.ui.overlay";
    private final BossViewService view;

    public BossOverlayController(BossViewService view) { this.view = view; }

    public void open(Player player) {
        if (player == null) return;
        // Escape closes native modal UI client-side, leaving only this stale marker.
        player.deleteAttribute(OVERLAY_ATTRIBUTE);
        player.setAttribute(OVERLAY_ATTRIBUTE, Boolean.TRUE);
        BossOverlay overlay = new BossOverlay(view, player);
        player.addUIElement(overlay, UITarget.Modal);
    }
}
