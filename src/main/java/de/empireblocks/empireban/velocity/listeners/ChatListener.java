package de.empireblocks.empireban.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catches chat sent through the proxy. From 1.19.1 on, clients may send chat signed for the
 * backend server directly; a SignedVelocity + backend companion plugin is required to still
 * intercept those (see the plugin page for setup) - see the project README.
 */
public class ChatListener {

    private final EmpireBanCore core;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public ChatListener(EmpireBanCore core) {
        this.core = core;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Optional<Punishment> mute = core.getPunishmentManager().getActiveMute(uuid);
        if (mute.isPresent() && !mute.get().isExpired()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            Punishment punishment = mute.get();
            player.sendMessage(VelocityPlatformAdapter.toComponent(
                    core.getMessagesManager().get("mute.screen", Map.of(
                            "reason", punishment.getReason() != null ? punishment.getReason() : "-",
                            "duration", TimeUtil.formatRemaining(punishment.remainingMillis())))));
            return;
        }

        if (core.getConfigManager().chatFilterEnabled() && !player.hasPermission("bansys.bypasschatfilter")) {
            String lowerMessage = event.getMessage().toLowerCase();
            for (String blocked : core.getConfigManager().chatFilterBlacklist()) {
                if (!blocked.isBlank() && lowerMessage.contains(blocked.toLowerCase())) {
                    event.setResult(PlayerChatEvent.ChatResult.denied());
                    player.sendMessage(VelocityPlatformAdapter.toComponent(
                            core.getMessagesManager().get("chat.filtered")));
                    return;
                }
            }
        }

        if (!player.hasPermission("bansys.bypasschatdelay")) {
            long delaySeconds = core.getConfigManager().chatDelaySeconds();
            if (delaySeconds > 0) {
                long now = System.currentTimeMillis();
                Long last = lastMessageAt.get(uuid);
                if (last != null) {
                    long elapsed = (now - last) / 1000L;
                    if (elapsed < delaySeconds) {
                        event.setResult(PlayerChatEvent.ChatResult.denied());
                        player.sendMessage(VelocityPlatformAdapter.toComponent(
                                core.getMessagesManager().get("chat.delay", Map.of("seconds", String.valueOf(delaySeconds - elapsed)))));
                        return;
                    }
                }
                lastMessageAt.put(uuid, now);
            }
        }
    }
}
