package client.controllers;

import client.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

public class profileController {

    @FXML
    private VBox userTweetsContainer;

    @FXML
    public void initialize() {
        // Load only the tweets belonging to this specific user profile context
        loadUserOwnTweets();
    }

    /**
     * Simulates fetching personal historical posts from the local storage/network buffer.
     */
    private void loadUserOwnTweets() {
        List<String> personalPosts = new ArrayList<>();
        personalPosts.add("Just deployed the new centralized Navigation Pipeline! Everything feels smooth. #JavaFX #XClone");
        personalPosts.add("Designing atomic layouts with inline CSS components is highly efficient for dark themes.");

        for (String content : personalPosts) {
            renderPersonalTweetCard(content);
        }
    }

    /**
     * Programmatically injects high-fidelity personal tweet rows into the profile feed.
     */
    private void renderPersonalTweetCard(String textContent) {
        HBox tweetRow = new HBox(12);
        tweetRow.setStyle("-fx-border-color: #333333; -fx-border-width: 0 0 1 0; -fx-padding: 12 16 12 16;");

        VBox avatarBox = new VBox();
        Label avatar = new Label("👤");
        avatar.setFont(Font.font("System", 24));
        avatar.setTextFill(Color.web("#71767b"));
        avatarBox.getChildren().add(avatar);

        VBox contentStack = new VBox(4);
        HBox headerRow = new HBox(8);

        Label displayName = new Label("Sample");
        displayName.setFont(Font.font("System", FontWeight.BOLD, 15));
        displayName.setTextFill(Color.WHITE);

        Label userHandle = new Label("@developer");
        userHandle.setFont(Font.font("System", 14));
        userHandle.setTextFill(Color.web("#71767b"));

        Label timestamp = new Label("· 1d");
        timestamp.setFont(Font.font("System", 14));
        timestamp.setTextFill(Color.web("#71767b"));

        headerRow.getChildren().addAll(displayName, userHandle, timestamp);

        Label bodyText = new Label(textContent);
        bodyText.setFont(Font.font("System", 15));
        bodyText.setTextFill(Color.web("#e7e9ea"));
        bodyText.setWrapText(true);
        bodyText.setMaxWidth(420);

        contentStack.getChildren().addAll(headerRow, bodyText);
        tweetRow.getChildren().addAll(avatarBox, contentStack);

        userTweetsContainer.getChildren().add(tweetRow);
    }

    /**
     * Reroutes view state context back to the central Home Timeline Viewport.
     */
    @FXML
    private void handleGoToHome() {
        NavigationManager.switchScene("/views/feed.fxml");
    }

    /**
     * Invalidates active user context and rolls back stage state to login view.
     */
    @FXML
    private void handleLogout() {
        NavigationManager.switchScene("/views/login.fxml");
    }
}