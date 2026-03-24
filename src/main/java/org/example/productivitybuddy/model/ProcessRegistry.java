package org.example.productivitybuddy.model;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessRegistry {
    public final int REFRESH_MILLISECONDS = 5000;
    private final ConcurrentHashMap<Integer, ProcessRecord> processes = new ConcurrentHashMap<>();

    public void updateProcess(ProcessRecord newProcess) {
        processes.merge(newProcess.getPid(), newProcess, (oldP, newP) -> {
            long prevTicks = oldP.getPreviousTotalCpuTicks();
            long prevTimestamp = oldP.getLastSeenTimestamp();

            if (prevTicks > 0 && prevTimestamp > 0) {
                long deltaTicks = newP.getTotalTicks() - prevTicks;
                long deltaTime = System.currentTimeMillis() - prevTimestamp;

                if (deltaTime > 0) {
                    double cpuPercent = (deltaTicks / (double) deltaTime) * 100.0;
                    oldP.setCpuUsage(cpuPercent);
                }
            }

            oldP.setTotalTimeMilliseconds(oldP.getTotalTimeMilliseconds() + System.currentTimeMillis() - prevTimestamp);
            oldP.setLastSeenTimestamp(System.currentTimeMillis());
            oldP.setPreviousTotalCpuTicks(newP.getTotalTicks());

            return oldP;
        });
    }

    public ProcessRecord getProcess(int pid) {
        return processes.get(pid);
    }

    public ProcessRecord putIfAbsent(int pid, ProcessRecord record) {
        return processes.putIfAbsent(pid, record);
    }

    public Collection<ProcessRecord> getAllProcesses() {
        return processes.values();
    }

    public void retainOnly(Set<Integer> pids) {
        processes.keySet().retainAll(pids);
    }
}
