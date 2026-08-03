package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.BanIdManager;
import de.empireblocks.empireban.core.manager.PunishmentManager;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MuteCommand extends Command {

    private final EmpireBanCore core;

    public MuteCommand(EmpireBanCore core) {
        super("mute");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/mute <Spieler> <ID|Dauer> [Grund]"));
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
        UUID operatorUuid = CommandUtil.senderUuid(sender);

        if (core.getPunishmentManager().getActiveMute(targetUuid).isPresent()) {
            CommandUtil.sendMessage(core, sender, "mute.already-muted", Map.of("player", targetName));
            return;
        }

        BanIdManager banIdManager = core.getBanIdManager();
        Optional<BanId> banId = banIdManager.get(args[1]);

        if (banId.isPresent()) {
            BanId id = banId.get();
            if (!sender.hasPermission("bansys.ban.all") && !sender.hasPermission("bansys.ban." + id.getKey())) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return;
            }
            PunishmentManager.PunishResult result = core.getPunishmentManager()
                    .punishWithId(targetUuid, targetName, CommandUtil.currentIp(target), id.getKey(), operatorUuid, sender.getName());
            CommandUtil.sendMessage(core, sender, "mute.success", Map.of(
                    "player", targetName, "reason", id.getReason(),
                    "duration", TimeUtil.formatRemaining(result.punishment().remainingMillis())));
            return;
        }

        if (!sender.hasPermission("bansys.ban.all")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }

        long durationSeconds;
        try {
            durationSeconds = TimeUtil.parseToSeconds(args[1]);
        } catch (IllegalArgumentException e) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return;
        }
        String reason = args.length > 2 ? String.join(" ", List.of(args).subList(2, args.length)) : "Kein Grund angegeben";
        var punishment = core.getPunishmentManager().punishManual(targetUuid, targetName, CommandUtil.currentIp(target),
                PunishmentType.MUTE, reason, durationSeconds, operatorUuid, sender.getName());
        CommandUtil.sendMessage(core, sender, "mute.success", Map.of(
                "player", targetName, "reason", reason, "duration", TimeUtil.formatRemaining(punishment.remainingMillis())));
    }
}
