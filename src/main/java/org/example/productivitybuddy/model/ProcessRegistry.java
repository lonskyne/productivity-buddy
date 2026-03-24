package org.example.productivitybuddy.model;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessRegistry {
    public final int REFRESH_MILLISECONDS = 5000;
    private final ConcurrentHashMap<Integer, ProcessRecord> processes = new ConcurrentHashMap<>();

    public void updateProcess(ProcessRecord newProcess) {
        processes.merge(newProcess.getPid(), newProcess, (oldP, newP) -> {
            if (!oldP.isTrackingFrozen()) {
                oldP.setTotalTimeMilliseconds(oldP.getTotalTimeMilliseconds() + System.currentTimeMillis() - oldP.getLastSeenTimestamp());
                oldP.setLastSeenTimestamp(System.currentTimeMillis());
            }

            return oldP;
        });
    }

    public Collection<ProcessRecord> getAllProcesses() {
        return processes.values();
    }

    public void retainOnly(Set<Integer> pids) {
        processes.keySet().retainAll(pids);
    }
}
