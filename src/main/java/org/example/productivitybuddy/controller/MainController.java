package org.example.productivitybuddy.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.example.productivitybuddy.model.CategoryStats;
import org.example.productivitybuddy.model.ProcessCategory;
import org.example.productivitybuddy.model.ProcessRecord;
import org.example.productivitybuddy.model.ProcessRegistry;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    public final int UI_REFRESH_MILLIS = 500;

    @FXML public StackPane rightPane;
    public BorderPane root;
    private Node pieView;
    private Node mainView;

    @FXML public TableView<CategoryStats> categoryTable;
    @FXML public TableColumn<CategoryStats, ProcessCategory> categoryNameColumn;
    @FXML public TableColumn<CategoryStats, String> categoryTimeColumn;

    @FXML private TableView<ProcessRecord> processTable;
    @FXML private TableColumn<ProcessRecord, Integer> pidColumn;
    @FXML private TableColumn<ProcessRecord, String> nameColumn;
    @FXML private TableColumn<ProcessRecord, ProcessCategory> categoryColumn;

    @FXML private PieChart categoryChart;

    private final ObservableList<ProcessRecord> processList = FXCollections.observableArrayList();

    private final Map<ProcessCategory, PieChart.Data> pieMap = new HashMap<>();
    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

    private final ObservableList<CategoryStats> categoryStatsList = FXCollections.observableArrayList();

    private ProcessRegistry registry;

    private ProcessDetailController processDetailController;
    private CategoryDetailController categoryDetailController;

    public void setRegistry(ProcessRegistry registry) {
        this.registry = registry;
        startUIUpdater();
    }

    @FXML
    public void initialize() {
        this.processDetailController = null;
        this.categoryDetailController = null;
        this.pieView = rightPane.getChildren().getFirst();
        this.mainView = root.getCenter();

        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("aliasName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));



        nameColumn.setCellFactory(col -> {
            TableCell<ProcessRecord, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getClickCount() == 2) {
                    ProcessRecord process = cell.getTableView()
                            .getItems()
                            .get(cell.getIndex());
                    showProcessDetail(process);
                }
            });

            return cell;
        });

        categoryColumn.setCellFactory(col -> {
            TableCell<ProcessRecord, ProcessCategory> cell = new TableCell<>() {
                @Override
                protected void updateItem(ProcessCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.toString());
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty() && event.getClickCount() == 2) {
                    ProcessCategory category = cell.getTableView()
                            .getItems()
                            .get(cell.getIndex()).getCategory();
                    showCategoryDetail(category);
                }
            });

            return cell;
        });

        categoryNameColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getCategory())
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
                    if(categoryDetailController != null) {
                        categoryDetailController.fetchProcesses();
                    }

                    Platform.runLater(() -> {
                        updateProcessTable(processes);
                        updatePieChart();

                        if(processDetailController != null) {
                            processDetailController.updateDetailView();
                        }

                        if(categoryDetailController != null) {
                            categoryDetailController.updateDetailView();
                        }
                    });
                }

                try {
                    Thread.sleep(UI_REFRESH_MILLIS);
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

    private void showProcessDetail(ProcessRecord process) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/productivitybuddy/process_detail-view.fxml")
            );

            Parent detailRoot = loader.load();

            processDetailController = loader.getController();
            processDetailController.setProcess(process, registry);
            processDetailController.setOnBack(() -> {
                processDetailController = null;
                rightPane.getChildren().setAll(pieView);
            });

            rightPane.getChildren().setAll(detailRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCategoryDetail(ProcessCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/productivitybuddy/category_detail-view.fxml"));
            Parent view = loader.load();

            categoryDetailController = loader.getController();
            categoryDetailController.initialize(registry, category);

            categoryDetailController.setOnBack(() -> {
                categoryDetailController = null;
                root.setCenter(mainView);
            });

            root.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
