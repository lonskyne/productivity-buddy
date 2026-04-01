package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.AnalyticsSnapshot;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SnapshotTask implements Runnable {

    private final AnalyticsSnapshot snapshot;
    private final Path outputPath;

    public SnapshotTask(AnalyticsSnapshot snapshot, Path outputPath) {
        this.snapshot = snapshot;
        this.outputPath = outputPath;
    }

    @Override
    public void run() {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            System.out.println(outputPath);
            writer.write("TOTAL_TIME_MS," + snapshot.getTotalTime() + "\n\n");
            writer.write("CATEGORY,TOTAL_TIME_MS\n");

            for (var entry : snapshot.getTimeByCategory().entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            writer.write("\n");

            writer.write("PID,Name,Category,CPU(%),RAM(bytes),Time(ms),CPU_Rank,RAM_Rank\n");
            for (ProcessRecord p : snapshot.getAllProcesses()) {
                writer.write(String.format("%d,%s,%s,%.2f,%d,%d,%d,%d\n",
                        p.getPid(),
                        p.getOriginalName(),
                        p.getCategory(),
                        p.getCpuUsage(),
                        p.getRamUsage(),
                        p.getSessionTimeMilliseconds(),
                        snapshot.getCpuRankByPid(p.getPid()),
                        snapshot.getRamRankByPid(p.getPid())
                ));
            }
            writer.write("\n");

            writer.write("TOP_10_PER_CATEGORY\n");
            for (ProcessCategory category : snapshot.getTimeByCategory().keySet()) {
                writer.write("\nCATEGORY: " + category + "\n");
                writer.write("PID,Name,CPU(%),RAM(bytes),Time(ms)\n");

                List<ProcessRecord> topList = snapshot.getTop10ByCategory(category);
                if (topList != null) {
                    for (ProcessRecord p : topList) {
                        writer.write(String.format("%d,%s,%.2f,%d,%d\n",
                                p.getPid(),
                                p.getOriginalName(),
                                p.getCpuUsage(),
                                p.getRamUsage(),
                                p.getSessionTimeMilliseconds()
                        ));
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}