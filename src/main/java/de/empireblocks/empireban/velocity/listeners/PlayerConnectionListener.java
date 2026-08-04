package de.empireblocks.empireban.velocity.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.manager.IpManager;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerConnectionListener {

    private final EmpireBanCore core;

    public PlayerConnectionListener(EmpireBanCore core) {
        this.core = core;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getRemoteAddress() instanceof InetSocketAddress address
                ? address.getAddress().getHostAddress() : "0.0.0.0";
        String name = player.getUsername();

        Optional<Punishment> ban = core.getPunishmentManager().getActiveBan(uuid);
        if (ban.isEmpty()) {
            ban = core.getPunishmentManager().getActiveIpBan(ip);
        }
        if (ban.isPresent() && !ban.get().isExpired()) {
            event.setResult(ResultedEvent.ComponentResult.denied(VelocityPlatformAdapter.toComponent(buildBanScreen(ban.get()))));
            return;
        }

        if (core.getIpManager().autobanEnabled() || core.getIpManager().notifyStaffEnabled()) {
            List<Punishment> altBans = core.getIpManager().findAltAccounts(ip, uuid);
            if (!altBans.isEmpty()) {
                Punishment altBan = altBans.get(0);
                if (core.getIpManager().autobanEnabled()) {
                    core.getPunishmentManager().punishManual(uuid, name, ip, PunishmentType.IP_BAN,
                            "Alt account of banned player " + altBan.getPlayerName(), -1, null, "EmpireBan");
                    event.setResult(ResultedEvent.ComponentResult.denied(VelocityPlatformAdapter.toComponent(
                            buildBanScreen(core.getPunishmentManager().getActiveBan(uuid).orElse(altBan)))));
                    core.getPlatform().broadcastToPermission("bansys.notify", core.getMessagesManager()
                            .get("ip.autobanned", Map.of("player", name, "banned_player", altBan.getPlayerName())));
                } else {
                    core.getPlatform().broadcastToPermission("bansys.notify", core.getMessagesManager()
                            .get("ip.alt-detected", Map.of("player", name, "banned_player", altBan.getPlayerName())));
                }
            }
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getUsername();
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
