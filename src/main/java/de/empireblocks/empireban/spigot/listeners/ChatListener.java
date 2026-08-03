package de.empireblocks.empireban.spigot.listeners;

import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final EmpireBanCore core;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public ChatListener(EmpireBanCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Optional<Punishment> mute = core.getPunishmentManager().getActiveMute(uuid);
        if (mute.isPresent() && !mute.get().isExpired()) {
            event.setCancelled(true);
            Punishment punishment = mute.get();
            player.sendMessage(core.getMessagesManager().get("mute.screen", Map.of(
                    "reason", punishment.getReason() != null ? punishment.getReason() : "-",
                    "duration", TimeUtil.formatRemaining(punishment.remainingMillis())
            )));
            return;
        }

        if (core.getConfigManager().chatFilterEnabled() && !player.hasPermission("bansys.bypasschatfilter")) {
            String lowerMessage = event.getMessage().toLowerCase();
            for (String blocked : core.getConfigManager().chatFilterBlacklist()) {
                if (!blocked.isBlank() && lowerMessage.contains(blocked.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(core.getMessagesManager().get("chat.filtered"));
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
                        player.sendMessage(core.getMessagesManager().get("chat.delay",
                                Map.of("seconds", String.valueOf(delaySeconds - elapsed))));
                        return;
                    }
                }
                lastMessageAt.put(uuid, now);
            }
        }
    }
}
