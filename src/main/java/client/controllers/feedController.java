package client.controllers;

import client.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import shared.models.Tweet;

import java.util.ArrayList;
import java.util.List;

public class feedController {

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

        Label displayName = new Label("Fatemeh");
        displayName.setTextFill(Color.WHITE);
        displayName.setFont(Font.font("System", FontWeight.BOLD, 15));

        Label userHandle = new Label("@developer");
        userHandle.setTextFill(Color.web("#71767b"));
        userHandle.setFont(Font.font("System", 14));

        Label timestamp = new Label("· 2h");
        timestamp.setTextFill(Color.web("#71767b"));
        timestamp.setFont(Font.font("System", 14));

        headerRow.getChildren().addAll(displayName, userHandle, timestamp);

        // Core Post Text Component
        Label bodyText = new Label(textContent);
        bodyText.setTextFill(Color.web("#e7e9ea")); // 𝕏 primary text color
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

        Label likeIcon = new Label("❤️ 0");
        likeIcon.setTextFill(Color.web("#71767b"));

        actionToolbar.getChildren().addAll(replyIcon, repostIcon, likeIcon);

        // Assemble structural nodes into the stack context
        contentStack.getChildren().addAll(headerRow, bodyText, actionToolbar);
        tweetRow.getChildren().addAll(avatarBox, contentStack);

        // Inject the complete multi-row layout object inside the scrolling container viewport
        timelineContainer.getChildren().add(tweetRow);
    }
}