package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.*;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AnalyticsService {
    private final int REFRESH_MILLISECONDS = 1000;

    private final ProcessRegistry registry;

    private volatile AnalyticsSnapshot snapshot;

    private Thread worker;
    private volatile boolean running = false;

    public AnalyticsService(ProcessRegistry registry) {
        this.registry = registry;
    }

    public void start() {
        if (running) return;

        running = true;

        worker = new Thread(() -> {
            while (running) {
                try {
                    recompute();
                    Thread.sleep(REFRESH_MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    public AnalyticsSnapshot getSnapshot() {
        return snapshot;
    }

    private void recompute() {
        List<ProcessRecord> processList = List.copyOf(registry.getAllProcesses());

        Map<String, ProcessRecord> uniqueByName = processList.stream()
                .collect(Collectors.toMap(
                        ProcessRecord::getOriginalName,
                        p -> p,
                        (p1, p2) -> p1
                ));

        Collection<ProcessRecord> uniqueProcesses = uniqueByName.values();

        Map<ProcessCategory, Long> timeByCategory = uniqueProcesses.stream()
                .collect(Collectors.groupingBy(
                        p -> Optional.ofNullable(p.getCategory())
                                .orElse(ProcessCategory.UNCATEGORIZED),
                        Collectors.summingLong(ProcessRecord::getSessionTimeMilliseconds)
                ));

        long totalTime = uniqueProcesses.stream()
                .mapToLong(ProcessRecord::getSessionTimeMilliseconds)
                .sum();

        HashMap<ProcessCategory, List<ProcessRecord>> processesByCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            processesByCategory.put(cat,
                    processList.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .toList()
            );
        }

        HashMap<ProcessCategory, List<ProcessRecord>> top10PerCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            top10PerCategory.put(cat,
                    uniqueProcesses.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .sorted(Comparator.comparingLong(ProcessRecord::getSessionTimeMilliseconds).reversed())
                            .limit(10)
                            .toList()
            );
        }

        HashMap<ProcessCategory, Long> totalTimePerCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            totalTimePerCategory.put(cat,
                    uniqueProcesses.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .mapToLong(ProcessRecord::getSessionTimeMilliseconds)
                            .sum()
            );
        }

        HashMap<Integer, Integer> cpuRankPerPid = new HashMap<>();
        HashMap<Integer, Integer> ramRankPerPid = new HashMap<>();

        for(ProcessRecord p : processList) {
            cpuRankPerPid.put(p.getPid(), getRank(processList, p, Comparator.comparingDouble(ProcessRecord::getCpuUsage).reversed()));
            ramRankPerPid.put(p.getPid(), getRank(processList, p, Comparator.comparingDouble(ProcessRecord::getRamUsage).reversed()));
        }

        snapshot = new AnalyticsSnapshot(
                timeByCategory,
                processList,
                totalTime,
                processesByCategory,
                top10PerCategory,
                totalTimePerCategory,
                cpuRankPerPid,
                ramRankPerPid
        );
    }

    private int getRank(Collection<ProcessRecord> all,
                        ProcessRecord target,
                        Comparator<ProcessRecord> comparator) {

        List<ProcessRecord> sorted = all.stream()
                .sorted(comparator)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPid() == target.getPid()) {
                return i + 1;
            }
        }
        return -1;
    }
}