package de.empireblocks.empireban.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class IpRepository {

    private final DatabaseManager db;

    public IpRepository(DatabaseManager db) {
        this.db = db;
    }

    public void recordJoin(UUID uuid, String ip, String playerName) {
        boolean mysql = db.getDialect() == DatabaseManager.Dialect.MYSQL;
        String sql = mysql
                ? "INSERT INTO eb_ips (uuid, ip, player_name, last_seen) VALUES (?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_seen = VALUES(last_seen)"
                : "INSERT INTO eb_ips (uuid, ip, player_name, last_seen) VALUES (?, ?, ?, ?) " +
                  "ON CONFLICT(uuid, ip) DO UPDATE SET player_name = excluded.player_name, last_seen = excluded.last_seen";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, ip);
            statement.setString(3, playerName);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save IP", e);
        }
    }

    public java.util.List<String> lastKnownIps(UUID uuid) {
        String sql = "SELECT ip FROM eb_ips WHERE uuid = ? ORDER BY last_seen DESC";
        java.util.List<String> result = new java.util.ArrayList<>();
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load IPs", e);
        }
        return result;
    }

    // best-effort name->uuid lookup for offline players on proxies (no OfflinePlayer there),
    // just looks at the most recently seen record for that name
    public Optional<UUID> findUuidByName(String name) {
        String sql = "SELECT uuid FROM eb_ips WHERE player_name = ? ORDER BY last_seen DESC LIMIT 1";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not look up player", e);
        }
        return Optional.empty();
    }

    public Optional<String> latestIp(UUID uuid) {
        String sql = "SELECT ip FROM eb_ips WHERE uuid = ? ORDER BY last_seen DESC LIMIT 1";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load IP", e);
        }
        return Optional.empty();
    }
}
