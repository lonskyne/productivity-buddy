package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.productivitybuddy.model.ProcessRecord;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.util.Collection;

public class MainController {


    @FXML private TableView<ProcessRecord> processTable;
    @FXML private TableColumn<ProcessRecord, Integer> pidColumn;
    @FXML private TableColumn<ProcessRecord, String> nameColumn;
    @FXML private TableColumn<ProcessRecord, Double> cpuColumn;
    @FXML private TableColumn<ProcessRecord, Double> ramColumn;
    @FXML private TableColumn<ProcessRecord, String> categoryColumn;
    @FXML private TableColumn<ProcessRecord, Double> totalTimeColumn;

    @FXML private PieChart categoryChart;

    private final ObservableList<ProcessRecord> processList = FXCollections.observableArrayList();

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

        processTable.setItems(processList);
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
