package de.empireblocks.empireban.spigot;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.spigot.commands.BanCommand;
import de.empireblocks.empireban.spigot.commands.BanSystemCommand;
import de.empireblocks.empireban.spigot.commands.CheckCommand;
import de.empireblocks.empireban.spigot.commands.DeleteHistoryCommand;
import de.empireblocks.empireban.spigot.commands.HistoryCommand;
import de.empireblocks.empireban.spigot.commands.KickCommand;
import de.empireblocks.empireban.spigot.commands.MuteCommand;
import de.empireblocks.empireban.spigot.commands.UnbanCommand;
import de.empireblocks.empireban.spigot.commands.UnmuteCommand;
import de.empireblocks.empireban.spigot.listeners.ChatListener;
import de.empireblocks.empireban.spigot.listeners.PlayerConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class EmpireBanSpigot extends JavaPlugin {

    private EmpireBanCore core;

    @Override
    public void onEnable() {
        try {
            this.core = new EmpireBanCore(new SpigotPlatformAdapter(this), EmpireBanSpigot.class);
        } catch (IOException e) {
            getLogger().severe("Konnte EmpireBan nicht initialisieren: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(core), this);
        getServer().getPluginManager().registerEvents(new ChatListener(core), this);

        getCommand("ban").setExecutor(new BanCommand(core));
        getCommand("unban").setExecutor(new UnbanCommand(core));
        getCommand("mute").setExecutor(new MuteCommand(core));
        getCommand("unmute").setExecutor(new UnmuteCommand(core));
        getCommand("kick").setExecutor(new KickCommand(core));
        getCommand("check").setExecutor(new CheckCommand(core));
        getCommand("history").setExecutor(new HistoryCommand(core));
        getCommand("deletehistory").setExecutor(new DeleteHistoryCommand(core));
        getCommand("bansystem").setExecutor(new BanSystemCommand(core));

        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> core.getPunishmentManager().purgeExpired(), 20L * 60, 20L * 60);

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
