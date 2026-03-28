package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.productivitybuddy.model.ProcessRecord;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ProcessDetailController {

    @FXML private Label nameLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label cpuLabel;
    @FXML private Label ramLabel;
    @FXML private Label cpuRankLabel;
    @FXML private Label ramRankLabel;

    private ProcessRecord process;
    private Collection<ProcessRecord> allProcesses;
    private Runnable onBack;

    public void setProcess(ProcessRecord process, Collection<ProcessRecord> allProcesses) {
        this.process = process;
        this.allProcesses = allProcesses;
    }

    private int getRank(Collection<ProcessRecord> all,
                        ProcessRecord target,
                        Comparator<ProcessRecord> comparator) {

        List<ProcessRecord> sorted = all.stream()
                .sorted(comparator)
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getPid() == target.getPid()) {
                return i + 1;
            }
        }
        return -1;
    }


    public void updateDetailView() {
        nameLabel.setText(process.getOriginalName());
        totalTimeLabel.setText(process.getTotalTimeMilliseconds() + "");

        cpuLabel.setText(String.format("%.2f%%", process.getCpuUsage()));

        double ramMB = process.getRamUsage() / (1024 * 1024.0);
        ramLabel.setText(String.format("%.0f MB", ramMB));

        // Rankings
        int cpuRank = getRank(allProcesses, process, Comparator.comparingDouble(ProcessRecord::getCpuUsage).reversed());
        int ramRank = getRank(allProcesses, process, Comparator.comparingDouble(ProcessRecord::getRamUsage).reversed());

        cpuRankLabel.setText(formatRank(cpuRank));
        ramRankLabel.setText(formatRank(ramRank));
    }

    private String formatRank(int rank) {
        if (rank % 10 == 1) return rank + "st";
        if (rank % 10 == 2) return rank + "nd";
        if (rank % 10 == 3) return rank + "rd";
        return rank + "th";
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void onBack() {
        if (onBack != null) onBack.run();
    }

    @FXML private void onKill() {
        System.out.println("Kill process " + process.getPid());
    }

    @FXML private void onRename() {
        System.out.println("Rename process");
    }

    @FXML private void onFreeze() {
        System.out.println("Freeze tracking");
    }

    @FXML private void onChangeCategory() {
        System.out.println("Change category");
    }
}
