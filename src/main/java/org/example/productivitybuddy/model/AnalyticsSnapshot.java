package org.example.productivitybuddy.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsSnapshot {
    private final Map<ProcessCategory, Long> timeByCategory;
    private final Collection<ProcessRecord> allProcesses;
    private final long totalTime;

    private final HashMap<ProcessCategory, List<ProcessRecord>> processesByCategory;
    private final HashMap<ProcessCategory, List<ProcessRecord>> top10PerCategory;
    private final HashMap<ProcessCategory, Long> totalTimePerCategory;
    private final HashMap<Integer, Integer> cpuRankPerPid;
    private final HashMap<Integer, Integer> ramRankPerPid;

    public AnalyticsSnapshot(Map<ProcessCategory, Long> timeByCategory, Collection<ProcessRecord> allProcesses, long totalTime, HashMap<ProcessCategory, List<ProcessRecord>> processesByCategory, HashMap<ProcessCategory, List<ProcessRecord>> top10PerCategory, HashMap<ProcessCategory, Long> totalTimePerCategory, HashMap<Integer, Integer> cpuRankPerPid, HashMap<Integer, Integer> ramRankPerPid) {
        this.timeByCategory = timeByCategory;
        this.allProcesses = allProcesses;
        this.totalTime = totalTime;
        this.processesByCategory = processesByCategory;
        this.top10PerCategory = top10PerCategory;
        this.totalTimePerCategory = totalTimePerCategory;
        this.cpuRankPerPid = cpuRankPerPid;
        this.ramRankPerPid = ramRankPerPid;

    }

    public Map<ProcessCategory, Long> getTimeByCategory() {
        return timeByCategory;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public Collection<ProcessRecord> getAllProcesses() {
        return allProcesses;
    }

    public List<ProcessRecord> getProcessesByCategory(ProcessCategory category) {
        return processesByCategory.get(category);
    }

    public List<ProcessRecord> getTop10ByCategory(ProcessCategory category) {
        return top10PerCategory.get(category);
    }

    public Long getTotalTimeByCategory(ProcessCategory category) {
        return totalTimePerCategory.get(category);
    }

    public Integer getCpuRankByPid(Integer pid) {
        return cpuRankPerPid.get(pid);
    }

    public Integer getRamRankByPid(Integer pid) {
        return ramRankPerPid.get(pid);
    }
}
