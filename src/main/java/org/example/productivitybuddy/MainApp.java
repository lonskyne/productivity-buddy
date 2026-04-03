package org.example.productivitybuddy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.productivitybuddy.model.MyConfig;
import org.example.productivitybuddy.services.AnalyticsService;
import org.example.productivitybuddy.controller.MainController;
import org.example.productivitybuddy.model.ProcessRegistry;
import org.example.productivitybuddy.scanner.ScheduledScanner;
import org.example.productivitybuddy.services.FileService;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainApp extends Application {

    private final ProcessRegistry registry = new ProcessRegistry();
    private final AnalyticsService analyticsService = new AnalyticsService(registry);
    private final FileService fileService = new FileService(analyticsService);
    private ScheduledExecutorService scheduler;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/example/productivitybuddy/main-view.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        primaryStage.setTitle("Productivity Buddy");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(600);

        primaryStage.show();

        //--------------------------------------------------

        MainController controller = loader.getController();
        controller.setServicesAndRegistry(registry, analyticsService, fileService);
        controller.setScene(scene);

        startScheduledScanner();
        analyticsService.start();
        fileService.startSnapshots();

        scene.getStylesheets().add(getClass().getResource("/org/example/productivitybuddy/light-theme.css").toExternalForm());
    }

    private void startScheduledScanner() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ScheduledScanner");
            return t;
        });

        ScheduledScanner scannerTask = new ScheduledScanner(registry, forkJoinPool);

        scheduler.scheduleWithFixedDelay(
                scannerTask,
                0,
                MyConfig.MONITOR_INTERVAL.get(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Scheduler did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            try {
                if (!forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("ForkJoinPool did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        super.stop();
    }


    public static void main(String[] args) {
        launch(args);
    }
}