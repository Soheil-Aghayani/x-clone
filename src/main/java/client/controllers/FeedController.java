package client.controllers;

import client.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

import client.UserSession;

public class FeedController {

    @FXML
    private TextArea tweetTextArea;

    @FXML
    private VBox timelineContainer;

    @FXML
    public void initialize() {
        // Clean and load the mock database rows for visual testing
        loadMockTimeline();
    }

    /**
     * Captures content from text-area context and clears the field buffer.
     */
    @FXML
    private void handlePostTweet() {
        String content = tweetTextArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        System.out.println("Mock Pipeline: Staging new tweet -> " + content);
        // Add the user's new tweet to the top of the timeline feed
        renderAdvancedTweetCard(content);
        tweetTextArea.clear();
    }

    /**
     * Generates custom sample datasets for verification.
     */
    private void loadMockTimeline() {
        List<String> mockContents = new ArrayList<>();
        mockContents.add("Just deployed the new centralized Navigation Pipeline! Everything feels smooth. #JavaFX #XClone");
        mockContents.add("Designing atomic layouts with inline CSS components is highly efficient for dark themes.");

        // Loop and isolate render calls
        for (String content : mockContents) {
            renderAdvancedTweetCard(content);
        }
    }

    /**
     * Constructs a high-fidelity X style tweet layout.
     */
    private void renderAdvancedTweetCard(String textContent) {
        // Main horizontal container to isolate profile picture from text context
        HBox tweetRow = new HBox(12);
        tweetRow.setStyle("-fx-border-color: #333333; -fx-border-width: 0 0 1 0; -fx-padding: 12 16 12 16;");

        // Mock Profile Avatar Placeholder Circle or Box Icon
        VBox avatarBox = new VBox();
        Label avatar = new Label("👤");
        avatar.setFont(Font.font("System", 24));
        avatar.setTextFill(Color.web("#71767b"));
        avatarBox.getChildren().add(avatar);

        // Vertical block layout for textual information stack
        VBox contentStack = new VBox(4);

        // Metadata Header Row: Display Name -> Handle -> Timestamp
        HBox headerRow = new HBox(8);

        // Dynamic configuration using the active user session context:
        String activeName = UserSession.getInstance().getDisplayName();
        if (activeName == null) activeName = "Guest";
        Label displayName = new Label(activeName);

        displayName.setTextFill(Color.WHITE);
        displayName.setFont(Font.font("System", FontWeight.BOLD, 15));

        String activeUsername = UserSession.getInstance().getUsername();
        if (activeUsername == null) activeUsername = "developer";
        Label userHandle = new Label("@" + activeUsername);
        userHandle.setTextFill(Color.web("#71767b"));
        userHandle.setFont(Font.font("System", 14));

        Label timestamp = new Label("· 2h");
        timestamp.setTextFill(Color.web("#71767b"));
        timestamp.setFont(Font.font("System", 14));

        headerRow.getChildren().addAll(displayName, userHandle, timestamp);

        // Core Post Text Component
        Label bodyText = new Label(textContent);
        bodyText.setTextFill(Color.web("#e7e9ea"));
        bodyText.setFont(Font.font("System", 15));
        bodyText.setWrapText(true);
        bodyText.setMaxWidth(420);

        // Interaction Action Toolbar Row (Reply, Retweet, Like Mock placeholders)
        HBox actionToolbar = new HBox(40);
        actionToolbar.setStyle("-fx-padding: 6 0 0 0;");

        Label replyIcon = new Label("💬 0");
        replyIcon.setTextFill(Color.web("#71767b"));

        Label repostIcon = new Label("🔁 0");
        repostIcon.setTextFill(Color.web("#71767b"));

        Button likeButton = new Button("❤️ 0");
        likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");

        // Local atomic wrappers to host the current like status properties dynamically
        final boolean[] isLiked = {false};
        final int[] likeCount = {0};

        // Click handler to instantly trigger state transitions on the UI thread without db blockades
        likeButton.setOnAction(event -> {
            if (!isLiked[0]) {
                likeCount[0]++;
                likeButton.setText("❤️ " + likeCount[0]);
                likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #f91880; -fx-padding: 0; -fx-cursor: hand;"); // X pinkish-red heart
                isLiked[0] = true;
            }
            else {
                likeCount[0]--;
                likeButton.setText("🤍 " + likeCount[0]);
                likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;"); // Reset to default grey
                isLiked[0] = false;
            }
        });

        actionToolbar.getChildren().addAll(replyIcon, repostIcon, likeButton);

        // Assemble structural nodes into the stack context
        contentStack.getChildren().addAll(headerRow, bodyText, actionToolbar);
        tweetRow.getChildren().addAll(avatarBox, contentStack);

        // Inject the complete multi-row layout object inside the scrolling container viewport
        timelineContainer.getChildren().add(tweetRow);
    }
    /**
     * Reroutes the application view context to the user's Profile screen layout.
     */
    @FXML
    private void handleGoToProfile() {
        //System.out.println("Switching viewport to Profile view...");
        NavigationManager.switchScene("/views/Profile.fxml");
    }

    /**
     * Terminates session simulation context and rolls back_stage state to Login view.
     */
    @FXML
    private void handleLogout() {
        System.out.println("Invalidating active user context channel. Rerouting to login...");
        // TERMINATING ACTIVE USER SESSION CONTEXT
        UserSession.getInstance().clearSession();
        client.NavigationManager.switchScene("/views/Login.fxml");
    }
}