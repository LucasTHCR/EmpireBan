package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnbanCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public UnbanCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bansys.unban")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/unban <player>"));
            return true;
        }
        Optional<OfflinePlayer> targetOpt = CommandUtil.resolvePlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "No reason given";
        boolean unbanned = core.getPunishmentManager().unban(targetOpt.get().getUniqueId(), sender.getName(), reason);
        if (unbanned) {
            CommandUtil.sendMessage(core, sender, "ban.unban-success", Map.of("player", args[0]));
        } else {
            CommandUtil.sendMessage(core, sender, "ban.not-banned", Map.of("player", args[0]));
        }
        return true;
    }
}
