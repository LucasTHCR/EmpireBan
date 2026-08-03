package de.empireblocks.empireban.core;

import de.empireblocks.empireban.core.config.ConfigManager;
import de.empireblocks.empireban.core.config.MessagesManager;
import de.empireblocks.empireban.core.db.DatabaseManager;
import de.empireblocks.empireban.core.db.IpRepository;
import de.empireblocks.empireban.core.db.LogRepository;
import de.empireblocks.empireban.core.db.PunishmentRepository;
import de.empireblocks.empireban.core.manager.BanIdManager;
import de.empireblocks.empireban.core.manager.HistoryManager;
import de.empireblocks.empireban.core.manager.IpManager;
import de.empireblocks.empireban.core.manager.LogManager;
import de.empireblocks.empireban.core.manager.PunishmentManager;
import de.empireblocks.empireban.core.platform.PlatformAdapter;

import java.io.IOException;

/**
 * Wires config, database and all managers together. One instance per platform plugin
 * (Spigot/Bungee/Velocity main class owns exactly one of these).
 */
public class EmpireBanCore {

    private final PlatformAdapter platform;
    private final Class<?> resourceHolder;
    private final ConfigManager configManager;
    private MessagesManager messagesManager;
    private final BanIdManager banIdManager;
    private final DatabaseManager databaseManager;
    private final PunishmentManager punishmentManager;
    private final HistoryManager historyManager;
    private final LogManager logManager;
    private final IpManager ipManager;

    public EmpireBanCore(PlatformAdapter platform, Class<?> resourceHolder) throws IOException {
        this.platform = platform;
        this.resourceHolder = resourceHolder;
        this.configManager = new ConfigManager(platform.getDataFolder(), resourceHolder);
        this.messagesManager = new MessagesManager(platform.getDataFolder(), configManager.defaultLanguage(), resourceHolder);
        this.banIdManager = new BanIdManager(platform.getDataFolder(), resourceHolder);
        this.databaseManager = new DatabaseManager(configManager, platform.getDataFolder());

        PunishmentRepository punishmentRepository = new PunishmentRepository(databaseManager);
        LogRepository logRepository = new LogRepository(databaseManager);
        IpRepository ipRepository = new IpRepository(databaseManager);

        this.punishmentManager = new PunishmentManager(punishmentRepository, logRepository, banIdManager);
        this.historyManager = new HistoryManager(punishmentRepository);
        this.logManager = new LogManager(logRepository);
        this.ipManager = new IpManager(configManager, ipRepository, punishmentRepository);
    }

    public void reload() throws IOException {
        configManager.reload();
        messagesManager = new MessagesManager(platform.getDataFolder(), configManager.defaultLanguage(), resourceHolder);
        banIdManager.reload();
    }

    public void shutdown() {
        databaseManager.close();
    }

    public PlatformAdapter getPlatform() {
        return platform;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public BanIdManager getBanIdManager() {
        return banIdManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public IpManager getIpManager() {
        return ipManager;
    }
}
