package org.example.productivitybuddy.model;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyConfig {
    public final static AtomicInteger MONITOR_INTERVAL = new AtomicInteger(1000);
    public static Path MAPPING_FILE = Path.of("/home/lonskyne/Documents/process_info.json");
    public final static AtomicInteger SNAPSHOT_INTERVAL = new AtomicInteger(60);
    public static Collection<LocalTime> SNAPSHOT_FIXED_TIMES = List.of(
            LocalTime.of(23, 46, 50),
            LocalTime.of(12, 0, 0),
            LocalTime.of(17, 0, 0)
    );
}
