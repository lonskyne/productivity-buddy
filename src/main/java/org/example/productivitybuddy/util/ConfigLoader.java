package org.example.productivitybuddy.util;

import org.example.productivitybuddy.model.MyConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static void load(Path configPath) {
        Properties props = new Properties();

        try (InputStream is = Files.newInputStream(configPath)) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configPath, e);
        }

        String monitorInterval = props.getProperty("monitor.interval");
        if (monitorInterval != null) {
            MyConfig.MONITOR_INTERVAL.set(Integer.parseInt(monitorInterval.trim()));
        }

        String mappingFile = props.getProperty("mapping.file");
        if (mappingFile != null) {
            MyConfig.MAPPING_FILE = Path.of(mappingFile.trim());
        }

        String snapshotInterval = props.getProperty("snapshot.interval");
        if (snapshotInterval != null) {
            MyConfig.SNAPSHOT_INTERVAL.set(Integer.parseInt(snapshotInterval.trim()));
        }

        List<LocalTime> fixedTimes = new ArrayList<>();

        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("snapshot.fixed_time")) {
                String value = props.getProperty(key);
                if (value != null && !value.isBlank()) {
                    fixedTimes.add(LocalTime.parse(value.trim()));
                }
            }
        }

        if (!fixedTimes.isEmpty()) {
            MyConfig.SNAPSHOT_FIXED_TIMES = fixedTimes;
        }
    }
}
