package de.empireblocks.empireban.spigot;

import de.empireblocks.empireban.core.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SpigotPlatformAdapter implements PlatformAdapter {

    private final EmpireBanSpigot plugin;

    public SpigotPlatformAdapter(EmpireBanSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public Path getDataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public void log(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public boolean isOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }

    @Override
    public Optional<String> getIp(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || player.getAddress() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(player.getAddress().getAddress().getHostAddress());
    }

    @Override
    public Optional<UUID> getOnlineUuidByName(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player != null ? Optional.of(player.getUniqueId()) : Optional.empty();
    }

    @Override
    public Optional<String> getNameByUuid(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return Optional.of(player.getName());
        }
        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        return Optional.ofNullable(offlineName);
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public void broadcastToPermission(String permission, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
    }

    @Override
    public void kick(UUID uuid, String kickMessage) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.kickPlayer(kickMessage);
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
