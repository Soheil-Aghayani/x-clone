package client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
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
    public static boolean switchScene(String fxmlPath) {
        try {
            MainApp app = MainApp.getInstance();
            if (app != null) {
                if (fxmlPath.endsWith("Login.fxml")) app.setPageTitle("Sign in");
                else if (fxmlPath.endsWith("Register.fxml")) app.setPageTitle("Join X");
                else if (fxmlPath.endsWith("Profile.fxml")) app.setPageTitle("Profile");
            }
            // Load the target architectural design layout dynamically
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent rootContainer = loader.load();

            boolean authenticationScreen = fxmlPath.endsWith("Login.fxml") || fxmlPath.endsWith("Register.fxml");
            boolean hadScene = primaryStage.getScene() != null;
            boolean wasMainScreen = hadScene && primaryStage.isResizable();
            boolean wasMaximized = primaryStage.isMaximized();
            double previousWidth = primaryStage.getWidth();
            double previousHeight = primaryStage.getHeight();

            // Re-bind the window scene content view tree context
            Scene newScene = new Scene(rootContainer);
            newScene.setFill(authenticationScreen ? Color.BLACK : Color.WHITE);
            primaryStage.setScene(newScene);
            if (authenticationScreen) {
                primaryStage.setMaximized(false);
                primaryStage.setResizable(false);
                primaryStage.setMinWidth(600);
                primaryStage.setMinHeight(700);
                primaryStage.setWidth(600);
                primaryStage.setHeight(700);
                primaryStage.centerOnScreen();
            } else {
                primaryStage.setResizable(true);
                primaryStage.setMinWidth(1100);
                primaryStage.setMinHeight(520);
                if (wasMainScreen && previousWidth >= 1100 && previousHeight >= 520) {
                    primaryStage.setWidth(previousWidth);
                    primaryStage.setHeight(previousHeight);
                    primaryStage.setMaximized(wasMaximized);
                } else {
                    primaryStage.setWidth(1280);
                    primaryStage.setHeight(760);
                    primaryStage.centerOnScreen();
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Navigation Execution Fault: Unable to load view context -> " + fxmlPath);
            e.printStackTrace();
            return false;
        }
    }
}
