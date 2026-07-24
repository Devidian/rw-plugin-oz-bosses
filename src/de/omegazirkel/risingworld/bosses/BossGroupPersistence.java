package de.omegazirkel.risingworld.bosses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** Stores the active boss-group snapshot independently from event handling. */
public final class BossGroupPersistence {
    private final Connection db;
    private final Map<Integer, BossGroup> activeGroups;

    public BossGroupPersistence(Connection db, Map<Integer, BossGroup> activeGroups) {
        this.db = db;
        this.activeGroups = activeGroups;
    }

    public void save() {
        Map<Integer, BossGroup> groups = new HashMap<>();
        for (BossGroup group : activeGroups.values())
            if (!group.finished && !group.members.isEmpty()) groups.put(group.id, group);
        try (Statement statement = db.createStatement()) {
            statement.executeUpdate("DELETE FROM boss_group_damage");
            statement.executeUpdate("DELETE FROM boss_group_members");
            statement.executeUpdate("DELETE FROM boss_groups");
            try (PreparedStatement groupQuery = db.prepareStatement("INSERT INTO boss_groups (id,sector_key,name,boss_id,level,boss_defeated,invalid,killer_name,type_key,loot_key,gender_key) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
                    PreparedStatement memberQuery = db.prepareStatement("INSERT INTO boss_group_members VALUES(?,?)");
                    PreparedStatement damageQuery = db.prepareStatement("INSERT INTO boss_group_damage VALUES(?,?,?)")) {
                for (BossGroup group : groups.values()) {
                    groupQuery.setInt(1, group.id); groupQuery.setString(2, group.sector.key); groupQuery.setString(3, group.name);
                    groupQuery.setLong(4, group.boss); groupQuery.setInt(5, group.level); groupQuery.setInt(6, group.bossDefeated ? 1 : 0);
                    groupQuery.setInt(7, group.invalid ? 1 : 0); groupQuery.setString(8, group.killerName);
                    groupQuery.setString(9, group.typeKey); groupQuery.setString(10, group.lootKey); groupQuery.setString(11, group.genderKey); groupQuery.addBatch();
                    for (long member : group.members) { memberQuery.setInt(1, group.id); memberQuery.setLong(2, member); memberQuery.addBatch(); }
                    for (var damage : group.damage.entrySet()) { damageQuery.setInt(1, group.id); damageQuery.setInt(2, damage.getKey()); damageQuery.setLong(3, damage.getValue()); damageQuery.addBatch(); }
                }
                groupQuery.executeBatch(); memberQuery.executeBatch(); damageQuery.executeBatch();
            }
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot save active boss groups: " + ex.getMessage());
        }
    }

    public Map<Integer, BossGroup> load(Map<String, BossSector> sectors) {
        Map<Integer, BossGroup> groups = new HashMap<>();
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_groups");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                String key = result.getString("sector_key");
                BossSector sector = sectors.computeIfAbsent(key, ignored -> sector(key));
                BossGroup group = new BossGroup(result.getInt("id"), sector, result.getString("name"));
                group.boss = result.getLong("boss_id");
                group.level = result.getInt("level");
                group.bossDefeated = result.getInt("boss_defeated") != 0;
                group.invalid = result.getInt("invalid") != 0;
                group.killerName = result.getString("killer_name");
                group.typeKey = valueOrDefault(result.getString("type_key"), "default");
                group.lootKey = valueOrDefault(result.getString("loot_key"), "default");
                group.genderKey = valueOrDefault(result.getString("gender_key"), "any");
                groups.put(group.id, group);
            }
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot load stored boss groups: " + ex.getMessage());
            return groups;
        }
        loadMembers(groups);
        loadDamage(groups);
        return groups;
    }

    private void loadMembers(Map<Integer, BossGroup> groups) {
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_group_members");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                BossGroup group = groups.get(result.getInt("group_id"));
                if (group != null)
                    group.members.add(result.getLong("npc_id"));
            }
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot load stored boss members: " + ex.getMessage());
        }
    }

    private void loadDamage(Map<Integer, BossGroup> groups) {
        try (PreparedStatement query = db.prepareStatement("SELECT * FROM boss_group_damage");
                ResultSet result = query.executeQuery()) {
            while (result.next()) {
                BossGroup group = groups.get(result.getInt("group_id"));
                if (group != null)
                    group.damage.put(result.getInt("player_id"), result.getLong("damage"));
            }
        } catch (SQLException ex) {
            BossUtils.logger().error("Cannot load stored boss damage: " + ex.getMessage());
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
