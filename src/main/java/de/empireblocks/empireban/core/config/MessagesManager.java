package de.empireblocks.empireban.core.config;

import de.empireblocks.empireban.core.util.ColorUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MessagesManager {

    private final YamlDocument document;

    public MessagesManager(Path dataFolder, String language, Class<?> resourceHolder) throws IOException {
        this.document = YamlDocument.load(dataFolder, "messages_" + language + ".yml", resourceHolder);
    }

    public void reload() throws IOException {
        document.reload();
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> placeholders) {
        Object raw = document.get(path);
        String message = raw != null ? String.valueOf(raw) : path;
        message = message.replace("%prefix%", prefix());
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ColorUtil.translate(message);
    }

    public java.util.List<String> getList(String path, Map<String, String> placeholders) {
        java.util.List<String> raw = document.getStringList(path);
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String line : raw) {
            String formatted = line.replace("%prefix%", prefix());
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            result.add(ColorUtil.translate(formatted));
        }
        return result;
    }

    /** The "prefix" message is a plain (non-colorized-yet) fragment reused inside other messages via %prefix%. */
    private String prefix() {
        Object raw = document.get("prefix");
        return raw != null ? String.valueOf(raw) : "";
    }
}
