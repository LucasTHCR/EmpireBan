package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;

public class DeleteHistoryCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public DeleteHistoryCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bansys.history.delete")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/deletehistory <Spieler>"));
            return true;
        }
        Optional<OfflinePlayer> targetOpt = CommandUtil.resolvePlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        String name = targetOpt.get().getName() != null ? targetOpt.get().getName() : args[0];
        int amount = core.getHistoryManager().deleteHistory(targetOpt.get().getUniqueId());
        CommandUtil.sendMessage(core, sender, "history.deleted", Map.of("player", name, "amount", String.valueOf(amount)));
        return true;
    }
}
