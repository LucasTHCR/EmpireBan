package de.empireblocks.empireban.bungeecord;

import de.empireblocks.empireban.core.platform.PlatformAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BungeeCordPlatformAdapter implements PlatformAdapter {

    private final EmpireBanBungeeCord plugin;

    public BungeeCordPlatformAdapter(EmpireBanBungeeCord plugin) {
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
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }

    @Override
    public Optional<String> getIp(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player == null || !(player.getSocketAddress() instanceof InetSocketAddress address)) {
            return Optional.empty();
        }
        return Optional.ofNullable(address.getAddress().getHostAddress());
    }

    @Override
    public Optional<UUID> getOnlineUuidByName(String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return player != null ? Optional.of(player.getUniqueId()) : Optional.empty();
    }

    @Override
    public Optional<String> getNameByUuid(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null ? Optional.of(player.getName()) : Optional.empty();
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public void broadcastToPermission(String permission, String message) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(TextComponent.fromLegacyText(message));
            }
        }
    }

    @Override
    public void kick(UUID uuid, String kickMessage) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.disconnect(TextComponent.fromLegacyText(kickMessage));
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }

    @Override
    public void runAsync(Runnable task) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
    }

    @Override
    public void runSync(Runnable task) {
        // BungeeCord has no dedicated main thread concept for these operations - run inline.
        task.run();
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
