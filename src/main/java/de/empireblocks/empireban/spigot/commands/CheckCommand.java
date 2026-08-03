package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Optional;

public class CheckCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public CheckCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bansys.check")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/check <Spieler>"));
            return true;
        }
        Optional<OfflinePlayer> targetOpt = CommandUtil.resolvePlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        var targetUuid = targetOpt.get().getUniqueId();
        String name = targetOpt.get().getName() != null ? targetOpt.get().getName() : args[0];

        CommandUtil.sendMessage(core, sender, "check.header", Map.of("player", name));

        Optional<Punishment> ban = core.getPunishmentManager().getActiveBan(targetUuid);
        Optional<Punishment> mute = core.getPunishmentManager().getActiveMute(targetUuid);

        boolean any = false;
        if (ban.isPresent()) {
            any = true;
            sender.sendMessage(core.getMessagesManager().get("check.banned", Map.of(
                    "reason", ban.get().getReason() != null ? ban.get().getReason() : "-",
                    "duration", TimeUtil.formatRemaining(ban.get().remainingMillis()))));
        }
        if (mute.isPresent()) {
            any = true;
            sender.sendMessage(core.getMessagesManager().get("check.muted", Map.of(
                    "reason", mute.get().getReason() != null ? mute.get().getReason() : "-",
                    "duration", TimeUtil.formatRemaining(mute.get().remainingMillis()))));
        }
        if (!any) {
            sender.sendMessage(core.getMessagesManager().get("check.clean"));
        }
        return true;
    }
}
