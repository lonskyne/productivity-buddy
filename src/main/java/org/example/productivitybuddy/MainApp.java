package org.example.productivitybuddy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.productivitybuddy.controller.MainController;
import org.example.productivitybuddy.model.ProcessRegistry;
import org.example.productivitybuddy.scanner.ScheduledScanner;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainApp extends Application {

    private final ProcessRegistry registry = new ProcessRegistry();
    private ScheduledExecutorService scheduler;
    private ForkJoinPool forkJoinPool;   // managed here

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
        controller.setRegistry(registry);

        forkJoinPool = new ForkJoinPool();
        startScheduledScanner();
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
                registry.REFRESH_MILLISECONDS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() throws Exception {
        // Shutdown the scheduler first
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown the ForkJoinPool – optional but good practice
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdown();
            try {
                if (!forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    forkJoinPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                forkJoinPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}