package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HistoryCommand implements CommandExecutor {

    private final EmpireBanCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public HistoryCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bansys.history.show")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/history <player>"));
            return true;
        }
        Optional<OfflinePlayer> targetOpt = CommandUtil.resolvePlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        String name = targetOpt.get().getName() != null ? targetOpt.get().getName() : args[0];
        List<Punishment> history = core.getHistoryManager().history(targetOpt.get().getUniqueId());

        if (history.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "history.empty", Map.of("player", name));
            return true;
        }

        CommandUtil.sendMessage(core, sender, "history.header", Map.of("player", name));
        for (Punishment punishment : history) {
            sender.sendMessage(core.getMessagesManager().get("history.entry", Map.of(
                    "date", dateFormat.format(new Date(punishment.getCreatedAt())),
                    "type", punishment.getType().name(),
                    "operator", punishment.getOperatorName() != null ? punishment.getOperatorName() : "Console",
                    "reason", punishment.getReason() != null ? punishment.getReason() : "-"
            )));
        }
        return true;
    }
}
