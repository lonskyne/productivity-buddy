package org.example.productivitybuddy.scanner;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ScheduledScanner implements Runnable {
    private final ProcessRegistry registry;
    private final ForkJoinPool forkJoinPool;
    private final SystemInfo systemInfo = new SystemInfo();

    public ScheduledScanner(ProcessRegistry registry, ForkJoinPool forkJoinPool) {
        this.registry = registry;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    public void run() {
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            var allProcesses = os.getProcesses();

            ProcessScannerTask rootTask = new ProcessScannerTask(allProcesses, registry);

            forkJoinPool.invoke(rootTask);

            Set<Integer> currentPids = allProcesses.stream()
                    .map(OSProcess::getProcessID)
                    .collect(Collectors.toSet());
            registry.retainOnly(currentPids);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
