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
            synchronized (oldP) {
                if(!oldP.getIsTrackingFrozen()) {
                    long prevTicks = oldP.getPreviousTotalCpuTicks();
                    long prevTimestamp = oldP.getLastSeenTimestamp();
                    long systemTimeMillis = System.currentTimeMillis();
                    double cpuUsage = oldP.getCpuUsage();

                    if (prevTicks > 0 && prevTimestamp > 0) {
                        long deltaTicks = newP.getTotalTicks() - prevTicks;
                        long deltaTime = systemTimeMillis - prevTimestamp;

                        if (deltaTime > 0) {
                            cpuUsage = (deltaTicks / (double) deltaTime) * 100.0;
                        }
                    }

                    oldP.setSessionTimeMilliseconds(oldP.getSessionTimeMilliseconds() + MyConfig.MONITOR_INTERVAL.get());
                    oldP.setLastSeenTimestamp(systemTimeMillis);
                    oldP.setPreviousTotalCpuTicks(newP.getTotalTicks());
                    oldP.setRamUsage(newP.getRamUsage());
                    oldP.setCpuUsage(cpuUsage);
                }
            }

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

    public void killProcess(ProcessRecord process) {
        ProcessHandle.of(process.getPid()).ifPresent(ProcessHandle::destroy);
    }

    public void renameProcess(ProcessRecord process, String newName) {
        String oldName = process.getAliasName();

        Object lock = getLockForName(oldName);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (oldName.equals(p.getAliasName())) {
                    synchronized (p) {
                        p.setAliasName(newName);
                    }
                }
            }
        }
    }

    public void toggleFreezeProcess(ProcessRecord process) {
        String name = process.getAliasName();

        Object lock = getLockForName(name);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (name.equals(p.getAliasName())) {
                    synchronized (p) {
                        p.toggleIsTrackingFrozen();
                    }
                }
            }
        }
    }

    public void changeProcessCategory(ProcessRecord process, ProcessCategory newCategory) {
        String name = process.getAliasName();

        Object lock = getLockForName(name);

        synchronized (lock) {
            for (ProcessRecord p : processes.values()) {
                if (name.equals(p.getAliasName())) {
                    synchronized (p) {
                        p.setCategory(newCategory);
                    }
                }
            }
        }
    }

    public List<ProcessRecord> getProcessesByCategory(ProcessCategory category) {
        return processes.values().stream()
                .filter(p -> category.equals(p.getCategory()))
                .toList();
    }

    private Object getLockForName(String name) {
        return nameLocks.computeIfAbsent(name, k -> new Object());
    }

    public void applyLoadedState(ProcessInfoDTO dto) {
        processes.values().forEach(p -> {
            if (p.getOriginalName().equals(dto.originalName)) {
                p.setAliasName(dto.aliasName);
                p.setCategory(ProcessCategory.valueOf(dto.category));
                p.setIsTrackingFrozen(dto.isTrackingFreezed);
                p.setStartTimeMilliseconds(dto.totalTimeSeconds * 1000);
                p.setSessionTimeMilliseconds(0);
            }
        });
    }
}
