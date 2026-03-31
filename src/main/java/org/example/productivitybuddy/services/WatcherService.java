package org.example.productivitybuddy.services;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WatcherService {
    private final ExecutorService executor;
    private WatchService watchService;
    private volatile boolean running = false;
    public final AtomicBoolean isInternalWrite;

    public WatcherService(ExecutorService executor) {
        this.executor = executor;
        isInternalWrite = new AtomicBoolean(false);
    }

    public void startWatching(Path filePath, Consumer<Path> onChange) {
        stopWatching();

        running = true;

        executor.submit(() -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();

                Path dir = filePath.getParent();
                dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                String fileName = filePath.getFileName().toString();

                while (running) {
                    WatchKey key = watchService.take(); // blocking

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();

                        if (changed.toString().equals(fileName)) {
                            //debounce
                            Thread.sleep(100);

                            if (!isInternalWrite.get()) {
                                onChange.accept(filePath);
                            }
                        }
                    }

                    key.reset();
                }

            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        });
    }

    public void stopWatching() {
        running = false;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
    }
}
