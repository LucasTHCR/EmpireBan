package de.empireblocks.empireban.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogRepository {

    public record LogEntry(long id, UUID actorUuid, String actorName, String action,
                            String targetName, String details, long createdAt) {
    }

    private final DatabaseManager db;

    public LogRepository(DatabaseManager db) {
        this.db = db;
    }

    public void log(UUID actorUuid, String actorName, String action, String targetName, String details) {
        String sql = "INSERT INTO eb_logs (actor_uuid, actor_name, action, target_name, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actorUuid != null ? actorUuid.toString() : null);
            statement.setString(2, actorName);
            statement.setString(3, action);
            statement.setString(4, targetName);
            statement.setString(5, details);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save log entry", e);
        }
    }

    public List<LogEntry> page(int page, int pageSize) {
        String sql = "SELECT * FROM eb_logs ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<LogEntry> result = new ArrayList<>();
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pageSize);
            statement.setInt(2, Math.max(0, page - 1) * pageSize);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String actorUuidStr = rs.getString("actor_uuid");
                    result.add(new LogEntry(
                            rs.getLong("id"),
                            actorUuidStr != null ? UUID.fromString(actorUuidStr) : null,
                            rs.getString("actor_name"),
                            rs.getString("action"),
                            rs.getString("target_name"),
                            rs.getString("details"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load logs", e);
        }
        return result;
    }

    public void clear() {
        try (Connection connection = db.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM eb_logs");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete logs", e);
        }
    }
}
