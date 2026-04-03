package org.example.productivitybuddy.services;

import javafx.application.Platform;
import org.example.productivitybuddy.dto.ProcessInfoDTO;
import org.example.productivitybuddy.dto.ProcessInfoWrapper;
import org.example.productivitybuddy.model.AnalyticsSnapshot;
import org.example.productivitybuddy.model.MyConfig;
import org.example.productivitybuddy.model.ProcessSnapshot;

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
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.watcher = new WatcherService(executor);
        this.analyticsService.addSnapshotListener(this);
    }

    @Override
    public void onSnapshot(AnalyticsSnapshot snapshot, LocalTime time) {
        submitSnapshotTask();
    }

    public void startSnapshots() {
        scheduler.scheduleAtFixedRate(this::submitSnapshotTask, 0, MyConfig.SNAPSHOT_INTERVAL.get(), TimeUnit.SECONDS);
    }

    private final AtomicLong lastSnapshotMillis = new AtomicLong(0);

    private void submitSnapshotTask() {
        long currentMillis = System.currentTimeMillis();

        boolean shouldSubmit = lastSnapshotMillis.getAndUpdate(prev -> Math.max(prev, currentMillis)) < currentMillis;

        if (!shouldSubmit) {
            return;
        }

        Path path = Path.of("snapshot_" + currentMillis + ".csv");
        executor.submit(new SnapshotTask(analyticsService.getSnapshot(), path));
    }

    public void saveAsync(Collection<ProcessSnapshot> processes, Path path) {
        executor.submit(() -> {
            try {
                Map<String, ProcessSnapshot> uniqueByName = processes.stream()
                        .collect(Collectors.toMap(
                                ProcessSnapshot::getOriginalName,
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
                try {
                    writeJson(wrapper, path);
                } finally {
                    watcher.isInternalWrite.set(false);
                }

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
            Collection<ProcessSnapshot> processes,
            Path path
    ) {
        return executor.submit(() -> {
            try {
                Map<String, ProcessSnapshot> uniqueByName = processes.stream()
                        .collect(Collectors.toMap(
                                ProcessSnapshot::getOriginalName,
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

                for (ProcessSnapshot p : uniqueByName.values()) {
                    ProcessInfoDTO dto = map.getOrDefault(p.getOriginalName(), new ProcessInfoDTO());

                    dto.originalName = p.getOriginalName();
                    dto.aliasName = p.getAliasName();
                    dto.category = p.getCategory().toString();
                    dto.isTrackingFreezed = p.getIsTrackingFrozen();
                    dto.totalTimeSeconds = (p.getSessionTimeMilliseconds() + p.getStartTimeMilliseconds()) / 1000;

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

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Scheduler did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void startWatching(Path path, Consumer<Path> onChange) {
        watcher.startWatching(path, onChange);
    }

    public void stopWatching() {
        watcher.stopWatching();
    }
}
