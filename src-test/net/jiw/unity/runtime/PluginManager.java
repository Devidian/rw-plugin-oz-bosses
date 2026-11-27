package net.jiw.unity.runtime;

import net.risingworld.api.Plugin;

/** Minimal in-process registry for PluginAPI integration tests. */
public final class PluginManager {
    public static Plugin wallet;

    private PluginManager() { }

    public static Plugin getPluginByName(String name) {
        return "OZ - Wallet".equals(name) ? wallet : null;
    }
}
