package client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class NavigationManager {

    private static Stage primaryStage;

    /**
     * Initializes the manager with the main application window stage context.
     */
    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Dynamically switches the visible viewport to the requested FXML path location.
     * @param fxmlPath The resource path string pointing to the targeted view layout.
     */
    public static void switchScene(String fxmlPath) {
        try {
            // Load the target architectural design layout dynamically
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            VBox rootContainer = loader.load();

            // Re-bind the window scene content view tree context
            Scene newScene = new Scene(rootContainer);
            primaryStage.setScene(newScene);

        } catch (IOException e) {
            System.err.println("Navigation Execution Fault: Unable to load view context -> " + fxmlPath);
            e.printStackTrace();
        }
    }
}
