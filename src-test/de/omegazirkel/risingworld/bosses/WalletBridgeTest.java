package de.omegazirkel.risingworld.bosses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.jiw.unity.runtime.PluginManager;
import net.risingworld.api.Plugin;
import org.junit.After;
import org.junit.Test;

public class WalletBridgeTest {
    @After public void clearRegistry() {
        PluginManager.wallet = null;
    }

    @Test public void absenceAndLegacyProviderFailWithoutPayment() {
        WalletBridge bridge = new WalletBridge(new Owner());
        assertFalse(bridge.isAvailable());
        assertFalse(bridge.depositDefault(7, 25, "Boss bounty", "oz-bosses").success());
        PluginManager.wallet = new LegacyWallet();
        assertTrue(bridge.isAvailable());
        assertFalse(bridge.depositDefault(7, 25, "Boss bounty", "oz-bosses").success());
    }

    @Test public void usesCurrentProviderAfterReloadAndPassesExactArguments() {
        Wallet first = new Wallet(true);
        Wallet second = new Wallet(true);
        PluginManager.wallet = first;
        WalletBridge bridge = new WalletBridge(new Owner());
        assertTrue(bridge.depositDefault(7, 25, "Boss bounty", "oz-bosses").success());
        PluginManager.wallet = second;
        assertTrue(bridge.depositDefault(8, 35, "Next bounty", "oz-bosses").success());
        assertEquals(1, first.calls);
        assertEquals(1, second.calls);
        assertEquals("8:35:Next bounty:oz-bosses", second.arguments);
        PluginManager.wallet = null;
        assertFalse(bridge.isAvailable());
        assertFalse(bridge.depositDefault(7, 25, "Boss bounty", "oz-bosses").success());
    }

    @Test public void failedDepositDoesNotReportSuccess() {
        PluginManager.wallet = new Wallet(false);
        assertFalse(new WalletBridge(new Owner()).depositDefault(7, 25, "Boss bounty", "oz-bosses").success());
    }

    public static class Owner extends Plugin {
        @Override public void onEnable() { }
        @Override public void onDisable() { }
    }

    public static class LegacyWallet extends Plugin {
        @Override public void onEnable() { }
        @Override public void onDisable() { }
    }

    public static class Wallet extends Plugin {
        final boolean success;
        int calls;
        String arguments;
        Wallet(boolean success) { this.success = success; }
        public Response depositDefault(int playerId, long amount, String reason, String source) {
            calls++;
            arguments = playerId + ":" + amount + ":" + reason + ":" + source;
            return new Response(success);
        }
        @Override public void onEnable() { }
        @Override public void onDisable() { }
    }

    public static class Response {
        public final boolean success;
        public Response(boolean success) { this.success = success; }
    }
}
