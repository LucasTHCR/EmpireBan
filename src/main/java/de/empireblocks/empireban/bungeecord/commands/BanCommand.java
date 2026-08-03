package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.BanIdManager;
import de.empireblocks.empireban.core.manager.PunishmentManager;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BanCommand extends Command {

    private final EmpireBanCore core;

    public BanCommand(EmpireBanCore core) {
        super("ban");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/ban <Spieler> <ID|Dauer> [Grund]"));
            return;
        }

        Optional<CommandUtil.TargetPlayer> targetOpt = CommandUtil.resolvePlayer(core, args[0]);
        if (targetOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        CommandUtil.TargetPlayer target = targetOpt.get();
        UUID targetUuid = target.uuid();
        String targetName = target.name();
        String ip = CommandUtil.currentIp(target);
        UUID operatorUuid = CommandUtil.senderUuid(sender);
        String operatorName = sender.getName();

        if (core.getPunishmentManager().getActiveBan(targetUuid).isPresent()) {
            CommandUtil.sendMessage(core, sender, "ban.already-banned", Map.of("player", targetName));
            return;
        }

        String secondArg = args[1];
        BanIdManager banIdManager = core.getBanIdManager();
        Optional<BanId> banId = banIdManager.get(secondArg);

        if (banId.isPresent()) {
            BanId id = banId.get();
            if (!sender.hasPermission("bansys.ban.all") && !sender.hasPermission("bansys.ban." + id.getKey())) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return;
            }
            if (id.isOnlyAdmins() && !sender.hasPermission("bansys.ban.admin") && !sender.hasPermission("bansys.ban.all")) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return;
            }

            PunishmentManager.PunishResult result = core.getPunishmentManager()
                    .punishWithId(targetUuid, targetName, ip, id.getKey(), operatorUuid, operatorName);

            CommandUtil.sendMessage(core, sender, "ban.success", Map.of(
                    "player", targetName, "reason", id.getReason(),
                    "duration", TimeUtil.formatRemaining(result.punishment().remainingMillis())));
            kickIfOnline(target, result.punishment().getReason(), result.punishment().remainingMillis());
            return;
        }

        if (!sender.hasPermission("bansys.ban.all")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }

        long durationSeconds;
        try {
            durationSeconds = TimeUtil.parseToSeconds(secondArg);
        } catch (IllegalArgumentException e) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return;
        }
        String reason = args.length > 2 ? String.join(" ", List.of(args).subList(2, args.length)) : "Kein Grund angegeben";

        var punishment = core.getPunishmentManager().punishManual(targetUuid, targetName, ip,
                PunishmentType.BAN, reason, durationSeconds, operatorUuid, operatorName);

        CommandUtil.sendMessage(core, sender, "ban.success", Map.of(
                "player", targetName, "reason", reason, "duration", TimeUtil.formatRemaining(punishment.remainingMillis())));
        kickIfOnline(target, reason, punishment.remainingMillis());
    }

    private void kickIfOnline(CommandUtil.TargetPlayer target, String reason, long remainingMillis) {
        if (target.online() == null || target.online().hasPermission("bansys.ban.bypass")) {
            return;
        }
        String duration = TimeUtil.formatRemaining(remainingMillis);
        List<String> lines = core.getMessagesManager().getList("ban.screen", Map.of("reason", reason, "duration", duration));
        target.online().disconnect(TextComponent.fromLegacyText(String.join("\n", lines)));
    }
}
