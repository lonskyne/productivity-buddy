package org.example.productivitybuddy.scanner;

import oshi.software.os.OSProcess;
import org.example.productivitybuddy.model.ProcessRegistry;
import org.example.productivitybuddy.model.ProcessRecord;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class ProcessScannerTask extends RecursiveAction {
    private static final int THRESHOLD = 10;
    private final List<OSProcess> processes;
    private final ProcessRegistry registry;

    public ProcessScannerTask(List<OSProcess> processes, ProcessRegistry registry) {
        this.processes = processes;
        this.registry = registry;
    }

    @Override
    protected void compute() {
        if (processes.size() <= THRESHOLD) {
            for (OSProcess osProcess : processes) {
                long totalTicks = osProcess.getUserTime() + osProcess.getKernelTime();

                ProcessRecord record = new ProcessRecord(
                        osProcess.getProcessID(),
                        osProcess.getName(),
                        osProcess.getResidentSetSize(),
                        totalTicks
                );

                ProcessRecord existing = registry.findAnyByOriginalName(record.getOriginalName());

                if (existing != null) {
                    record.setCategory(existing.getCategory());
                    record.setAliasName(existing.getAliasName());
                    record.setIsTrackingFrozen(existing.getIsTrackingFrozen());
                    record.setTotalTimeMilliseconds(existing.getTotalTimeMilliseconds());
                }

                registry.updateProcess(record);
            }
        } else {
            int mid = processes.size() / 2;
            ProcessScannerTask left = new ProcessScannerTask(processes.subList(0, mid), registry);
            ProcessScannerTask right = new ProcessScannerTask(processes.subList(mid, processes.size()), registry);
            invokeAll(left, right);
        }
    }
}