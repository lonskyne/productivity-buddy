package org.example.productivitybuddy.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessRecord {
    private final int pid;
    private final String originalName;
    private volatile String aliasName;

    private volatile ProcessCategory category;

    private volatile double cpuUsage;
    private volatile long ramUsage;
    private final AtomicLong previousTotalCpuTicks;

    private AtomicBoolean isTrackingFrozen;
    private AtomicLong totalTimeMilliseconds;

    private AtomicLong lastSeenTimestamp;
    private AtomicLong totalTicks;

    public ProcessRecord(int pid, String originalName, long ramUsage, long totalTicks) {
        this.pid = pid;
        this.originalName = originalName;
        this.ramUsage = ramUsage;
        this.totalTicks = new AtomicLong(totalTicks);

        this.aliasName = originalName;
        this.totalTimeMilliseconds = new AtomicLong(0);
        this.category = ProcessCategory.UNCATEGORIZED;
        this.isTrackingFrozen = new AtomicBoolean(false);
        this.lastSeenTimestamp = new AtomicLong(System.currentTimeMillis());
        this.previousTotalCpuTicks = new AtomicLong(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessRecord that)) return false;
        return this.pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(pid);
    }

    public int getPid() {
        return pid;
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

    public void setPreviousTotalCpuTicks(long ticks) {
        previousTotalCpuTicks.set(ticks);
    }

    public long getPreviousTotalCpuTicks() {
        return previousTotalCpuTicks.get();
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public long getTotalTicks() {
        return totalTicks.get();
    }

    public ProcessCategory getCategory() {
        return category;
    }

    public long getRamUsage() {
        return ramUsage;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setRamUsage(long ramUsage) {
        this.ramUsage = ramUsage;
    }

    public void setCategory(ProcessCategory category) {
        this.category = category;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean getIsTrackingFrozen() {
        return isTrackingFrozen.get();
    }

    public void toggleIsTrackingFrozen() {
        boolean currentValue = isTrackingFrozen.get();
        boolean newValue = !currentValue;
        // Keep trying until the update is successful
        while (!isTrackingFrozen.compareAndSet(currentValue, newValue)) {
            currentValue = isTrackingFrozen.get();
            newValue = !currentValue;
        }
    }

    public String getTimeFormatted() {
        long totalSeconds = getTotalTimeMilliseconds() / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %2dm %2ds", hours, minutes, seconds);
        } else
        if (minutes > 0){
            return String.format("%dm %2ds", minutes, seconds);
        }
        else {
            return String.format("%2ds", seconds);
        }
    }

    public String getAliasName() {
        return aliasName;
    }
}

