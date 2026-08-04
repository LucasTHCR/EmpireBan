package de.empireblocks.empireban.core.db;

import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.model.PunishmentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentRepository {

    private final DatabaseManager db;

    public PunishmentRepository(DatabaseManager db) {
        this.db = db;
    }

    public long insert(Punishment punishment) {
        String sql = "INSERT INTO eb_punishments " +
                "(uuid, player_name, ip, type, reason, id_key, level, operator_uuid, operator_name, created_at, expires_at, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, punishment.getUuid().toString());
            statement.setString(2, punishment.getPlayerName());
            statement.setString(3, punishment.getIp());
            statement.setString(4, punishment.getType().name());
            statement.setString(5, punishment.getReason());
            statement.setString(6, punishment.getIdKey());
            statement.setInt(7, punishment.getLevel());
            statement.setString(8, punishment.getOperatorUuid() != null ? punishment.getOperatorUuid().toString() : null);
            statement.setString(9, punishment.getOperatorName());
            statement.setLong(10, punishment.getCreatedAt());
            statement.setLong(11, punishment.getExpiresAt());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save punishment", e);
        }
        return -1;
    }

    public Optional<Punishment> findActive(UUID uuid, PunishmentType type) {
        String sql = "SELECT * FROM eb_punishments WHERE uuid = ? AND type = ? AND active = 1 ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, type.name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load punishment", e);
        }
        return Optional.empty();
    }

    public Optional<Punishment> findActiveBan(UUID uuid) {
        String sql = "SELECT * FROM eb_punishments WHERE uuid = ? AND type IN ('BAN','IP_BAN') AND active = 1 ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load ban", e);
        }
        return Optional.empty();
    }

    public Optional<Punishment> findActiveIpBan(String ip) {
        String sql = "SELECT * FROM eb_punishments WHERE ip = ? AND type IN ('BAN','IP_BAN') AND active = 1 ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load IP ban", e);
        }
        return Optional.empty();
    }

    /** Active bans on the given ip belonging to a different uuid than the one supplied - used for alt detection. */
    public List<Punishment> findActiveBansByIpExcluding(String ip, UUID excludeUuid) {
        String sql = "SELECT * FROM eb_punishments WHERE ip = ? AND type IN ('BAN','IP_BAN') AND active = 1 AND uuid <> ?";
        List<Punishment> result = new ArrayList<>();
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ip);
            statement.setString(2, excludeUuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load IP bans", e);
        }
        return result;
    }

    public int countByUuidAndIdKey(UUID uuid, String idKey) {
        String sql = "SELECT COUNT(*) FROM eb_punishments WHERE uuid = ? AND id_key = ?";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, idKey);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load count", e);
        }
        return 0;
    }

    public void deactivate(long id, String removedBy, String removedReason) {
        String sql = "UPDATE eb_punishments SET active = 0, removed_by = ?, removed_reason = ?, removed_at = ? WHERE id = ?";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, removedBy);
            statement.setString(2, removedReason);
            statement.setLong(3, System.currentTimeMillis());
            statement.setLong(4, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not lift punishment", e);
        }
    }

    public void expireOutdated() {
        String sql = "UPDATE eb_punishments SET active = 0 WHERE active = 1 AND expires_at >= 0 AND expires_at <= ?";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clean up expired punishments", e);
        }
    }

    public List<Punishment> history(UUID uuid) {
        String sql = "SELECT * FROM eb_punishments WHERE uuid = ? ORDER BY created_at DESC";
        List<Punishment> result = new ArrayList<>();
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load history", e);
        }
        return result;
    }

    public int deleteHistory(UUID uuid) {
        String sql = "DELETE FROM eb_punishments WHERE uuid = ?";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete history", e);
        }
    }

    private Punishment map(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(rs.getLong("id"));
        punishment.setUuid(UUID.fromString(rs.getString("uuid")));
        punishment.setPlayerName(rs.getString("player_name"));
        punishment.setIp(rs.getString("ip"));
        punishment.setType(PunishmentType.valueOf(rs.getString("type")));
        punishment.setReason(rs.getString("reason"));
        punishment.setIdKey(rs.getString("id_key"));
        punishment.setLevel(rs.getInt("level"));
        String operatorUuid = rs.getString("operator_uuid");
        punishment.setOperatorUuid(operatorUuid != null ? UUID.fromString(operatorUuid) : null);
        punishment.setOperatorName(rs.getString("operator_name"));
        punishment.setCreatedAt(rs.getLong("created_at"));
        punishment.setExpiresAt(rs.getLong("expires_at"));
        punishment.setActive(rs.getInt("active") == 1);
        punishment.setRemovedBy(rs.getString("removed_by"));
        punishment.setRemovedReason(rs.getString("removed_reason"));
        punishment.setRemovedAt(rs.getLong("removed_at"));
        return punishment;
    }
}
