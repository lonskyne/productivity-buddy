package org.example.productivitybuddy.model;

import org.example.productivitybuddy.util.TimeFormatter;

public class ProcessSnapshot {

    private final int pid;
    private final String originalName;
    private final String aliasName;
    private final ProcessCategory category;

    private final double cpuUsage;
    private final long ramUsage;

    private final long previousTotalCpuTicks;
    private final long totalTicks;

    private final boolean isTrackingFrozen;

    private final long sessionTimeMilliseconds;
    private final long startTimeMilliseconds;

    private final long lastSeenTimestamp;

    public ProcessSnapshot(ProcessRecord p) {
        this.pid = p.getPid();
        this.originalName = p.getOriginalName();
        this.aliasName = p.getAliasName();
        this.category = p.getCategory();

        this.cpuUsage = p.getCpuUsage();
        this.ramUsage = p.getRamUsage();

        this.previousTotalCpuTicks = p.getPreviousTotalCpuTicks();
        this.totalTicks = p.getTotalTicks();

        this.isTrackingFrozen = p.getIsTrackingFrozen();

        this.sessionTimeMilliseconds = p.getSessionTimeMilliseconds();
        this.startTimeMilliseconds = p.getStartTimeMilliseconds();

        this.lastSeenTimestamp = p.getLastSeenTimestamp();
    }

    public int getPid() {
        return pid;
    }

    public long getSessionTimeMilliseconds() {
        return sessionTimeMilliseconds;
    }

    public long getStartTimeMilliseconds() {
        return startTimeMilliseconds;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public long getPreviousTotalCpuTicks() {
        return previousTotalCpuTicks;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public long getTotalTicks() {
        return totalTicks;
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

    public boolean getIsTrackingFrozen() {
        return isTrackingFrozen;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getTimeFormatted() {
        return TimeFormatter.formatTime(sessionTimeMilliseconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessSnapshot that)) return false;
        return this.pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(pid);
    }
}