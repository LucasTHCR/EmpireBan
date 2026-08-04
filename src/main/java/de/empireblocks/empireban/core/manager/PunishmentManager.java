package de.empireblocks.empireban.core.manager;

import de.empireblocks.empireban.core.db.LogRepository;
import de.empireblocks.empireban.core.db.PunishmentRepository;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.IdLevel;
import de.empireblocks.empireban.core.model.Punishment;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;

import java.util.Optional;
import java.util.UUID;

// applies/lifts bans, mutes, kicks and warns - punishWithId() goes through the ID/level
// escalation system, punishManual() takes type/duration/reason straight from the operator
public class PunishmentManager {

    private final PunishmentRepository punishmentRepository;
    private final LogRepository logRepository;
    private final BanIdManager banIdManager;

    public PunishmentManager(PunishmentRepository punishmentRepository, LogRepository logRepository, BanIdManager banIdManager) {
        this.punishmentRepository = punishmentRepository;
        this.logRepository = logRepository;
        this.banIdManager = banIdManager;
    }

    public record PunishResult(Punishment punishment, BanId banId, IdLevel level, int priorCount) {
    }

    public PunishResult punishWithId(UUID targetUuid, String targetName, String ip, String idKey,
                                      UUID operatorUuid, String operatorName) {
        BanId banId = banIdManager.get(idKey)
                .orElseThrow(() -> new IllegalArgumentException("ID '" + idKey + "' does not exist"));
        int priorCount = punishmentRepository.countByUuidAndIdKey(targetUuid, banId.getKey());
        IdLevel level = banId.getLevelFor(priorCount);
        if (level == null) {
            throw new IllegalStateException("ID '" + idKey + "' has no configured levels");
        }

        Punishment punishment = new Punishment();
        punishment.setUuid(targetUuid);
        punishment.setPlayerName(targetName);
        punishment.setIp(ip);
        punishment.setType(level.getType());
        punishment.setReason(banId.getReason());
        punishment.setIdKey(banId.getKey());
        punishment.setLevel(level.getLevel());
        punishment.setOperatorUuid(operatorUuid);
        punishment.setOperatorName(operatorName);
        punishment.setCreatedAt(System.currentTimeMillis());
        punishment.setExpiresAt(TimeUtil.secondsToExpiry(level.getDurationSeconds()));

        long id = punishmentRepository.insert(punishment);
        punishment.setId(id);

        logRepository.log(operatorUuid, operatorName, punishment.getType().name(), targetName,
                "ID=" + banId.getKey() + " Level=" + level.getLevel() + " Reason=" + banId.getReason());

        return new PunishResult(punishment, banId, level, priorCount);
    }

    public Punishment punishManual(UUID targetUuid, String targetName, String ip, PunishmentType type,
                                    String reason, long durationSeconds, UUID operatorUuid, String operatorName) {
        Punishment punishment = new Punishment();
        punishment.setUuid(targetUuid);
        punishment.setPlayerName(targetName);
        punishment.setIp(ip);
        punishment.setType(type);
        punishment.setReason(reason);
        punishment.setLevel(1);
        punishment.setOperatorUuid(operatorUuid);
        punishment.setOperatorName(operatorName);
        punishment.setCreatedAt(System.currentTimeMillis());
        punishment.setExpiresAt(TimeUtil.secondsToExpiry(durationSeconds));

        long id = punishmentRepository.insert(punishment);
        punishment.setId(id);

        logRepository.log(operatorUuid, operatorName, type.name(), targetName, "Reason=" + reason);

        return punishment;
    }

    public void kick(UUID targetUuid, String targetName, String reason, UUID operatorUuid, String operatorName) {
        Punishment punishment = new Punishment();
        punishment.setUuid(targetUuid);
        punishment.setPlayerName(targetName);
        punishment.setType(PunishmentType.KICK);
        punishment.setReason(reason);
        punishment.setLevel(1);
        punishment.setOperatorUuid(operatorUuid);
        punishment.setOperatorName(operatorName);
        punishment.setCreatedAt(System.currentTimeMillis());
        punishment.setExpiresAt(punishment.getCreatedAt());
        long id = punishmentRepository.insert(punishment);
        punishmentRepository.deactivate(id, operatorName, "kick");
        logRepository.log(operatorUuid, operatorName, "KICK", targetName, "Reason=" + reason);
    }

    public Optional<Punishment> getActiveBan(UUID uuid) {
        return punishmentRepository.findActiveBan(uuid);
    }

    public Optional<Punishment> getActiveMute(UUID uuid) {
        return punishmentRepository.findActive(uuid, PunishmentType.MUTE);
    }

    public Optional<Punishment> getActiveIpBan(String ip) {
        return punishmentRepository.findActiveIpBan(ip);
    }

    public boolean unban(UUID uuid, String by, String reason) {
        Optional<Punishment> active = punishmentRepository.findActiveBan(uuid);
        if (active.isEmpty()) {
            return false;
        }
        punishmentRepository.deactivate(active.get().getId(), by, reason);
        logRepository.log(null, by, "UNBAN", active.get().getPlayerName(), "Reason=" + reason);
        return true;
    }

    public boolean unmute(UUID uuid, String by, String reason) {
        Optional<Punishment> active = punishmentRepository.findActive(uuid, PunishmentType.MUTE);
        if (active.isEmpty()) {
            return false;
        }
        punishmentRepository.deactivate(active.get().getId(), by, reason);
        logRepository.log(null, by, "UNMUTE", active.get().getPlayerName(), "Reason=" + reason);
        return true;
    }

    public void purgeExpired() {
        punishmentRepository.expireOutdated();
    }
}
