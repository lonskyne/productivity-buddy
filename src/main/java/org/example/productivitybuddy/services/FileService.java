package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.AnalyticsSnapshot;
import org.example.productivitybuddy.model.MyConfig;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class FileService implements SnapshotListener {
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AnalyticsService analyticsService;

    public FileService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.executor = Executors.newFixedThreadPool(2);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onSnapshot(AnalyticsSnapshot snapshot, LocalTime time) {
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

        boolean shouldSubmit = lastSnapshotMillis.updateAndGet(prev -> Math.max(prev, currentMillis)) < currentMillis;

        if (!shouldSubmit) {
            return;
        }

        Path path = Path.of("snapshot_" + currentMillis + ".csv");
        executor.submit(new SnapshotTask(analyticsService.getSnapshot(), path));
    }

    public void save() {

    }

    public void load() {

    }
}
