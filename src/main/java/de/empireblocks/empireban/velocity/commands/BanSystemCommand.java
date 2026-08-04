package de.empireblocks.empireban.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.empireblocks.empireban.core.EmpireBanCore;
import de.empireblocks.empireban.core.db.LogRepository;
import de.empireblocks.empireban.core.manager.BanIdManager;
import de.empireblocks.empireban.core.model.BanId;
import de.empireblocks.empireban.core.model.IdLevel;
import de.empireblocks.empireban.core.model.PunishmentType;
import de.empireblocks.empireban.core.util.TimeUtil;
import de.empireblocks.empireban.velocity.VelocityPlatformAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BanSystemCommand implements SimpleCommand {

    private final EmpireBanCore core;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public BanSystemCommand(EmpireBanCore core) {
        this.core = core;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        if (!sender.hasPermission("bansys.bansys")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        if (args.length == 0) {
            CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/bansystem <reload|ids|logs>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "ids" -> handleIds(sender, args);
            case "logs" -> handleLogs(sender, args);
            default -> CommandUtil.sendMessage(core, sender, "general.invalid-usage", Map.of("usage", "/bansystem <reload|ids|logs>"));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private void handleReload(CommandSource sender) {
        if (!sender.hasPermission("bansys.reload")) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
            return;
        }
        try {
            core.reload();
            CommandUtil.sendMessage(core, sender, "general.reload-success");
        } catch (Exception e) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cError while reloading: " + e.getMessage()));
        }
    }

    private void handleIds(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids <create|delete|edit|show>"));
            return;
        }
        BanIdManager manager = core.getBanIdManager();
        try {
            switch (args[1].toLowerCase()) {
                case "create" -> {
                    if (!sender.hasPermission("bansys.ids.create")) {
                        CommandUtil.sendMessage(core, sender, "general.no-permission");
                        return;
                    }
                    if (args.length < 6) {
                        sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids create <ID> <Type> <OnlyAdmins> <duration> <reason>"));
                        return;
                    }
                    if (manager.get(args[2]).isPresent()) {
                        CommandUtil.sendMessage(core, sender, "general.id-already-exists");
                        return;
                    }
                    PunishmentType type = PunishmentType.valueOf(args[3].toUpperCase());
                    boolean onlyAdmins = Boolean.parseBoolean(args[4]);
                    long duration = TimeUtil.parseToSeconds(args[5]);
                    String reason = args.length > 6 ? String.join(" ", List.of(args).subList(6, args.length)) : args[2];
                    manager.create(args[2], type, onlyAdmins, duration, reason);
                    CommandUtil.sendMessage(core, sender, "ids.created", Map.of("id", args[2]));
                }
                case "delete" -> {
                    if (!sender.hasPermission("bansys.ids.delete")) {
                        CommandUtil.sendMessage(core, sender, "general.no-permission");
                        return;
                    }
                    if (args.length < 3) {
                        sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids delete <ID>"));
                        return;
                    }
                    if (manager.delete(args[2])) {
                        CommandUtil.sendMessage(core, sender, "ids.deleted", Map.of("id", args[2]));
                    } else {
                        CommandUtil.sendMessage(core, sender, "general.id-not-found");
                    }
                }
                case "show" -> {
                    if (!sender.hasPermission("bansys.ids.show")) {
                        CommandUtil.sendMessage(core, sender, "general.no-permission");
                        return;
                    }
                    if (args.length < 3) {
                        for (String key : manager.getAll().keySet()) {
                            sender.sendMessage(VelocityPlatformAdapter.toComponent("§7- §e" + key));
                        }
                        return;
                    }
                    showId(sender, args[2]);
                }
                case "edit" -> handleIdsEdit(sender, args);
                default -> sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids <create|delete|edit|show>"));
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§c" + e.getMessage()));
        } catch (Exception e) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cError: " + e.getMessage()));
        }
    }

    private void showId(CommandSource sender, String key) {
        Optional<BanId> banIdOpt = core.getBanIdManager().get(key);
        if (banIdOpt.isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return;
        }
        BanId banId = banIdOpt.get();
        CommandUtil.sendMessage(core, sender, "ids.show-header", Map.of("id", banId.getKey()));
        sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get("ids.show-line", Map.of(
                "type", banId.getDefaultType().name(),
                "only_admins", String.valueOf(banId.isOnlyAdmins()),
                "reason", banId.getReason()))));
        for (IdLevel level : banId.getLevels()) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get("ids.show-level", Map.of(
                    "level", String.valueOf(level.getLevel()),
                    "duration", TimeUtil.formatRemaining(level.getDurationSeconds() < 0 ? -1 : level.getDurationSeconds() * 1000),
                    "type", level.getType().name()))));
        }
    }

    private void handleIdsEdit(CommandSource sender, String[] args) throws Exception {
        if (args.length < 5) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids edit <ID> <add|remove|set> ..."));
            return;
        }
        String id = args[2];
        BanIdManager manager = core.getBanIdManager();
        if (manager.get(id).isEmpty()) {
            CommandUtil.sendMessage(core, sender, "general.id-not-found");
            return;
        }
        String action = args[3].toLowerCase();

        if (action.equals("add") && args[4].equalsIgnoreCase("lvl")) {
            if (!sender.hasPermission("bansys.ids.addlvl")) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return;
            }
            if (args.length < 8) {
                sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids edit <ID> add lvl <lvl> <Duration> <Type>"));
                return;
            }
            int level = Integer.parseInt(args[5]);
            long duration = TimeUtil.parseToSeconds(args[6]);
            PunishmentType type = PunishmentType.valueOf(args[7].toUpperCase());
            manager.addLevel(id, level, duration, type);
            CommandUtil.sendMessage(core, sender, "ids.level-added", Map.of("id", id, "level", String.valueOf(level)));
            return;
        }

        if (action.equals("remove") && args[4].equalsIgnoreCase("lvl")) {
            if (!sender.hasPermission("bansys.ids.removelvl")) {
                CommandUtil.sendMessage(core, sender, "general.no-permission");
                return;
            }
            if (args.length < 6) {
                sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansystem ids edit <ID> remove lvl <lvl>"));
                return;
            }
            int level = Integer.parseInt(args[5]);
            manager.removeLevel(id, level);
            CommandUtil.sendMessage(core, sender, "ids.level-removed", Map.of("id", id, "level", String.valueOf(level)));
            return;
        }

        if (action.equals("set")) {
            String field = args[4].toLowerCase();
            switch (field) {
                case "lvlduration" -> {
                    if (!sender.hasPermission("bansys.ids.setduration") || args.length < 7) {
                        denyOrUsage(sender, "bansys.ids.setduration", "/bansystem ids edit <ID> set lvlduration <lvl> <Duration>");
                        return;
                    }
                    manager.setLevelDuration(id, Integer.parseInt(args[5]), TimeUtil.parseToSeconds(args[6]));
                    CommandUtil.sendMessage(core, sender, "ids.updated", Map.of("id", id));
                }
                case "lvltype" -> {
                    if (!sender.hasPermission("bansys.ids.settype") || args.length < 7) {
                        denyOrUsage(sender, "bansys.ids.settype", "/bansystem ids edit <ID> set lvltype <lvl> <Type>");
                        return;
                    }
                    manager.setLevelType(id, Integer.parseInt(args[5]), PunishmentType.valueOf(args[6].toUpperCase()));
                    CommandUtil.sendMessage(core, sender, "ids.updated", Map.of("id", id));
                }
                case "onlyadmins" -> {
                    if (!sender.hasPermission("bansys.ids.setonlyadmins") || args.length < 6) {
                        denyOrUsage(sender, "bansys.ids.setonlyadmins", "/bansystem ids edit <ID> set onlyadmins <True/False>");
                        return;
                    }
                    manager.setOnlyAdmins(id, Boolean.parseBoolean(args[5]));
                    CommandUtil.sendMessage(core, sender, "ids.updated", Map.of("id", id));
                }
                case "reason" -> {
                    if (!sender.hasPermission("bansys.ids.setreason") || args.length < 6) {
                        denyOrUsage(sender, "bansys.ids.setreason", "/bansystem ids edit <ID> set reason <reason>");
                        return;
                    }
                    manager.setReason(id, String.join(" ", List.of(args).subList(5, args.length)));
                    CommandUtil.sendMessage(core, sender, "ids.updated", Map.of("id", id));
                }
                default -> sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUnknown field: " + field));
            }
        }
    }

    private void denyOrUsage(CommandSource sender, String permission, String usage) {
        if (!sender.hasPermission(permission)) {
            CommandUtil.sendMessage(core, sender, "general.no-permission");
        } else {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: " + usage));
        }
    }

    private void handleLogs(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansys logs <show|clear>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "show" -> {
                if (!sender.hasPermission("bansys.logs.show")) {
                    CommandUtil.sendMessage(core, sender, "general.no-permission");
                    return;
                }
                int page = args.length > 2 ? parsePageOrDefault(args[2]) : 1;
                List<LogRepository.LogEntry> entries = core.getLogManager().page(page, 10);
                CommandUtil.sendMessage(core, sender, "logs.header", Map.of("page", String.valueOf(page)));
                for (LogRepository.LogEntry entry : entries) {
                    sender.sendMessage(VelocityPlatformAdapter.toComponent(core.getMessagesManager().get("logs.entry", Map.of(
                            "date", dateFormat.format(new Date(entry.createdAt())),
                            "actor", entry.actorName() != null ? entry.actorName() : "Console",
                            "action", entry.action(),
                            "target", entry.targetName() != null ? entry.targetName() : "-",
                            "details", entry.details() != null ? entry.details() : "-"
                    ))));
                }
            }
            case "clear" -> {
                if (!sender.hasPermission("bansys.logs.clear")) {
                    CommandUtil.sendMessage(core, sender, "general.no-permission");
                    return;
                }
                core.getLogManager().clear();
                CommandUtil.sendMessage(core, sender, "logs.cleared");
            }
            default -> sender.sendMessage(VelocityPlatformAdapter.toComponent("§cUsage: /bansys logs <show|clear>"));
        }
    }

    private int parsePageOrDefault(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
