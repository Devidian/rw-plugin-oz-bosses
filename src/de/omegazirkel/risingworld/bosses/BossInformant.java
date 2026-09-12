package de.omegazirkel.risingworld.bosses;

/** Persisted Bosses-owned NPC endpoint for sector intelligence. */
public record BossInformant(long npcId, String name, boolean male, float x, float y, float z,
        float rx, float ry, float rz, float rw, String accountId) {
}
