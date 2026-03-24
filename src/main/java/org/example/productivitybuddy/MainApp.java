package org.example.productivitybuddy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.productivitybuddy.controller.MainController;
import org.example.productivitybuddy.model.ProcessRegistry;
import org.example.productivitybuddy.scanner.ProcessScannerThread;

public class MainApp extends Application {

    private ProcessScannerThread scannerThread;
    private final ProcessRegistry registry = new ProcessRegistry();

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

        scannerThread = new ProcessScannerThread(registry);
        scannerThread.setDaemon(true);
        scannerThread.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (scannerThread != null && scannerThread.isAlive()) {
            scannerThread.interrupt();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}