package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.productivitybuddy.model.CategoryStats;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRecord;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    @FXML public TableView<CategoryStats> categoryTable;
    @FXML public TableColumn<CategoryStats, String> categoryNameColumn;
    @FXML public TableColumn<CategoryStats, String> categoryTimeColumn;

    @FXML private TableView<ProcessRecord> processTable;
    @FXML private TableColumn<ProcessRecord, Integer> pidColumn;
    @FXML private TableColumn<ProcessRecord, String> nameColumn;
    @FXML private TableColumn<ProcessRecord, Double> cpuColumn;
    @FXML private TableColumn<ProcessRecord, Double> ramColumn;
    @FXML private TableColumn<ProcessRecord, String> categoryColumn;
    @FXML private TableColumn<ProcessRecord, Double> totalTimeColumn;

    @FXML private PieChart categoryChart;

    private final ObservableList<ProcessRecord> processList = FXCollections.observableArrayList();

    private final Map<ProcessCategory, PieChart.Data> pieMap = new HashMap<>();
    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

    private final ObservableList<CategoryStats> categoryStatsList = FXCollections.observableArrayList();

    private ProcessRegistry registry;

    public void setRegistry(ProcessRegistry registry) {
        this.registry = registry;
        startUIUpdater();
    }

    @FXML
    public void initialize() {
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("originalName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        totalTimeColumn.setCellValueFactory(cellData -> {
            long totalMs = cellData.getValue().getTotalTimeMilliseconds();
            double seconds = totalMs / 1000.0f;
            return new ReadOnlyObjectWrapper<Double>(seconds);
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


        categoryNameColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getCategory().toString())
        );
        categoryTimeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getTimeFormatted())
        );


        processTable.setItems(processList);
        categoryChart.setData(pieData);
        categoryTable.setItems(categoryStatsList);
    }

    private void startUIUpdater() {
        Thread uiUpdater = new Thread(() -> {
            while (true) {
                if (registry != null) {
                    Collection<ProcessRecord> processes = registry.getAllProcesses();

                    Platform.runLater(() -> {
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
                        updatePieChart();
                    });
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        uiUpdater.setDaemon(true);
        uiUpdater.start();
    }

    private void updatePieChart() {
        Map<ProcessCategory, Long> timeByCategory = processList.stream()
                .collect(Collectors.groupingBy(
                        ProcessRecord::getCategory,
                        Collectors.summingLong(ProcessRecord::getTotalTimeMilliseconds)
                ));

        long totalTime = timeByCategory.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        if (totalTime == 0) return;

        categoryStatsList.clear();
        for (var entry : timeByCategory.entrySet()) {
            ProcessCategory category = entry.getKey();
            long categoryTime = entry.getValue();

            PieChart.Data data = pieMap.get(category);

            if (data == null) {
                data = new PieChart.Data(category.toString(), categoryTime);
                pieMap.put(category, data);
                pieData.add(data);
            } else {
                data.setPieValue(categoryTime);
            }

            categoryStatsList.add(new CategoryStats(category, categoryTime));
        }

        pieMap.keySet().removeIf(category -> {
            if (!timeByCategory.containsKey(category)) {
                PieChart.Data data = pieMap.get(category);
                pieData.remove(data);
                return true;
            }
            return false;
        });
    }

    @FXML
    private void onSave() {
        System.out.println("Save clicked");
    }

    @FXML
    private void onLoad() {
        System.out.println("Load clicked");
    }

    @FXML
    private void onShutdown() {
        System.exit(0);
    }
}
