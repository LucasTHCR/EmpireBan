package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;

public class KickCommand extends Command {

    private final EmpireBanCore core;

    public KickCommand(EmpireBanCore core) {
        super("kick");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bansys.kick")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/kick <player> [reason]"));
            return;
        }
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null) {
            CommandUtil.sendMessage(core, sender, "general.player-not-found");
            return;
        }
        if (target.hasPermission("bansys.kick.bypass")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", List.of(args).subList(1, args.length)) : "No reason given";

        core.getPunishmentManager().kick(target.getUniqueId(), target.getName(), reason,
                CommandUtil.senderUuid(sender), sender.getName());

        List<String> lines = core.getMessagesManager().getList("kick.screen", Map.of("reason", reason));
        target.disconnect(TextComponent.fromLegacyText(String.join("\n", lines)));

        CommandUtil.sendMessage(core, sender, "kick.success", Map.of("player", target.getName(), "reason", reason));
    }
}
