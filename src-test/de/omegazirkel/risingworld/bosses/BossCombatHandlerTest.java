package de.omegazirkel.risingworld.bosses;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BossCombatHandlerTest {
    @Test
    public void remainingFollowersExcludesTheBossAndHandlesEmptyState() {
        BossGroup group = new BossGroup(1, null, "Test boss");
        group.boss = 100L;
        group.members.add(100L);
        group.members.add(101L);
        group.members.add(102L);

        assertEquals(2, BossCombatHandler.remainingFollowers(group));
        assertEquals(0, BossCombatHandler.remainingFollowers(null));

        group.members.remove(101L);
        group.members.remove(102L);
        assertEquals(0, BossCombatHandler.remainingFollowers(group));
    }
}
