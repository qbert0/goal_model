package org.tzi.use.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Persists UI chooser paths in a local project file.
 */
public final class LastPathStore {
    private static final Object LOCK = new Object();
    private static final String KEY_USE_SPEC = "use.spec";
    private static final String KEY_GOAL_FILE = "goal.file";
    private static final Path STORE_PATH = Paths.get(System.getProperty("user.dir"), ".local", "last-paths.properties");

    private LastPathStore() {
    }

    public static Path getUseSpecDirectory(Path fallback) {
        return getDirectory(KEY_USE_SPEC, fallback);
    }

    public static Path getGoalFileDirectory(Path fallback) {
        return getDirectory(KEY_GOAL_FILE, fallback);
    }

    public static void setUseSpecDirectory(Path directory) {
        setDirectory(KEY_USE_SPEC, directory);
    }

    public static void setGoalFileDirectory(Path directory) {
        setDirectory(KEY_GOAL_FILE, directory);
    }

    private static Path getDirectory(String key, Path fallback) {
        synchronized (LOCK) {
            Properties props = load();
            String raw = props.getProperty(key);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            Path dir = Paths.get(raw);
            if (Files.isDirectory(dir)) {
                return dir;
            }
            return fallback;
        }
    }

    private static void setDirectory(String key, Path directory) {
        if (directory == null) {
            return;
        }
        synchronized (LOCK) {
            Properties props = load();
            props.setProperty(key, directory.toAbsolutePath().normalize().toString());
            save(props);
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        if (!Files.isReadable(STORE_PATH)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(STORE_PATH)) {
            props.load(in);
        } catch (IOException ignored) {
            // Ignore local persistence errors.
        }
        return props;
    }

    private static void save(Properties props) {
        try {
            Files.createDirectories(STORE_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(STORE_PATH)) {
                props.store(out, "Local chooser paths");
            }
        } catch (IOException ignored) {
            // Ignore local persistence errors.
        }
    }
}
