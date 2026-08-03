package de.empireblocks.empireban.core.model;

// one escalation step of a BanId - e.g. a level can warn first, then mute, then ban
public class IdLevel {

    private int level;
    private long durationSeconds; // seconds, -1 = permanent
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
