package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    @FXML private TableColumn<ProcessRecord, String> cpuColumn;
    @FXML private TableColumn<ProcessRecord, String> ramColumn;
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
                new SimpleStringProperty(
                        String.format("%.2f", cellData.getValue().getCpuUsage())
                )
        );

        ramColumn.setCellValueFactory(cellData -> {
            double ram = cellData.getValue().getRamUsage();
            String formatted = ram >= 1024*1024*1024
                    ? String.format("%.2f GB", ram / (1024*1024*1024))
                    : String.format("%.0f MB", ram / (1024*1024));

            return new SimpleStringProperty(formatted);
        });


        processTable.setItems(processList);
    }

    private void startUIUpdater() {
        Thread uiUpdater = new Thread(() -> {
            while (true) {
                if (registry != null) {
                    Collection<ProcessRecord> processes = registry.getAllProcesses();

                    Platform.runLater(() -> {
                        processList.setAll(processes);
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
