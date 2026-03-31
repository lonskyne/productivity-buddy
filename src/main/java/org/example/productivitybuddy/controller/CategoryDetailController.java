package org.example.productivitybuddy.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.productivitybuddy.services.AnalyticsService;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRecord;
import org.example.productivitybuddy.util.TimeFormatter;

import java.util.*;
import java.util.stream.Collectors;

public class CategoryDetailController {

    @FXML public TableColumn<ProcessRecord, String> nameColumn;
    @FXML public TableColumn<ProcessRecord, Double> cpuColumn;
    @FXML public TableColumn<ProcessRecord, Double> ramColumn;
    @FXML public TableColumn<ProcessRecord, String> timeColumn;
    @FXML private Label categoryTitle;
    @FXML private Label categorySummary;
    @FXML private TableView<ProcessRecord> processTable;
    @FXML private PieChart pieChart;

    private AnalyticsService analyticsService;
    private ProcessCategory category;
    private List<ProcessRecord> processes;

    private final Map<String, PieChart.Data> pieMap = new HashMap<>();
    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    private final ObservableList<ProcessRecord> processList = FXCollections.observableArrayList();

    private Runnable onBack;

    public void initialize(AnalyticsService analyticsService, ProcessCategory category) {
        this.analyticsService = analyticsService;
        this.category = category;

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasName"));

        timeColumn.setCellValueFactory(cellData -> {
            long totalMs = cellData.getValue().getSessionTimeMilliseconds();
            return new ReadOnlyObjectWrapper<String>(TimeFormatter.formatTime(totalMs));
        });

        cpuColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getCpuUsage()).asObject()
        );

        cpuColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f ", value) + "%");
                }
            }
        });

        ramColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getRamUsage()).asObject()
        );

        ramColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    if (value >= (1024*1024*1024)) {
                        setText(String.format("%.2f GB", value / (1024*1024*1024)));
                    } else {
                        setText(String.format("%.0f MB", value / (1024*1024)));
                    }
                }
            }
        });

        categoryTitle.setText(category.toString());

        processTable.setItems(processList);
        pieChart.setData(pieData);

        fetchProcesses();
        updateDetailView();
    }

    public void fetchProcesses() {
        this.processes = analyticsService.getSnapshot().getProcessesByCategory(category);
    }

    public void updateDetailView() {
        updateProcessTable(processes);
        updatePieChart();
        updateSummary();
    }

    private void updatePieChart() {
        List<ProcessRecord> top10 = analyticsService.getSnapshot().getTop10ByCategory(category);

        Set<String> currentSet = top10.stream()
                .map(ProcessRecord::getOriginalName)
                .collect(Collectors.toSet());

        for (ProcessRecord p : top10) {
            String name = p.getOriginalName();
            long time = p.getSessionTimeMilliseconds();

            PieChart.Data data = pieMap.get(name);

            if (data == null) {
                data = new PieChart.Data(name, time);
                pieMap.put(name, data);
                pieData.add(data);
            } else {
                data.setPieValue(time);
            }
        }

        pieMap.keySet().removeIf(name -> {
            if (!currentSet.contains(name)) {
                PieChart.Data data = pieMap.get(name);
                pieData.remove(data);
                return true;
            }
            return false;
        });
    }

    private void updateProcessTable(Collection<ProcessRecord> processes) {
        // Add/update
        for (ProcessRecord newProc : processes) {
            int index = processList.indexOf(newProc);

            if (index < 0) {
                processList.add(newProc);
            }
        }

        // Remove missing
        processList.removeIf(oldProc ->
                processes.stream().noneMatch(p -> p.getPid() == oldProc.getPid())
        );

        processTable.refresh();
    }

    private void updateSummary() {
        long total = analyticsService.getSnapshot().getTotalTimeByCategory(category);

        categorySummary.setText(category + " total time - " + TimeFormatter.formatTime(total));
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void onBack() {
        if (onBack != null) onBack.run();
    }
}
