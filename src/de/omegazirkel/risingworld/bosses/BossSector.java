package de.omegazirkel.risingworld.bosses;

import net.risingworld.api.utils.Vector3f;

/** Mutable threat and active-group state for one world sector. */
public final class BossSector {
    public final String key;
    public final int x;
    public final int z;
    public int threat;
    public int active;
    public Vector3f position;

    public BossSector(String key, int x, int z) {
        this.key = key;
        this.x = x;
        this.z = z;
    }
}
