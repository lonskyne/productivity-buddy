package org.example.productivitybuddy.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class MyConfig {
    public final static AtomicInteger MONITOR_INTERVAL = new AtomicInteger(1000);
    public final static String MAPPING_FILE = "process_info.json";
    public final static AtomicInteger SNAPSHOT_INTERVAL = new AtomicInteger(60);
    public static Collection<LocalTime> SNAPSHOT_FIXED_TIMES = new ArrayList<>();
}
