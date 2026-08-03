package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HistoryCommand extends Command {

    private final EmpireBanCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public HistoryCommand(EmpireBanCore core) {
        super("history");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bansys.history.show")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/history <Spieler>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, args[0]);
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
            sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("history.entry", Map.of(
                    "date", dateFormat.format(new Date(punishment.getCreatedAt())),
                    "type", punishment.getType().name(),
                    "operator", punishment.getOperatorName() != null ? punishment.getOperatorName() : "Konsole",
                    "reason", punishment.getReason() != null ? punishment.getReason() : "-"
            ))));
        }
    }
}
