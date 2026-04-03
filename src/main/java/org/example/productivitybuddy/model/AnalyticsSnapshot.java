package org.example.productivitybuddy.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsSnapshot {
    private final Map<ProcessCategory, Long> timeByCategory;
    private final Collection<ProcessSnapshot> allProcesses;
    private final long totalTime;

    private final HashMap<ProcessCategory, List<ProcessSnapshot>> processesByCategory;
    private final HashMap<ProcessCategory, List<ProcessSnapshot>> top10PerCategory;
    private final HashMap<ProcessCategory, Long> totalTimePerCategory;
    private final HashMap<Integer, Integer> cpuRankPerPid;
    private final HashMap<Integer, Integer> ramRankPerPid;
    private final Map<Integer, ProcessSnapshot> processByPid;

    public AnalyticsSnapshot(Map<ProcessCategory, Long> timeByCategory, Collection<ProcessSnapshot> allProcesses, long totalTime, HashMap<ProcessCategory, List<ProcessSnapshot>> processesByCategory, HashMap<ProcessCategory, List<ProcessSnapshot>> top10PerCategory, HashMap<ProcessCategory, Long> totalTimePerCategory, HashMap<Integer, Integer> cpuRankPerPid, HashMap<Integer, Integer> ramRankPerPid, Map<Integer, ProcessSnapshot> processByPid) {
        this.timeByCategory = timeByCategory;
        this.allProcesses = allProcesses;
        this.totalTime = totalTime;
        this.processesByCategory = processesByCategory;
        this.top10PerCategory = top10PerCategory;
        this.totalTimePerCategory = totalTimePerCategory;
        this.cpuRankPerPid = cpuRankPerPid;
        this.ramRankPerPid = ramRankPerPid;
        this.processByPid = processByPid;
    }

    public Map<ProcessCategory, Long> getTimeByCategory() {
        return timeByCategory;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public Collection<ProcessSnapshot> getAllProcesses() {
        return allProcesses;
    }

    public List<ProcessSnapshot> getProcessesByCategory(ProcessCategory category) {
        return processesByCategory.get(category);
    }

    public List<ProcessSnapshot> getTop10ByCategory(ProcessCategory category) {
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

    public ProcessSnapshot getProcessByPid(Integer pid) { return processByPid.get(pid); }
}
