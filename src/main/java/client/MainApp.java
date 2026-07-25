package client;

import client.chat.ChatStore;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

public class MainApp extends Application {
    private static MainApp instance;
    private Stage primaryStage;
    private String pageTitle = "Sign in";

    public static MainApp getInstance() { return instance; }

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        primaryStage = stage;
        AppFonts.load();
        // Load chat state on the JavaFX thread so legacy impersonated auto-replies
        // are cleaned even before the user opens Chat.
        ChatStore.getInstance();

        NavigationManager.setStage(stage);
        refreshWindowTitle();
        Timeline titleRefresh = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshWindowTitle()));
        titleRefresh.setCycleCount(Timeline.INDEFINITE);
        titleRefresh.play();

        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/x-logo-icon.png")));
        } catch (Exception exception) {
            System.err.println("Could not load application icon: " + exception.getMessage());
        }

        NavigationManager.switchScene("/views/Login.fxml");
        stage.show();
        stage.toFront();
        stage.requestFocus();
        if (UserSession.getInstance().hasSavedSession()) {
            Thread restore = new Thread(() -> {
                if (UserSession.getInstance().restoreSavedSession()) {
                    Platform.runLater(() -> NavigationManager.switchScene("/views/Feed.fxml"));
                }
            }, "x-session-restore");
            restore.setDaemon(true);
            restore.start();
        }
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle == null || pageTitle.isBlank() ? "X" : pageTitle;
        refreshWindowTitle();
    }

    private void refreshWindowTitle() {
        if (primaryStage == null) return;
        String username = UserSession.getInstance().getUsername();
        int unread = username == null ? 0 : client.timeline.PostStore.getInstance()
                .getUnreadNotificationCount(username);
        primaryStage.setTitle(formatWindowTitle(pageTitle, unread));
    }

    public static String formatWindowTitle(String pageTitle, int unread) {
        String page = pageTitle == null || pageTitle.isBlank() ? "X" : pageTitle;
        return (unread > 0 ? "(" + unread + ") " : "") + page + " / X";
    }
}
