package de.omegazirkel.risingworld.bosses;

import de.omegazirkel.risingworld.bosses.ui.BossOverlay;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import net.risingworld.api.objects.Player;

/** Owns opening and lifetime guarding for the boss overlay. */
public final class BossOverlayController {
    private static final String OVERLAY_ATTRIBUTE = "oz.bosses.ui.overlay";
    private final BossViewService view;

    public BossOverlayController(BossViewService view) { this.view = view; }

    public void open(Player player) {
        if (player == null || player.hasAttribute(OVERLAY_ATTRIBUTE)) return;
        player.setAttribute(OVERLAY_ATTRIBUTE, Boolean.TRUE);
        CursorManager.show(player);
        player.addUIElement(new BossOverlay(view, player));
    }
}
