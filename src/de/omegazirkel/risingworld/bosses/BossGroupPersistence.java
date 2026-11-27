package de.omegazirkel.risingworld.bosses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** Stores the active boss-group snapshot independently from event handling. */
public final class BossGroupPersistence {
    private final Connection db;
    private final Map<Integer, BossGroup> activeGroups;
    private boolean loadFailed;

    public BossGroupPersistence(Connection db, Map<Integer, BossGroup> activeGroups) {
        this.db = db;
        this.activeGroups = activeGroups;
    }

    void requireCompleteLoad() {
        synchronized (db) {
            loadFailed = true;
        }
    }

    public void save() {
        synchronized (db) {
            if (loadFailed) {
                BossUtils.logger().error("Cannot save boss groups after an incomplete snapshot load");
                return;
            }
            saveSnapshot();
        }
    }

    private void saveSnapshot() {
        Map<Integer, BossGroup> groups = new HashMap<>();
        for (BossGroup group : activeGroups.values())
            if (!group.finished && !group.members.isEmpty()) groups.put(group.id, group);
        boolean ownTransaction = false;
        boolean restoreAutoCommit = false;
        Savepoint savepoint = null;
        try {
            ownTransaction = db.getAutoCommit();
            if (ownTransaction) {
                db.setAutoCommit(false);
                restoreAutoCommit = true;
            } else {
                savepoint = db.setSavepoint();
            }
            try (Statement statement = db.createStatement()) {
                statement.executeUpdate("DELETE FROM boss_group_damage");
                statement.executeUpdate("DELETE FROM boss_group_members");
                statement.executeUpdate("DELETE FROM boss_groups");
                try (PreparedStatement groupQuery = db.prepareStatement("INSERT INTO boss_groups (id,sector_key,name,boss_id,level,boss_defeated,invalid,killer_name,type_key,loot_key,gender_key,definition_key,killer_db_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)");
                        PreparedStatement memberQuery = db.prepareStatement("INSERT INTO boss_group_members VALUES(?,?)");
                        PreparedStatement damageQuery = db.prepareStatement("INSERT INTO boss_group_damage VALUES(?,?,?)")) {
                    for (BossGroup group : groups.values()) {
                        groupQuery.setInt(1, group.id); groupQuery.setString(2, group.sector.key); groupQuery.setString(3, group.name);
                        groupQuery.setLong(4, group.boss); groupQuery.setInt(5, group.level); groupQuery.setInt(6, group.bossDefeated ? 1 : 0);
                        groupQuery.setInt(7, group.invalid ? 1 : 0); groupQuery.setString(8, group.killerName);
                        groupQuery.setString(9, group.typeKey); groupQuery.setString(10, group.lootKey); groupQuery.setString(11, group.genderKey);
                        groupQuery.setString(12, group.definitionKey); groupQuery.setInt(13, group.killerDbId); groupQuery.addBatch();
                        for (long member : group.members) { memberQuery.setInt(1, group.id); memberQuery.setLong(2, member); memberQuery.addBatch(); }
                        for (var damage : group.damage.entrySet()) { damageQuery.setInt(1, group.id); damageQuery.setInt(2, damage.getKey()); damageQuery.setLong(3, damage.getValue()); damageQuery.addBatch(); }
                    }
                    groupQuery.executeBatch(); memberQuery.executeBatch(); damageQuery.executeBatch();
                }
            }
            if (ownTransaction) db.commit();
            else db.releaseSavepoint(savepoint);
        } catch (SQLException | RuntimeException ex) {
            try {
                if (ownTransaction && restoreAutoCommit) {
                    db.rollback();
                } else if (savepoint != null) {
                    db.rollback(savepoint);
                    db.releaseSavepoint(savepoint);
                }
            } catch (SQLException rollbackError) {
                restoreAutoCommit = false;
                BossUtils.logger().error("Cannot roll back boss snapshot: " + rollbackError.getMessage());
            }
            BossUtils.logger().error("Cannot save active boss groups: " + ex.getMessage());
        } finally {
            if (restoreAutoCommit) {
                try {
                    db.setAutoCommit(true);
                } catch (SQLException ex) {
                    BossUtils.logger().error("Cannot restore boss database autocommit: " + ex.getMessage());
                }
            }
        }
    }

    public Map<Integer, BossGroup> load(Map<String, BossSector> sectors) {
        synchronized (db) {
            return loadSnapshot(sectors);
        }
    }

    private Map<Integer, BossGroup> loadSnapshot(Map<String, BossSector> sectors) {
        Map<Integer, BossGroup> groups = new HashMap<>();
        Map<String, BossSector> loadedSectors = new HashMap<>(sectors);
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_groups");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                String key = result.getString("sector_key");
                BossSector sector = loadedSectors.computeIfAbsent(key, ignored -> sector(key));
                BossGroup group = new BossGroup(result.getInt("id"), sector, result.getString("name"));
                group.boss = result.getLong("boss_id");
                group.level = result.getInt("level");
                group.bossDefeated = result.getInt("boss_defeated") != 0;
                group.invalid = result.getInt("invalid") != 0;
                group.killerName = result.getString("killer_name");
                group.killerDbId = result.getInt("killer_db_id");
                group.typeKey = valueOrDefault(result.getString("type_key"), "default");
                group.lootKey = valueOrDefault(result.getString("loot_key"), "default");
                group.genderKey = valueOrDefault(result.getString("gender_key"), "any");
                group.definitionKey = valueOrDefault(result.getString("definition_key"), "");
                groups.put(group.id, group);
            }
            loadMembers(groups);
            loadDamage(groups);
            sectors.putAll(loadedSectors);
            loadFailed = false;
            return groups;
        } catch (SQLException | RuntimeException ex) {
            loadFailed = true;
            throw new IllegalStateException("Cannot load complete boss-group snapshot", ex);
        }
    }

    private void loadMembers(Map<Integer, BossGroup> groups) throws SQLException {
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_group_members");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                BossGroup group = groups.get(result.getInt("group_id"));
                if (group != null)
                    group.members.add(result.getLong("npc_id"));
            }
        }
    }

    private void loadDamage(Map<Integer, BossGroup> groups) throws SQLException {
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_group_damage");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                BossGroup group = groups.get(result.getInt("group_id"));
                if (group != null)
                    group.damage.put(result.getInt("player_id"), result.getLong("damage"));
            }
        }
    }

    private BossSector sector(String key) {
        String[] coordinates = key.split(",");
        return new BossSector(key, Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
