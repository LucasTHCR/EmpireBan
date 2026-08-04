package de.empireblocks.empireban.bungeecord;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.bungeecord.commands.BanCommand;
import de.empireblocks.empireban.bungeecord.commands.BanSystemCommand;
import de.empireblocks.empireban.bungeecord.commands.CheckCommand;
import de.empireblocks.empireban.bungeecord.commands.DeleteHistoryCommand;
import de.empireblocks.empireban.bungeecord.commands.HistoryCommand;
import de.empireblocks.empireban.bungeecord.commands.KickCommand;
import de.empireblocks.empireban.bungeecord.commands.MuteCommand;
import de.empireblocks.empireban.bungeecord.commands.UnbanCommand;
import de.empireblocks.empireban.bungeecord.commands.UnmuteCommand;
import de.empireblocks.empireban.bungeecord.listeners.ChatListener;
import de.empireblocks.empireban.bungeecord.listeners.PlayerConnectionListener;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EmpireBanBungeeCord extends Plugin {

    private EmpireBanCore core;

    @Override
    public void onEnable() {
        try {
            this.core = new EmpireBanCore(new BungeeCordPlatformAdapter(this), EmpireBanBungeeCord.class);
        } catch (IOException e) {
            getLogger().severe("Could not initialize EmpireBan: " + e.getMessage());
            return;
        }

        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener(core, this));
        getProxy().getPluginManager().registerListener(this, new ChatListener(core));

        getProxy().getPluginManager().registerCommand(this, new BanCommand(core));
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand(core));
        getProxy().getPluginManager().registerCommand(this, new MuteCommand(core));
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand(core));
        getProxy().getPluginManager().registerCommand(this, new KickCommand(core));
        getProxy().getPluginManager().registerCommand(this, new CheckCommand(core));
        getProxy().getPluginManager().registerCommand(this, new HistoryCommand(core));
        getProxy().getPluginManager().registerCommand(this, new DeleteHistoryCommand(core));
        getProxy().getPluginManager().registerCommand(this, new BanSystemCommand(core));

        getProxy().getScheduler().schedule(this, () -> core.getPunishmentManager().purgeExpired(), 1, 1, TimeUnit.MINUTES);

        getLogger().info("EmpireBan wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
    }

    public EmpireBanCore getCore() {
        return core;
    }
}
