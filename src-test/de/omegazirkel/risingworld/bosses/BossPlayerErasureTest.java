package de.omegazirkel.risingworld.bosses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BossPlayerErasureTest {
    @Test public void removesScoresAndDamageAndAnonymizesKiller() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            BossStateRepository repository = new BossStateRepository(connection);
            repository.initialize();
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO boss_scores VALUES (17,'Alice',1,2,3),(18,'Bob',2,3,4)");
                statement.executeUpdate("INSERT INTO boss_groups (id,sector_key,name,killer_name,killer_db_id) "
                        + "VALUES (1,'1,1','Boss','Alice',17),(2,'2,2','Other','Bob',18)");
                statement.executeUpdate("INSERT INTO boss_group_damage VALUES (1,17,10),(1,18,20)");
            }
            assertTrue(repository.erasePlayerData(17, "Alice"));
            assertTrue(repository.erasePlayerData(17, "Alice"));
            assertFalse(repository.erasePlayerData(0, "Alice"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM boss_scores WHERE id=17"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_scores WHERE id=18"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM boss_group_damage WHERE player_id=17"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_group_damage WHERE player_id=18"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_groups WHERE id=1 AND killer_name='Unknown' AND killer_db_id=0"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_groups WHERE id=2 AND killer_name='Bob' AND killer_db_id=18"));
        }
    }

    @Test public void migratesLegacyGroupAndAnonymizesKnownName() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE boss_groups (id INTEGER PRIMARY KEY, sector_key TEXT, name TEXT, "
                        + "boss_id INTEGER, level INTEGER, boss_defeated INTEGER, killer_name TEXT)");
                statement.executeUpdate("INSERT INTO boss_groups(id,sector_key,name,killer_name) VALUES(1,'1,1','Boss','Alice')");
            }
            BossStateRepository repository = new BossStateRepository(connection);
            repository.initialize();
            assertTrue(repository.erasePlayerData(17, "Alice"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_groups WHERE id=1 AND killer_name='Unknown' AND killer_db_id=0"));
        }
    }

    @Test public void persistsKillerIdForPreciseFutureErasure() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            BossStateRepository repository = new BossStateRepository(connection);
            repository.initialize();
            BossGroup group = new BossGroup(1, new BossSector("1,1", 1, 1), "Boss");
            group.killerName = "Old Name";
            group.killerDbId = 17;
            group.members.add(42L);
            Map<Integer, BossGroup> active = new HashMap<>();
            active.put(group.id, group);
            BossGroupPersistence persistence = new BossGroupPersistence(connection, active);
            persistence.save();
            Map<Integer, BossGroup> loaded = persistence.load(new HashMap<>());
            assertEquals(17, loaded.get(1).killerDbId);
            assertTrue(repository.erasePlayerData(17, "New Name"));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM boss_groups WHERE id=1 AND killer_db_id=0 AND killer_name='Unknown'"));
        }
    }

    private static int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
