package de.empireblocks.empireban.spigot.commands;

import de.empireblocks.empireban.core.EmpireBanCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class CommandUtil {

    private CommandUtil() {
    }

    static void sendMessage(EmpireBanCore core, CommandSender sender, String path) {
        sender.sendMessage(core.getMessagesManager().get(path));
    }

    static void sendMessage(EmpireBanCore core, CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(core.getMessagesManager().get(path, placeholders));
    }

    /** Resolves a player by name, online first, falling back to an offline player that has played before. */
    static Optional<OfflinePlayer> resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return Optional.of(online);
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return Optional.of(offline);
        }
        return Optional.empty();
    }

    static String currentIp(OfflinePlayer player) {
        if (player.isOnline() && player.getPlayer() != null && player.getPlayer().getAddress() != null) {
            return player.getPlayer().getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    static UUID senderUuid(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }
}
