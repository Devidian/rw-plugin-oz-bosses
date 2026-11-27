package de.omegazirkel.risingworld.bosses;

import net.risingworld.api.Plugin;

/** Retains the Bosses API while using the shared optional Wallet adapter. */
public final class WalletBridge {
    private final de.omegazirkel.risingworld.tools.bridge.WalletBridge delegate;

    public WalletBridge(Plugin owner) {
        this.delegate = new de.omegazirkel.risingworld.tools.bridge.WalletBridge(owner);
    }

    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    public Result depositDefault(int playerDbId, long amount, String reason, String source) {
        return new Result(delegate.depositDefault(playerDbId, amount, reason, source).success());
    }

    public record Result(boolean success) { }
}
