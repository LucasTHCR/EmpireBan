package de.empireblocks.empireban.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A configurable, named punishment reason ("ID") with an ordered list of escalation
 * levels. When a player is punished with this id, the level applied is
 * {@code min(pastPunishmentsWithThisId + 1, levels.size())} - i.e. once every level has
 * been used up, the player keeps getting punished at the last (harshest) level.
 */
public class BanId {

    private String key;
    private PunishmentType defaultType;
    private boolean onlyAdmins;
    private String reason;
    private final List<IdLevel> levels = new ArrayList<>();

    public BanId() {
    }

    public BanId(String key, PunishmentType defaultType, boolean onlyAdmins, String reason) {
        this.key = key;
        this.defaultType = defaultType;
        this.onlyAdmins = onlyAdmins;
        this.reason = reason;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PunishmentType getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(PunishmentType defaultType) {
        this.defaultType = defaultType;
    }

    public boolean isOnlyAdmins() {
        return onlyAdmins;
    }

    public void setOnlyAdmins(boolean onlyAdmins) {
        this.onlyAdmins = onlyAdmins;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<IdLevel> getLevels() {
        return levels;
    }

    public IdLevel getLevelFor(int priorPunishmentCount) {
        if (levels.isEmpty()) {
            return null;
        }
        int targetLevel = priorPunishmentCount + 1;
        IdLevel best = null;
        for (IdLevel level : levels) {
            if (level.getLevel() <= targetLevel && (best == null || level.getLevel() > best.getLevel())) {
                best = level;
            }
        }
        if (best == null) {
            // requested level below the lowest configured level -> use the lowest one
            for (IdLevel level : levels) {
                if (best == null || level.getLevel() < best.getLevel()) {
                    best = level;
                }
            }
        }
        return best;
    }

    public void addLevel(IdLevel level) {
        levels.removeIf(l -> l.getLevel() == level.getLevel());
        levels.add(level);
        levels.sort((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
    }

    public boolean removeLevel(int level) {
        return levels.removeIf(l -> l.getLevel() == level);
    }
}
