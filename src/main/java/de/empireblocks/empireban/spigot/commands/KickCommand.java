package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class KickCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public KickCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bansys.kick")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/kick <player> [reason]"));
            return true;
        }
        Player target = org.bukkit.Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        if (target.hasPermission("bansys.kick.bypass")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "No reason given";

        core.getPunishmentManager().kick(target.getUniqueId(), target.getName(), reason,
                CommandUtil.senderUuid(sender), sender.getName());

        List<String> lines = core.getMessagesManager().getList("kick.screen", Map.of("reason", reason));
        core.getPlatform().kick(target.getUniqueId(), String.join("\n", lines));

        CommandUtil.sendMessage(core, sender, "kick.success", Map.of("player", target.getName(), "reason", reason));
        return true;
    }
}
