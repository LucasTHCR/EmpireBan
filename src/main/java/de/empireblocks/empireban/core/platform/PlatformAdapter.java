package de.empireblocks.empireban.core.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// bridges core punishment/config logic to a concrete platform (Spigot, BungeeCord, Velocity) -
// core code never touches platform APIs directly, it all goes through here
public interface PlatformAdapter {

    Path getDataFolder();

    void log(String message);

    void warn(String message);

    boolean isOnline(UUID uuid);

    Optional<String> getIp(UUID uuid);

    Optional<UUID> getOnlineUuidByName(String name);

    Optional<String> getNameByUuid(UUID uuid);

    void sendMessage(UUID uuid, String message);

    /** Sends the message to every online player who has the given permission. */
    void broadcastToPermission(String permission, String message);

    /** Disconnects the player with a (multi-line, already formatted) kick message. */
    void kick(UUID uuid, String kickMessage);

    boolean hasPermission(UUID uuid, String permission);

    /** Runs the task off the main thread (I/O, DB, HTTP). */
    void runAsync(Runnable task);

    /** Runs the task back on the main thread once available (needed before touching platform APIs from async work). */
    void runSync(Runnable task);

    <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier);
}
