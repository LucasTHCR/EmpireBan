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

public class BanCommand implements CommandExecutor {

    private final EmpireBanCore core;

    public BanCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/ban <player> <ID|duration> [reason]"));
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
        String ip = CommandUtil.currentIp(target);
        UUID operatorUuid = CommandUtil.senderUuid(sender);
        String operatorName = sender.getName();

        if (core.getPunishmentManager().getActiveBan(targetUuid).isPresent()) {
            CommandUtil.sendMessage(core, sender, "ban.already-banned", Map.of("player", targetName));
            return true;
        }

        String secondArg = args[1];
        BanIdManager banIdManager = core.getBanIdManager();
        Optional<BanId> banId = banIdManager.get(secondArg);

        if (banId.isPresent()) {
            BanId id = banId.get();
            if (!sender.hasPermission("bansys.ban.all")
                    && !sender.hasPermission("bansys.ban." + id.getKey())) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return true;
            }
            if (id.isOnlyAdmins() && !sender.hasPermission("bansys.ban.admin") && !sender.hasPermission("bansys.ban.all")) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return true;
            }

            PunishmentManager.PunishResult result = core.getPunishmentManager()
                    .punishWithId(targetUuid, targetName, ip, id.getKey(), operatorUuid, operatorName);

            if (result.punishment().getType() != PunishmentType.BAN && result.punishment().getType() != PunishmentType.IP_BAN) {
                // The escalation level resolved to a non-ban type (e.g. mute/warn) - still applied above, just inform the operator.
                CommandUtil.sendMessage(core, sender, "ban.success", Map.of(
                        "player", targetName,
                        "reason", id.getReason() + " (applied as " + result.punishment().getType() + ")",
                        "duration", TimeUtil.formatRemaining(result.punishment().remainingMillis())));
            } else {
                CommandUtil.sendMessage(core, sender, "ban.success", Map.of(
                        "player", targetName,
                        "reason", id.getReason(),
                        "duration", TimeUtil.formatRemaining(result.punishment().remainingMillis())));
            }
            kickIfOnline(target, result.punishment().getReason(), result.punishment().remainingMillis());
            return true;
        }

        if (!sender.hasPermission("bansys.ban.all")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return true;
        }

        long durationSeconds;
        try {
            durationSeconds = TimeUtil.parseToSeconds(secondArg);
        } catch (IllegalArgumentException e) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return true;
        }
        String reason = args.length > 2 ? String.join(" ", List.of(args).subList(2, args.length)) : "No reason given";

        var punishment = core.getPunishmentManager().punishManual(targetUuid, targetName, ip,
                PunishmentType.BAN, reason, durationSeconds, operatorUuid, operatorName);

        CommandUtil.sendMessage(core, sender, "ban.success", Map.of(
                "player", targetName, "reason", reason, "duration", TimeUtil.formatRemaining(punishment.remainingMillis())));
        kickIfOnline(target, reason, punishment.remainingMillis());
        return true;
    }

    private void kickIfOnline(OfflinePlayer target, String reason, long remainingMillis) {
        if (!target.isOnline() || target.getPlayer() == null) {
            return;
        }
        if (target.getPlayer().hasPermission("bansys.ban.bypass")) {
            return;
        }
        String duration = TimeUtil.formatRemaining(remainingMillis);
        List<String> lines = core.getMessagesManager().getList("ban.screen", Map.of("reason", reason, "duration", duration));
        core.getPlatform().kick(target.getUniqueId(), String.join("\n", lines));
    }
}
