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
     * Captures content from text-area context, appends it dynamically to the live UI,
     * and clears the input field buffer.
     */
    @FXML
    private void handlePostTweet() {
        String content = tweetTextArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        // Dynamically inject the newly composed tweet at the very top of the scrollable timeline
        renderAdvancedTweetCard(content, "0s", 0, 0, 0);
        tweetTextArea.clear();
    }

    /**
     * Generates custom sample datasets for verification.
     */
    private void loadMockTimeline() {
        List<String> mockContents = new ArrayList<>();
        mockContents.add("Just deployed the new centralized Navigation Pipeline! Everything feels smooth. #JavaFX #XClone");
        mockContents.add("Designing atomic layouts with inline CSS components is highly efficient for dark themes.");
    }

    /**
     * Constructs a high-fidelity X style tweet layout with dynamic user metadata and fully interactive buttons.
     *
     * @param textContent  The core body text of the tweet
     * @param timeAgo      Realistic timestamp representation (e.g., "now", "2h", "1d")
     * @param initialLikes Starting counter value for the like action button
     * @param initialReplies Starting counter value for the reply action button
     * @param initialRetweets Starting counter value for the retweet action button
     */
    private void renderAdvancedTweetCard(String textContent, String timeAgo, int initialLikes, int initialReplies, int initialRetweets) {
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

        Label timestamp = new Label("· " + timeAgo);
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

        // MENTION BUTTON
        Button replyButton = new Button("💬 " + initialReplies);
        replyButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
        final boolean[] isReplied = {false};
        final int[] replyCount = {initialReplies};
        replyButton.setOnAction(event -> {
            if (!isReplied[0]) {
                replyCount[0]++;
                replyButton.setText("💬 " + replyCount[0]);
                replyButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #1d9bf0; -fx-padding: 0; -fx-cursor: hand;"); // X blue color
                isReplied[0] = true;
            }
            else {
                replyCount[0]--;
                replyButton.setText("💬 " + replyCount[0]);
                replyButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
                isReplied[0] = false;
            }
        });

        // RETWEET BUTTON
        Button retweetButton = new Button("🔁 " + initialRetweets);
        retweetButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
        final boolean[] isRetweeted = {false};
        final int[] retweetCount = {initialRetweets};
        retweetButton.setOnAction(event -> {
            if (!isRetweeted[0]) {
                retweetCount[0]++;
                retweetButton.setText("🔁 " + retweetCount[0]);
                retweetButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ba7c; -fx-padding: 0; -fx-cursor: hand;"); // X green color
                isRetweeted[0] = true;
            }
            else {
                retweetCount[0]--;
                retweetButton.setText("🔁 " + retweetCount[0]);
                retweetButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
                isRetweeted[0] = false;
            }
        });

        // LIKE BUTTON
        Button likeButton = new Button("♡ " + initialLikes);
        likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
        final boolean[] isLiked = {false};
        final int[] likeCount = {initialLikes};
        likeButton.setOnAction(event -> {
            if (!isLiked[0]) {
                likeCount[0]++;
                likeButton.setText("❤ " + likeCount[0]);
                likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #f91880; -fx-padding: 0; -fx-cursor: hand;"); // X pinkish-red heart
                isLiked[0] = true;
            }
            else {
                likeCount[0]--;
                likeButton.setText("♡ " + likeCount[0]);
                likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-padding: 0; -fx-cursor: hand;");
                isLiked[0] = false;
            }
        });

        actionToolbar.getChildren().addAll(replyButton, retweetButton, likeButton);

        // Assemble structural nodes into the stack context
        contentStack.getChildren().addAll(headerRow, bodyText, actionToolbar);
        tweetRow.getChildren().addAll(avatarBox, contentStack);

        // Inject the  object inside the container SO new tweets appear at the top of the viewport
        timelineContainer.getChildren().addFirst(tweetRow);
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