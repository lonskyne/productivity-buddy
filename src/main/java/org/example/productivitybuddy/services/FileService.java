package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.ProcessRecord;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FileService {
    private final int SNAPSHOT_SECONDS = 10;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final AnalyticsService analyticsService;

    public FileService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.executor = Executors.newFixedThreadPool(2);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
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

    public void startSnapshots(Supplier<Collection<ProcessRecord>> supplier) {
        scheduler.scheduleAtFixedRate(() -> {
            Path path = Path.of("snapshot_" + System.currentTimeMillis() + ".csv");

            executor.submit(new SnapshotTask(analyticsService.getSnapshot(), path));

        }, 0, SNAPSHOT_SECONDS, TimeUnit.SECONDS);
    }
}
