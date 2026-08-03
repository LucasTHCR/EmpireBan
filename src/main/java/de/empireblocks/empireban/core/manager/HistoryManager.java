package de.empireblocks.empireban.core.manager;

import de.empireblocks.empireban.core.db.PunishmentRepository;
import de.empireblocks.empireban.core.model.Punishment;

import java.util.List;
import java.util.UUID;

public class HistoryManager {

    private final PunishmentRepository punishmentRepository;

    public HistoryManager(PunishmentRepository punishmentRepository) {
        this.punishmentRepository = punishmentRepository;
    }

    public List<Punishment> history(UUID uuid) {
        return punishmentRepository.history(uuid);
    }

    public int deleteHistory(UUID uuid) {
        return punishmentRepository.deleteHistory(uuid);
    }
}
