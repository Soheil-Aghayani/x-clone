package client.controllers;

import client.NavigationManager;
import client.AppFonts;
import client.AppIcons;
import client.MainApp;
import client.UserSession;
import client.chat.ChatStore;
import client.media.MediaLibrary;
import client.network.SharedSocialClient;
import client.profile.AccountDirectory;
import client.profile.AccountProfile;
import client.timeline.Post;
import client.timeline.PostStore;
import client.timeline.NotificationItem;
import client.ui.PostComposerDialog;
import client.ui.PostInteractions;
import client.ui.ProfileHoverCard;
import client.ui.MediaViewer;
import client.ui.PollComposerDialog;
import client.ui.PollView;
import client.ui.XDialog;
import javafx.beans.binding.Bindings;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import javafx.scene.control.ContentDisplay;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.Scene;
import shared.models.User;
import shared.models.SharedTrend;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedController {
    private static final int MAX_POST_LENGTH = 280;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}_])([#@][\\p{L}\\p{N}_]+)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String INACTIVE_ACTION_STYLE = "-fx-background-color: transparent; -fx-text-fill: #536471; -fx-padding: 5 4; -fx-cursor: hand;";

    private final PostStore postStore = PostStore.getInstance();
    private final SharedSocialClient sharedSocialClient = new SharedSocialClient();
    private String activeHashtag;
    private String viewMode = "home";
    private String searchQuery = "";
    private String selectedMediaUri;
    private String notificationTab = "All";
    private String exploreTab = "For you";
    private String chatFilter = "All";
    private String chatView = "inbox";
    private String homeTab = "For you";
    private String searchResultTab = "Top";
    private Long selectedPostId;
    private Long selectedConversationId;
    private String moreSection = "More";
    private int lastUnreadNotificationCount = -1;
    private boolean sharedSyncInProgress;
    private Timeline sharedRefreshTimeline;
    private Timeline searchDebounce;

    @FXML
    private TextArea tweetTextArea;

    @FXML
    private VBox timelineContainer;

    @FXML
    private Label headerTitleLabel;

    @FXML
    private Label characterCountLabel;

    @FXML
    private Circle characterProgress;

    @FXML
    private StackPane characterCounterStack;

    @FXML
    private Button postButton;

    @FXML
    private Label sidebarNameLabel;

    @FXML
    private Label sidebarUsernameLabel;

    @FXML
    private Label sidebarAvatarLabel;

    @FXML
    private Label composerAvatarLabel;

    @FXML private HBox composerSection;
    @FXML private HBox attachmentBar;
    @FXML private Label attachmentLabel;
    @FXML private TextField searchField;
    @FXML private TextField centerSearchField;
    @FXML private Button brandLabel;
    @FXML private VBox centerColumn;
    @FXML private VBox rightSidebar;
    @FXML private VBox suggestionWidget;
    @FXML private VBox trendsWidget;
    @FXML private HBox topHeader;
    @FXML private Region headerSpacer;
    @FXML private ScrollPane contentScroll;

    @FXML private Button homeNavButton;
    @FXML private Button exploreNavButton;
    @FXML private Button notificationsNavButton;
    @FXML private Button chatNavButton;
    @FXML private Button bookmarksNavButton;
    @FXML private Button profileNavButton;
    @FXML private Button moreNavButton;
    @FXML private Button accountMoreButton;
    @FXML private Button sidebarPostButton;
    @FXML private Button headerFilterButton;
    @FXML private Button imageButton;
    @FXML private Button gifButton;
    @FXML private Button pollButton;
    @FXML private Button ideaButton;
    @FXML private Button locationButton;
    @FXML private Button followJavaFxButton;
    @FXML private Button followDesignButton;

    @FXML
    public void initialize() {
        setupIcons();
        updateNotificationChrome();
        populateSuggestionWidget();
        populateTrendsWidget();
        tweetTextArea.textProperty().addListener((observable, oldText, newText) -> {
            tweetTextArea.setFont(AppFonts.fontFor(newText, 20));
            updateInlineComposerHeight(newText);
            updateComposerState(newText);
        });
        updateAccountSummary();
        updateComposerState("");
        activeHashtag = postStore.consumeRequestedHashtag();
        String requestedView = postStore.consumeRequestedView();
        if (requestedView != null) {
            viewMode = requestedView;
            if (viewMode.equals("people")) searchResultTab = "People";
        }
        selectedPostId = postStore.consumeRequestedPost();
        String requestedSearch = postStore.consumeRequestedSearch();
        if (requestedSearch != null) {
            searchQuery = requestedSearch;
            searchField.setText(requestedSearch);
            centerSearchField.setText(requestedSearch);
        }
        searchField.textProperty().addListener((observable, oldText, newText) -> {
            searchQuery = newText == null ? "" : newText.trim();
            refreshTimeline();
            scheduleRemoteSearch();
        });
        centerSearchField.textProperty().addListener((observable, oldText, newText) -> {
            searchQuery = newText == null ? "" : newText.trim();
            if (viewMode.equals("explore") || viewMode.equals("people")) refreshTimeline();
            scheduleRemoteSearch();
        });
        refreshFollowButtons();
        refreshTimeline();
        refreshSharedSocialState();
        startSharedRefresh();
        postStore.clockProperty().addListener((observable, oldValue, newValue) -> updateNotificationChrome());
        if (postStore.consumeComposerFocusRequest()) {
            Platform.runLater(tweetTextArea::requestFocus);
        }
    }

    private void refreshSharedSocialState() {
        refreshSharedSocialState(false);
    }

    private void refreshSharedSocialState(boolean preserveScroll) {
        if (UserSession.getInstance().getToken() == null || sharedSyncInProgress) return;
        sharedSyncInProgress = true;
        double previousScroll = contentScroll == null ? 0 : contentScroll.getVvalue();
        String previousView = viewMode;
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return postStore.syncSharedState();
            }
        };
        task.setOnSucceeded(event -> {
            sharedSyncInProgress = false;
            if (Boolean.TRUE.equals(task.getValue())) {
                refreshFollowButtons();
                populateSuggestionWidget();
                populateTrendsWidget();
                refreshTimeline();
                if (preserveScroll && previousView.equals(viewMode)) {
                    Platform.runLater(() -> contentScroll.setVvalue(previousScroll));
                }
            }
        });
        task.setOnFailed(event -> sharedSyncInProgress = false);
        task.setOnCancelled(event -> sharedSyncInProgress = false);
        Thread thread = new Thread(task, "x-shared-social-sync");
        thread.setDaemon(true);
        thread.start();
    }

    private void scheduleRemoteSearch() {
        if (UserSession.getInstance().getToken() == null) return;
        if (searchDebounce != null) searchDebounce.stop();
        searchDebounce = new Timeline(new KeyFrame(Duration.millis(350), event -> {
            String requested = searchQuery;
            if (requested.isBlank()) {
                refreshSharedSocialState(true);
                return;
            }
            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() {
                    return postStore.searchSharedState(requested, searchResultTab);
                }
            };
            task.setOnSucceeded(done -> {
                if (requested.equals(searchQuery) && Boolean.TRUE.equals(task.getValue())) {
                    refreshTimeline();
                }
            });
            Thread thread = new Thread(task, "x-remote-search");
            thread.setDaemon(true);
            thread.start();
        }));
        searchDebounce.play();
    }

    private void startSharedRefresh() {
        if (UserSession.getInstance().getToken() == null) return;
        sharedRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(45), event -> {
            if (contentScroll.getScene() == null) {
                sharedRefreshTimeline.stop();
                return;
            }
            Window window = contentScroll.getScene().getWindow();
            if (window != null && window.isShowing()) {
                refreshSharedSocialState(true);
            }
        }));
        sharedRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        sharedRefreshTimeline.play();
    }

    private void setupIcons() {
        brandLabel.setText("");
        brandLabel.setGraphic(AppIcons.icon("x-logo-icon.svg", 28, "#0f1419"));
        setNavIcon(homeNavButton, "home-icon.svg");
        setNavIcon(exploreNavButton, "search-icon.svg");
        setNavIcon(notificationsNavButton, "bell-icon.svg");
        setNavIcon(chatNavButton, "chat-icon.svg");
        setNavIcon(bookmarksNavButton, "bookmark-icon.svg");
        profileNavButton.setGraphic(createProfileGlyph(24));
        profileNavButton.setContentDisplay(ContentDisplay.LEFT);
        profileNavButton.setGraphicTextGap(18);
        setNavIcon(moreNavButton, "more-horizontal-filled-icon.svg");
        accountMoreButton.setGraphic(AppIcons.icon("more-horizontal-filled-icon.svg", 20, "#0f1419"));
        headerFilterButton.setGraphic(AppIcons.icon("list-filter-icon.svg", 20, "#0f1419"));
        headerFilterButton.setOnAction(event -> handleShowExplore());
        imageButton.setGraphic(AppIcons.icon("image-off-icon.svg", 20, "#1d9bf0"));
        gifButton.setGraphic(AppIcons.icon("gif-in-square-icon.svg", 20, "#1d9bf0"));
        pollButton.setGraphic(AppIcons.icon("list-filter-icon.svg", 20, "#1d9bf0"));
        ideaButton.setGraphic(AppIcons.icon("lightbulb-icon.svg", 20, "#1d9bf0"));
        locationButton.setGraphic(AppIcons.icon("location-pin-icon.svg", 20, "#1d9bf0"));
    }

    private void updateNotificationChrome() {
        if (notificationsNavButton == null) return;
        String username = UserSession.getInstance().getUsername();
        int unread = username == null ? 0 : postStore.getUnreadNotificationCount(username);
        if (unread != lastUnreadNotificationCount) {
            lastUnreadNotificationCount = unread;
            StackPane bellWithBadge = new StackPane();
            bellWithBadge.setMinSize(28, 28);
            bellWithBadge.setPrefSize(28, 28);
            bellWithBadge.getChildren().add(AppIcons.icon("bell-icon.svg", 24, "#0f1419"));
            if (unread > 0) {
                Label badge = new Label(unread > 99 ? "99+" : Integer.toString(unread));
                badge.getStyleClass().add("notification-badge");
                StackPane.setAlignment(badge, Pos.TOP_RIGHT);
                bellWithBadge.getChildren().add(badge);
            }
            notificationsNavButton.setGraphic(bellWithBadge);
            notificationsNavButton.setAccessibleText(unread == 0
                    ? "Notifications"
                    : "Notifications, " + unread + " unread");
        }
        MainApp app = MainApp.getInstance();
        if (app != null) app.setPageTitle(pageTitle());
    }

    private String pageTitle() {
        if (viewMode.equals("notifications")) return "Notifications";
        if (viewMode.equals("chat")) {
            if (chatView.equals("settings")) return "Chat settings";
            if (chatView.equals("change-passcode")) return "Change passcode";
            return "Chat";
        }
        if (viewMode.equals("bookmarks")) return "Bookmarks";
        if (viewMode.equals("explore") || viewMode.equals("people")) return "Explore";
        if (viewMode.equals("post-detail")) return "Post";
        return "Home";
    }

    private void setNavIcon(Button button, String filename) {
        button.setGraphic(AppIcons.icon(filename, 24, "#0f1419"));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(18);
    }

    private void populateSuggestionWidget() {
        suggestionWidget.getChildren().clear();
        Label title = new Label("You might like");
        title.getStyleClass().add("widget-title");
        suggestionWidget.getChildren().add(title);
        List<String> suggestions = postStore.getSharedProfileUsernames().stream()
                .filter(username -> !username.equalsIgnoreCase(UserSession.getInstance().getUsername()))
                .filter(username -> !UserSession.getInstance().isFollowing(username))
                .sorted()
                .limit(3)
                .toList();
        for (String username : suggestions) {
            AccountProfile account = AccountDirectory.find(username);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Node avatar = ProfileHoverCard.avatarNode(account, 40);
            VBox identity = new VBox(1);
            HBox.setHgrow(identity, Priority.ALWAYS);
            Label name = new Label(account.displayName());
            name.getStyleClass().add("widget-name");
            Label handle = new Label("@" + account.username());
            handle.getStyleClass().add("secondary-text");
            identity.getChildren().addAll(name, handle);
            Button follow = new Button();
            follow.getStyleClass().add("follow-button");
            Runnable refresh = () -> updateFollowButton(follow, UserSession.getInstance().isFollowing(username));
            refresh.run();
            follow.setOnAction(event -> { UserSession.getInstance().toggleFollow(username); refresh.run(); });
            row.getChildren().addAll(avatar, identity, follow);
            ProfileHoverCard.attachIdentity(avatar, name, handle, username, () -> openProfile(username));
            suggestionWidget.getChildren().add(row);
        }
        Label more = new Label(suggestions.isEmpty() ? "No suggestions yet" : "Show more");
        more.getStyleClass().add("show-more");
        if (!suggestions.isEmpty()) {
            more.setOnMouseClicked(event -> {
                exploreTab = "Who to follow";
                handleShowExplore();
            });
        }
        suggestionWidget.getChildren().add(more);
    }

    private void populateTrendsWidget() {
        if (trendsWidget == null) return;
        trendsWidget.getChildren().clear();
        Label title = new Label("What's happening");
        title.getStyleClass().add("widget-title");
        trendsWidget.getChildren().add(title);

        List<String[]> trendData = postStore.getSharedTrends().stream()
                .limit(3)
                .map(trend -> new String[]{
                        "Trending",
                        trend.hashtag(),
                        trend.postCount() + (trend.postCount() == 1 ? " post" : " posts")
                })
                .toList();

        for (String[] data : trendData) {
            VBox item = new VBox(2);
            item.setStyle("-fx-cursor: hand;");
            Label category = new Label(data[0]);
            category.getStyleClass().add("secondary-text");
            Label name = new Label(data[1]);
            name.getStyleClass().add("widget-name");
            Label posts = new Label(data[2]);
            posts.getStyleClass().add("secondary-text");
            item.getChildren().addAll(category, name, posts);
            item.setOnMouseClicked(event -> triggerExploreSearch(data[1]));
            trendsWidget.getChildren().add(item);
        }
        if (trendData.isEmpty()) {
            Label empty = new Label("Trends appear as people use hashtags.");
            empty.setWrapText(true);
            empty.getStyleClass().add("secondary-text");
            trendsWidget.getChildren().add(empty);
        }

        Label more = new Label("Show more");
        more.getStyleClass().add("show-more");
        more.setOnMouseClicked(event -> handleShowExplore());
        trendsWidget.getChildren().add(more);
    }

    private javafx.scene.Node createProfileGlyph(double size) {
        javafx.scene.shape.Circle head = new javafx.scene.shape.Circle(size * 0.18, Color.web("#0f1419"));
        javafx.scene.shape.Arc body = new javafx.scene.shape.Arc(size / 2, size * 0.96, size * 0.34, size * 0.34, 0, 180);
        body.setType(javafx.scene.shape.ArcType.CHORD);
        body.setFill(Color.web("#0f1419"));
        VBox glyph = new VBox(2, head, body);
        glyph.setAlignment(javafx.geometry.Pos.CENTER);
        glyph.setMinSize(size, size);
        glyph.setPrefSize(size, size);
        return glyph;
    }

    private void updateAccountSummary() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
        String initial = displayName.isBlank() ? "U" : displayName.substring(0, 1).toUpperCase();
        sidebarNameLabel.setText(displayName);
        sidebarNameLabel.setFont(AppFonts.fontFor(displayName, 15, FontWeight.BOLD));
        sidebarUsernameLabel.setText("@" + user.getUsername());
        sidebarUsernameLabel.setFont(AppFonts.fontFor(sidebarUsernameLabel.getText(), 14));
        applyCurrentAvatar(sidebarAvatarLabel, user, initial, 40);
        applyCurrentAvatar(composerAvatarLabel, user, initial, 44);
        sidebarNameLabel.setOnMouseClicked(event -> handleAccountMenu());
        sidebarUsernameLabel.setOnMouseClicked(event -> handleAccountMenu());
        sidebarAvatarLabel.setOnMouseClicked(event -> handleAccountMenu());
        addAccountMenuTriggerStyle(sidebarNameLabel);
        addAccountMenuTriggerStyle(sidebarUsernameLabel);
        addAccountMenuTriggerStyle(sidebarAvatarLabel);
    }

    private void applyCurrentAvatar(Label host, User user, String fallbackInitial, double size) {
        Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(user.getUsername()), size);
        boolean hasImage = avatar instanceof ImageView;
        host.setGraphic(hasImage ? avatar : null);
        host.setContentDisplay(hasImage ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.TEXT_ONLY);
        host.setText(hasImage ? "" : fallbackInitial);
        if (hasImage && !host.getStyleClass().contains("avatar-image-host")) {
            host.getStyleClass().add("avatar-image-host");
        } else if (!hasImage) {
            host.getStyleClass().remove("avatar-image-host");
        }
    }

    private void addAccountMenuTriggerStyle(javafx.scene.control.Label label) {
        if (!label.getStyleClass().contains("account-menu-trigger")) {
            label.getStyleClass().add("account-menu-trigger");
        }
    }

    private void updateComposerState(String content) {
        int length = content == null ? 0 : content.length();
        int remaining = MAX_POST_LENGTH - length;
        characterCountLabel.setText(Integer.toString(remaining));
        boolean showCounter = remaining <= 20;
        boolean showCircle = showCounter && remaining >= -9;
        characterCounterStack.setManaged(showCounter);
        characterCounterStack.setVisible(showCounter);
        characterProgress.setManaged(showCircle);
        characterProgress.setVisible(showCircle);
        String warningColor = remaining > 0 ? "#fddd3e" : "#f42330";
        characterCountLabel.setStyle("-fx-text-fill: " + warningColor + ";");
        characterProgress.setStroke(Color.web(warningColor));
        postButton.setDisable(((content == null || content.trim().isEmpty()) && selectedMediaUri == null)
                || length > MAX_POST_LENGTH
                || UserSession.getInstance().getCurrentUser() == null);
    }

    private void updateInlineComposerHeight(String content) {
        int visualLines = 1;
        if (content != null && !content.isEmpty()) {
            visualLines = 0;
            for (String line : content.split("\\R", -1)) {
                visualLines += Math.max(1, (int) Math.ceil(line.length() / 52.0));
            }
        }
        double height = Math.max(68, Math.min(360, 30 + visualLines * 26.0));
        tweetTextArea.setMinHeight(height);
        tweetTextArea.setPrefHeight(height);
        tweetTextArea.setMaxHeight(height);
    }

    @FXML
    private void handlePostTweet() {
        String content = tweetTextArea.getText().trim();
        User currentUser = UserSession.getInstance().getCurrentUser();
        String mediaToPublish = attachmentBar.isVisible()
                && selectedMediaUri != null
                && MediaLibrary.isAvailable(selectedMediaUri)
                ? selectedMediaUri : null;
        if ((content.isEmpty() && mediaToPublish == null)
                || content.length() > MAX_POST_LENGTH || currentUser == null) {
            return;
        }

        Post published = postStore.createPost(currentUser, content, mediaToPublish);
        if (published == null) {
            XDialog.info(tweetTextArea.getScene() == null ? null : tweetTextArea.getScene().getWindow(),
                    "Post not sent",
                    "The shared server could not publish this post. Your text is still here so you can retry.");
            return;
        }
        tweetTextArea.clear();
        selectedMediaUri = null;
        handleRemoveAttachment();
        activeHashtag = null;
        refreshTimeline();
        Platform.runLater(() -> contentScroll.setVvalue(0));
    }

    @FXML
    private void handleShowHome() {
        viewMode = "home";
        activeHashtag = null;
        searchQuery = "";
        if (searchField != null && !searchField.getText().isEmpty()) searchField.clear();
        if (centerSearchField != null && !centerSearchField.getText().isEmpty()) centerSearchField.clear();
        refreshTimeline();
        refreshSharedSocialState();
    }

    @FXML
    private void handleShowExplore() {
        viewMode = "explore";
        activeHashtag = null;
        refreshTimeline();
        refreshSharedSocialState();
    }

    @FXML
    private void handleShowBookmarks() {
        viewMode = "bookmarks";
        activeHashtag = null;
        refreshTimeline();
        refreshSharedSocialState();
    }

    @FXML
    private void handleShowNotifications() {
        viewMode = "notifications";
        activeHashtag = null;
        refreshTimeline();
        refreshSharedSocialState();
        updateNotificationChrome();
    }

    @FXML
    private void handleShowChat() {
        viewMode = "chat";
        chatView = "inbox";
        activeHashtag = null;
        refreshTimeline();
    }

    @FXML
    private void handleShowUnavailable() {
        viewMode = "unavailable";
        activeHashtag = null;
        refreshTimeline();
    }

    @FXML
    private void handleMoreMenu() {
        ContextMenu menu = new ContextMenu();
        for (String option : List.of("Lists", "Communities", "Monetization", "Settings and privacy", "Help Center")) {
            MenuItem item = new MenuItem(option);
            item.setOnAction(event -> { moreSection = option; viewMode = "more"; refreshTimeline(); });
            menu.getItems().add(item);
        }
        menu.show(moreNavButton, Side.BOTTOM, 0, 2);
    }

    @FXML
    private void handleOpenPostComposer() {
        PostInteractions.showPost(sidebarPostButton, this::refreshTimeline);
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach an image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File image = chooser.showOpenDialog(tweetTextArea.getScene().getWindow());
        if (image == null) return;
        attachMedia(image);
    }

    @FXML
    private void handleChooseGif() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach a GIF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF images", "*.gif"));
        File image = chooser.showOpenDialog(tweetTextArea.getScene().getWindow());
        if (image == null) return;
        attachMedia(image);
    }

    private void attachMedia(File media) {
        try {
            selectedMediaUri = MediaLibrary.importFile(media);
            attachmentLabel.setText(media.getName());
            attachmentLabel.setStyle("");
            attachmentBar.setManaged(true);
            attachmentBar.setVisible(true);
        } catch (IOException exception) {
            selectedMediaUri = null;
            attachmentLabel.setText("Could not attach " + media.getName());
            attachmentLabel.setStyle("-fx-text-fill: #f4212e;");
            attachmentBar.setManaged(true);
            attachmentBar.setVisible(true);
        }
        updateComposerState(tweetTextArea.getText());
    }

    @FXML
    private void handleWritingIdeas() {
        ContextMenu ideas = new ContextMenu();
        for (String suggestion : List.of("What are you working on today?", "Share something you learned…",
                "What’s happening in your community?", "Add #JavaFX to the conversation")) {
            MenuItem item = new MenuItem(suggestion);
            item.setOnAction(event -> {
                if (!tweetTextArea.getText().isBlank()) tweetTextArea.appendText("\n");
                tweetTextArea.appendText(suggestion);
                tweetTextArea.requestFocus();
            });
            ideas.getItems().add(item);
        }
        ideas.show(ideaButton, Side.BOTTOM, 0, 2);
    }

    @FXML
    private void handleAddLocation() {
        // ── Custom location modal ─────────────────────────────────────────
        javafx.stage.Stage modal = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (tweetTextArea.getScene() != null) modal.initOwner(tweetTextArea.getScene().getWindow());

        // Header
        javafx.scene.control.Button close = new javafx.scene.control.Button("×");
        close.getStyleClass().add("compose-close-button");
        javafx.scene.layout.Region hSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(hSpacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label heading = new javafx.scene.control.Label("Add location");
        heading.setFont(AppFonts.fontFor("Add location", 17, javafx.scene.text.FontWeight.BOLD));
        heading.setStyle("-fx-text-fill: #0f1419;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12, close, heading, hSpacer);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new javafx.geometry.Insets(14, 16, 10, 16));

        // Pin icon + label row
        javafx.scene.layout.HBox iconRow = new javafx.scene.layout.HBox(10);
        iconRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        iconRow.setPadding(new javafx.geometry.Insets(0, 16, 6, 16));
        javafx.scene.Node pinIcon = AppIcons.icon("location-pin-icon.svg", 18, "#1d9bf0");
        javafx.scene.control.Label subLabel = new javafx.scene.control.Label("Where are you posting from?");
        subLabel.setFont(AppFonts.fontFor("Where are you posting from?", 14));
        subLabel.setStyle("-fx-text-fill: #536471;");
        iconRow.getChildren().addAll(pinIcon, subLabel);

        // Separator
        javafx.scene.shape.Line sep1 = new javafx.scene.shape.Line(0, 0, 500, 0);
        sep1.setStroke(javafx.scene.paint.Color.web("#eff3f4"));
        sep1.setStrokeWidth(1);

        // Text field
        javafx.scene.control.TextField locationField = new javafx.scene.control.TextField();
        locationField.setPromptText("City, neighbourhood…");
        locationField.getStyleClass().add("modal-search-field");
        locationField.setStyle("-fx-font-size: 15px; -fx-padding: 10 14;");
        javafx.scene.layout.VBox fieldBox = new javafx.scene.layout.VBox(locationField);
        fieldBox.setPadding(new javafx.geometry.Insets(12, 16, 4, 16));

        // Separator
        javafx.scene.shape.Line sep2 = new javafx.scene.shape.Line(0, 0, 500, 0);
        sep2.setStroke(javafx.scene.paint.Color.web("#eff3f4"));
        sep2.setStrokeWidth(1);

        // Footer buttons
        javafx.scene.control.Button cancel = new javafx.scene.control.Button("Cancel");
        cancel.getStyleClass().add("audience-pill");
        cancel.setStyle("-fx-border-color: #cfd9de; -fx-background-color: white; -fx-text-fill: #0f1419; -fx-padding: 8 18;");

        javafx.scene.control.Button add = new javafx.scene.control.Button("Add");
        add.getStyleClass().add("composer-post-button");
        add.setDisable(true);

        locationField.textProperty().addListener((o, oldV, newV) -> add.setDisable(newV == null || newV.isBlank()));

        javafx.scene.layout.Region footerSpacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(10, footerSpacer, cancel, add);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(10, 16, 14, 16));

        // Card assembly
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(0, header, iconRow, sep1, fieldBox, sep2, footer);
        card.getStyleClass().add("compose-dialog");
        card.setMinWidth(400);
        card.setMaxWidth(400);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        // Actions
        close.setOnAction(e -> modal.close());
        cancel.setOnAction(e -> modal.close());
        add.setOnAction(e -> {
            String loc = locationField.getText().trim();
            if (!loc.isBlank()) {
                if (!tweetTextArea.getText().isBlank()) tweetTextArea.appendText("\n");
                tweetTextArea.appendText("📍 " + loc);
            }
            modal.close();
        });
        locationField.setOnAction(e -> { if (!add.isDisabled()) add.fire(); });

        // Scene
        javafx.scene.layout.StackPane shell = new javafx.scene.layout.StackPane(card);
        shell.setPadding(new javafx.geometry.Insets(10));
        shell.setAlignment(javafx.geometry.Pos.CENTER);
        shell.getStyleClass().add("compose-modal-shell");

        javafx.scene.Scene scene = new javafx.scene.Scene(shell, javafx.scene.paint.Color.TRANSPARENT);
        var stylesheet = getClass().getResource("/styles/twitter.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());
        scene.setOnKeyPressed(ev -> { if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) modal.close(); });
        modal.setScene(scene);

        modal.setOnShown(ev -> {
            modal.sizeToScene();
            javafx.stage.Window owner = tweetTextArea.getScene().getWindow();
            modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
            modal.setY(owner.getY() + (owner.getHeight() - modal.getHeight()) / 2.5);
            locationField.requestFocus();
        });
        modal.showAndWait();
    }


    @FXML
    private void handleRemoveAttachment() {
        selectedMediaUri = null;
        attachmentLabel.setText("");
        attachmentBar.setManaged(false);
        attachmentBar.setVisible(false);
        updateComposerState(tweetTextArea.getText());
    }

    @FXML
    private void handleCreatePoll() {
        PollComposerDialog.show(pollButton.getScene() == null ? null : pollButton.getScene().getWindow())
                .ifPresent(poll -> {
                    User user = UserSession.getInstance().getCurrentUser();
                    if (user == null) return;
                    postStore.createPoll(user, poll.question(), poll.choices(), poll.duration());
                    refreshTimeline();
                });
    }

    @FXML
    private void handleFollowJavaFx() {
        UserSession.getInstance().toggleFollow("openjfx");
        refreshFollowButtons();
    }

    @FXML
    private void handleFollowDesign() {
        UserSession.getInstance().toggleFollow("designdaily");
        refreshFollowButtons();
    }

    private void refreshFollowButtons() {
        updateFollowButton(followJavaFxButton, UserSession.getInstance().isFollowing("openjfx"));
        updateFollowButton(followDesignButton, UserSession.getInstance().isFollowing("designdaily"));
    }

    private void updateFollowButton(Button button, boolean following) {
        button.setText(following ? "Following" : "Follow");
        button.setStyle(following
                ? "-fx-background-color: white; -fx-text-fill: #0f1419; -fx-border-color: #cfd9de; -fx-border-radius: 18; -fx-background-radius: 18; -fx-font-weight: bold; -fx-padding: 7 12;"
                : "");
    }

    private void refreshTimeline() {
        timelineContainer.getChildren().clear();
        resetViewChrome();
        updateNotificationChrome();
        if (viewMode.equals("post-detail")) {
            renderPostDetail();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("notifications")) {
            renderNotifications();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("chat")) {
            renderChat();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("bookmarks")) {
            renderBookmarks();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("creator")) {
            renderCreatorStudio();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("grok")) {
            renderGrok();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("premium")) {
            renderPremium();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("more")) {
            renderMoreCenter();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("people")) {
            renderPeopleDirectory();
            updateActiveNavigation();
            return;
        }
        if (viewMode.equals("explore") && activeHashtag == null) {
            renderExplore();
            updateActiveNavigation();
            return;
        }
        boolean contentView = viewMode.equals("home") || viewMode.equals("explore") || viewMode.equals("bookmarks");
        boolean showComposer = viewMode.equals("home") && searchQuery.isBlank() && activeHashtag == null;
        composerSection.setManaged(showComposer);
        composerSection.setVisible(showComposer);

        String headerTitle;
        if (!searchQuery.isBlank()) headerTitle = "Search";
        else if (activeHashtag != null) headerTitle = activeHashtag;
        else headerTitle = switch (viewMode) {
            case "explore" -> "Explore";
            case "bookmarks" -> "Bookmarks";
            case "notifications" -> "Notifications";
            case "chat" -> "Chat";
            case "unavailable" -> "Coming soon";
            default -> "Home";
        };
        headerTitleLabel.setText(headerTitle);
        headerTitleLabel.setFont(AppFonts.fontFor(headerTitle, 18, FontWeight.BOLD));

        if (viewMode.equals("home") && searchQuery.isBlank() && activeHashtag == null) {
            timelineContainer.getChildren().add(createSectionTabs(List.of("For you", "Following"), homeTab, selected -> {
                homeTab = selected;
                refreshTimeline();
            }));
        }
        if (!searchQuery.isBlank() && contentView) {
            timelineContainer.getChildren().add(createSectionTabs(List.of("Top", "Latest", "People", "Media"), searchResultTab, selected -> {
                searchResultTab = selected;
                refreshTimeline();
            }));
            if (searchResultTab.equals("People")) {
                renderAccountSearch(searchQuery);
                updateActiveNavigation();
                return;
            }
        }

        List<Post> posts = switch (viewMode) {
            case "bookmarks" -> postStore.getBookmarkedPosts();
            case "notifications", "chat", "unavailable" -> List.of();
            default -> viewMode.equals("home") && searchQuery.isBlank()
                    ? (homeTab.equals("Following") ? postStore.getFollowingPosts(UserSession.getInstance().getUsername()) : postStore.getForYouPosts())
                    : postStore.getAllPosts();
        };
        if (activeHashtag != null) {
            posts = posts.stream().filter(post -> post.hasHashtag(activeHashtag)).toList();
        }
        if (!searchQuery.isBlank() && contentView) {
            String normalized = searchQuery.toLowerCase();
            posts = posts.stream().filter(post -> post.getContent().toLowerCase().contains(normalized)
                    || post.getAuthorName().toLowerCase().contains(normalized)
                    || post.getAuthorUsername().toLowerCase().contains(normalized)).toList();
            if (searchResultTab.equals("Latest")) {
                posts = posts.stream().sorted(Comparator.comparing(Post::getCreatedAt).reversed()).toList();
            } else if (searchResultTab.equals("Media")) {
                posts = posts.stream().filter(post -> post.getMediaUri() != null).toList();
            } else {
                posts = posts.stream().sorted(Comparator.comparingInt((Post post) ->
                        post.getLikes() + post.getReplies() * 2 + post.getRetweets() * 2).reversed()).toList();
            }
        }

        for (Post post : posts) {
            timelineContainer.getChildren().add(createPostCard(post));
        }

        if (searchQuery.isBlank() && activeHashtag == null && posts.size() >= 50) {
            Button loadMore = new Button("Load more posts");
            loadMore.getStyleClass().add("load-more-posts");
            loadMore.setMaxWidth(Double.MAX_VALUE);
            loadMore.setOnAction(event -> {
                loadMore.setDisable(true);
                loadMore.setText("Loading…");
                Task<Integer> task = new Task<>() {
                    @Override protected Integer call() {
                        return postStore.loadMoreSharedPosts(
                                viewMode.equals("home") && homeTab.equals("Following"));
                    }
                };
                task.setOnSucceeded(done -> {
                    if (task.getValue() > 0) {
                        refreshTimeline();
                    } else {
                        loadMore.setText("You’re all caught up");
                        loadMore.setDisable(true);
                    }
                });
                task.setOnFailed(done -> {
                    loadMore.setText("Try again");
                    loadMore.setDisable(false);
                });
                Thread loader = new Thread(task, "x-feed-pagination");
                loader.setDaemon(true);
                loader.start();
            });
            timelineContainer.getChildren().add(loadMore);
        }

        if (posts.isEmpty()) {
            timelineContainer.getChildren().add(createEmptyState(headerTitle));
        }
        updateActiveNavigation();
    }

    private void resetViewChrome() {
        topHeader.setManaged(true);
        topHeader.setVisible(true);
        headerTitleLabel.setManaged(true);
        headerTitleLabel.setVisible(true);
        headerTitleLabel.setGraphic(null);
        headerTitleLabel.setOnMouseClicked(null);
        headerTitleLabel.setStyle("");
        centerSearchField.setManaged(false);
        centerSearchField.setVisible(false);
        headerSpacer.setManaged(true);
        headerSpacer.setVisible(true);
        rightSidebar.setManaged(true);
        rightSidebar.setVisible(true);
        rightSidebar.getChildren().setAll(searchField, suggestionWidget, trendsWidget);
        headerFilterButton.setManaged(true);
        headerFilterButton.setVisible(true);
        headerFilterButton.setGraphic(AppIcons.icon("list-filter-icon.svg", 20, "#0f1419"));
        headerFilterButton.setOnAction(event -> handleShowExplore());
        centerColumn.setPrefWidth(600);
        centerColumn.setMaxWidth(600);
        composerSection.setManaged(false);
        composerSection.setVisible(false);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void renderNotifications() {
        hideRightSidebar();
        headerTitleLabel.setText("Notifications");
        headerTitleLabel.setFont(AppFonts.fontFor("Notifications", 20, FontWeight.BOLD));
        headerFilterButton.setGraphic(AppIcons.icon("settings-gear-icon.svg", 20, "#0f1419"));
        headerFilterButton.setOnAction(event -> {
            postStore.markNotificationsRead(UserSession.getInstance().getUsername());
            refreshTimeline();
        });

        HBox tabs = createSectionTabs(List.of("All", "Priority", "Mentions"), notificationTab, selected -> {
            notificationTab = selected;
            refreshTimeline();
        });
        timelineContainer.getChildren().add(tabs);

        List<NotificationItem> items = postStore.getNotifications(UserSession.getInstance().getUsername());
        if (notificationTab.equals("Mentions")) {
            items = items.stream().filter(item -> item.type() == NotificationItem.Type.MENTION).toList();
        } else if (notificationTab.equals("Priority")) {
            items = items.stream().filter(item -> UserSession.getInstance().isFollowing(item.actorUsername())
                    || AccountDirectory.find(item.actorUsername()).verified()).toList();
        }
        if (!items.isEmpty()) {
            items.forEach(item -> timelineContainer.getChildren().add(createNotificationCard(item)));
            postStore.markNotificationsRead(UserSession.getInstance().getUsername());
            updateNotificationChrome();
            return;
        }

        String details = notificationTab.equals("Mentions")
                ? "When someone mentions you, you’ll find it here."
                : "From likes to reposts and a whole lot more, this is where all the action happens.";
        timelineContainer.getChildren().add(createLargeEmptyState("Nothing to see here — yet", details));
    }

    private void renderExplore() {
        hideRightSidebar();
        centerColumn.setPrefWidth(600);
        centerColumn.setMaxWidth(600);
        headerTitleLabel.setManaged(false);
        headerTitleLabel.setVisible(false);
        headerSpacer.setManaged(false);
        headerSpacer.setVisible(false);
        centerSearchField.setManaged(true);
        centerSearchField.setVisible(true);
        centerSearchField.setPromptText("Search");
        centerSearchField.setMaxWidth(Double.MAX_VALUE);
        headerFilterButton.setGraphic(AppIcons.icon("settings-gear-icon.svg", 20, "#0f1419"));
        headerFilterButton.setOnAction(event -> { });

        if (centerSearchField.getText().isBlank()) {
            timelineContainer.getChildren().add(createSectionTabs(
                    List.of("For you", "Trending", "News", "Sports", "Entertainment", "Who to follow"), exploreTab, selected -> {
                        exploreTab = selected;
                        refreshTimeline();
                    }));
        }

        if (!centerSearchField.getText().isBlank()) {
            String query = centerSearchField.getText().trim();
            timelineContainer.getChildren().add(createSectionTabs(
                    List.of("Top", "Latest", "People", "Media"), searchResultTab, selected -> {
                        searchResultTab = selected;
                        refreshTimeline();
                    }));
            if (searchResultTab.equals("People")) {
                renderAccountSearch(query);
                return;
            }
            List<Post> matches = postStore.search(query);
            if (searchResultTab.equals("Latest")) matches = matches.stream().sorted(Comparator.comparing(Post::getCreatedAt).reversed()).toList();
            if (searchResultTab.equals("Media")) matches = matches.stream().filter(post -> post.getMediaUri() != null).toList();
            if (searchResultTab.equals("Top")) matches = matches.stream().sorted(Comparator.comparingInt((Post post) ->
                    post.getLikes() + post.getReplies() * 2 + post.getRetweets() * 2).reversed()).toList();
            matches.forEach(post -> timelineContainer.getChildren().add(createPostCard(post)));
            if (matches.isEmpty()) {
                timelineContainer.getChildren().add(createLargeEmptyState(
                        "No results for “" + centerSearchField.getText().trim() + "”",
                        "Try searching for another name, topic, or keyword."));
            }
            return;
        }

        if (!exploreTab.equals("Trending") && !exploreTab.equals("Who to follow")) {
            List<Post> discoveryPosts = postStore.getAllPosts().stream()
                    .filter(post -> switch (exploreTab) {
                        case "News" -> post.hasHashtag("#News")
                                || post.getContent().toLowerCase().contains("news");
                        case "Sports" -> post.hasHashtag("#Sports")
                                || post.getContent().toLowerCase().contains("sport");
                        case "Entertainment" -> post.hasHashtag("#Entertainment")
                                || post.getContent().toLowerCase().contains("entertainment");
                        default -> true;
                    })
                    .toList();
            discoveryPosts.forEach(post ->
                    timelineContainer.getChildren().add(createPostCard(post)));
            if (discoveryPosts.isEmpty()) {
                timelineContainer.getChildren().add(createLargeEmptyState(
                        "Nothing here yet",
                        "Posts from the shared server will appear here as people publish them."));
            }
            return;
        }

        VBox discovery = new VBox();
        discovery.getStyleClass().add("discovery-content");
        switch (exploreTab) {
            case "Trending" -> {
                discovery.getChildren().add(sectionHeading("Trends for you"));
                List<String> tags = postStore.getSharedTrends().stream()
                        .map(SharedTrend::hashtag)
                        .toList();
                addTrendItems(discovery, tags);
                if (tags.isEmpty()) {
                    discovery.getChildren().add(createLargeEmptyState(
                            "No trends yet",
                            "Hashtags will appear here when people start using them."));
                }
            }
            case "News" -> {
                discovery.getChildren().add(sectionHeading("News"));
                addNewsItem(discovery, "Developers ship a faster generation of desktop apps", "2 hours ago · Technology · 18K posts");
                addNewsItem(discovery, "Open-source communities announce new releases", "5 hours ago · News · 9,430 posts");
                addNewsItem(discovery, "Local teams build privacy-first social tools", "Trending · News · 4,218 posts");
            }
            case "Sports" -> {
                discovery.getChildren().add(sectionHeading("Sports"));
                addNewsItem(discovery, "Tonight’s biggest matches and live conversations", "Live · Sports · 42K posts");
                addNewsItem(discovery, "Championship race enters its final week", "3 hours ago · Sports · 21K posts");
                addTrendItems(discovery, List.of("Football", "Formula 1", "Basketball"));
            }
            case "Entertainment" -> {
                discovery.getChildren().add(sectionHeading("Entertainment"));
                addNewsItem(discovery, "The stories everyone is talking about today", "Trending · Entertainment · 99K posts");
                addNewsItem(discovery, "New films and series arriving this week", "6 hours ago · Entertainment · 12K posts");
                addTrendItems(discovery, List.of("Movies", "Music", "Gaming"));
            }
            case "Who to follow" -> {
                discovery.getChildren().add(sectionHeading("Suggested accounts"));
                List<AccountProfile> accounts = postStore.getSharedProfileUsernames().stream()
                        .filter(username -> !username.equalsIgnoreCase(UserSession.getInstance().getUsername()))
                        .map(AccountDirectory::find)
                        .toList();
                for (AccountProfile account : accounts) {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("account-result-row");
                    row.setMaxWidth(650);
                    Node avatar = ProfileHoverCard.avatarNode(account, 44);
                    VBox identity = new VBox(2);
                    HBox.setHgrow(identity, Priority.ALWAYS);
                    Label name = new Label(account.displayName());
                    name.setFont(AppFonts.fontFor(name.getText(), 15, FontWeight.BOLD));
                    Label handle = new Label("@" + account.username());
                    handle.setTextFill(Color.web("#536471"));
                    Label bio = new Label(account.bio());
                    bio.setWrapText(true);
                    bio.setMaxWidth(430);
                    identity.getChildren().addAll(name, handle, bio);
                    Button follow = new Button();
                    boolean ownAccount = account.username().equalsIgnoreCase(UserSession.getInstance().getUsername());
                    follow.setManaged(!ownAccount);
                    follow.setVisible(!ownAccount);
                    follow.getStyleClass().add("follow-button");
                    Runnable refreshFollow = () -> follow.setText(UserSession.getInstance().isFollowing(account.username()) ? "Following" : "Follow");
                    refreshFollow.run();
                    follow.setOnAction(event -> { UserSession.getInstance().toggleFollow(account.username()); refreshFollow.run(); });
                    row.getChildren().addAll(avatar, identity, follow);
                    row.setOnMouseClicked(event -> {
                        if (!isInsideButton((Node) event.getTarget(), row)) openProfile(account.username());
                    });
                    discovery.getChildren().add(row);
                }
            }
            default -> {
                discovery.getChildren().add(sectionHeading("Today’s News"));
                addNewsItem(discovery, "Brain Teaser Shirt Puzzle Divides Opinions on Hole Count", "16 hours ago · Entertainment · 99K posts");
                addNewsItem(discovery, "Developer Unboxes Laptop, Deletes Only Browser, Asks for Help", "Trending now · News · 248 posts");
                addNewsItem(discovery, "JavaFX applications bring native experiences to every desktop", "2 hours ago · Technology · 12K posts");
                addTrendItems(discovery, List.of("#JavaFX", "#Design", "Desktop apps", "Open source"));
            }
        }
        timelineContainer.getChildren().add(discovery);
    }

    private void renderPostDetail() {
        composerSection.setManaged(false);
        composerSection.setVisible(false);
        headerTitleLabel.setText("←  Post");
        headerTitleLabel.setText("Post");
        headerTitleLabel.setGraphic(AppIcons.icon("arrow-left-icon.svg", 20, "#0f1419"));
        headerTitleLabel.setContentDisplay(ContentDisplay.LEFT);
        headerTitleLabel.setGraphicTextGap(24);
        headerTitleLabel.setFont(AppFonts.fontFor("Post", 20, FontWeight.BOLD));
        headerTitleLabel.setStyle("-fx-cursor: hand;");
        headerTitleLabel.setOnMouseClicked(event -> handleShowHome());
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);
        Post post = selectedPostId == null ? null : postStore.getPost(selectedPostId);
        if (post == null) {
            timelineContainer.getChildren().add(createLargeEmptyState("Post unavailable",
                    "This post may have been deleted or is no longer visible."));
            return;
        }
        postStore.recordView(post);
        List<Post> ancestors = postStore.getConversationAncestors(post.getId());
        for (Post ancestor : ancestors) {
            HBox contextCard = createPostCard(ancestor);
            contextCard.getStyleClass().add("thread-context-card");
            timelineContainer.getChildren().add(contextCard);
        }
        HBox detailCard = createPostCard(post);
        detailCard.getStyleClass().add("post-detail-card");
        timelineContainer.getChildren().add(detailCard);

        Button replyPrompt = new Button("Post your reply");
        replyPrompt.getStyleClass().add("thread-reply-prompt");
        replyPrompt.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(replyPrompt, Priority.ALWAYS);
        Button replyAction = new Button("Reply");
        replyAction.getStyleClass().add("composer-post-button");
        Runnable openReplyComposer = () -> PostInteractions.showReply(
                replyAction, post, this::refreshTimeline);
        replyPrompt.setOnAction(event -> openReplyComposer.run());
        replyAction.setOnAction(event -> openReplyComposer.run());
        HBox replyComposer = new HBox(12, createProfileGlyph(38), replyPrompt, replyAction);
        replyComposer.setAlignment(Pos.CENTER_LEFT);
        replyComposer.getStyleClass().add("thread-reply-composer");
        timelineContainer.getChildren().add(replyComposer);

        List<Post> replies = postStore.getThreadReplies(post.getId());
        if (!replies.isEmpty()) {
            Label conversation = sectionHeading("Replies");
            conversation.getStyleClass().add("conversation-heading");
            timelineContainer.getChildren().add(conversation);
            replies.forEach(reply -> timelineContainer.getChildren().add(createPostCard(reply)));
        } else {
            timelineContainer.getChildren().add(createLargeEmptyState("Join the conversation",
                    "Be the first person to reply to this post."));
        }
    }

    private void renderPeopleDirectory() {
        hideRightSidebar();
        centerColumn.setPrefWidth(600);
        centerColumn.setMaxWidth(600);
        headerTitleLabel.setManaged(false);
        headerTitleLabel.setVisible(false);
        headerSpacer.setManaged(false);
        headerSpacer.setVisible(false);
        centerSearchField.setManaged(true);
        centerSearchField.setVisible(true);
        centerSearchField.setPromptText("Search people");
        centerSearchField.setMaxWidth(Double.MAX_VALUE);
        headerFilterButton.setGraphic(AppIcons.icon("settings-gear-icon.svg", 20, "#0f1419"));
        headerFilterButton.setOnAction(event -> { });

        Label heading = sectionHeading("Connect");
        heading.setPadding(new Insets(18, 20, 8, 20));
        timelineContainer.getChildren().add(heading);
        renderAccountSearch(centerSearchField.getText().trim());
    }

    private void renderAccountSearch(String query) {
        List<AccountProfile> accounts = AccountDirectory.search(query);
        for (AccountProfile account : accounts) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("account-result-row");
            row.setMaxWidth(650);
            Node avatar = ProfileHoverCard.avatarNode(account, 44);
            VBox identity = new VBox(2);
            HBox.setHgrow(identity, Priority.ALWAYS);
            Label name = new Label(account.displayName());
            name.setFont(AppFonts.fontFor(name.getText(), 15, FontWeight.BOLD));
            Label handle = new Label("@" + account.username());
            handle.setTextFill(Color.web("#536471"));
            Label bio = new Label(account.bio());
            bio.setWrapText(true);
            bio.setMaxWidth(430);
            identity.getChildren().addAll(name, handle, bio);
            Button follow = new Button();
            boolean ownAccount = account.username().equalsIgnoreCase(UserSession.getInstance().getUsername());
            follow.setManaged(!ownAccount);
            follow.setVisible(!ownAccount);
            follow.getStyleClass().add("follow-button");
            Runnable refreshFollow = () -> follow.setText(UserSession.getInstance().isFollowing(account.username()) ? "Following" : "Follow");
            refreshFollow.run();
            follow.setOnAction(event -> { UserSession.getInstance().toggleFollow(account.username()); refreshFollow.run(); });
            row.getChildren().addAll(avatar, identity, follow);
            row.setOnMouseClicked(event -> {
                if (!isInsideButton((Node) event.getTarget(), row)) openProfile(account.username());
            });
            timelineContainer.getChildren().add(row);
        }
        if (accounts.isEmpty()) timelineContainer.getChildren().add(createLargeEmptyState(
                "No people found", "Try another name or username."));
    }

    private HBox createNotificationCard(NotificationItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("notification-row");
        Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(item.actorUsername()), 42);
        VBox body = new VBox(5);
        HBox.setHgrow(body, Priority.ALWAYS);
        String action = switch (item.type()) {
            case LIKE -> "liked your post";
            case REPOST -> "reposted your post";
            case REPLY -> "replied to your post";
            case FOLLOW -> "followed you";
            case MENTION -> "mentioned you";
            case SYSTEM -> "sent an update";
        };
        Label actor = new Label(item.actorName());
        actor.setFont(AppFonts.fontFor(actor.getText(), 15, FontWeight.BOLD));
        Label actionLabel = new Label(" " + action);
        actionLabel.setFont(AppFonts.fontFor(actionLabel.getText(), 15));
        Label time = new Label(relativeTime(item.createdAt()));
        time.setTextFill(Color.web("#536471"));
        HBox heading = new HBox(0, actor, actionLabel, time);
        HBox.setMargin(time, new Insets(0, 0, 0, 8));
        body.getChildren().add(heading);
        if (item.excerpt() != null && !item.excerpt().isBlank()) {
            Label excerpt = new Label(item.excerpt());
            excerpt.setWrapText(true);
            excerpt.setTextFill(Color.web("#536471"));
            body.getChildren().add(excerpt);
        }
        if (!item.read()) row.getStyleClass().add("notification-unread");
        row.getChildren().addAll(avatar, body);
        ProfileHoverCard.attachIdentity(
                avatar,
                actor,
                null,
                item.actorUsername(),
                () -> openProfile(item.actorUsername())
        );
        row.setOnMouseClicked(event -> {
            if (item.postId() != null) openPostDetail(item.postId());
            else openProfile(item.actorUsername());
        });
        return row;
    }

    private String relativeTime(java.time.Instant instant) {
        long seconds = Math.max(0, java.time.Instant.now().getEpochSecond() - instant.getEpochSecond());
        if (seconds < 60) return seconds + "s";
        if (seconds < 3_600) return seconds / 60 + "m";
        if (seconds < 86_400) return seconds / 3_600 + "h";
        return seconds / 86_400 + "d";
    }

    private void renderBookmarks() {
        headerTitleLabel.setText("Bookmarks");
        headerTitleLabel.setFont(AppFonts.fontFor("Bookmarks", 20, FontWeight.BOLD));
        Text backArrow = new Text("←");
        backArrow.setFont(AppFonts.fontFor("←", 22, FontWeight.BOLD));
        headerTitleLabel.setGraphic(backArrow);
        headerTitleLabel.setGraphicTextGap(14);
        headerTitleLabel.setStyle("-fx-cursor: hand;");
        headerTitleLabel.setOnMouseClicked(event -> handleShowHome());
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);
        rightSidebar.getChildren().setAll(searchField, trendsWidget, suggestionWidget);

        VBox body = new VBox(12);
        body.setPadding(new Insets(0, 16, 30, 16));
        TextField bookmarkSearch = new TextField();
        bookmarkSearch.setPromptText("Search Bookmarks");
        bookmarkSearch.getStyleClass().add("search-field");
        VBox results = new VBox();
        body.getChildren().addAll(bookmarkSearch, results);
        bookmarkSearch.textProperty().addListener((observable, oldText, newText) ->
                fillBookmarkResults(results, newText));
        fillBookmarkResults(results, "");
        timelineContainer.getChildren().add(body);
    }

    private void renderCreatorStudio() {
        headerTitleLabel.setText("Creator Studio");
        headerTitleLabel.setFont(AppFonts.fontFor("Creator Studio", 20, FontWeight.BOLD));
        Text backArrow = new Text("←");
        backArrow.setFont(AppFonts.fontFor("←", 22, FontWeight.BOLD));
        headerTitleLabel.setGraphic(backArrow);
        headerTitleLabel.setGraphicTextGap(14);
        headerTitleLabel.setStyle("-fx-cursor: hand;");
        headerTitleLabel.setOnMouseClicked(event -> handleShowHome());
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);

        VBox studio = new VBox(4);
        studio.setPadding(new Insets(8, 14, 30, 14));
        studio.getChildren().add(studioHeading("Programs"));
        studio.getChildren().addAll(
                studioItem("fire-icon.svg", "Revenue Sharing", "Earn from your posts", "Ineligible"),
                studioItem("superfollows-icon.svg", "Subscriptions", "Build a subscriber community", "Ineligible"));
        studio.getChildren().add(studioHeading("Tools"));
        studio.getChildren().addAll(
                studioItem("movies-tv-icon.svg", "Live Studio", "Go live professionally", "New"),
                studioItem("bar-chart-icon.svg", "Analytics", "Understand how your posts perform", ""),
                studioItem("sparkle-icon.svg", "Inspiration", "Top posts by engagement", ""));
        studio.getChildren().add(studioHeading("Support"));
        studio.getChildren().addAll(
                studioItem("chat-icon.svg", "Contact support", "Get help from X support", ""),
                studioItem("help-circle-icon.svg", "Learn more", "Creator Studio resources", ""));
        timelineContainer.getChildren().add(studio);
    }

    private Label studioHeading(String text) {
        Label heading = new Label(text);
        heading.setFont(AppFonts.fontFor(text, 16, FontWeight.BOLD));
        heading.setPadding(new Insets(15, 0, 6, 0));
        return heading;
    }

    private HBox studioItem(String icon, String title, String detail, String badge) {
        VBox words = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setFont(AppFonts.fontFor(title, 16, FontWeight.BOLD));
        Label detailLabel = new Label(detail);
        detailLabel.setTextFill(Color.web("#536471"));
        detailLabel.setFont(AppFonts.fontFor(detail, 14));
        words.getChildren().addAll(titleLabel, detailLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badgeLabel = new Label(badge);
        badgeLabel.setManaged(!badge.isBlank());
        badgeLabel.setVisible(!badge.isBlank());
        badgeLabel.getStyleClass().add(badge.equals("New") ? "studio-badge-new" : "studio-badge-muted");
        Label chevron = new Label("›");
        chevron.setTextFill(Color.web("#536471"));
        chevron.setFont(AppFonts.fontFor("›", 25));
        HBox item = new HBox(14, AppIcons.icon(icon, 24, "#0f1419"), words, spacer, badgeLabel, chevron);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("studio-item");
        return item;
    }

    private void renderGrok() {
        hideRightSidebar();
        headerTitleLabel.setText("Grok");
        headerTitleLabel.setFont(AppFonts.fontFor("Grok", 20, FontWeight.BOLD));
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);
        VBox center = new VBox(16);
        center.setPadding(new Insets(48, 36, 36, 36));
        center.setAlignment(Pos.TOP_CENTER);
        center.getChildren().add(AppIcons.icon("logo-icon.svg", 54, "#0f1419"));
        Label title = new Label("Ask Grok");
        title.setFont(AppFonts.fontFor(title.getText(), 30, FontWeight.BOLD));
        Label detail = new Label("Explore a topic, explain an idea, or draft your next post.");
        detail.setTextFill(Color.web("#536471"));
        VBox answers = new VBox(10);
        TextField question = new TextField();
        question.setPromptText("What do you want to know?");
        question.getStyleClass().add("search-field");
        Button ask = new Button("Ask");
        ask.getStyleClass().add("composer-post-button");
        Runnable respond = () -> {
            String value = question.getText().trim();
            if (value.isBlank()) return;
            Label prompt = new Label(value);
            prompt.getStyleClass().add("message-sent");
            Label response = new Label("Here’s a useful way to approach it: identify the goal, verify the important facts, then turn the result into a clear next action.");
            response.setWrapText(true);
            response.setMaxWidth(520);
            response.getStyleClass().add("message-received");
            answers.getChildren().addAll(new HBox(prompt), response);
            question.clear();
        };
        ask.setOnAction(event -> respond.run());
        question.setOnAction(event -> respond.run());
        HBox composer = new HBox(8, question, ask);
        HBox.setHgrow(question, Priority.ALWAYS);
        center.getChildren().addAll(title, detail, answers, composer);
        timelineContainer.getChildren().add(center);
    }

    private void renderPremium() {
        hideRightSidebar();
        headerTitleLabel.setText("Premium");
        headerTitleLabel.setFont(AppFonts.fontFor("Premium", 20, FontWeight.BOLD));
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);
        VBox body = new VBox(14);
        body.setPadding(new Insets(32));
        Label title = new Label("Upgrade your experience");
        title.setFont(AppFonts.fontFor(title.getText(), 30, FontWeight.BOLD));
        Label detail = new Label("Get additional creation tools and profile features.");
        detail.setTextFill(Color.web("#536471"));
        body.getChildren().addAll(title, detail,
                premiumFeature("Longer posts", "Write beyond the standard 280-character limit."),
                premiumFeature("Edit posts", "Correct a post after publishing."),
                premiumFeature("Highlights", "Feature your best posts on your profile."),
                premiumFeature("Reduced ads", "See fewer promoted posts in your timeline."));
        Button subscribe = new Button("Subscribe");
        subscribe.getStyleClass().add("primary-post-button");
        subscribe.setMaxWidth(260);
        subscribe.setOnAction(event -> subscribe.setText("Premium isn’t available in this local clone"));
        body.getChildren().add(subscribe);
        timelineContainer.getChildren().add(body);
    }

    private HBox premiumFeature(String title, String detail) {
        Label check = new Label("✓");
        check.setTextFill(Color.web("#1d9bf0"));
        check.setFont(AppFonts.fontFor("✓", 22, FontWeight.BOLD));
        Label heading = new Label(title);
        heading.setFont(AppFonts.fontFor(title, 16, FontWeight.BOLD));
        Label description = new Label(detail);
        description.setTextFill(Color.web("#536471"));
        return new HBox(12, check, new VBox(3, heading, description));
    }

    private void renderMoreCenter() {
        hideRightSidebar();
        headerTitleLabel.setText(moreSection);
        headerTitleLabel.setFont(AppFonts.fontFor(moreSection, 20, FontWeight.BOLD));
        headerFilterButton.setManaged(false);
        headerFilterButton.setVisible(false);
        String description = switch (moreSection) {
            case "Lists" -> "Create curated timelines for the accounts and topics you care about.";
            case "Communities" -> "Join focused conversations with people who share your interests.";
            case "Monetization" -> "Creator earnings and subscriptions appear here when enabled.";
            case "Settings and privacy" -> "Manage account security, privacy, accessibility, and notification preferences.";
            case "Help Center" -> "Find guidance for using the app and reporting a problem.";
            default -> "Choose an option from the More menu.";
        };
        timelineContainer.getChildren().add(createLargeEmptyState(moreSection, description));
    }

    private void fillBookmarkResults(VBox results, String query) {
        results.getChildren().clear();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        List<Post> bookmarks = postStore.getBookmarkedPosts().stream()
                .filter(post -> normalized.isBlank()
                        || post.getContent().toLowerCase().contains(normalized)
                        || post.getAuthorName().toLowerCase().contains(normalized)
                        || post.getAuthorUsername().toLowerCase().contains(normalized))
                .toList();
        bookmarks.forEach(post -> results.getChildren().add(createPostCard(post)));
        if (bookmarks.isEmpty()) {
            String title = normalized.isBlank() ? "Save posts for later" : "No matching bookmarks";
            String detail = normalized.isBlank()
                    ? "Bookmark posts to easily find them again in the future."
                    : "Try another word or phrase.";
            results.getChildren().add(createLargeEmptyState(title, detail));
        }
    }

    private void renderChat() {
        hideRightSidebar();
        topHeader.setManaged(false);
        topHeader.setVisible(false);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        String username = UserSession.getInstance().getUsername();
        if (!ChatStore.getInstance().hasPasscode(username)) {
            renderChatOnboarding();
        } else if (!UserSession.getInstance().isChatUnlocked()) {
            renderChatUnlock();
        } else if (chatView.equals("settings")) {
            renderChatSettings();
        } else if (chatView.equals("change-passcode")) {
            renderChangePasscode();
        } else {
            renderChatCenter();
        }
    }

    private void renderChatOnboarding() {
        VBox welcome = new VBox(24);
        welcome.setAlignment(Pos.CENTER_LEFT);
        welcome.setMaxWidth(520);
        welcome.setPadding(new Insets(90, 30, 50, 30));
        Label title = new Label("Welcome to the\nnew X Chat");
        title.setFont(AppFonts.fontFor(title.getText(), 36, FontWeight.BOLD));
        welcome.getChildren().addAll(
                title,
                chatFeature("verified-check-icon.svg", "End-to-End Encryption", "Messages are end-to-end encrypted across all your devices."),
                chatFeature("verified-check-icon.svg", "State-of-the-Art Privacy", "There’s no way for anyone, including X, to read your messages."),
                chatFeature("settings-gear-icon.svg", "Set Passcode", "To secure your messages, you’ll need to set up a passcode."));
        Button create = new Button("Create Passcode");
        create.setMaxWidth(Double.MAX_VALUE);
        create.getStyleClass().add("primary-post-button");
        create.setOnAction(event -> renderChatPasscodeSetup());
        welcome.getChildren().add(create);
        HBox holder = new HBox(welcome);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(holder, Priority.ALWAYS);
        timelineContainer.getChildren().add(holder);
    }

    private void renderChatUnlock() {
        VBox unlock = new VBox(20);
        unlock.setAlignment(Pos.CENTER);
        unlock.setMaxWidth(520);
        unlock.setPadding(new Insets(100, 30, 100, 30));

        Label lockIcon = new Label();
        lockIcon.setGraphic(AppIcons.icon("settings-gear-icon.svg", 48, "#0f1419"));
        
        Label title = new Label("Enter your passcode");
        title.setFont(AppFonts.fontFor(title.getText(), 28, FontWeight.BOLD));
        
        Label detail = new Label("To access your encrypted direct messages, enter your chat passcode.");
        detail.setWrapText(true);
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        detail.setTextFill(Color.web("#536471"));
        detail.setFont(AppFonts.fontFor(detail.getText(), 15));

        PasswordField[] digits = new PasswordField[4];
        HBox digitRow = new HBox(18);
        digitRow.setAlignment(Pos.CENTER);
        for (int index = 0; index < digits.length; index++) {
            PasswordField digit = new PasswordField();
            digit.setPrefSize(58, 58);
            digit.setMaxWidth(58);
            digit.setAlignment(Pos.CENTER);
            digit.getStyleClass().add("passcode-digit");
            digits[index] = digit;
            digitRow.getChildren().add(digit);
        }

        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#f42330"));
        errorLabel.setFont(AppFonts.fontFor("", 14, FontWeight.BOLD));
        errorLabel.setWrapText(true);

        unlock.getChildren().addAll(lockIcon, title, detail, digitRow, errorLabel);
        
        String username = UserSession.getInstance().getUsername();
        for (int index = 0; index < digits.length; index++) {
            int currentIndex = index;
            int nextIndex = index + 1;
            PasswordField field = digits[index];
            field.textProperty().addListener((observable, oldText, newText) -> {
                String sanitized = newText.replaceAll("\\D", "");
                if (sanitized.length() > 1) sanitized = sanitized.substring(sanitized.length() - 1);
                if (!sanitized.equals(newText)) {
                    field.setText(sanitized);
                    return;
                }
                if (sanitized.length() == 1 && nextIndex < digits.length) {
                    digits[nextIndex].requestFocus();
                }
                
                boolean allEntered = java.util.Arrays.stream(digits).allMatch(candidate -> candidate.getText().length() == 1);
                if (allEntered) {
                    String passcode = java.util.Arrays.stream(digits).map(PasswordField::getText).reduce("", String::concat);
                    if (ChatStore.getInstance().verifyPasscode(username, passcode)) {
                        UserSession.getInstance().setChatUnlocked(true);
                        UserSession.getInstance().setChatPasscode(passcode);
                        refreshTimeline();
                    } else {
                        errorLabel.setText("Incorrect passcode. Please try again.");
                        for (PasswordField d : digits) d.clear();
                        digits[0].requestFocus();
                    }
                }
            });
        }

        HBox holder = new HBox(unlock);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(holder, Priority.ALWAYS);
        timelineContainer.getChildren().add(holder);
        Platform.runLater(() -> digits[0].requestFocus());
    }

    private HBox chatFeature(String icon, String title, String detail) {
        VBox words = new VBox(3);
        Label heading = new Label(title);
        heading.setFont(AppFonts.fontFor(title, 16, FontWeight.BOLD));
        Label description = new Label(detail);
        description.setWrapText(true);
        description.setMaxWidth(370);
        description.setTextFill(Color.web("#536471"));
        description.setFont(AppFonts.fontFor(detail, 15));
        words.getChildren().addAll(heading, description);
        HBox row = new HBox(16, AppIcons.icon(icon, 28, "#0f1419"), words);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void renderChatPasscodeSetup() {
        timelineContainer.getChildren().clear();

        VBox setup = new VBox(20);
        setup.setAlignment(Pos.CENTER);
        setup.setMaxWidth(520);
        setup.setPadding(new Insets(100, 30, 100, 30));

        Label gearIcon = new Label();
        gearIcon.setGraphic(AppIcons.icon("settings-gear-icon.svg", 48, "#0f1419"));

        Label title = new Label("Create Passcode");
        title.setFont(AppFonts.fontFor(title.getText(), 28, FontWeight.BOLD));

        Label detail = new Label("Choose a four-digit passcode to protect your encrypted chats.");
        detail.setWrapText(true);
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        detail.setTextFill(Color.web("#536471"));
        detail.setFont(AppFonts.fontFor(detail.getText(), 15));

        PasswordField[] digits = new PasswordField[4];
        HBox digitRow = new HBox(18);
        digitRow.setAlignment(Pos.CENTER);
        for (int index = 0; index < digits.length; index++) {
            PasswordField digit = new PasswordField();
            digit.setPrefSize(58, 58);
            digit.setMaxWidth(58);
            digit.setAlignment(Pos.CENTER);
            digit.getStyleClass().add("passcode-digit");
            digits[index] = digit;
            digitRow.getChildren().add(digit);
        }

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#f91880"));
        errorLabel.setFont(AppFonts.fontFor("", 14));
        errorLabel.setVisible(false);

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER);

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: #eff3f4; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 99; -fx-cursor: hand;");
        cancel.setOnAction(event -> {
            timelineContainer.getChildren().clear();
            renderChatOnboarding();
        });

        Button createBtn = new Button("Create Passcode");
        createBtn.getStyleClass().add("primary-post-button");
        createBtn.setDisable(true);

        buttonRow.getChildren().addAll(cancel, createBtn);

        setup.getChildren().addAll(gearIcon, title, detail, digitRow, errorLabel, buttonRow);

        HBox holder = new HBox(setup);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(holder, Priority.ALWAYS);
        timelineContainer.getChildren().add(holder);

        Platform.runLater(digits[0]::requestFocus);

        for (int index = 0; index < digits.length; index++) {
            int nextIndex = index + 1;
            PasswordField field = digits[index];
            field.textProperty().addListener((observable, oldText, newText) -> {
                String sanitized = newText.replaceAll("\\D", "");
                if (sanitized.length() > 1) sanitized = sanitized.substring(sanitized.length() - 1);
                if (!sanitized.equals(newText)) {
                    field.setText(sanitized);
                    return;
                }
                if (sanitized.length() == 1 && nextIndex < digits.length) digits[nextIndex].requestFocus();
                createBtn.setDisable(java.util.Arrays.stream(digits).anyMatch(candidate -> candidate.getText().length() != 1));
            });
            field.setOnMouseClicked(e -> field.selectAll());
        }

        createBtn.setOnAction(event -> {
            String passcode = java.util.Arrays.stream(digits).map(PasswordField::getText).reduce("", String::concat);
            if (passcode.length() == 4) {
                String username = UserSession.getInstance().getUsername();
                ChatStore.getInstance().setPasscode(username, passcode);
                UserSession.getInstance().setChatUnlocked(true);
                UserSession.getInstance().setChatPasscode(passcode);
                refreshTimeline();
            }
        });
    }

    private void renderChatSettings() {
        String username = UserSession.getInstance().getUsername();
        ChatStore.ChatPreferences preferences = ChatStore.getInstance().preferences(username);

        VBox page = new VBox();
        page.setMaxWidth(720);
        page.setPadding(new Insets(0, 18, 36, 18));
        page.getChildren().add(chatSettingsHeader("Settings", () -> {
            chatView = "inbox";
            refreshTimeline();
        }));

        VBox requestSection = settingsSection("Allow message requests:",
                "People you follow will always be able to message you.");
        ToggleGroup requestGroup = new ToggleGroup();
        RadioButton noOne = settingsRadio("No one", requestGroup, preferences.messageRequests().equals("No one"));
        RadioButton checkmark = settingsRadio("Checkmark users", requestGroup,
                preferences.messageRequests().equals("Checkmark users"));
        RadioButton everyone = settingsRadio("Everyone", requestGroup, preferences.messageRequests().equals("Everyone"));
        requestSection.getChildren().addAll(noOne, checkmark, everyone);

        CheckBox subscribers = new CheckBox("Allow messages from my subscribers");
        subscribers.setSelected(preferences.subscriberMessages());
        subscribers.getStyleClass().add("chat-settings-check");
        Label subscriberDetail = settingsDetail(
                "Your subscribers will always be able to send you messages independent of other messaging settings.");
        requestSection.getChildren().addAll(new Separator(), subscribers, subscriberDetail);

        VBox encrypted = settingsSection("Encrypted messages", "");
        Button changePasscode = settingsLinkButton("Change passcode", "passcode-icon.svg");
        changePasscode.setOnAction(event -> {
            chatView = "change-passcode";
            refreshTimeline();
        });
        encrypted.getChildren().add(changePasscode);

        VBox storage = settingsSection("Local data & storage",
                "Manage cached media and auto-delete settings.");
        HBox cachedMedia = settingsValueRow("Total cached media", formatBytes(MediaLibrary.cacheSizeBytes()));
        Label autoDeleteHeading = new Label("Auto-delete media older than");
        autoDeleteHeading.getStyleClass().add("chat-settings-label");
        ToggleGroup retentionGroup = new ToggleGroup();
        RadioButton keepAll = settingsRadio("Keep all", retentionGroup, preferences.autoDeleteMedia().equals("Keep all"));
        RadioButton thirtyDays = settingsRadio("30 days", retentionGroup, preferences.autoDeleteMedia().equals("30 days"));
        RadioButton sixtyDays = settingsRadio("60 days", retentionGroup, preferences.autoDeleteMedia().equals("60 days"));
        RadioButton sixMonths = settingsRadio("6 months", retentionGroup, preferences.autoDeleteMedia().equals("6 months"));
        Button deleteMedia = new Button("Delete all media");
        deleteMedia.getStyleClass().add("chat-danger-link");
        deleteMedia.setOnAction(event -> {
            Window owner = deleteMedia.getScene() == null ? null : deleteMedia.getScene().getWindow();
            if (XDialog.confirm(
                    owner,
                    "Delete cached media?",
                    "Your post and profile uploads will not be affected.",
                    "Delete",
                    true
            )) {
                try {
                    MediaLibrary.clearCache();
                    refreshTimeline();
                } catch (IOException exception) {
                    System.err.println("Could not clear chat media cache: " + exception.getMessage());
                }
            }
        });
        storage.getChildren().addAll(cachedMedia, autoDeleteHeading, keepAll, thirtyDays, sixtyDays, sixMonths, deleteMedia);

        VBox troubleshooting = settingsSection("Troubleshooting", "");
        CheckBox debugLogs = new CheckBox("Enable Debug Logs");
        debugLogs.setSelected(preferences.debugLogs());
        debugLogs.getStyleClass().add("chat-settings-check");
        Label debugDetail = settingsDetail(
                "Enabling this will write X Chat logs to your device. Message contents are not included so engineers can troubleshoot issues.");
        troubleshooting.getChildren().addAll(debugLogs, debugDetail);

        Runnable savePreferences = () -> {
            RadioButton selectedRequest = (RadioButton) requestGroup.getSelectedToggle();
            RadioButton selectedRetention = (RadioButton) retentionGroup.getSelectedToggle();
            ChatStore.getInstance().savePreferences(username, new ChatStore.ChatPreferences(
                    selectedRequest == null ? "Checkmark users" : selectedRequest.getText(),
                    subscribers.isSelected(),
                    selectedRetention == null ? "30 days" : selectedRetention.getText(),
                    debugLogs.isSelected()));
        };
        requestGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> savePreferences.run());
        retentionGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> savePreferences.run());
        subscribers.selectedProperty().addListener((observable, oldValue, newValue) -> savePreferences.run());
        debugLogs.selectedProperty().addListener((observable, oldValue, newValue) -> savePreferences.run());

        page.getChildren().addAll(requestSection, encrypted, storage, troubleshooting);
        timelineContainer.getChildren().add(page);
    }

    private void renderChangePasscode() {
        VBox page = new VBox(20);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(0, 24, 80, 24));
        page.getChildren().add(chatSettingsHeader("Change Passcode", () -> {
            chatView = "settings";
            refreshTimeline();
        }));

        Label icon = new Label();
        icon.setGraphic(AppIcons.icon("passcode-icon.svg", 42, "#0f1419"));
        Label title = new Label("Change Passcode");
        title.setFont(AppFonts.fontFor(title.getText(), 25, FontWeight.BOLD));
        Label detail = new Label("This passcode should be memorable and kept private.\n"
                + "Without it, you will not be able to access your messages.");
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        detail.setTextFill(Color.web("#536471"));

        PasswordField[] digits = new PasswordField[4];
        HBox digitRow = new HBox(18);
        digitRow.setAlignment(Pos.CENTER);
        Label status = new Label();
        status.setTextFill(Color.web("#00ba7c"));
        for (int index = 0; index < digits.length; index++) {
            PasswordField digit = new PasswordField();
            digit.setPrefSize(58, 58);
            digit.setMaxSize(58, 58);
            digit.setAlignment(Pos.CENTER);
            digit.getStyleClass().add("passcode-digit");
            digits[index] = digit;
            digitRow.getChildren().add(digit);
        }

        for (int index = 0; index < digits.length; index++) {
            int nextIndex = index + 1;
            PasswordField field = digits[index];
            field.textProperty().addListener((observable, oldText, newText) -> {
                String sanitized = newText.replaceAll("\\D", "");
                if (sanitized.length() > 1) sanitized = sanitized.substring(sanitized.length() - 1);
                if (!sanitized.equals(newText)) {
                    field.setText(sanitized);
                    return;
                }
                if (sanitized.length() == 1 && nextIndex < digits.length) digits[nextIndex].requestFocus();
                if (java.util.Arrays.stream(digits).allMatch(candidate -> candidate.getText().length() == 1)) {
                    String passcode = java.util.Arrays.stream(digits)
                            .map(PasswordField::getText).reduce("", String::concat);
                    String username = UserSession.getInstance().getUsername();
                    ChatStore.getInstance().setPasscode(username, passcode);
                    UserSession.getInstance().setChatPasscode(passcode);
                    status.setText("Passcode changed");
                    Timeline returnToSettings = new Timeline(new KeyFrame(Duration.millis(450), event -> {
                        chatView = "settings";
                        refreshTimeline();
                    }));
                    returnToSettings.play();
                }
            });
            field.setOnMouseClicked(event -> field.selectAll());
        }

        page.getChildren().addAll(icon, title, detail, digitRow, status);
        timelineContainer.getChildren().add(page);
        Platform.runLater(digits[0]::requestFocus);
    }

    private HBox chatSettingsHeader(String title, Runnable backAction) {
        Button back = new Button();
        back.getStyleClass().add("icon-button");
        back.setGraphic(AppIcons.icon("arrow-left-icon.svg", 20, "#0f1419"));
        back.setAccessibleText("Back");
        back.setOnAction(event -> backAction.run());
        Label heading = new Label(title);
        heading.setFont(AppFonts.fontFor(title, 21, FontWeight.BOLD));
        HBox header = new HBox(12, back, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 0, 14, 0));
        header.setMaxWidth(Double.MAX_VALUE);
        return header;
    }

    private VBox settingsSection(String title, String detail) {
        VBox section = new VBox(7);
        section.getStyleClass().add("chat-settings-section");
        Label heading = new Label(title);
        heading.getStyleClass().add("chat-settings-heading");
        section.getChildren().add(heading);
        if (detail != null && !detail.isBlank()) section.getChildren().add(settingsDetail(detail));
        return section;
    }

    private Label settingsDetail(String text) {
        Label detail = new Label(text);
        detail.setWrapText(true);
        detail.getStyleClass().add("chat-settings-detail");
        return detail;
    }

    private RadioButton settingsRadio(String text, ToggleGroup group, boolean selected) {
        RadioButton button = new RadioButton(text);
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.getStyleClass().add("chat-settings-radio");
        return button;
    }

    private Button settingsLinkButton(String text, String icon) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setGraphic(AppIcons.icon(icon, 20, "#536471"));
        button.getStyleClass().add("chat-settings-link");
        return button;
    }

    private HBox settingsValueRow(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("chat-settings-label");
        Label amount = new Label(value);
        amount.getStyleClass().add("chat-settings-detail");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(name, spacer, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.ENGLISH, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void renderChatCenter() {
        VBox conversationListHeader = new VBox(12);
        Label heading = new Label("Chat");
        heading.setFont(AppFonts.fontFor("Chat", 21, FontWeight.BOLD));
        Button filter = new Button(chatFilter);
        filter.getStyleClass().add("chat-filter-button");
        filter.setGraphic(AppIcons.icon("chevron-down-icon.svg", 10, "#536471"));
        filter.setContentDisplay(ContentDisplay.RIGHT);
        filter.setGraphicTextGap(6);
        filter.setOnAction(event -> showChatFilter(filter));

        Button newChatBtn = new Button();
        newChatBtn.getStyleClass().add("icon-button");
        newChatBtn.setGraphic(AppIcons.icon("messages-plus-icon.svg", 20, "#0f1419"));
        newChatBtn.setOnAction(event -> showNewChatDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox chatHeader = new HBox(8, heading, spacer, filter, newChatBtn);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        TextField chatSearch = new TextField();
        chatSearch.setPromptText("Search");
        chatSearch.getStyleClass().add("search-field");
        conversationListHeader.getChildren().addAll(chatHeader, chatSearch);

        VBox conversationRows = new VBox(12);
        String activeUsername = UserSession.getInstance().getUsername();
        List<ChatStore.Conversation> rawList = ChatStore.getInstance().forUser(activeUsername);
        if (chatFilter.equals("Unread")) rawList = rawList.stream()
                .filter(conversation -> conversation.unreadFor(activeUsername)).toList();
        if (chatFilter.equals("Groups")) rawList = rawList.stream()
                .filter(conversation -> conversation.participants().size() > 2).toList();
        if (chatFilter.equals("Direct")) rawList = rawList.stream()
                .filter(conversation -> conversation.participants().size() == 2).toList();
        
        List<ChatStore.Conversation> conversations = new java.util.ArrayList<>(rawList);
        conversations.sort((c1, c2) -> {
            boolean p1 = ChatStore.getInstance().isPinned(c1.id());
            boolean p2 = ChatStore.getInstance().isPinned(c2.id());
            if (p1 != p2) return p1 ? -1 : 1;
            return c2.lastActivity().compareTo(c1.lastActivity());
        });

        for (ChatStore.Conversation conversation : conversations) {
            HBox row = createConversationRow(conversation, activeUsername);
            conversationRows.getChildren().add(row);
            chatSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                String other = conversation.otherParticipant(activeUsername);
                boolean matches = newValue == null || newValue.isBlank() || other.toLowerCase().contains(newValue.toLowerCase());
                row.setManaged(matches);
                row.setVisible(matches);
            });
        }
        if (conversations.isEmpty()) {
            Label emptyList = new Label("No " + chatFilter.toLowerCase() + " conversations yet");
            emptyList.setTextFill(Color.web("#536471"));
            emptyList.setPadding(new Insets(28, 10, 10, 10));
            conversationRows.getChildren().add(emptyList);
        }

        ScrollPane listScroll = new ScrollPane(conversationRows);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        VBox leftPane = new VBox(12, conversationListHeader, listScroll);
        leftPane.setMinWidth(390);
        leftPane.setPrefWidth(390);
        leftPane.setMaxWidth(390);
        leftPane.setPadding(new Insets(14));
        leftPane.setStyle("-fx-border-color: #eff3f4; -fx-border-width: 0 1 0 0;");

        VBox start = new VBox(14);
        start.setAlignment(Pos.CENTER);
        HBox.setHgrow(start, Priority.ALWAYS);
        start.setMinHeight(650);
        start.getChildren().add(AppIcons.icon("chat-icon.svg", 56, "#0f1419"));
        Label title = new Label("Start Conversation");
        title.setFont(AppFonts.fontFor(title.getText(), 22, FontWeight.BOLD));
        Label detail = new Label("Choose from your existing conversations, or start a new one.");
        detail.setTextFill(Color.web("#536471"));
        detail.setFont(AppFonts.fontFor(detail.getText(), 15));
        Button newChat = new Button("New chat");
        newChat.getStyleClass().add("composer-post-button");
        newChat.setOnAction(event -> showNewChatDialog());
        start.getChildren().addAll(title, detail, newChat);

        VBox rightPane = selectedConversationId == null ? start : createConversationPane(
                ChatStore.getInstance().get(selectedConversationId), activeUsername);
        
        HBox chatLayout = new HBox(leftPane, rightPane);
        chatLayout.prefHeightProperty().bind(contentScroll.heightProperty().subtract(2));
        
        timelineContainer.getChildren().add(chatLayout);
    }

    private HBox createConversationRow(ChatStore.Conversation conversation, String activeUsername) {
        String otherUsername = conversation.otherParticipant(activeUsername);
        AccountProfile account = AccountDirectory.find(otherUsername);
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("conversation-row");
        if (conversation.unreadFor(activeUsername)) row.getStyleClass().add("conversation-unread");
        if (selectedConversationId != null && selectedConversationId == conversation.id()) {
            row.getStyleClass().add("conversation-selected");
        }
        Node avatar = ProfileHoverCard.avatarNode(account, 44);
        VBox text = new VBox(3);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox identity = new HBox(6);
        identity.setAlignment(Pos.CENTER_LEFT);
        
        Label name = new Label(account.displayName());
        name.setFont(AppFonts.fontFor(name.getText(), 15, FontWeight.BOLD));
        name.setStyle("-fx-text-fill: #0f1419;");
        
        Label handle = new Label("@" + account.username());
        handle.setFont(AppFonts.fontFor(handle.getText(), 13));
        handle.setStyle("-fx-text-fill: #536471;");
        ProfileHoverCard.attachIdentity(
                avatar,
                name,
                handle,
                account.username(),
                () -> openProfile(account.username())
        );
        
        identity.getChildren().addAll(name, handle);
        if (ChatStore.getInstance().isPinned(conversation.id())) {
            identity.getChildren().add(AppIcons.icon("bookmark-icon.svg", 12, "#536471"));
        }
        if (ChatStore.getInstance().isMuted(conversation.id())) {
            identity.getChildren().add(AppIcons.icon("bell-icon.svg", 12, "#536471"));
        }

        String preview = conversation.messages().isEmpty() ? "Start a conversation"
                : conversation.messages().getLast().text();
        Label last = new Label(preview.length() > 42 ? preview.substring(0, 42) + "…" : preview);
        last.setStyle("-fx-text-fill: #536471;");
        text.getChildren().addAll(identity, last);
        row.getChildren().addAll(avatar, text);
        row.setOnMouseClicked(event -> {
            selectedConversationId = conversation.id();
            ChatStore.getInstance().markRead(conversation, activeUsername);
            refreshTimeline();
        });
        return row;
    }

    private VBox createConversationPane(ChatStore.Conversation conversation, String activeUsername) {
        if (conversation == null) { selectedConversationId = null; return new VBox(); }
        ChatStore.getInstance().markRead(conversation, activeUsername);
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(14, 20, 20, 20));
        HBox.setHgrow(pane, Priority.ALWAYS);
        pane.setMinHeight(650);
        AccountProfile other = AccountDirectory.find(conversation.otherParticipant(activeUsername));
        
        Node headerAvatar = ProfileHoverCard.avatarNode(other, 32);
        Label headingLabel = new Label(other.displayName() + "  @" + other.username());
        headingLabel.setFont(AppFonts.fontFor(headingLabel.getText(), 18, FontWeight.BOLD));
        ProfileHoverCard.attachIdentity(
                headerAvatar,
                headingLabel,
                null,
                other.username(),
                () -> openProfile(other.username())
        );
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        
        Button optionsBtn = new Button();
        optionsBtn.getStyleClass().add("icon-button");
        optionsBtn.setGraphic(AppIcons.icon("more-horizontal-filled-icon.svg", 18, "#536471"));
        optionsBtn.setOnAction(event -> {
            ContextMenu menu = new ContextMenu();
            
            MenuItem viewProfileItem = new MenuItem("View profile");
            viewProfileItem.setGraphic(AppIcons.icon("logo-icon.svg", 14, "#536471"));
            viewProfileItem.setOnAction(e -> openProfile(other.username()));
            
            boolean isPinned = ChatStore.getInstance().isPinned(conversation.id());
            MenuItem pinItem = new MenuItem(isPinned ? "Unpin conversation" : "Pin conversation");
            pinItem.setGraphic(AppIcons.icon("bookmark-icon.svg", 14, "#536471"));
            pinItem.setOnAction(e -> {
                ChatStore.getInstance().pinConversation(conversation.id(), !isPinned);
                refreshTimeline();
            });

            boolean isMuted = ChatStore.getInstance().isMuted(conversation.id());
            MenuItem muteItem = new MenuItem(isMuted ? "Unmute conversation" : "Mute conversation");
            muteItem.setGraphic(AppIcons.icon("bell-icon.svg", 14, "#536471"));
            muteItem.setOnAction(e -> {
                ChatStore.getInstance().muteConversation(conversation.id(), !isMuted);
                refreshTimeline();
            });

            MenuItem deleteItem = new MenuItem("Delete conversation");
            deleteItem.setStyle("-fx-text-fill: #f4212e;");
            deleteItem.setGraphic(AppIcons.icon("image-off-icon.svg", 14, "#f4212e"));
            deleteItem.setOnAction(e -> {
                Window owner = pane.getScene() == null ? null : pane.getScene().getWindow();
                if (XDialog.confirm(
                        owner,
                        "Delete conversation with " + other.displayName() + "?",
                        "This will delete the conversation from your inbox. The other person will still be able to see it.",
                        "Delete",
                        true
                )) {
                    ChatStore.getInstance().deleteConversation(conversation.id());
                    selectedConversationId = null;
                    refreshTimeline();
                }
            });
            
            menu.getItems().addAll(viewProfileItem, pinItem, muteItem, deleteItem);
            menu.show(optionsBtn, Side.BOTTOM, 0, 0);
        });

        HBox headingBox = new HBox(10, headerAvatar, headingLabel, headerSpacer, optionsBtn);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        headingBox.setStyle("-fx-border-color: #eff3f4; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 8 0;");
        
        VBox messages = new VBox(10);
        messages.setPadding(new Insets(10, 20, 10, 10));
        int messageCount = conversation.messages().size();
        for (int i = 0; i < messageCount; i++) {
            ChatStore.ChatMessage message = conversation.messages().get(i);
            boolean isSent = message.sender().equalsIgnoreCase(activeUsername);
            
            VBox bubbleWrapper = new VBox(2);
            bubbleWrapper.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            bubbleWrapper.setFillWidth(false);
            
            Label bubble = new Label(message.text());
            bubble.setWrapText(true);
            bubble.setMaxWidth(430);
            bubble.getStyleClass().add(isSent ? "message-sent" : "message-received");
            
            String timeStr = formatTime(message.createdAt());
            
            bubbleWrapper.getChildren().add(bubble);
            
            if (isSent && i == messageCount - 1) {
                String otherParticipant = conversation.otherParticipant(activeUsername);
                boolean isUnreadByOther = conversation.unreadFor(otherParticipant);
                String status = isUnreadByOther ? "Sent" : "Seen";
                Label statusLabel = new Label(timeStr + " · " + status);
                statusLabel.setTextFill(Color.web("#71767b"));
                statusLabel.setFont(AppFonts.fontFor(statusLabel.getText(), 11));
                bubbleWrapper.getChildren().add(statusLabel);
            } else {
                Label timeLabel = new Label(timeStr);
                timeLabel.setTextFill(Color.web("#71767b"));
                timeLabel.setFont(AppFonts.fontFor(timeStr, 11));
                bubbleWrapper.getChildren().add(timeLabel);
            }
            
            HBox line = new HBox();
            line.setSpacing(8);
            line.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            if (isSent) {
                line.getChildren().add(bubbleWrapper);
            } else {
                Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(message.sender()), 28);
                ProfileHoverCard.attach(
                        avatar,
                        message.sender(),
                        () -> openProfile(message.sender())
                );
                VBox avatarHolder = new VBox(avatar);
                avatarHolder.setAlignment(Pos.TOP_LEFT);
                line.getChildren().addAll(avatarHolder, bubbleWrapper);
            }
            messages.getChildren().add(line);
        }
        ScrollPane messageScroll = new ScrollPane(messages);
        messageScroll.setFitToWidth(true);
        messageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(messageScroll, Priority.ALWAYS);
        TextField input = new TextField();
        input.setPromptText("Start a new message");
        input.getStyleClass().add("search-field");
        Button send = new Button("Send");
        send.getStyleClass().add("composer-post-button");
        Runnable sendMessage = () -> {
            if (input.getText().isBlank()) return;
            String textContent = input.getText().trim();
            ChatStore.getInstance().send(conversation, activeUsername, textContent);
            input.clear();
            refreshTimeline();
        };
        send.setOnAction(event -> sendMessage.run());
        input.setOnAction(event -> sendMessage.run());
        HBox composer = new HBox(8, input, send);
        HBox.setHgrow(input, Priority.ALWAYS);
        pane.getChildren().addAll(headingBox, messageScroll, composer);
        Platform.runLater(() -> messageScroll.setVvalue(1));
        return pane;
    }

    private String formatTime(java.time.Instant instant) {
        if (instant == null) return "";
        java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);
        return zdt.format(formatter);
    }

    private void showNewChatDialog() {
        String activeUsername = UserSession.getInstance().getUsername();
        List<AccountProfile> profiles = AccountDirectory.all().stream()
                .filter(p -> !p.username().equalsIgnoreCase(activeUsername))
                .sorted(Comparator.comparing(AccountProfile::displayName))
                .toList();

        if (profiles.isEmpty()) return;

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        
        Window owner = homeNavButton.getScene().getWindow();
        modal.initOwner(owner);

        VBox root = new VBox(14);
        root.getStyleClass().add("premium-modal");
        root.setPrefWidth(420);
        root.setMaxWidth(420);
        root.setPadding(new Insets(18));

        Label title = new Label("New message");
        title.setFont(AppFonts.fontFor(title.getText(), 19, FontWeight.BOLD));
        title.setTextFill(Color.web("#0f1419"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setFont(AppFonts.fontFor("✕", 13, FontWeight.BOLD));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #536471; -fx-cursor: hand; -fx-padding: 4;");
        closeBtn.setOnAction(event -> modal.close());

        HBox header = new HBox(title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search people");
        searchField.getStyleClass().add("modal-search-field");

        VBox listContainer = new VBox(6);
        listContainer.setStyle("-fx-background-color: transparent;");

        Runnable updateList = () -> {
            listContainer.getChildren().clear();
            String query = searchField.getText().toLowerCase().trim();
            for (AccountProfile profile : profiles) {
                if (!query.isEmpty() && 
                    !profile.displayName().toLowerCase().contains(query) && 
                    !profile.username().toLowerCase().contains(query)) {
                    continue;
                }
                
                HBox row = new HBox(12);
                row.getStyleClass().add("modal-account-row");
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-cursor: hand;");
                
                Node avatar = ProfileHoverCard.avatarNode(profile, 36);
                
                VBox textDetails = new VBox(2);
                HBox.setHgrow(textDetails, Priority.ALWAYS);
                
                Label name = new Label(profile.displayName());
                name.setFont(AppFonts.fontFor(profile.displayName(), 15, FontWeight.BOLD));
                name.setTextFill(Color.web("#0f1419"));
                name.setStyle("-fx-text-fill: #0f1419;");
                
                Label handle = new Label("@" + profile.username());
                handle.setFont(AppFonts.fontFor(profile.username(), 13));
                handle.setTextFill(Color.web("#536471"));
                handle.setStyle("-fx-text-fill: #536471;");
                
                textDetails.getChildren().addAll(name, handle);
                row.getChildren().addAll(avatar, textDetails);
                
                row.setOnMouseClicked(event -> {
                    ChatStore.Conversation conversation = ChatStore.getInstance().startDirect(activeUsername, profile.username());
                    selectedConversationId = conversation.id();
                    refreshTimeline();
                    modal.close();
                });
                
                listContainer.getChildren().add(row);
            }
            if (listContainer.getChildren().isEmpty()) {
                Label noResults = new Label("No results found");
                noResults.setTextFill(Color.web("#536471"));
                noResults.setPadding(new Insets(12));
                noResults.setFont(AppFonts.fontFor("No results found", 14));
                listContainer.getChildren().add(noResults);
            }
        };

        searchField.textProperty().addListener((obs, ov, nv) -> updateList.run());
        updateList.run();

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.getStyleClass().add("modal-scroll-pane");

        root.getChildren().addAll(header, searchField, scroll);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/twitter.css").toExternalForm());
        modal.setScene(scene);

        modal.setOnShown(event -> {
            modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2);
            modal.setY(owner.getY() + (owner.getHeight() - modal.getHeight()) / 2);
        });

        modal.show();
    }

    private void showChatFilter(Button anchor) {
        ContextMenu menu = new ContextMenu();
        java.util.Map<String, String> icons = java.util.Map.of(
                "All", "chat-icon.svg",
                "Unread", "chat-unread-icon.svg",
                "Direct", "direct-message-icon.svg",
                "Groups", "group-message-icon.svg");
        for (String option : List.of("All", "Unread", "Direct", "Groups")) {
            MenuItem item = new MenuItem(option);
            item.setGraphic(AppIcons.icon(icons.get(option), 18, "#0f1419"));
            item.setOnAction(event -> {
                chatFilter = option;
                chatView = "inbox";
                refreshTimeline();
            });
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem settings = new MenuItem("Settings");
        settings.setGraphic(AppIcons.icon("settings-gear-icon.svg", 18, "#0f1419"));
        settings.setOnAction(event -> {
            chatView = "settings";
            refreshTimeline();
        });
        MenuItem markRead = new MenuItem("Mark all as read");
        markRead.setGraphic(AppIcons.icon("mark-read-icon.svg", 18, "#0f1419"));
        markRead.setOnAction(event -> {
            ChatStore.getInstance().markAllRead(UserSession.getInstance().getUsername());
            refreshTimeline();
        });
        menu.getItems().addAll(settings, markRead);
        menu.show(anchor, Side.BOTTOM, -10, 2);
    }

    private void hideRightSidebar() {
        rightSidebar.setManaged(false);
        rightSidebar.setVisible(false);
        centerColumn.setPrefWidth(1010);
        centerColumn.setMaxWidth(1010);
    }

    private HBox createSectionTabs(List<String> names, String active, java.util.function.Consumer<String> action) {
        HBox tabs = new HBox();
        tabs.getStyleClass().add("section-tabs");
        for (String name : names) {
            Button button = new Button(name);
            button.getStyleClass().add("section-tab-button");
            button.setMouseTransparent(true);
            if (name.equals(active)) button.getStyleClass().add("section-tab-active");
            
            StackPane wrapper = new StackPane(button);
            StackPane.setAlignment(button, Pos.CENTER);
            HBox.setHgrow(wrapper, Priority.ALWAYS);
            wrapper.setMaxWidth(Double.MAX_VALUE);
            wrapper.getStyleClass().add("section-tab-wrapper");
            wrapper.setOnMouseClicked(event -> action.accept(name));
            
            tabs.getChildren().add(wrapper);
        }
        return tabs;
    }

    private VBox createLargeEmptyState(String titleText, String detailText) {
        VBox state = new VBox(8);
        state.setMaxWidth(520);
        state.setPadding(new Insets(34, 28, 50, 28));
        Label title = new Label(titleText);
        title.setWrapText(true);
        title.setFont(AppFonts.fontFor(titleText, 31, FontWeight.BOLD));
        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.setTextFill(Color.web("#536471"));
        detail.setFont(AppFonts.fontFor(detailText, 15));
        state.getChildren().addAll(title, detail);
        return state;
    }

    private Label sectionHeading(String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add("discovery-section-heading");
        heading.setPadding(new Insets(14, 14, 10, 14));
        return heading;
    }

    private void addNewsItem(VBox target, String headline, String metadata) {
        VBox item = new VBox(5);
        item.getStyleClass().add("discovery-item");
        item.setStyle("-fx-cursor: hand;");
        Label title = new Label(headline);
        title.setWrapText(true);
        title.getStyleClass().add("discovery-item-title");
        Label details = new Label(metadata);
        details.setTextFill(Color.web("#536471"));
        details.setFont(AppFonts.fontFor(metadata, 14));
        item.getChildren().addAll(title, details);
        item.setOnMouseClicked(event -> triggerExploreSearch(headline));
        target.getChildren().add(item);
    }

    private void addTrendItems(VBox target, List<String> trends) {
        for (String trend : trends) {
            VBox item = new VBox(3);
            item.getStyleClass().add("discovery-item");
            item.setStyle("-fx-cursor: hand;");
            Label context = new Label("Trending now");
            context.setTextFill(Color.web("#536471"));
            Label name = new Label(trend);
            name.getStyleClass().add("discovery-item-trend-name");
            Label posts = new Label("4,218 posts");
            posts.setTextFill(Color.web("#536471"));
            item.getChildren().addAll(context, name, posts);
            item.setOnMouseClicked(event -> triggerExploreSearch(trend));
            target.getChildren().add(item);
        }
    }

    private void triggerExploreSearch(String query) {
        if (query == null || query.isBlank()) return;
        viewMode = "explore";
        searchResultTab = "Top";
        activeHashtag = null;
        if (centerSearchField != null) {
            centerSearchField.setText(query);
        }
        refreshTimeline();
    }

    private VBox createEmptyState(String title) {
        String message = switch (viewMode) {
            case "bookmarks" -> "Save posts for later with the bookmark icon.";
            case "notifications" -> "Likes, reposts, and follows will appear here when the backend supports them.";
            case "chat" -> "Messages require backend conversations and delivery support.";
            case "unavailable" -> "This section is planned but is not connected yet.";
            default -> !searchQuery.isBlank() ? "No posts match “" + searchQuery + "”." : "No posts yet.";
        };
        VBox state = new VBox(8);
        state.setAlignment(javafx.geometry.Pos.CENTER);
        state.setStyle("-fx-padding: 70 30;");
        String icon = switch (viewMode) {
            case "bookmarks" -> "bookmark-icon.svg";
            case "notifications" -> "bell-icon.svg";
            case "chat" -> "chat-icon.svg";
            default -> "search-icon.svg";
        };
        Label heading = new Label(title);
        heading.setFont(AppFonts.fontFor(title, 24, FontWeight.BOLD));
        Label description = new Label(message);
        description.setWrapText(true);
        description.setMaxWidth(420);
        description.setTextFill(Color.web("#536471"));
        description.setFont(AppFonts.fontFor(message, 15));
        state.getChildren().addAll(AppIcons.icon(icon, 34, "#0f1419"), heading, description);
        return state;
    }

    private void updateActiveNavigation() {
        List<Button> buttons = List.of(homeNavButton, exploreNavButton, notificationsNavButton,
                chatNavButton, bookmarksNavButton, profileNavButton, moreNavButton);
        buttons.forEach(button -> button.getStyleClass().remove("nav-button-active"));
        Button active = switch (viewMode) {
            case "explore", "people" -> exploreNavButton;
            case "notifications" -> notificationsNavButton;
            case "chat" -> chatNavButton;
            case "bookmarks" -> bookmarksNavButton;
            case "more" -> moreNavButton;
            case "unavailable" -> moreNavButton;
            default -> homeNavButton;
        };
        if (!active.getStyleClass().contains("nav-button-active")) active.getStyleClass().add("nav-button-active");
    }

    private HBox createPostCard(Post post) {
        HBox postRow = new HBox(12);
        postRow.getStyleClass().add("post-row");
        postRow.setMaxWidth(600);
        postRow.setPrefWidth(600);

        String initial = post.getAuthorName().isBlank() ? "U" : post.getAuthorName().substring(0, 1).toUpperCase();
        javafx.scene.Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(post.getAuthorUsername()), 42);

        VBox contentStack = new VBox(5);
        HBox.setHgrow(contentStack, Priority.ALWAYS);
        contentStack.setMaxWidth(526);
        HBox headerRow = new HBox(8);

        Label displayName = new Label(post.getAuthorName());
        displayName.setTextFill(Color.web("#0f1419"));
        displayName.setFont(AppFonts.fontFor(post.getAuthorName(), 15, FontWeight.BOLD));

        Label userHandle = new Label("@" + post.getAuthorUsername());
        userHandle.setTextFill(Color.web("#536471"));
        userHandle.setFont(AppFonts.fontFor(userHandle.getText(), 14));
        Runnable openAuthor = () -> openProfile(post.getAuthorUsername());
        ProfileHoverCard.attachIdentity(
                avatar,
                displayName,
                userHandle,
                post.getAuthorUsername(),
                openAuthor
        );
        markPostLink(avatar);
        markPostLink(displayName);
        markPostLink(userHandle);

        Label timestamp = new Label();
        timestamp.setTextFill(Color.web("#536471"));
        timestamp.setFont(AppFonts.fontFor("0s", 14));
        timestamp.textProperty().bind(Bindings.createStringBinding(
                () -> "· " + postStore.relativeTime(post), postStore.clockProperty()));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button moreButton = new Button();
        moreButton.setGraphic(AppIcons.icon("more-horizontal-filled-icon.svg", 18, "#536471"));
        moreButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        moreButton.setStyle(INACTIVE_ACTION_STYLE);
        moreButton.setOnAction(event -> PostInteractions.showPostMenu(moreButton, post, this::refreshTimeline));
        headerRow.getChildren().addAll(displayName, userHandle, timestamp, headerSpacer, moreButton);

        TextFlow bodyText = createContent(post.getContent());
        bodyText.setMaxWidth(480);

        HBox actionToolbar = new HBox(26);
        actionToolbar.setStyle("-fx-padding: 6 0 0 0;");

        Button replyButton = new Button();
        Button retweetButton = new Button();
        Button likeButton = new Button();
        Button bookmarkButton = new Button();
        Button viewsButton = new Button();
        Button shareButton = new Button();
        refreshActionButtons(post, replyButton, retweetButton, likeButton, bookmarkButton);
        setActionButton(viewsButton, "bar-chart-icon.svg", post.getViews(), false, "#1d9bf0");
        shareButton.setGraphic(AppIcons.icon("link-icon.svg", 18, "#536471"));
        shareButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        shareButton.setStyle(INACTIVE_ACTION_STYLE);

        replyButton.setOnAction(event -> PostInteractions.showReply(replyButton, post, this::refreshTimeline));
        retweetButton.setOnAction(event -> PostInteractions.showRepostMenu(
                retweetButton,
                post,
                () -> refreshActionButtons(post, replyButton, retweetButton, likeButton, bookmarkButton),
                this::refreshTimeline));
        likeButton.setOnAction(event -> {
            boolean increasing = !post.isLiked();
            postStore.toggleLike(post);
            refreshActionButtons(post, replyButton, retweetButton, likeButton, bookmarkButton);
            if (increasing) PostInteractions.animateReaction(likeButton);
        });
        bookmarkButton.setOnAction(event -> {
            postStore.toggleBookmark(post);
            refreshActionButtons(post, replyButton, retweetButton, likeButton, bookmarkButton);
            if (viewMode.equals("bookmarks")) refreshTimeline();
        });
        viewsButton.setOnAction(event -> openPostDetail(post.getId()));
        shareButton.setOnAction(event -> copyPostLink(post, shareButton));

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actionToolbar.getChildren().addAll(replyButton, retweetButton, likeButton, viewsButton, actionSpacer, bookmarkButton, shareButton);
        if (post.isPinned()) {
            Label pinned = new Label("Pinned");
            pinned.setGraphic(AppIcons.icon("bookmark-icon.svg", 14, "#536471"));
            pinned.setTextFill(Color.web("#536471"));
            pinned.setFont(AppFonts.fontFor("Pinned", 13, FontWeight.BOLD));
            contentStack.getChildren().add(pinned);
        }
        contentStack.getChildren().add(headerRow);
        if (post.getReplyToId() != null) {
            Post original = postStore.getPost(post.getReplyToId());
            if (original != null) {
                Label replyingTo = new Label("Replying to @" + original.getAuthorUsername());
                replyingTo.setTextFill(Color.web("#1d9bf0"));
                replyingTo.setFont(AppFonts.fontFor(replyingTo.getText(), 14));
                replyingTo.setStyle("-fx-cursor: hand;");
                replyingTo.setOnMouseClicked(event -> openProfile(original.getAuthorUsername()));
                markPostLink(replyingTo);
                contentStack.getChildren().add(replyingTo);
            }
        }
        if (!post.getContent().isBlank()) {
            bodyText.setStyle("-fx-cursor: hand;");
            contentStack.getChildren().add(bodyText);
        }
        if (post.getMediaUri() != null) {
            Image mediaImage = MediaLibrary.loadImage(post.getMediaUri());
            if (mediaImage != null) {
                ImageView media = new ImageView(mediaImage);
                media.setFitWidth(480);
                media.setFitHeight(320);
                media.setPreserveRatio(true);
                media.setSmooth(true);
                media.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-cursor: hand;");
                media.setAccessibleText("Open media from @" + post.getAuthorUsername());
                media.setOnMouseClicked(event -> {
                    event.consume();
                    MediaViewer.show(media.getScene().getWindow(), post, postStore.getAllPosts());
                });
                markPostLink(media);
                contentStack.getChildren().add(media);
            } else {
                contentStack.getChildren().add(unavailableMediaLabel(post.getMediaUri()));
            }
        }
        if (post.getQuotedPostId() != null) {
            Post quoted = postStore.getPost(post.getQuotedPostId());
            if (quoted != null) {
                Node preview = PostComposerDialog.createPostPreview(quoted);
                markPostLink(preview);
                preview.setOnMouseClicked(event -> openPostDetail(quoted.getId()));
                contentStack.getChildren().add(preview);
            }
        }
        if (post.getPoll() != null) contentStack.getChildren().add(PollView.create(post, this::refreshTimeline));
        contentStack.getChildren().add(actionToolbar);
        postRow.getChildren().addAll(avatar, contentStack);
        postRow.setOnMouseClicked(event -> {
            if (!isInteractivePostTarget((Node) event.getTarget(), postRow)) {
                openPostDetail(post.getId());
            }
        });
        return postRow;
    }

    private Label unavailableMediaLabel(String uri) {
        Label unavailable = new Label("Media unavailable · " + MediaLibrary.displayName(uri)
                + "\nThe original file was moved or deleted.");
        unavailable.setWrapText(true);
        unavailable.setMaxWidth(480);
        unavailable.setStyle("-fx-background-color: #eff3f4; -fx-background-radius: 14;"
                + "-fx-text-fill: #536471; -fx-padding: 14;");
        return unavailable;
    }

    private TextFlow createContent(String content) {
        TextFlow flow = new TextFlow();
        Matcher matcher = TOKEN_PATTERN.matcher(content);
        int cursor = 0;

        while (matcher.find()) {
            addBodyText(flow, content.substring(cursor, matcher.start()), false);
            String token = matcher.group();
            Text tokenText = addBodyText(flow, token, true);
            markPostLink(tokenText);
            tokenText.setOnMouseClicked(event -> {
                if (token.startsWith("@")) {
                    openProfile(token.substring(1));
                } else {
                    activeHashtag = token;
                    refreshTimeline();
                }
            });
            cursor = matcher.end();
        }
        addBodyText(flow, content.substring(cursor), false);
        return flow;
    }

    private Text addBodyText(TextFlow flow, String value, boolean hashtag) {
        if (!hashtag && !value.isEmpty()) {
            Text first = null;
            int runStart = 0;
            boolean complexRun = AppFonts.usesComplexScript(value.substring(0, 1));
            for (int index = 1; index < value.length(); index++) {
                boolean complexCharacter = AppFonts.usesComplexScript(value.substring(index, index + 1));
                if (complexCharacter != complexRun && !Character.isWhitespace(value.charAt(index))) {
                    Text run = appendText(flow, value.substring(runStart, index), false);
                    if (first == null) first = run;
                    runStart = index;
                    complexRun = complexCharacter;
                }
            }
            Text run = appendText(flow, value.substring(runStart), false);
            return first == null ? run : first;
        }
        return appendText(flow, value, hashtag);
    }

    private Text appendText(TextFlow flow, String value, boolean hashtag) {
        Text text = new Text(value);
        text.setFont(AppFonts.fontFor(value, 15));
        text.setFill(hashtag ? Color.web("#1d9bf0") : Color.web("#0f1419"));
        if (hashtag) {
            text.setStyle("-fx-cursor: hand;");
        }
        flow.getChildren().add(text);
        return text;
    }

    private void refreshActionButtons(Post post, Button reply, Button retweet, Button like, Button bookmark) {
        addActionClass(reply, "reply-action");
        addActionClass(retweet, "repost-action");
        addActionClass(like, "like-action");
        addActionClass(bookmark, "bookmark-action");
        setActionButton(reply, "message-outline-icon.svg", post.getReplies(), post.isReplied(), "#1d9bf0");
        setActionButton(retweet, "repost-icon.svg", post.getRetweets(), post.isRetweeted(), "#00ba7c");
        setActionButton(like, "heart-outline-icon.svg", post.getLikes(), post.isLiked(), "#f91880");
        reply.setAccessibleText((post.isReplied() ? "Replied to" : "Reply to") + " post, " + post.getReplies() + " replies");
        retweet.setAccessibleText((post.isRetweeted() ? "Undo repost" : "Repost") + ", " + post.getRetweets() + " reposts");
        like.setAccessibleText((post.isLiked() ? "Unlike" : "Like") + " post, " + post.getLikes() + " likes");
        bookmark.setText("");
        bookmark.setGraphic(AppIcons.icon("bookmark-icon.svg", 18, post.isBookmarked() ? "#1d9bf0" : "#536471"));
        bookmark.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        bookmark.setStyle(INACTIVE_ACTION_STYLE);
        bookmark.setAccessibleText(post.isBookmarked() ? "Remove bookmark" : "Bookmark post");
    }

    private void addActionClass(Button button, String styleClass) {
        if (!button.getStyleClass().contains(styleClass)) button.getStyleClass().add(styleClass);
    }

    private void setActionButton(Button button, String icon, int count, boolean active, String activeColor) {
        String color = active ? activeColor : "#536471";
        String resolvedIcon = active && icon.equals("heart-outline-icon.svg") ? "heart-filled-icon.svg" : icon;
        button.setText(formatCount(count));
        button.setGraphic(AppIcons.icon(resolvedIcon, 18, color));
        button.setGraphicTextGap(7);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setStyle(active
                ? "-fx-background-color: transparent; -fx-text-fill: " + activeColor + "; -fx-padding: 5 4; -fx-cursor: hand;"
                : INACTIVE_ACTION_STYLE);
    }

    private String formatCount(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        if (count < 999_950) {
            double value = count / 1000.0;
            if (value < 100.0) {
                String formatted = String.format(java.util.Locale.ENGLISH, "%.1f", value);
                if (formatted.endsWith(".0")) formatted = formatted.substring(0, formatted.length() - 2);
                return formatted + "K";
            } else {
                return Math.round(value) + "K";
            }
        }
        double value = count / 1_000_000.0;
        String formatted = String.format(java.util.Locale.ENGLISH, "%.1f", value);
        if (formatted.endsWith(".0")) formatted = formatted.substring(0, formatted.length() - 2);
        return formatted + "M";
    }

    @FXML
    private void handleGoToProfile() {
        if (UserSession.getInstance().getUsername() != null) {
            postStore.requestProfile(UserSession.getInstance().getUsername());
        }
        NavigationManager.switchScene("/views/Profile.fxml");
    }

    private void openProfile(String username) {
        postStore.requestProfile(username);
        NavigationManager.switchScene("/views/Profile.fxml");
    }

    private void openPostDetail(long postId) {
        selectedPostId = postId;
        viewMode = "post-detail";
        activeHashtag = null;
        searchQuery = "";
        refreshTimeline();
        Platform.runLater(() -> contentScroll.setVvalue(0));
    }

    private void copyPostLink(Post post, Button source) {
        ClipboardContent content = new ClipboardContent();
        content.putString("xclone://post/" + post.getId());
        Clipboard.getSystemClipboard().setContent(content);
        source.setGraphic(AppIcons.icon("verified-check-icon.svg", 18, "#00ba7c"));
        Timeline reset = new Timeline(new KeyFrame(Duration.seconds(1.2), event ->
                source.setGraphic(AppIcons.icon("link-icon.svg", 18, "#536471"))));
        reset.play();
    }

    private boolean isInsideButton(Node target, Node boundary) {
        Node cursor = target;
        while (cursor != null && cursor != boundary) {
            if (cursor instanceof Button) return true;
            cursor = cursor.getParent();
        }
        return false;
    }

    private boolean isInteractivePostTarget(Node target, Node boundary) {
        Node cursor = target;
        while (cursor != null && cursor != boundary) {
            if (cursor instanceof Button || cursor.getStyleClass().contains("post-link-target")) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private void markPostLink(Node node) {
        if (node != null && !node.getStyleClass().contains("post-link-target")) {
            node.getStyleClass().add("post-link-target");
        }
    }

    @FXML
    private void handleLogoHome() {
        handleShowHome();
    }

    @FXML
    private void handleAccountMenu() {
        PostInteractions.showAccountMenu(accountMoreButton,
                () -> NavigationManager.switchScene("/views/Login.fxml"), this::handleLogout);
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        NavigationManager.switchScene("/views/Login.fxml");
    }
}
