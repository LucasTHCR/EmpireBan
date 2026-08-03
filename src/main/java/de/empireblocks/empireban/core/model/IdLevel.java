package de.empireblocks.empireban.core.model;

/**
 * One escalation step of a {@link BanId}: how long a punishment lasts once a player
 * has reached this level of prior punishments for the same id, and which punishment
 * type is applied at this level (a level can e.g. warn first, then mute, then ban).
 */
public class IdLevel {

    private int level;
    /** duration in seconds, -1 = permanent */
    private long durationSeconds;
    private PunishmentType type;

    public IdLevel() {
    }

    public IdLevel(int level, long durationSeconds, PunishmentType type) {
        this.level = level;
        this.durationSeconds = durationSeconds;
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public PunishmentType getType() {
        return type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }
}
