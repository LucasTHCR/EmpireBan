package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CheckCommand extends Command {

    private final EmpireBanCore core;

    public CheckCommand(EmpireBanCore core) {
        super("check");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bansys.check")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/check <Spieler>"));
            return;
        }
        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        UUID targetUuid = targetOpt.get().uuid();
        String name = targetOpt.get().name();

        CommandUtil.sendMessage(core, sender, "check.header", Map.of("player", name));

        Optional<Punishment> ban = core.getPunishmentManager().getActiveBan(targetUuid);
        Optional<Punishment> mute = core.getPunishmentManager().getActiveMute(targetUuid);

        boolean any = false;
        if (ban.isPresent()) {
            any = true;
            sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("check.banned", Map.of(
                    "reason", ban.get().getReason() != null ? ban.get().getReason() : "-",
                    "duration", TimeUtil.formatRemaining(ban.get().remainingMillis())))));
        }
        if (mute.isPresent()) {
            any = true;
            sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("check.muted", Map.of(
                    "reason", mute.get().getReason() != null ? mute.get().getReason() : "-",
                    "duration", TimeUtil.formatRemaining(mute.get().remainingMillis())))));
        }
        if (!any) {
            sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("check.clean")));
        }
    }
}
