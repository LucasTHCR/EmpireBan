package de.empireblocks.empireban.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HistoryCommand implements SimpleCommand {

    private final EmpireBanCore core;
    private final ProxyServer proxyServer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public HistoryCommand(EmpireBanCore core, ProxyServer proxyServer) {
        this.core = core;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        var sender = invocation.source();
        String[] args = invocation.arguments();
        if (!sender.hasPermission("bansys.history.show")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/history <player>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, proxyServer, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        String name = targetOpt.get().name();
        List<Punishment> history = core.getHistoryManager().history(targetOpt.get().uuid());

        if (history.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "history.empty", Map.of("player", name));
            return;
        }

        CommandUtil.sendMessage(core, sender, "history.header", Map.of("player", name));
        for (Punishment punishment : history) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get("history.entry", Map.of(
                    "date", dateFormat.format(new Date(punishment.getCreatedAt())),
                    "type", punishment.getType().name(),
                    "operator", punishment.getOperatorName() != null ? punishment.getOperatorName() : "Console",
                    "reason", punishment.getReason() != null ? punishment.getReason() : "-"
            ))));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
