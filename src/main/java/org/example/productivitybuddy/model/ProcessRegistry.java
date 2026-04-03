package org.example.productivitybuddy.model;

import org.example.productivitybuddy.dto.ProcessInfoDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessRegistry {
    private final ConcurrentHashMap<Integer, ProcessRecord> processes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> nameLocks = new ConcurrentHashMap<>();

    public void updateProcess(ProcessRecord newProcess) {
        processes.merge(newProcess.getPid(), newProcess, (oldP, newP) -> {
            long systemTimeMillis = System.currentTimeMillis();

            if(!oldP.getIsTrackingFrozen()) {
                long prevTicks = oldP.getPreviousTotalCpuTicks();
                long prevTimestamp = oldP.getLastSeenTimestamp();
                long deltaTime = systemTimeMillis - prevTimestamp;
                double cpuUsage = oldP.getCpuUsage();

                if (prevTicks > 0 && prevTimestamp > 0) {
                    long deltaTicks = newP.getTotalTicks() - prevTicks;


                    if (deltaTime > 0) {
                        cpuUsage = (deltaTicks / (double) deltaTime) * 100.0;
                    }
                }

                oldP.setSessionTimeMilliseconds(oldP.getSessionTimeMilliseconds() + deltaTime);
                oldP.setPreviousTotalCpuTicks(newP.getTotalTicks());
                oldP.setRamUsage(newP.getRamUsage());
                oldP.setCpuUsage(cpuUsage);
            }
            oldP.setLastSeenTimestamp(systemTimeMillis);

            return oldP;
        });
    }

    public ProcessRecord findAnyByOriginalName(String originalName) {
        return processes.values().stream()
                .filter(p -> originalName.equals(p.getOriginalName()))
                .findAny()
                .orElse(null);
    }

    public Collection<ProcessRecord> getAllProcesses() {
        return processes.values();
    }

    public void retainOnly(Set<Integer> pids) {
        processes.keySet().retainAll(pids);
    }

    public void killProcess(int pid) {
        ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
    }

    public void renameProcess(String originalName, String newName) {
        Object lock = getLockForName(originalName);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (originalName.equals(p.getOriginalName())) {
                    p.setAliasName(newName);
                }
            }
        }
    }

    public void toggleFreezeProcess(String originalName) {
        Object lock = getLockForName(originalName);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (originalName.equals(p.getOriginalName())) {
                    p.toggleIsTrackingFrozen();
                }
            }
        }
    }

    public void changeProcessCategory(String originalName, ProcessCategory newCategory) {
        Object lock = getLockForName(originalName);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (originalName.equals(p.getOriginalName())) {
                    p.setCategory(newCategory);
                }
            }
        }
    }

    public Object getLockForName(String name) {
        return nameLocks.computeIfAbsent(name, k -> new Object());
    }

    public void applyLoadedState(ProcessInfoDTO dto, boolean resetSessionTime) {
        processes.values().forEach(p -> {
            if (p.getOriginalName().equals(dto.originalName)) {
                Object lock = getLockForName(p.getOriginalName());

                synchronized (lock) {
                    p.setAliasName(dto.aliasName);
                    p.setCategory(ProcessCategory.valueOf(dto.category));
                    p.setIsTrackingFrozen(dto.isTrackingFreezed);
                    p.setStartTimeMilliseconds(dto.totalTimeSeconds * 1000);

                    if (resetSessionTime) {
                        p.setSessionTimeMilliseconds(0);
                    }
                }
            }
        });
    }
}
