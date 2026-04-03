package org.example.productivitybuddy.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import org.example.productivitybuddy.model.AnalyticsSnapshot;
import org.example.productivitybuddy.model.ProcessSnapshot;
import org.example.productivitybuddy.services.AnalyticsService;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Optional;

public class ProcessDetailController {

    @FXML private Button freezeButton;
    @FXML private Label nameLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label cpuLabel;
    @FXML private Label ramLabel;
    @FXML private Label cpuRankLabel;
    @FXML private Label ramRankLabel;

    private int processPid;
    private ProcessSnapshot process;
    private Runnable onBack;
    private AnalyticsService analyticsService;
    private ProcessRegistry registry;

    public void setProcess(int pid, ProcessRegistry registry, AnalyticsService analyticsService) {
        this.processPid = pid;
        this.analyticsService = analyticsService;
        this.registry = registry;
    }


    public void updateDetailView(AnalyticsSnapshot snapshot) {
        this.process = snapshot.getProcessByPid(processPid);

        if(this.process == null) {
            onBack();
        }

        nameLabel.setText(process.getAliasName());

        totalTimeLabel.setText(process.getTimeFormatted());

        cpuLabel.setText(String.format("%.2f%%", process.getCpuUsage()));

        double ramMB = process.getRamUsage() / (1024 * 1024.0);
        ramLabel.setText(String.format("%.0f MB", ramMB));

        // Rankings
        Integer cpuRank = analyticsService.getSnapshot().getCpuRankByPid(process.getPid());
        Integer ramRank = analyticsService.getSnapshot().getRamRankByPid(process.getPid());

        if(cpuRank == null || ramRank == null) {
            onBack();
            return;
        }

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
        registry.killProcess(process.getPid());
        onBack.run();
    }

    @FXML private void onFreeze() {
        registry.toggleFreezeProcess(process.getOriginalName());

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
            registry.renameProcess(process.getOriginalName(), newName);
        });
    }

    @FXML private void onChangeCategory() {
        ChoiceDialog<ProcessCategory> dialog = new ChoiceDialog<>(ProcessCategory.OTHER, ProcessCategory.values());
        dialog.setTitle("Change Category");
        dialog.setHeaderText("Select category:");
        dialog.setContentText("Category:");

        Optional<ProcessCategory> result = dialog.showAndWait();
        result.ifPresent(category -> {
            registry.changeProcessCategory(process.getOriginalName(), category);
        });
    }
}
