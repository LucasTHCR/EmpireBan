package de.empireblocks.empireban.bungeecord.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CommandUtil {

    record TargetPlayer(UUID uuid, String name, ProxiedPlayer online) {
    }

    private CommandUtil() {
    }

    static void sendMessage(EmpireBanCore core, CommandSender sender, String path) {
        sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get(path)));
    }

    static void sendMessage(EmpireBanCore core, CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get(path, placeholders)));
    }

    static Optional<TargetPlayer> resolvePlayer(EmpireBanCore core, String name) {
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(name);
        if (online != null) {
            return Optional.of(new TargetPlayer(online.getUniqueId(), online.getName(), online));
        }
        Optional<UUID> known = core.getIpManager().findKnownUuidByName(name);
        return known.map(uuid -> new TargetPlayer(uuid, name, null));
    }

    static String currentIp(TargetPlayer target) {
        if (target.online() != null && target.online().getSocketAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return null;
    }

    static UUID senderUuid(CommandSender sender) {
        return sender instanceof ProxiedPlayer player ? player.getUniqueId() : null;
    }
}
