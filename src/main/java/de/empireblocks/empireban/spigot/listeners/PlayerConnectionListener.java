package de.empireblocks.empireban.spigot.listeners;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.IpManager;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final EmpireBanCore core;

    public PlayerConnectionListener(EmpireBanCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        Optional<Punishment> ban = core.getPunishmentManager().getActiveBan(uuid);
        if (ban.isEmpty()) {
            ban = core.getPunishmentManager().getActiveIpBan(ip);
        }
        if (ban.isPresent() && !ban.get().isExpired()) {
            Punishment punishment = ban.get();
            String kickMessage = buildBanScreen(punishment);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
            return;
        }

        if (core.getIpManager().autobanEnabled() || core.getIpManager().notifyStaffEnabled()) {
            List<Punishment> altBans = core.getIpManager().findAltAccounts(ip, uuid);
            if (!altBans.isEmpty()) {
                Punishment altBan = altBans.get(0);
                if (core.getIpManager().autobanEnabled()) {
                    core.getPunishmentManager().punishManual(uuid, event.getName(), ip,
                            de.empireblocks.empireban.core.model.PunishmentType.IP_BAN,
                            "Alt-Account von gebanntem Spieler " + altBan.getPlayerName(), -1,
                            null, "EmpireBan");
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            buildBanScreen(core.getPunishmentManager().getActiveBan(uuid).orElse(altBan)));
                    core.getPlatform().runSync(() -> core.getPlatform().broadcastToPermission("bansys.notify",
                            core.getMessagesManager().get("ip.autobanned", Map.of(
                                    "player", event.getName(), "banned_player", altBan.getPlayerName()))));
                    return;
                } else {
                    core.getPlatform().runSync(() -> core.getPlatform().broadcastToPermission("bansys.notify",
                            core.getMessagesManager().get("ip.alt-detected", Map.of(
                                    "player", event.getName(), "banned_player", altBan.getPlayerName()))));
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String ip = core.getPlatform().getIp(uuid).orElse("0.0.0.0");
        core.getPlatform().runAsync(() -> {
            core.getIpManager().recordJoin(uuid, ip, event.getPlayer().getName());

            if (core.getIpManager().vpnCheckEnabled()) {
                IpManager.VpnResult result = core.getIpManager().checkVpn(ip);
                if (result.isSuspicious()) {
                    core.getPlatform().runSync(() -> core.getPlatform().broadcastToPermission("bansys.notify",
                            core.getMessagesManager().get("ip.vpn-detected", Map.of("player", event.getPlayer().getName()))));
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
