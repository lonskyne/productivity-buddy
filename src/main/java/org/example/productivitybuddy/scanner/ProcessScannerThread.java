package org.example.productivitybuddy.scanner;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ProcessScannerThread extends Thread {
    private final ProcessRegistry registry;
    private final SystemInfo systemInfo = new SystemInfo();
    private final ForkJoinPool pool = new ForkJoinPool();

    public ProcessScannerThread(ProcessRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        OperatingSystem os = systemInfo.getOperatingSystem();

        while (!Thread.currentThread().isInterrupted()) {
            var allProcesses = os.getProcesses();

            pool.invoke(new ProcessScannerTask(allProcesses, registry));

            Set<Integer> currentPids = allProcesses.stream().map(OSProcess::getProcessID).collect(Collectors.toSet());
            registry.retainOnly(currentPids);

            try {
                Thread.sleep(registry.REFRESH_MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
