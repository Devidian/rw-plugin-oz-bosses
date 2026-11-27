package de.omegazirkel.risingworld.bosses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BossGroupPersistenceTest {
    @Test public void failedMemberAndDamageWritesPreservePriorSnapshotAfterReopen() throws Exception {
        for (String table : new String[] { "boss_group_members", "boss_group_damage" }) {
            Path database = Files.createTempFile("boss-group-snapshot", ".db");
            try {
                try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + database)) {
                    new BossStateRepository(db).initialize();
                    Map<Integer, BossGroup> active = new HashMap<>();
                    active.put(1, group(1, 11, 100));
                    BossGroupPersistence persistence = new BossGroupPersistence(db, active);
                    persistence.save();
                    active.clear();
                    active.put(2, group(2, 22, 200));
                    try (Statement statement = db.createStatement()) {
                        statement.execute("CREATE TRIGGER fail_snapshot BEFORE INSERT ON " + table
                                + " BEGIN SELECT RAISE(ABORT, 'injected write failure'); END");
                    }
                    persistence.save();
                    assertTrue(db.getAutoCommit());
                }
                try (Connection reopened = DriverManager.getConnection("jdbc:sqlite:" + database)) {
                    assertEquals(1, count(reopened, "SELECT COUNT(*) FROM boss_groups WHERE id=1"));
                    assertEquals(1, count(reopened, "SELECT COUNT(*) FROM boss_group_members WHERE group_id=1 AND npc_id=11"));
                    assertEquals(1, count(reopened, "SELECT COUNT(*) FROM boss_group_damage WHERE group_id=1 AND damage=100"));
                    assertEquals(0, count(reopened, "SELECT COUNT(*) FROM boss_groups WHERE id=2"));
                }
            } finally {
                Files.deleteIfExists(database);
            }
        }
    }

    @Test public void failedLoadDoesNotAllowSubsequentSaveToEraseStoredSnapshot() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new BossStateRepository(db).initialize();
            Map<Integer, BossGroup> active = new HashMap<>();
            active.put(1, group(1, 11, 100));
            BossGroupPersistence persistence = new BossGroupPersistence(db, active);
            persistence.save();
            try (Statement statement = db.createStatement()) {
                statement.execute("ALTER TABLE boss_group_members RENAME TO temporarily_unavailable");
            }
            try {
                persistence.load(new HashMap<>());
                fail("incomplete snapshot must not be returned");
            } catch (IllegalStateException expected) {
                // A failed load must not turn into an empty or partial replacement.
            }
            try (Statement statement = db.createStatement()) {
                statement.execute("ALTER TABLE temporarily_unavailable RENAME TO boss_group_members");
            }
            active.clear();
            persistence.save();
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=1"));
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_group_members WHERE group_id=1"));
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_group_damage WHERE group_id=1"));
            assertEquals(1, persistence.load(new HashMap<>()).size());
            persistence.save();
            assertEquals(0, count(db, "SELECT COUNT(*) FROM boss_groups"));
        }
    }

    @Test public void initialRecoveryBlocksSnapshotWritesUntilLoadCompletes() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new BossStateRepository(db).initialize();
            Map<Integer, BossGroup> active = new HashMap<>();
            active.put(1, group(1, 11, 100));
            BossGroupPersistence persistence = new BossGroupPersistence(db, active);
            persistence.save();
            persistence.requireCompleteLoad();
            active.clear();
            persistence.save();
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=1"));
            assertEquals(1, persistence.load(new HashMap<>()).size());
            persistence.save();
            assertEquals(0, count(db, "SELECT COUNT(*) FROM boss_groups"));
        }
    }

    @Test public void existingTransactionRemainsOwnedByCaller() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new BossStateRepository(db).initialize();
            Map<Integer, BossGroup> active = new HashMap<>();
            active.put(1, group(1, 11, 100));
            BossGroupPersistence persistence = new BossGroupPersistence(db, active);
            persistence.save();
            db.setAutoCommit(false);
            active.clear();
            active.put(2, group(2, 22, 200));
            persistence.save();
            assertFalse(db.getAutoCommit());
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=2"));
            db.rollback();
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=1"));
            assertEquals(0, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=2"));
        }
    }

    @Test public void failedSaveRollsBackOnlyItsSavepointInCallerTransaction() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            new BossStateRepository(db).initialize();
            Map<Integer, BossGroup> active = new HashMap<>();
            active.put(1, group(1, 11, 100));
            BossGroupPersistence persistence = new BossGroupPersistence(db, active);
            persistence.save();
            try (Statement statement = db.createStatement()) {
                statement.execute("CREATE TRIGGER fail_snapshot BEFORE INSERT ON boss_group_damage "
                        + "BEGIN SELECT RAISE(ABORT, 'injected write failure'); END");
            }
            db.setAutoCommit(false);
            try (Statement statement = db.createStatement()) {
                statement.execute("INSERT INTO boss_scores VALUES (7, 'Caller', 0, 0, 0)");
            }
            active.clear();
            active.put(2, group(2, 22, 200));
            persistence.save();
            assertFalse(db.getAutoCommit());
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_groups WHERE id=1"));
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_scores WHERE id=7"));
            db.commit();
            assertEquals(1, count(db, "SELECT COUNT(*) FROM boss_scores WHERE id=7"));
        }
    }

    private static BossGroup group(int id, long member, long damage) {
        BossGroup group = new BossGroup(id, new BossSector("1,1", 1, 1), "Boss " + id);
        group.boss = member;
        group.members.add(member);
        group.damage.put(7, damage);
        return group;
    }

    private static int count(Connection db, String query) throws Exception {
        try (Statement statement = db.createStatement(); ResultSet result = statement.executeQuery(query)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
