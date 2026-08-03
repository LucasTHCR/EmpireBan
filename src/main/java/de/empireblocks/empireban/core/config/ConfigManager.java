package de.empireblocks.empireban.core.config;

import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {

    private final YamlDocument document;

    public ConfigManager(Path dataFolder, Class<?> resourceHolder) throws IOException {
        this.document = YamlDocument.load(dataFolder, "config.yml", resourceHolder);
    }

    public void reload() throws IOException {
        document.reload();
    }

    public String getString(String path, String def) {
        return document.getString(path, def);
    }

    public int getInt(String path, int def) {
        return document.getInt(path, def);
    }

    public long getLong(String path, long def) {
        return document.getLong(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return document.getBoolean(path, def);
    }

    public java.util.List<String> getStringList(String path) {
        return document.getStringList(path);
    }

    // --- convenience accessors for well-known settings ---

    public String databaseType() {
        return getString("database.type", "sqlite").toLowerCase();
    }

    public String mysqlHost() {
        return getString("database.mysql.host", "localhost");
    }

    public int mysqlPort() {
        return getInt("database.mysql.port", 3306);
    }

    public String mysqlDatabase() {
        return getString("database.mysql.database", "empireban");
    }

    public String mysqlUsername() {
        return getString("database.mysql.username", "root");
    }

    public String mysqlPassword() {
        return getString("database.mysql.password", "");
    }

    public boolean ipAutoban() {
        return getBoolean("ip-handling.autoban", false);
    }

    public boolean ipNotifyStaff() {
        return getBoolean("ip-handling.notify-staff", true);
    }

    public boolean vpnCheckEnabled() {
        return getBoolean("vpn-check.enabled", false);
    }

    public String vpnApiKey() {
        return getString("vpn-check.api-key", "");
    }

    public boolean chatFilterEnabled() {
        return getBoolean("chat-filter.enabled", true);
    }

    public java.util.List<String> chatFilterBlacklist() {
        return getStringList("chat-filter.blacklist");
    }

    public long chatDelaySeconds() {
        return getLong("chat-delay.seconds", 3);
    }

    public String defaultLanguage() {
        return getString("language", "german");
    }

    /** BungeeCord-only: whether signed (1.19.1+) chat messages should still be caught via the companion Spigot chat adapter. */
    public boolean signedChatBypass() {
        return getBoolean("signed-chat-bypass", true);
    }
}
