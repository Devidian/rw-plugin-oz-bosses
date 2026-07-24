package de.omegazirkel.risingworld.bosses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import net.risingworld.api.utils.Vector3f;

/** Persistent sector threat and player score state. */
public final class BossStateRepository {
    private final Connection db;
    public BossStateRepository(Connection db) { this.db = db; }
    public void initialize() throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS boss_sectors (key TEXT PRIMARY KEY,x INTEGER,z INTEGER,threat INTEGER,pos_x REAL,pos_y REAL,pos_z REAL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS boss_scores (id INTEGER PRIMARY KEY,name TEXT,boss INTEGER,follower INTEGER,damage INTEGER)");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS boss_groups (id INTEGER PRIMARY KEY,sector_key TEXT,name TEXT,boss_id INTEGER,level INTEGER,boss_defeated INTEGER,invalid INTEGER,killer_name TEXT,type_key TEXT,loot_key TEXT,gender_key TEXT,definition_key TEXT)");
            addColumn(statement, "invalid INTEGER DEFAULT 0");
            addColumn(statement, "type_key TEXT");
            addColumn(statement, "loot_key TEXT");
            addColumn(statement, "gender_key TEXT");
            addColumn(statement, "definition_key TEXT");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS boss_group_members (group_id INTEGER,npc_id INTEGER,PRIMARY KEY(group_id,npc_id))");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS boss_group_damage (group_id INTEGER,player_id INTEGER,damage INTEGER,PRIMARY KEY(group_id,player_id))");
        }
    }

    public void load(Map<String, BossSector> sectors, Map<Integer, BossScore> scores) throws SQLException {
        try (Statement statement = db.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT * FROM boss_sectors")) { while (result.next()) { BossSector sector = new BossSector(result.getString(1), result.getInt(2), result.getInt(3)); sector.threat = result.getInt(4); if (result.getObject("pos_x") != null) sector.position = new Vector3f(result.getFloat("pos_x"), result.getFloat("pos_y"), result.getFloat("pos_z")); sectors.put(sector.key, sector); } }
            try (ResultSet result = statement.executeQuery("SELECT * FROM boss_scores")) { while (result.next()) { BossScore score = new BossScore(result.getInt(1), result.getString(2)); score.bossKills = result.getInt(3); score.followerKills = result.getInt(4); score.damage = result.getLong(5); scores.put(score.id, score); } }
        }
    }

    public void save(Map<String, BossSector> sectors, Map<Integer, BossScore> scores) throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.executeUpdate("DELETE FROM boss_sectors"); statement.executeUpdate("DELETE FROM boss_scores");
            try (PreparedStatement q = db.prepareStatement("INSERT INTO boss_sectors VALUES(?,?,?,?,?,?,?)")) { for (BossSector s : sectors.values()) { q.setString(1,s.key);q.setInt(2,s.x);q.setInt(3,s.z);q.setInt(4,s.threat);if(s.position==null){q.setNull(5,java.sql.Types.REAL);q.setNull(6,java.sql.Types.REAL);q.setNull(7,java.sql.Types.REAL);}else{q.setFloat(5,s.position.x);q.setFloat(6,s.position.y);q.setFloat(7,s.position.z);}q.addBatch(); } q.executeBatch(); }
            try (PreparedStatement q = db.prepareStatement("INSERT INTO boss_scores VALUES(?,?,?,?,?)")) { for (BossScore s : scores.values()) { q.setInt(1,s.id);q.setString(2,s.name);q.setInt(3,s.bossKills);q.setInt(4,s.followerKills);q.setLong(5,s.damage);q.addBatch(); } q.executeBatch(); }
        }
    }

    private void addColumn(Statement statement, String column) {
        try {
            statement.executeUpdate("ALTER TABLE boss_groups ADD COLUMN " + column);
        } catch (SQLException ignored) {
            // Existing installations already contain the column.
        }
    }
}
