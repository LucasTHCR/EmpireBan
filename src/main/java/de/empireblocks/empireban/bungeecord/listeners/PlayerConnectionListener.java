package de.empireblocks.empireban.bungeecord.listeners;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.IpManager;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerConnectionListener implements Listener {

    private final EmpireBanCore core;
    private final net.md_5.bungee.api.plugin.Plugin plugin;

    public PlayerConnectionListener(EmpireBanCore core, net.md_5.bungee.api.plugin.Plugin plugin) {
        this.core = core;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();
        UUID uuid = connection.getUniqueId();
        String ip = connection.getSocketAddress() instanceof InetSocketAddress address
                ? address.getAddress().getHostAddress() : "0.0.0.0";
        String name = connection.getName();

        event.registerIntent(plugin);
        CompletableFuture.runAsync(() -> {
            try {
                Optional<Punishment> ban = core.getPunishmentManager().getActiveBan(uuid);
                if (ban.isEmpty()) {
                    ban = core.getPunishmentManager().getActiveIpBan(ip);
                }
                if (ban.isPresent() && !ban.get().isExpired()) {
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacyText(buildBanScreen(ban.get())));
                    return;
                }

                if (core.getIpManager().autobanEnabled() || core.getIpManager().notifyStaffEnabled()) {
                    List<Punishment> altBans = core.getIpManager().findAltAccounts(ip, uuid);
                    if (!altBans.isEmpty()) {
                        Punishment altBan = altBans.get(0);
                        if (core.getIpManager().autobanEnabled()) {
                            core.getPunishmentManager().punishManual(uuid, name, ip, PunishmentType.IP_BAN,
                                    "Alt account of banned player " + altBan.getPlayerName(), -1, null, "EmpireBan");
                            event.setCancelled(true);
                            event.setCancelReason(TextComponent.fromLegacyText(
                                    buildBanScreen(core.getPunishmentManager().getActiveBan(uuid).orElse(altBan))));
                            core.getPlatform().broadcastToPermission("bansys.notify", core.getMessagesManager()
                                    .get("ip.autobanned", Map.of("player", name, "banned_player", altBan.getPlayerName())));
                        } else {
                            core.getPlatform().broadcastToPermission("bansys.notify", core.getMessagesManager()
                                    .get("ip.alt-detected", Map.of("player", name, "banned_player", altBan.getPlayerName())));
                        }
                    }
                }
            } finally {
                event.completeIntent(plugin);
            }
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        String ip = core.getPlatform().getIp(uuid).orElse("0.0.0.0");
        core.getPlatform().runAsync(() -> {
            core.getIpManager().recordJoin(uuid, ip, name);
            if (core.getIpManager().vpnCheckEnabled()) {
                IpManager.VpnResult result = core.getIpManager().checkVpn(ip);
                if (result.isSuspicious()) {
                    core.getPlatform().broadcastToPermission("bansys.notify",
                            core.getMessagesManager().get("ip.vpn-detected", Map.of("player", name)));
                }
            }
        });
    }

    private String buildBanScreen(Punishment punishment) {
        String duration = TimeUtil.formatRemaining(punishment.remainingMillis());
        Map<String, String> placeholders = Map.of("reason", punishment.getReason() != null ? punishment.getReason() : "-", "duration", duration);
        List<String> lines = core.getMessagesManager().getList("ban.screen", placeholders);
        return String.join("\n", lines);
    }
}
