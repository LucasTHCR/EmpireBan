package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnbanCommand extends Command {

    private final EmpireBanCore core;

    public UnbanCommand(EmpireBanCore core) {
        super("unban");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bansys.unban")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/unban <Spieler>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "Kein Grund angegeben";
        boolean unbanned = core.getPunishmentManager().unban(targetOpt.get().uuid(), sender.getName(), reason);
        if (unbanned) {
            CommandUtil.sendMessage(core, sender, "ban.unban-success", Map.of("player", args[0]));
        } else {
            CommandUtil.sendMessage(core, sender, "ban.not-banned", Map.of("player", args[0]));
        }
    }
}
