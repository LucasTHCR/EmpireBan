package de.empireblocks.empireban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CommandUtil {

    record TargetPlayer(UUID uuid, String name, Player online) {
    }

    private CommandUtil() {
    }

    static void sendMessage(EmpireBanCore core, CommandSource sender, String path) {
        sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get(path)));
    }

    static void sendMessage(EmpireBanCore core, CommandSource sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get(path, placeholders)));
    }

    static Optional<TargetPlayer> resolvePlayer(EmpireBanCore core, ProxyServer proxyServer, String name) {
        Optional<Player> online = proxyServer.getPlayer(name);
        if (online.isPresent()) {
            Player player = online.get();
            return Optional.of(new TargetPlayer(player.getUniqueId(), player.getUsername(), player));
        }
        Optional<UUID> known = core.getIpManager().findKnownUuidByName(name);
        return known.map(uuid -> new TargetPlayer(uuid, name, null));
    }

    static String currentIp(TargetPlayer target) {
        if (target.online() != null && target.online().getRemoteAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return null;
    }

    static UUID senderUuid(CommandSource sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    static String senderName(CommandSource sender) {
        return sender instanceof Player player ? player.getUsername() : "Konsole";
    }
}
