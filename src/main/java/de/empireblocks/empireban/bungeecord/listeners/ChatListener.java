package de.empireblocks.empireban.bungeecord.listeners;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// catches unsigned chat - signed chat (1.19.1+) can't be cancelled here, that needs
// signed-chat-bypass + the companion Spigot chat adapter
public class ChatListener implements Listener {

    private final EmpireBanCore core;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public ChatListener(EmpireBanCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(ChatEvent event) {
        if (event.isCommand() || !(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        UUID uuid = player.getUniqueId();

        Optional<Punishment> mute = core.getPunishmentManager().getActiveMute(uuid);
        if (mute.isPresent() && !mute.get().isExpired()) {
            event.setCancelled(true);
            Punishment punishment = mute.get();
            player.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("mute.screen", Map.of(
                    "reason", punishment.getReason() != null ? punishment.getReason() : "-",
                    "duration", TimeUtil.formatRemaining(punishment.remainingMillis())))));
            return;
        }

        if (core.getConfigManager().chatFilterEnabled() && !player.hasPermission("bansys.bypasschatfilter")) {
            String lowerMessage = event.getMessage().toLowerCase();
            for (String blocked : core.getConfigManager().chatFilterBlacklist()) {
                if (!blocked.isBlank() && lowerMessage.contains(blocked.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("chat.filtered")));
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
                        event.setCancelled(true);
                        player.sendMessage(TextComponent.fromLegacyText(core.getMessagesManager().get("chat.delay",
                                Map.of("seconds", String.valueOf(delaySeconds - elapsed)))));
                        return;
                    }
                }
                lastMessageAt.put(uuid, now);
            }
        }
    }
}
