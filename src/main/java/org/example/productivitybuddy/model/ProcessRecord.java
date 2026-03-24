package org.example.productivitybuddy.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessRecord {
    private final int pid;
    private final String originalName;
    private final String aliasName;
    private volatile ProcessCategory category;
    private AtomicBoolean isTrackingFrozen;
    private AtomicLong totalTimeMilliseconds;

    private AtomicLong lastSeenTimestamp;

    public ProcessRecord(int pid, String originalName) {
        this.pid = pid;
        this.originalName = originalName;
        this.aliasName = "";
        this.totalTimeMilliseconds = new AtomicLong(0);
        this.category = ProcessCategory.OTHER;
        this.isTrackingFrozen = new AtomicBoolean(false);
        lastSeenTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public int getPid() {
        return pid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public ProcessCategory getCategory() {
        return category;
    }

    public boolean isTrackingFrozen() {
        return isTrackingFrozen.get();
    }

    public void setTrackingFrozen(boolean trackingFrozen) {
        isTrackingFrozen.set(trackingFrozen);
    }

    public long getTotalTimeMilliseconds() {
        return totalTimeMilliseconds.get();
    }

    public void setTotalTimeMilliseconds(long totalTimeMilliseconds) {
        this.totalTimeMilliseconds.set(totalTimeMilliseconds);
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp.get();
    }

    public void setLastSeenTimestamp(long lastSeenTimestamp) {
        this.lastSeenTimestamp.set(lastSeenTimestamp);
    }
}

