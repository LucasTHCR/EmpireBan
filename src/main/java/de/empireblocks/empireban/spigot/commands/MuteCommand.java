package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.BanIdManager;
import de.empireblocks.empireban.core.manager.PunishmentManager;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MuteCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public MuteCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/mute <player> <ID|duration> [reason]"));
            return true;
        }

        Optional<OfflinePlayer> targetOpt = CommandUtil.resolvePlayer(args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return true;
        }
        OfflinePlayer target = targetOpt.get();
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : args[0];
        UUID operatorUuid = CommandUtil.senderUuid(sender);

        if (core.getPunishmentManager().getActiveMute(targetUuid).isPresent()) {
            CommandUtil.sendMessage(core, sender, "mute.already-muted", Map.of("player", targetName));
            return true;
        }

        BanIdManager banIdManager = core.getBanIdManager();
        Optional<BanId> banId = banIdManager.get(args[1]);

        if (banId.isPresent()) {
            BanId id = banId.get();
            if (!sender.hasPermission("bansys.ban.all") && !sender.hasPermission("bansys.ban." + id.getKey())) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return true;
            }
            PunishmentManager.PunishResult result = core.getPunishmentManager()
                    .punishWithId(targetUuid, targetName, CommandUtil.currentIp(target), id.getKey(), operatorUuid, sender.getName());
            CommandUtil.sendMessage(core, sender, "mute.success", Map.of(
                    "player", targetName, "reason", id.getReason(),
                    "duration", TimeUtil.formatRemaining(result.punishment().remainingMillis())));
            return true;
        }

        if (!sender.hasPermission("bansys.ban.all")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }

        long durationSeconds;
        try {
            durationSeconds = TimeUtil.parseToSeconds(args[1]);
        } catch (IllegalArgumentException e) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", List.of(args).subList(2, args.length)) : "No reason given";
        var punishment = core.getPunishmentManager().punishManual(targetUuid, targetName, CommandUtil.currentIp(target),
                PunishmentType.MUTE, reason, durationSeconds, operatorUuid, sender.getName());
        CommandUtil.sendMessage(core, sender, "mute.success", Map.of(
                "player", targetName, "reason", reason, "duration", TimeUtil.formatRemaining(punishment.remainingMillis())));
        return true;
    }
}
