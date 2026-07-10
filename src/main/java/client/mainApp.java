package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class mainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Register the active window stage to the global navigation routing system
        NavigationManager.setStage(stage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/feed.fxml"));
        stage.setScene(new Scene(loader.load(), 380, 480));
        stage.show();

        // Boot directly into the default login layout sequence
        NavigationManager.switchScene("/views/login.fxml");
    }

}