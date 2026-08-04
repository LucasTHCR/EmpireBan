package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;
import java.util.Optional;

public class DeleteHistoryCommand extends Command {

    private final EmpireBanCore core;

    public DeleteHistoryCommand(EmpireBanCore core) {
        super("deletehistory");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bansys.history.delete")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/deletehistory <player>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        String name = targetOpt.get().name();
        int amount = core.getHistoryManager().deleteHistory(targetOpt.get().uuid());
        CommandUtil.sendMessage(core, sender, "history.deleted", Map.of("player", name, "amount", String.valueOf(amount)));
    }
}
