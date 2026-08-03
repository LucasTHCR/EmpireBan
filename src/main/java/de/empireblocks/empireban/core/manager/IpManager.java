package de.empireblocks.empireban.core.manager;

import de.empireblocks.empireban.core.config.ConfigManager;
import de.empireblocks.empireban.core.db.IpRepository;
import de.empireblocks.empireban.core.db.PunishmentRepository;
import de.empireblocks.empireban.core.model.Punishment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP-side of the ban system: records last-seen IPs, detects alt accounts sharing an IP with
 * an actively banned player (either flags staff or auto-bans, per config), and optionally
 * checks joining IPs against vpnapi.io for VPN/proxy/tor usage.
 */
public class IpManager {

    public record VpnResult(boolean vpn, boolean proxy, boolean tor, boolean relay) {
        public boolean isSuspicious() {
            return vpn || proxy || tor || relay;
        }
    }

    private static final String BOOL_FIELD_FORMAT = "\"%s\"\\s*:\\s*(true|false)";

    private final ConfigManager config;
    private final IpRepository ipRepository;
    private final PunishmentRepository punishmentRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public IpManager(ConfigManager config, IpRepository ipRepository, PunishmentRepository punishmentRepository) {
        this.config = config;
        this.ipRepository = ipRepository;
        this.punishmentRepository = punishmentRepository;
    }

    public void recordJoin(UUID uuid, String ip, String playerName) {
        ipRepository.recordJoin(uuid, ip, playerName);
    }

    public List<Punishment> findAltAccounts(String ip, UUID excludeUuid) {
        return punishmentRepository.findActiveBansByIpExcluding(ip, excludeUuid);
    }

    /** Best-effort name->uuid lookup for players who are currently offline (proxy platforms have no OfflinePlayer equivalent). */
    public java.util.Optional<UUID> findKnownUuidByName(String name) {
        return ipRepository.findUuidByName(name);
    }

    public java.util.Optional<String> latestIp(UUID uuid) {
        return ipRepository.latestIp(uuid);
    }

    public boolean autobanEnabled() {
        return config.ipAutoban();
    }

    public boolean notifyStaffEnabled() {
        return config.ipNotifyStaff();
    }

    public boolean vpnCheckEnabled() {
        return config.vpnCheckEnabled();
    }

    /** Blocking call - always invoke via {@code PlatformAdapter#runAsync}/{@code supplyAsync}. */
    public VpnResult checkVpn(String ip) {
        String apiKey = config.vpnApiKey();
        String url = apiKey.isBlank()
                ? "https://vpnapi.io/api/" + ip
                : "https://vpnapi.io/api/" + ip + "?key=" + apiKey;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new VpnResult(false, false, false, false);
            }
            String body = response.body();
            return new VpnResult(
                    extractBool(body, "vpn"),
                    extractBool(body, "proxy"),
                    extractBool(body, "tor"),
                    extractBool(body, "relay")
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new VpnResult(false, false, false, false);
        }
    }

    private boolean extractBool(String body, String field) {
        Matcher matcher = Pattern.compile(String.format(BOOL_FIELD_FORMAT, field)).matcher(body);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }
}
