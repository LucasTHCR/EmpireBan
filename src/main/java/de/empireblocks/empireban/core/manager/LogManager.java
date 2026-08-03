package de.empireblocks.empireban.core.manager;

import de.empireblocks.empireban.core.db.LogRepository;

import java.util.List;

public class LogManager {

    private final LogRepository logRepository;

    public LogManager(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<LogRepository.LogEntry> page(int page, int pageSize) {
        return logRepository.page(page, pageSize);
    }

    public void clear() {
        logRepository.clear();
    }
}
