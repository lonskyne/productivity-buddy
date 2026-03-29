package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import org.example.productivitybuddy.analytics.AnalyticsService;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRecord;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ProcessDetailController {

    @FXML private Button freezeButton;
    @FXML private Label nameLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label cpuLabel;
    @FXML private Label ramLabel;
    @FXML private Label cpuRankLabel;
    @FXML private Label ramRankLabel;

    private ProcessRecord process;
    private Collection<ProcessRecord> allProcesses;
    private Runnable onBack;
    private AnalyticsService analyticsService;
    private ProcessRegistry registry;

    public void setProcess(ProcessRecord process, ProcessRegistry registry, AnalyticsService analyticsService) {
        this.process = process;
        this.analyticsService = analyticsService;
        this.registry = registry;
        this.allProcesses = analyticsService.getSnapshot().getAllProcesses();
    }


    public void updateDetailView() {
        nameLabel.setText(process.getAliasName());

        totalTimeLabel.setText(process.getTimeFormatted());

        cpuLabel.setText(String.format("%.2f%%", process.getCpuUsage()));

        double ramMB = process.getRamUsage() / (1024 * 1024.0);
        ramLabel.setText(String.format("%.0f MB", ramMB));

        // Rankings
        int cpuRank = analyticsService.getSnapshot().getCpuRankByPid(process.getPid());
        int ramRank = analyticsService.getSnapshot().getRamRankByPid(process.getPid());

        cpuRankLabel.setText(formatRank(cpuRank));
        ramRankLabel.setText(formatRank(ramRank));

        if(process.getIsTrackingFrozen()) {
            freezeButton.setText("Unfreeze Tracking");
        }
        else {
            freezeButton.setText("Freeze Tracking");
        }
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
        registry.killProcess(process);
        onBack.run();
    }

    @FXML private void onFreeze() {
        registry.toggleFreezeProcess(process);

    }

    @FXML
    private void onRename() {
        TextInputDialog dialog = new TextInputDialog(nameLabel.getText());
        dialog.setTitle("Rename Process");
        dialog.setHeaderText("Enter new name:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            nameLabel.setText(newName);
            registry.renameProcess(process, newName);
        });
    }

    @FXML private void onChangeCategory() {
        ChoiceDialog<ProcessCategory> dialog = new ChoiceDialog<>(ProcessCategory.OTHER, ProcessCategory.values());
        dialog.setTitle("Change Category");
        dialog.setHeaderText("Select category:");
        dialog.setContentText("Category:");

        Optional<ProcessCategory> result = dialog.showAndWait();
        result.ifPresent(category -> {
            registry.changeProcessCategory(process, category);
        });
    }
}
