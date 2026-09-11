package de.omegazirkel.risingworld.bosses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Additive persistence for fixed Headhunter Informant endpoints. */
public final class BossInformantRepository {
    private final Connection db;
    public BossInformantRepository(Connection db) { this.db = db; }
    public void initialize() throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS boss_informants (npc_id INTEGER PRIMARY KEY,name TEXT NOT NULL,male INTEGER NOT NULL,x REAL NOT NULL,y REAL NOT NULL,z REAL NOT NULL,rx REAL NOT NULL,ry REAL NOT NULL,rz REAL NOT NULL,rw REAL NOT NULL)");
        }
    }
    public List<BossInformant> all() throws SQLException {
        List<BossInformant> values = new ArrayList<>();
        try (Statement statement = db.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM boss_informants")) {
            while (rows.next()) values.add(read(rows));
        }
        return values;
    }
    public Optional<BossInformant> find(long npcId) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement("SELECT * FROM boss_informants WHERE npc_id=?")) {
            statement.setLong(1, npcId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(read(rows)) : Optional.empty(); }
        }
    }
    public void save(BossInformant value) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement("INSERT INTO boss_informants VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(npc_id) DO UPDATE SET name=excluded.name,male=excluded.male,x=excluded.x,y=excluded.y,z=excluded.z,rx=excluded.rx,ry=excluded.ry,rz=excluded.rz,rw=excluded.rw")) {
            statement.setLong(1, value.npcId()); statement.setString(2, value.name()); statement.setInt(3, value.male() ? 1 : 0);
            statement.setFloat(4, value.x()); statement.setFloat(5, value.y()); statement.setFloat(6, value.z());
            statement.setFloat(7, value.rx()); statement.setFloat(8, value.ry()); statement.setFloat(9, value.rz()); statement.setFloat(10, value.rw()); statement.executeUpdate();
        }
    }
    public void replaceId(long oldId, BossInformant value) throws SQLException {
        try (PreparedStatement statement = db.prepareStatement("DELETE FROM boss_informants WHERE npc_id=?")) { statement.setLong(1, oldId); statement.executeUpdate(); }
        save(value);
    }
    private BossInformant read(ResultSet rows) throws SQLException { return new BossInformant(rows.getLong("npc_id"), rows.getString("name"), rows.getInt("male") != 0, rows.getFloat("x"), rows.getFloat("y"), rows.getFloat("z"), rows.getFloat("rx"), rows.getFloat("ry"), rows.getFloat("rz"), rows.getFloat("rw")); }
}
