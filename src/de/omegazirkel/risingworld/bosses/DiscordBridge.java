package de.omegazirkel.risingworld.bosses;

import java.lang.reflect.Method;

import net.risingworld.api.Plugin;

/** Optional, dependency-free bridge to OZ - Discord Connect. */
public final class DiscordBridge {
    private final Plugin owner;

    public DiscordBridge(Plugin owner) {
        this.owner = owner;
    }

    public boolean sendTextMessage(String message, long channelId) {
        if (owner == null || channelId <= 0L || message == null || message.isBlank()) return false;
        Plugin discord = owner.getPluginByName("OZ - Discord Connect");
        if (discord == null) return false;
        try {
            Method method = discord.getClass().getMethod("sendDiscordMessageToTextChannel", String.class,
                    long.class, byte[].class);
            method.invoke(discord, message, channelId, null);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
