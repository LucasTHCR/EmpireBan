package de.empireblocks.empireban.core.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal, platform-independent YAML file wrapper (dotted-path get/set + save) built on
 * SnakeYAML, so the same config/messages/ids code runs unchanged on Spigot, BungeeCord and
 * Velocity instead of depending on Bukkit's YamlConfiguration.
 */
public class YamlDocument {

    private final Path file;
    private Map<String, Object> data;

    private YamlDocument(Path file, Map<String, Object> data) {
        this.file = file;
        this.data = data;
    }

    /**
     * Loads {@code fileName} from {@code dataFolder}. If it doesn't exist yet, it is copied
     * from the plugin jar's classpath resource of the same name (if present) before loading.
     */
    public static YamlDocument load(Path dataFolder, String fileName, Class<?> resourceHolder) throws IOException {
        Files.createDirectories(dataFolder);
        Path target = dataFolder.resolve(fileName);
        if (!Files.exists(target)) {
            try (InputStream in = resourceHolder.getResourceAsStream("/" + fileName)) {
                if (in != null) {
                    Files.copy(in, target);
                } else {
                    Files.createFile(target);
                }
            }
        }
        Yaml yaml = new Yaml();
        Map<String, Object> loaded;
        try (InputStream in = Files.newInputStream(target)) {
            loaded = yaml.load(in);
        }
        return new YamlDocument(target, loaded != null ? loaded : new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Object get(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }
        return current.get(parts[parts.length - 1]);
    }

    public String getString(String path, String def) {
        Object value = get(path);
        return value != null ? String.valueOf(value) : def;
    }

    public int getInt(String path, int def) {
        Object value = get(path);
        return value instanceof Number ? ((Number) value).intValue() : def;
    }

    public long getLong(String path, long def) {
        Object value = get(path);
        return value instanceof Number ? ((Number) value).longValue() : def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object value = get(path);
        return value instanceof Boolean ? (Boolean) value : def;
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof java.util.List) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (Object o : (java.util.List<Object>) value) {
                result.add(String.valueOf(o));
            }
            return result;
        }
        return java.util.Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }

    public void save() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (var writer = Files.newBufferedWriter(file)) {
            yaml.dump(data, writer);
        }
    }

    public void reload() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(file)) {
            Map<String, Object> loaded = yaml.load(in);
            this.data = loaded != null ? loaded : new LinkedHashMap<>();
        }
    }
}
