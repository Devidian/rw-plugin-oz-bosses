package de.omegazirkel.risingworld.bosses;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.risingworld.api.Plugin;

/** Reflection bridge keeps Wallet optional and avoids a compile-time plugin dependency. */
public final class WalletBridge {
    private final Plugin owner;
    private Plugin wallet;
    public WalletBridge(Plugin owner) { this.owner = owner; refresh(); }
    private void refresh() { wallet = owner.getPluginByName("OZ - Wallet"); }
    public boolean isAvailable() { return wallet != null || (refreshAndAvailable()); }
    private boolean refreshAndAvailable() { refresh(); return wallet != null; }
    public Result depositDefault(int playerDbId, long amount, String reason, String source) {
        if (!isAvailable()) return new Result(false);
        try {
            Method method = wallet.getClass().getMethod("depositDefault", int.class, long.class, String.class, String.class);
            Object result = method.invoke(wallet, playerDbId, amount, reason, source);
            Field success = result.getClass().getField("success");
            return new Result(Boolean.TRUE.equals(success.get(result)));
        } catch (ReflectiveOperationException ex) { return new Result(false); }
    }
    public record Result(boolean success) { }
}
