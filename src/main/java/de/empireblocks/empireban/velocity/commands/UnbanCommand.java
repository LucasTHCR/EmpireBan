package de.empireblocks.empireban.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.EmpireBanCore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnbanCommand implements SimpleCommand {

    private final EmpireBanCore core;
    private final ProxyServer proxyServer;

    public UnbanCommand(EmpireBanCore core, ProxyServer proxyServer) {
        this.core = core;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        var sender = invocation.source();
        String[] args = invocation.arguments();
        if (!sender.hasPermission("bansys.unban")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/unban <Spieler>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, proxyServer, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "Kein Grund angegeben";
        boolean unbanned = core.getPunishmentManager().unban(targetOpt.get().uuid(), CommandUtil.senderName(sender), reason);
        if (unbanned) {
            CommandUtil.sendMessage(core, sender, "ban.unban-success", Map.of("player", args[0]));
        } else {
            CommandUtil.sendMessage(core, sender, "ban.not-banned", Map.of("player", args[0]));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
