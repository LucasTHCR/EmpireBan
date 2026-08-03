package de.empireblocks.empireban.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.empireblocks.empireban.core.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VelocityPlatformAdapter implements PlatformAdapter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Object pluginInstance;
    private final ProxyServer proxyServer;
    private final Path dataFolder;
    private final Logger logger;

    public VelocityPlatformAdapter(Object pluginInstance, ProxyServer proxyServer, Path dataFolder, Logger logger) {
        this.pluginInstance = pluginInstance;
        this.proxyServer = proxyServer;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public Path getDataFolder() {
        return dataFolder;
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return proxyServer.getPlayer(uuid).isPresent();
    }

    @Override
    public Optional<String> getIp(UUID uuid) {
        return proxyServer.getPlayer(uuid)
                .map(Player::getRemoteAddress)
                .map(InetSocketAddress::getAddress)
                .map(addr -> addr.getHostAddress());
    }

    @Override
    public Optional<UUID> getOnlineUuidByName(String name) {
        return proxyServer.getPlayer(name).map(Player::getUniqueId);
    }

    @Override
    public Optional<String> getNameByUuid(UUID uuid) {
        return proxyServer.getPlayer(uuid).map(Player::getUsername);
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        proxyServer.getPlayer(uuid).ifPresent(player -> player.sendMessage(toComponent(message)));
    }

    @Override
    public void broadcastToPermission(String permission, String message) {
        Component component = toComponent(message);
        for (Player player : proxyServer.getAllPlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }

    @Override
    public void kick(UUID uuid, String kickMessage) {
        proxyServer.getPlayer(uuid).ifPresent(player -> player.disconnect(toComponent(kickMessage)));
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return proxyServer.getPlayer(uuid).map(player -> player.hasPermission(permission)).orElse(false);
    }

    @Override
    public void runAsync(Runnable task) {
        proxyServer.getScheduler().buildTask(pluginInstance, task).schedule();
    }

    @Override
    public void runSync(Runnable task) {
        // Velocity has no dedicated "main thread" requirement for these operations - run inline.
        task.run();
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        proxyServer.getScheduler().buildTask(pluginInstance, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }).schedule();
        return future;
    }

    public static Component toComponent(String legacyText) {
        return LEGACY.deserialize(legacyText);
    }
}
