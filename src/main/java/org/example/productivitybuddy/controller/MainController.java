package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
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
        //cpuColumn.setCellValueFactory(new PropertyValueFactory<>("cpu"));
        //ramColumn.setCellValueFactory(new PropertyValueFactory<>("ram"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        totalTimeColumn.setCellValueFactory(cellData -> {
            long totalMs = cellData.getValue().getTotalTimeMilliseconds();
            double seconds = totalMs / 1000.0f;  // convert to seconds
            return new ReadOnlyObjectWrapper<Double>(seconds);
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
