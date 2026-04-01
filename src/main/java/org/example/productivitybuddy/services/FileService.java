package org.example.productivitybuddy.services;

import javafx.application.Platform;
import org.example.productivitybuddy.dto.ProcessInfoDTO;
import org.example.productivitybuddy.dto.ProcessInfoWrapper;
import org.example.productivitybuddy.model.AnalyticsSnapshot;
import org.example.productivitybuddy.model.MyConfig;
import org.example.productivitybuddy.model.ProcessRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.example.productivitybuddy.util.JsonHandler.readJson;
import static org.example.productivitybuddy.util.JsonHandler.writeJson;


public class FileService implements SnapshotListener {
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AnalyticsService analyticsService;
    private final WatcherService watcher;

    public FileService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.executor = Executors.newFixedThreadPool(2);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.watcher = new WatcherService(executor);
        this.analyticsService.addSnapshotListener(this);
    }

    @Override
    public void onSnapshot(AnalyticsSnapshot snapshot, LocalTime time) {
        System.out.println("HEARD SNAPSHOT");
        submitSnapshotTask();
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void startSnapshots() {
        scheduler.scheduleAtFixedRate(this::submitSnapshotTask, 0, MyConfig.SNAPSHOT_INTERVAL.get(), TimeUnit.SECONDS);
    }

    private final AtomicLong lastSnapshotMillis = new AtomicLong(0);

    private void submitSnapshotTask() {
        long currentMillis = System.currentTimeMillis();

        boolean shouldSubmit = lastSnapshotMillis.getAndUpdate(prev -> Math.max(prev, currentMillis)) < currentMillis;

        if (!shouldSubmit) {
            System.out.println("SHOULD NOT SUBMIT");
            return;
        }

        Path path = Path.of("snapshot_" + currentMillis + ".csv");
        System.out.println("SUBMITTING TASK TO " + path);
        executor.submit(new SnapshotTask(analyticsService.getSnapshot(), path));
    }

    public void saveAsync(Collection<ProcessRecord> processes, Path path) {
        executor.submit(() -> {
            try {
                Map<String, ProcessRecord> uniqueByName = processes.stream()
                        .collect(Collectors.toMap(
                                ProcessRecord::getOriginalName,
                                p -> p,
                                (p1, p2) -> p1
                        ));

                ProcessInfoWrapper wrapper = new ProcessInfoWrapper();
                wrapper.processes = uniqueByName.values().stream().map(p -> {
                    ProcessInfoDTO dto = new ProcessInfoDTO();
                    dto.originalName = p.getOriginalName();
                    dto.aliasName = p.getAliasName();
                    dto.category = p.getCategory().toString();
                    dto.isTrackingFreezed = p.getIsTrackingFrozen();
                    dto.totalTimeSeconds = (p.getSessionTimeMilliseconds() + p.getStartTimeMilliseconds()) / 1000;
                    return dto;
                }).toList();

                watcher.isInternalWrite.set(true);
                writeJson(wrapper, path);
                watcher.isInternalWrite.set(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void loadAsync(Path path, Consumer<ProcessInfoWrapper> onLoaded) {
        executor.submit(() -> {
            try {
                ProcessInfoWrapper wrapper = readJson(path);

                Platform.runLater(() -> onLoaded.accept(wrapper));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Future<?> shutdownSaveAsync(
            Collection<ProcessRecord> processes,
            Path path
    ) {
        return executor.submit(() -> {
            try {
                Map<String, ProcessRecord> uniqueByName = processes.stream()
                        .collect(Collectors.toMap(
                                ProcessRecord::getOriginalName,
                                p -> p,
                                (p1, p2) -> p1
                        ));

                ProcessInfoWrapper existing;

                if (Files.exists(path)) {
                    existing = readJson(path);
                } else {
                    existing = new ProcessInfoWrapper();
                    existing.processes = new ArrayList<>();
                }

                Map<String, ProcessInfoDTO> map = new HashMap<>();

                // existing data
                for (ProcessInfoDTO dto : existing.processes) {
                    map.put(dto.originalName, dto);
                }

                for (ProcessRecord p : uniqueByName.values()) {
                    ProcessInfoDTO dto = map.getOrDefault(p.getOriginalName(), new ProcessInfoDTO());

                    dto.originalName = p.getOriginalName();
                    dto.aliasName = p.getAliasName();
                    dto.category = p.getCategory().toString();
                    dto.isTrackingFreezed = p.getIsTrackingFrozen();
                    dto.totalTimeSeconds = (p.getSessionTimeMilliseconds() + p.getStartTimeMilliseconds()) / 1000;;

                    map.put(dto.originalName, dto);
                }

                ProcessInfoWrapper result = new ProcessInfoWrapper();
                result.processes = new ArrayList<>(map.values());

                writeJson(result, path);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void startWatching(Path path, Consumer<Path> onChange) {
        watcher.startWatching(path, onChange);
    }

    public void stopWatching() {
        watcher.stopWatching();
    }
}
