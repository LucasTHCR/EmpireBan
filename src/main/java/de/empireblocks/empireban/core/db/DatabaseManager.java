package de.empireblocks.empireban.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.empireblocks.empireban.core.config.ConfigManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager implements AutoCloseable {

    public enum Dialect {MYSQL, SQLITE}

    private final HikariDataSource dataSource;
    private final Dialect dialect;

    public DatabaseManager(ConfigManager config, Path dataFolder) {
        HikariConfig hikariConfig = new HikariConfig();
        if ("mysql".equals(config.databaseType())) {
            this.dialect = Dialect.MYSQL;
            String jdbcUrl = "jdbc:mysql://" + config.mysqlHost() + ":" + config.mysqlPort() + "/"
                    + config.mysqlDatabase() + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.mysqlUsername());
            hikariConfig.setPassword(config.mysqlPassword());
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setMaximumPoolSize(10);
        } else {
            this.dialect = Dialect.SQLITE;
            Path dbFile = dataFolder.resolve("empireban.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setMaximumPoolSize(1);
        }
        hikariConfig.setPoolName("EmpireBan-Pool");
        this.dataSource = new HikariDataSource(hikariConfig);
        migrate();
    }

    public Dialect getDialect() {
        return dialect;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void migrate() {
        boolean mysql = dialect == Dialect.MYSQL;
        String idType = mysql ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String boolType = mysql ? "TINYINT(1)" : "INTEGER";

        String punishments = "CREATE TABLE IF NOT EXISTS eb_punishments (" +
                "id " + idType + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "ip VARCHAR(45), " +
                "type VARCHAR(16) NOT NULL, " +
                "reason VARCHAR(255), " +
                "id_key VARCHAR(64), " +
                "level INTEGER DEFAULT 1, " +
                "operator_uuid VARCHAR(36), " +
                "operator_name VARCHAR(16), " +
                "created_at BIGINT NOT NULL, " +
                "expires_at BIGINT NOT NULL, " +
                "active " + boolType + " NOT NULL DEFAULT 1, " +
                "removed_by VARCHAR(16), " +
                "removed_reason VARCHAR(255), " +
                "removed_at BIGINT" +
                ")";

        String logs = "CREATE TABLE IF NOT EXISTS eb_logs (" +
                "id " + idType + ", " +
                "actor_uuid VARCHAR(36), " +
                "actor_name VARCHAR(16), " +
                "action VARCHAR(32) NOT NULL, " +
                "target_name VARCHAR(16), " +
                "details VARCHAR(500), " +
                "created_at BIGINT NOT NULL" +
                ")";

        String ips = "CREATE TABLE IF NOT EXISTS eb_ips (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "ip VARCHAR(45) NOT NULL, " +
                "player_name VARCHAR(16), " +
                "last_seen BIGINT NOT NULL, " +
                "PRIMARY KEY (uuid, ip)" +
                ")";

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(punishments);
            statement.execute(logs);
            statement.execute(ips);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_eb_punishments_uuid ON eb_punishments(uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_eb_punishments_ip ON eb_punishments(ip)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_eb_ips_ip ON eb_ips(ip)");
        } catch (SQLException e) {
            throw new IllegalStateException("Konnte Datenbank-Schema nicht anlegen", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
