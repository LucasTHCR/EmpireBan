package de.empireblocks.empireban.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KickCommand implements SimpleCommand {

    private final EmpireBanCore core;
    private final ProxyServer proxyServer;

    public KickCommand(EmpireBanCore core, ProxyServer proxyServer) {
        this.core = core;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        var sender = invocation.source();
        String[] args = invocation.arguments();
        if (!sender.hasPermission("bansys.kick")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/kick <Spieler> [Grund]"));
            return;
        }
        Optional<Player> targetOpt = proxyServer.getPlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        Player target = targetOpt.get();
        if (target.hasPermission("bansys.kick.bypass")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "Kein Grund angegeben";

        core.getPunishmentManager().kick(target.getUniqueId(), target.getUsername(), reason,
                CommandUtil.senderUuid(sender), CommandUtil.senderName(sender));

        List<String> lines = core.getMessagesManager().getList("kick.screen", Map.of("reason", reason));
        target.disconnect(VelocityPlatformAdapter.toComponent(String.join("\n", lines)));

        CommandUtil.sendMessage(core, sender, "kick.success", Map.of("player", target.getUsername(), "reason", reason));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
