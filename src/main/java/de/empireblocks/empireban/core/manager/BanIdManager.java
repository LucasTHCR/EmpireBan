package de.empireblocks.empireban.core.manager;

import de.empireblocks.empireban.core.config.YamlDocument;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.IdLevel;
import de.empireblocks.empireban.core.model.PunishmentType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// owns the configurable ID system - named, reusable punishment reasons with their own
// escalation levels, persisted to ids.yml, referenced via /ban <player> <id>
public class BanIdManager {

    private final YamlDocument document;
    private final Map<String, BanId> ids = new LinkedHashMap<>();

    public BanIdManager(Path dataFolder, Class<?> resourceHolder) throws IOException {
        this.document = YamlDocument.load(dataFolder, "ids.yml", resourceHolder);
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        ids.clear();
        Map<String, Object> section = document.getSection("ids");
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey();
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> idData = (Map<String, Object>) entry.getValue();
            PunishmentType type = PunishmentType.valueOf(String.valueOf(idData.getOrDefault("type", "BAN")));
            boolean onlyAdmins = Boolean.parseBoolean(String.valueOf(idData.getOrDefault("only-admins", "false")));
            String reason = String.valueOf(idData.getOrDefault("reason", key));
            BanId banId = new BanId(key, type, onlyAdmins, reason);

            Object levelsRaw = idData.get("levels");
            if (levelsRaw instanceof Map) {
                // YAML resolves unquoted numeric keys (1:, 2:, ...) to Integer, not String -
                // iterate as Map<?, ?> and stringify the key instead of assuming either type.
                for (Map.Entry<?, ?> levelEntry : ((Map<?, ?>) levelsRaw).entrySet()) {
                    int level = Integer.parseInt(String.valueOf(levelEntry.getKey()));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> levelData = (Map<String, Object>) levelEntry.getValue();
                    long duration = Long.parseLong(String.valueOf(levelData.getOrDefault("duration", -1)));
                    PunishmentType levelType = PunishmentType.valueOf(String.valueOf(levelData.getOrDefault("type", type.name())));
                    banId.addLevel(new IdLevel(level, duration, levelType));
                }
            }
            ids.put(key, banId);
        }
    }

    public void reload() throws IOException {
        document.reload();
        load();
    }

    public Optional<BanId> get(String key) {
        return Optional.ofNullable(ids.get(key.toLowerCase()));
    }

    public Map<String, BanId> getAll() {
        return ids;
    }

    public BanId create(String key, PunishmentType type, boolean onlyAdmins, long defaultDurationSeconds, String reason) throws IOException {
        String normalizedKey = key.toLowerCase();
        BanId banId = new BanId(normalizedKey, type, onlyAdmins, reason);
        banId.addLevel(new IdLevel(1, defaultDurationSeconds, type));
        ids.put(normalizedKey, banId);
        persist(banId);
        return banId;
    }

    public boolean delete(String key) throws IOException {
        String normalizedKey = key.toLowerCase();
        BanId removed = ids.remove(normalizedKey);
        if (removed == null) {
            return false;
        }
        document.set("ids." + normalizedKey, null);
        document.save();
        return true;
    }

    public void addLevel(String key, int level, long durationSeconds, PunishmentType type) throws IOException {
        BanId banId = requireId(key);
        banId.addLevel(new IdLevel(level, durationSeconds, type));
        persist(banId);
    }

    public boolean removeLevel(String key, int level) throws IOException {
        BanId banId = requireId(key);
        boolean removed = banId.removeLevel(level);
        if (removed) {
            persist(banId);
        }
        return removed;
    }

    public void setLevelDuration(String key, int level, long durationSeconds) throws IOException {
        BanId banId = requireId(key);
        IdLevel target = findLevel(banId, level);
        target.setDurationSeconds(durationSeconds);
        persist(banId);
    }

    public void setLevelType(String key, int level, PunishmentType type) throws IOException {
        BanId banId = requireId(key);
        IdLevel target = findLevel(banId, level);
        target.setType(type);
        persist(banId);
    }

    public void setOnlyAdmins(String key, boolean onlyAdmins) throws IOException {
        BanId banId = requireId(key);
        banId.setOnlyAdmins(onlyAdmins);
        persist(banId);
    }

    public void setReason(String key, String reason) throws IOException {
        BanId banId = requireId(key);
        banId.setReason(reason);
        persist(banId);
    }

    private IdLevel findLevel(BanId banId, int level) {
        for (IdLevel idLevel : banId.getLevels()) {
            if (idLevel.getLevel() == level) {
                return idLevel;
            }
        }
        throw new IllegalArgumentException("Level " + level + " does not exist for ID '" + banId.getKey() + "'");
    }

    private BanId requireId(String key) {
        BanId banId = ids.get(key.toLowerCase());
        if (banId == null) {
            throw new IllegalArgumentException("ID '" + key + "' does not exist");
        }
        return banId;
    }

    private void persist(BanId banId) throws IOException {
        String base = "ids." + banId.getKey();
        document.set(base + ".type", banId.getDefaultType().name());
        document.set(base + ".only-admins", banId.isOnlyAdmins());
        document.set(base + ".reason", banId.getReason());
        Map<String, Object> levels = new LinkedHashMap<>();
        for (IdLevel level : banId.getLevels()) {
            Map<String, Object> levelData = new LinkedHashMap<>();
            levelData.put("duration", level.getDurationSeconds());
            levelData.put("type", level.getType().name());
            levels.put(String.valueOf(level.getLevel()), levelData);
        }
        document.set(base + ".levels", levels);
        document.save();
    }
}
