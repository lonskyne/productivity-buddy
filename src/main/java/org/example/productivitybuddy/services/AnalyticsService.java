package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.*;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AnalyticsService {
    private final int REFRESH_MILLISECONDS = 1000;

    private final ProcessRegistry registry;

    private volatile AnalyticsSnapshot snapshot;

    private final List<SnapshotListener> listeners = new CopyOnWriteArrayList<>();

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
                    checkAndFireSnapshot();
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

        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public AnalyticsSnapshot getSnapshot() {
        return snapshot;
    }

    private void recompute() {
        List<ProcessSnapshot> processList = registry.getAllProcesses()
                .stream()
                .map(ProcessSnapshot::new)
                .toList();

        Map<String, ProcessSnapshot> uniqueByName = processList.stream()
                .collect(Collectors.toMap(
                        ProcessSnapshot::getOriginalName,
                        p -> p,
                        (p1, p2) -> p1
                ));

        Collection<ProcessSnapshot> uniqueProcesses = uniqueByName.values();

        Map<ProcessCategory, Long> timeByCategory = uniqueProcesses.stream()
                .collect(Collectors.groupingBy(
                        p -> Optional.ofNullable(p.getCategory())
                                .orElse(ProcessCategory.UNCATEGORIZED),
                        Collectors.summingLong(ProcessSnapshot::getSessionTimeMilliseconds)
                ));

        long totalTime = uniqueProcesses.stream()
                .mapToLong(ProcessSnapshot::getSessionTimeMilliseconds)
                .sum();

        HashMap<ProcessCategory, List<ProcessSnapshot>> processesByCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            processesByCategory.put(cat,
                    processList.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .toList()
            );
        }

        HashMap<ProcessCategory, List<ProcessSnapshot>> top10PerCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            top10PerCategory.put(cat,
                    uniqueProcesses.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .sorted(Comparator.comparingLong(ProcessSnapshot::getSessionTimeMilliseconds).reversed())
                            .limit(10)
                            .toList()
            );
        }

        HashMap<ProcessCategory, Long> totalTimePerCategory = new HashMap<>();

        for (ProcessCategory cat : ProcessCategory.values()) {
            totalTimePerCategory.put(cat,
                    uniqueProcesses.stream()
                            .filter(p -> cat.equals(p.getCategory()))
                            .mapToLong(ProcessSnapshot::getSessionTimeMilliseconds)
                            .sum()
            );
        }

        HashMap<Integer, Integer> cpuRankPerPid = new HashMap<>();
        HashMap<Integer, Integer> ramRankPerPid = new HashMap<>();

        for(ProcessSnapshot p : processList) {
            cpuRankPerPid.put(p.getPid(), getRank(processList, p, Comparator.comparingDouble(ProcessSnapshot::getCpuUsage).reversed()));
            ramRankPerPid.put(p.getPid(), getRank(processList, p, Comparator.comparingDouble(ProcessSnapshot::getRamUsage).reversed()));
        }

        Map<Integer, ProcessSnapshot> processesByPid = processList.stream()
                .collect(Collectors.toMap(ProcessSnapshot::getPid, p -> p));

        snapshot = new AnalyticsSnapshot(
                timeByCategory,
                processList,
                totalTime,
                processesByCategory,
                top10PerCategory,
                totalTimePerCategory,
                cpuRankPerPid,
                ramRankPerPid,
                processesByPid
        );
    }

    private int getRank(Collection<ProcessSnapshot> all,
                        ProcessSnapshot target,
                        Comparator<ProcessSnapshot> comparator) {

        List<ProcessSnapshot> sorted = all.stream()
                .sorted(comparator)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPid() == target.getPid()) {
                return i + 1;
            }
        }
        return -1;
    }

    public void addSnapshotListener(SnapshotListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(LocalTime time) {
        for (SnapshotListener listener : listeners) {
            listener.onSnapshot(snapshot, time);
        }
    }

    private void checkAndFireSnapshot() {
        if (MyConfig.SNAPSHOT_FIXED_TIMES.isEmpty() || snapshot == null) return;

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);

        boolean matches = MyConfig.SNAPSHOT_FIXED_TIMES.stream()
                .map(t -> t.truncatedTo(ChronoUnit.SECONDS))
                .anyMatch(now::equals);

        if (matches) {
            notifyListeners(now);
        }
    }
}