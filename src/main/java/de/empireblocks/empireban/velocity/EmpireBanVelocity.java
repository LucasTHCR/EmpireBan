package de.empireblocks.empireban.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.velocity.commands.BanCommand;
import de.empireblocks.empireban.velocity.commands.BanSystemCommand;
import de.empireblocks.empireban.velocity.commands.CheckCommand;
import de.empireblocks.empireban.velocity.commands.DeleteHistoryCommand;
import de.empireblocks.empireban.velocity.commands.HistoryCommand;
import de.empireblocks.empireban.velocity.commands.KickCommand;
import de.empireblocks.empireban.velocity.commands.MuteCommand;
import de.empireblocks.empireban.velocity.commands.UnbanCommand;
import de.empireblocks.empireban.velocity.commands.UnmuteCommand;
import de.empireblocks.empireban.velocity.listeners.ChatListener;
import de.empireblocks.empireban.velocity.listeners.PlayerConnectionListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "empireban",
        name = "EmpireBan",
        version = "1.0.0",
        description = "Konfigurierbares Ban/Mute/Kick-System mit ID-Level-System, IP-Handling und VPN-Check (Proxy).",
        authors = {"empireblocks"}
)
public class EmpireBanVelocity {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private EmpireBanCore core;

    @Inject
    public EmpireBanVelocity(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            this.core = new EmpireBanCore(
                    new VelocityPlatformAdapter(this, proxyServer, dataDirectory, logger),
                    EmpireBanVelocity.class);
        } catch (IOException e) {
            logger.error("Konnte EmpireBan nicht initialisieren", e);
            return;
        }

        proxyServer.getEventManager().register(this, new PlayerConnectionListener(core));
        proxyServer.getEventManager().register(this, new ChatListener(core));

        var commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("ban").build(), new BanCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("unban").build(), new UnbanCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("mute").build(), new MuteCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("unmute").build(), new UnmuteCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("kick").build(), new KickCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("check").build(), new CheckCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("history").build(), new HistoryCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("deletehistory").build(), new DeleteHistoryCommand(core, proxyServer));
        commandManager.register(commandManager.metaBuilder("bansystem").aliases("bansys").build(), new BanSystemCommand(core));

        proxyServer.getScheduler().buildTask(this, () -> core.getPunishmentManager().purgeExpired())
                .repeat(1, TimeUnit.MINUTES)
                .schedule();

        logger.info("EmpireBan wurde aktiviert.");
    }

    public EmpireBanCore getCore() {
        return core;
    }
}
