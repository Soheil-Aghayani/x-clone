package client.controllers;

import client.AppFonts;
import client.AppIcons;
import client.NavigationManager;
import client.UserSession;
import client.profile.AccountDirectory;
import client.profile.AccountProfile;
import client.media.MediaLibrary;
import client.timeline.Post;
import client.timeline.PostStore;
import client.ui.PostComposerDialog;
import client.ui.PostInteractions;
import client.ui.ProfileHoverCard;
import client.ui.PollView;
import client.ui.EditProfileDialog;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import shared.models.User;

import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileController {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}_])([#@][\\p{L}\\p{N}_]+)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String ACTION_STYLE = "-fx-background-color: transparent; -fx-text-fill: #536471; -fx-padding: 5 4; -fx-cursor: hand;";

    private final PostStore postStore = PostStore.getInstance();
    private String activeTab = "posts";
    private AccountProfile viewedProfile;
    private boolean ownProfile;

    @FXML private Label headerNameLabel;
    @FXML private Label nameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label bioLabel;
    @FXML private Label postCountLabel;
    @FXML private Label followingCountLabel;
    @FXML private Label followerCountLabel;
    @FXML private Label sidebarNameLabel;
    @FXML private Label sidebarUsernameLabel;
    @FXML private Label sidebarAvatarLabel;
    @FXML private Label profileAvatarLabel;
    @FXML private Button brandLabel;
    @FXML private Label locationMetaLabel;
    @FXML private Label linkMetaLabel;
    @FXML private Label calendarMetaLabel;
    @FXML private Label parodyMetaLabel;
    @FXML private VBox userTweetsContainer;
    @FXML private TextField profileSearchField;
    @FXML private ImageView profileBannerImage;
    @FXML private Region profileBannerFallback;
    @FXML private Circle profileAvatarCircle;
    @FXML private Circle javaFxSuggestionAvatar;
    @FXML private Circle designSuggestionAvatar;

    @FXML private Button homeNavButton;
    @FXML private Button exploreNavButton;
    @FXML private Button notificationsNavButton;
    @FXML private Button chatNavButton;
    @FXML private Button bookmarksNavButton;
    @FXML private Button profileNavButton;
    @FXML private Button moreNavButton;
    @FXML private Button accountMoreButton;
    @FXML private Button sidebarPostButton;
    @FXML private Button editProfileButton;
    @FXML private Button profileMessageButton;
    @FXML private Button profileSubscribeButton;
    @FXML private Button backButton;
    @FXML private Button headerSearchButton;
    @FXML private Button followJavaFxButton;
    @FXML private Button followDesignButton;
    @FXML private Button postsTabButton;
    @FXML private Button repliesTabButton;
    @FXML private Button highlightsTabButton;
    @FXML private Button articlesTabButton;
    @FXML private Button mediaTabButton;
    @FXML private Button likesTabButton;

    @FXML
    public void initialize() {
        String requestedProfile = postStore.consumeRequestedProfile();
        viewedProfile = AccountDirectory.find(requestedProfile);
        ownProfile = UserSession.getInstance().getUsername() != null
                && UserSession.getInstance().getUsername().equalsIgnoreCase(viewedProfile.username());
        setupIcons();
        setSuggestionAvatar(javaFxSuggestionAvatar, "openjfx");
        setSuggestionAvatar(designSuggestionAvatar, "designdaily");
        refreshFollowButtons();
        refreshProfileData();
        linkMetaLabel.setOnMouseClicked(event -> {
            String url = linkMetaLabel.getText();
            if (url != null && !url.isBlank()) {
                openWebsite(url);
            }
        });
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
        profileNavButton.setGraphicTextGap(18);
        setNavIcon(moreNavButton, "more-horizontal-filled-icon.svg");
        accountMoreButton.setGraphic(AppIcons.icon("more-horizontal-filled-icon.svg", 20, "#0f1419"));
        backButton.setText("←");
        headerSearchButton.setGraphic(AppIcons.icon("search-icon.svg", 20, "#0f1419"));
        profileMessageButton.setGraphic(AppIcons.icon("messages-plus-icon.svg", 19, "#0f1419"));
        profileSubscribeButton.setGraphic(AppIcons.icon("superfollows-filled-icon.svg", 19, "#c936d7"));
        locationMetaLabel.setGraphic(AppIcons.icon("location-pin-icon.svg", 16, "#536471"));
        linkMetaLabel.setGraphic(AppIcons.icon("link-icon.svg", 16, "#1d9bf0"));
        calendarMetaLabel.setGraphic(AppIcons.icon("calendar-icon.svg", 16, "#536471"));
    }

    private void setNavIcon(Button button, String filename) {
        button.setGraphic(AppIcons.icon(filename, 24, "#0f1419"));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(18);
    }

    private void setSuggestionAvatar(Circle circle, String username) {
        AccountProfile profile = AccountDirectory.find(username);
        Image image = loadProfileImage(profile.avatarResource(), 40, 40);
        circle.setFill(image == null ? Color.web("#cfd9de") : new ImagePattern(image));
        circle.setOnMouseClicked(event -> openProfile(username));
        circle.setStyle("-fx-cursor: hand;");
    }

    private javafx.scene.Node createProfileGlyph(double size) {
        javafx.scene.shape.Circle head = new javafx.scene.shape.Circle(size * 0.18, Color.web("#0f1419"));
        javafx.scene.shape.Arc body = new javafx.scene.shape.Arc(size / 2, size * 0.96, size * 0.34, size * 0.34, 0, 180);
        body.setType(javafx.scene.shape.ArcType.CHORD);
        body.setFill(Color.web("#0f1419"));
        VBox glyph = new VBox(2, head, body);
        glyph.setAlignment(Pos.CENTER);
        glyph.setMinSize(size, size);
        glyph.setPrefSize(size, size);
        return glyph;
    }

    public void refreshProfileData() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (viewedProfile == null) viewedProfile = AccountDirectory.current();
        if (ownProfile) viewedProfile = AccountDirectory.current();

        headerNameLabel.setText(viewedProfile.displayName());
        nameLabel.setText(viewedProfile.displayName());
        usernameLabel.setText("@" + viewedProfile.username());
        ProfileHoverCard.attach(usernameLabel, viewedProfile.username(), () -> openProfile(viewedProfile.username()));
        bioLabel.setText(viewedProfile.bio());
        followingCountLabel.setText(viewedProfile.following());
        followerCountLabel.setText(viewedProfile.followers());
        updateMetadata(viewedProfile);
        updateProfileImages(viewedProfile);

        nameLabel.setGraphic(viewedProfile.verified()
                ? AppIcons.icon("verified-check-icon.svg", 18, "#1d9bf0") : null);
        nameLabel.setContentDisplay(ContentDisplay.RIGHT);
        nameLabel.setGraphicTextGap(5);
        parodyMetaLabel.setManaged(viewedProfile.parody());
        parodyMetaLabel.setVisible(viewedProfile.parody());
        parodyMetaLabel.setGraphic(viewedProfile.parody()
                ? AppIcons.icon("verified-badge-icon.svg", 15, "#536471") : null);

        editProfileButton.setText(ownProfile ? "Edit profile"
                : (UserSession.getInstance().isFollowing(viewedProfile.username()) ? "Following" : "Follow"));
        editProfileButton.getStyleClass().remove("follow-button");
        if (!ownProfile) {
            editProfileButton.getStyleClass().remove("edit-profile-button");
            if (!editProfileButton.getStyleClass().contains("follow-button")) editProfileButton.getStyleClass().add("follow-button");
        } else if (!editProfileButton.getStyleClass().contains("edit-profile-button")) {
            editProfileButton.getStyleClass().add("edit-profile-button");
        }
        profileMessageButton.setManaged(!ownProfile);
        profileMessageButton.setVisible(!ownProfile);
        profileSubscribeButton.setManaged(!ownProfile);
        profileSubscribeButton.setVisible(!ownProfile);

        String displayName = currentUser == null ? "User"
                : (currentUser.getDisplayName() == null || currentUser.getDisplayName().isBlank()
                ? currentUser.getUsername() : currentUser.getDisplayName());
        String currentUsername = currentUser == null ? "user" : currentUser.getUsername();
        String initial = displayName.isBlank() ? "U" : displayName.substring(0, 1).toUpperCase();
        sidebarNameLabel.setText(displayName);
        sidebarUsernameLabel.setText("@" + currentUsername);
        applyCurrentAvatar(sidebarAvatarLabel, currentUser, initial, 40);
        sidebarNameLabel.setOnMouseClicked(event -> handleAccountMenu());
        sidebarUsernameLabel.setOnMouseClicked(event -> handleAccountMenu());
        sidebarAvatarLabel.setOnMouseClicked(event -> handleAccountMenu());
        addAccountMenuTriggerStyle(sidebarNameLabel);
        addAccountMenuTriggerStyle(sidebarUsernameLabel);
        addAccountMenuTriggerStyle(sidebarAvatarLabel);

        int postCount = postStore.getPostsByUsername(viewedProfile.username()).size();
        postCountLabel.setText(postCount + (postCount == 1 ? " post" : " posts"));
        applyProfileFonts();
        refreshTab();
    }

    private void applyCurrentAvatar(Label host, User user, String fallbackInitial, double size) {
        if (user == null) {
            host.setGraphic(null);
            host.setContentDisplay(ContentDisplay.TEXT_ONLY);
            host.setText(fallbackInitial);
            host.getStyleClass().remove("avatar-image-host");
            return;
        }
        javafx.scene.Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(user.getUsername()), size);
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

    private void updateMetadata(AccountProfile profile) {
        String location = profile.location() == null ? "" : profile.location().trim();
        String website = profile.website() == null ? "" : profile.website().trim();
        locationMetaLabel.setText(location);
        locationMetaLabel.setManaged(!location.isEmpty());
        locationMetaLabel.setVisible(!location.isEmpty());
        linkMetaLabel.setText(website);
        linkMetaLabel.setManaged(!website.isEmpty());
        linkMetaLabel.setVisible(!website.isEmpty());
        calendarMetaLabel.setText(profile.joined());
    }

    private void updateProfileImages(AccountProfile profile) {
        Image banner = loadProfileImage(profile.bannerResource(), 600, 200);
        boolean hasBanner = banner != null;
        profileBannerImage.setManaged(hasBanner);
        profileBannerImage.setVisible(hasBanner);
        profileBannerFallback.setManaged(!hasBanner);
        profileBannerFallback.setVisible(!hasBanner);
        if (hasBanner) profileBannerImage.setImage(banner);

        Image avatar = loadProfileImage(profile.avatarResource(), 132, 132);
        boolean hasAvatar = avatar != null;
        profileAvatarLabel.setManaged(!hasAvatar);
        profileAvatarLabel.setVisible(!hasAvatar);
        profileAvatarLabel.setText(profile.displayName().isBlank() ? "U" : profile.displayName().substring(0, 1).toUpperCase());
        if (hasAvatar) {
            profileAvatarCircle.getStyleClass().remove("profile-avatar");
            if (!profileAvatarCircle.getStyleClass().contains("profile-avatar-image")) {
                profileAvatarCircle.getStyleClass().add("profile-avatar-image");
            }
            profileAvatarCircle.setFill(new ImagePattern(avatar));
        } else {
            profileAvatarCircle.getStyleClass().remove("profile-avatar-image");
            if (!profileAvatarCircle.getStyleClass().contains("profile-avatar")) {
                profileAvatarCircle.getStyleClass().add("profile-avatar");
            }
            profileAvatarCircle.setFill(Color.web("#1d9bf0"));
        }
    }

    private Image loadProfileImage(String source, double width, double height) {
        if (source == null || source.isBlank()) return null;
        try {
            Image image;
            if (source.startsWith("file:") || source.startsWith("http:") || source.startsWith("https:") || source.startsWith("data:")) {
                image = new Image(source, width, height, false, true);
            } else {
                var resource = getClass().getResource(source);
                if (resource == null) return null;
                image = new Image(resource.toExternalForm(), width, height, false, true);
            }
            return image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0 ? null : image;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void applyProfileFonts() {
        headerNameLabel.setFont(AppFonts.fontFor(headerNameLabel.getText(), 16, FontWeight.BOLD));
        nameLabel.setFont(AppFonts.fontFor(nameLabel.getText(), 21, FontWeight.BOLD));
        usernameLabel.setFont(AppFonts.fontFor(usernameLabel.getText(), 14));
        bioLabel.setFont(AppFonts.fontFor(bioLabel.getText(), 15));
        sidebarNameLabel.setFont(AppFonts.fontFor(sidebarNameLabel.getText(), 15, FontWeight.BOLD));
        sidebarUsernameLabel.setFont(AppFonts.fontFor(sidebarUsernameLabel.getText(), 14));
    }

    private void refreshTab() {
        userTweetsContainer.getChildren().clear();
        String username = viewedProfile == null ? null : viewedProfile.username();
        if (activeTab.equals("following") || activeTab.equals("followers")) {
            Set<String> usernames = activeTab.equals("following")
                    ? postStore.getFollowing(username) : postStore.getFollowers(username);
            renderAccountList(usernames, activeTab.equals("following")
                    ? "This account isn’t following anyone yet."
                    : "This account doesn’t have any followers yet.");
            updateTabStyles();
            return;
        }
        List<Post> posts = switch (activeTab) {
            case "replies" -> postStore.getRepliesByUsername(username);
            case "likes" -> ownProfile ? postStore.getLikedPosts() : List.of();
            case "media" -> username == null ? List.of() : postStore.getPostsByUsername(username).stream()
                    .filter(post -> post.getMediaUri() != null).toList();
            case "highlights", "articles" -> List.of();
            default -> username == null ? List.of() : postStore.getProfilePosts(username);
        };
        posts.forEach(post -> userTweetsContainer.getChildren().add(createPostCard(post)));
        if (posts.isEmpty()) {
            Label empty = new Label(switch (activeTab) {
                case "replies" -> "Posts you reply to will appear here.";
                case "likes" -> "Posts you like will appear here.";
                case "media" -> "Your posts with images will appear here.";
                case "highlights" -> "You don't have any highlights yet.";
                case "articles" -> "You haven't published any articles yet.";
                default -> "You haven't posted yet.";
            });
            empty.setFont(AppFonts.fontFor(empty.getText(), 15));
            empty.setTextFill(Color.web("#536471"));
            empty.setStyle("-fx-padding: 34;");
            userTweetsContainer.getChildren().add(empty);
        }
        updateTabStyles();
    }

    private void updateTabStyles() {
        List<Button> tabs = List.of(postsTabButton, repliesTabButton, highlightsTabButton, articlesTabButton, mediaTabButton, likesTabButton);
        tabs.forEach(button -> button.getStyleClass().remove("tab-button-active"));
        if (activeTab.equals("following") || activeTab.equals("followers")) return;
        Button active = switch (activeTab) {
            case "replies" -> repliesTabButton;
            case "highlights" -> highlightsTabButton;
            case "articles" -> articlesTabButton;
            case "media" -> mediaTabButton;
            case "likes" -> likesTabButton;
            default -> postsTabButton;
        };
        active.getStyleClass().add("tab-button-active");
    }

    private void renderAccountList(Set<String> usernames, String emptyText) {
        for (String accountUsername : usernames) {
            AccountProfile account = AccountDirectory.find(accountUsername);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("account-result-row");
            javafx.scene.Node avatar = ProfileHoverCard.avatarNode(account, 44);
            VBox identity = new VBox(2);
            HBox.setHgrow(identity, Priority.ALWAYS);
            Label displayName = new Label(account.displayName());
            displayName.setFont(AppFonts.fontFor(displayName.getText(), 15, FontWeight.BOLD));
            Label handle = new Label("@" + account.username());
            handle.setTextFill(Color.web("#536471"));
            Label bio = new Label(account.bio());
            bio.setWrapText(true);
            identity.getChildren().addAll(displayName, handle, bio);
            Button follow = new Button(UserSession.getInstance().isFollowing(account.username()) ? "Following" : "Follow");
            boolean ownAccount = account.username().equalsIgnoreCase(UserSession.getInstance().getUsername());
            follow.setManaged(!ownAccount);
            follow.setVisible(!ownAccount);
            follow.getStyleClass().add("follow-button");
            follow.setOnAction(event -> {
                UserSession.getInstance().toggleFollow(account.username());
                follow.setText(UserSession.getInstance().isFollowing(account.username()) ? "Following" : "Follow");
            });
            row.getChildren().addAll(avatar, identity, follow);
            row.setOnMouseClicked(event -> {
                javafx.scene.Node cursor = (javafx.scene.Node) event.getTarget();
                while (cursor != null && cursor != row) {
                    if (cursor instanceof Button) return;
                    cursor = cursor.getParent();
                }
                openProfile(account.username());
            });
            userTweetsContainer.getChildren().add(row);
        }
        if (usernames.isEmpty()) {
            Label empty = new Label(emptyText);
            empty.setTextFill(Color.web("#536471"));
            empty.setStyle("-fx-padding: 34;");
            userTweetsContainer.getChildren().add(empty);
        }
    }

    private HBox createPostCard(Post post) {
        HBox row = new HBox(12);
        row.getStyleClass().add("post-row");
        javafx.scene.Node avatar = ProfileHoverCard.avatarNode(AccountDirectory.find(post.getAuthorUsername()), 42);

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox header = new HBox(8);
        Label name = new Label(post.getAuthorName());
        name.setFont(AppFonts.fontFor(name.getText(), 15, FontWeight.BOLD));
        Label handle = new Label("@" + post.getAuthorUsername());
        handle.setTextFill(Color.web("#536471"));
        Runnable openAuthor = () -> openProfile(post.getAuthorUsername());
        ProfileHoverCard.attachIdentity(
                avatar,
                name,
                handle,
                post.getAuthorUsername(),
                openAuthor
        );
        Label time = new Label();
        time.setTextFill(Color.web("#536471"));
        time.textProperty().bind(Bindings.createStringBinding(() -> "· " + postStore.relativeTime(post), postStore.clockProperty()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button more = iconOnlyButton("more-horizontal-filled-icon.svg", "#536471");
        more.setOnAction(event -> PostInteractions.showPostMenu(more, post, this::refreshProfileData));
        header.getChildren().addAll(name, handle, time, spacer, more);
        if (post.isPinned()) {
            Label pinned = new Label("Pinned");
            pinned.setGraphic(AppIcons.icon("bookmark-icon.svg", 14, "#536471"));
            pinned.setTextFill(Color.web("#536471"));
            pinned.setFont(AppFonts.fontFor("Pinned", 13, FontWeight.BOLD));
            content.getChildren().add(pinned);
        }
        content.getChildren().add(header);
        if (post.getReplyToId() != null) {
            Post original = postStore.getPost(post.getReplyToId());
            if (original != null) {
                Label replyingTo = new Label("Replying to @" + original.getAuthorUsername());
                replyingTo.setTextFill(Color.web("#1d9bf0"));
                replyingTo.setFont(AppFonts.fontFor(replyingTo.getText(), 14));
                replyingTo.setStyle("-fx-cursor: hand;");
                replyingTo.setOnMouseClicked(event -> openProfile(original.getAuthorUsername()));
                content.getChildren().add(replyingTo);
            }
        }
        if (!post.getContent().isBlank()) {
            TextFlow body = createContent(post.getContent());
            body.setStyle("-fx-cursor: hand;");
            body.setOnMouseClicked(event -> openPostDetail(post.getId()));
            content.getChildren().add(body);
        }
        if (post.getMediaUri() != null) {
            Image mediaImage = MediaLibrary.loadImage(post.getMediaUri());
            if (mediaImage != null) {
                ImageView image = new ImageView(mediaImage);
                image.setFitWidth(480);
                image.setFitHeight(320);
                image.setPreserveRatio(true);
                image.setOnMouseClicked(event -> openPostDetail(post.getId()));
                content.getChildren().add(image);
            } else {
                Label unavailable = new Label("Media unavailable · " + MediaLibrary.displayName(post.getMediaUri())
                        + "\nThe original file was moved or deleted.");
                unavailable.setWrapText(true);
                unavailable.setMaxWidth(480);
                unavailable.setStyle("-fx-background-color: #eff3f4; -fx-background-radius: 14;"
                        + "-fx-text-fill: #536471; -fx-padding: 14;");
                content.getChildren().add(unavailable);
            }
        }
        if (post.getQuotedPostId() != null) {
            Post quoted = postStore.getPost(post.getQuotedPostId());
            if (quoted != null) content.getChildren().add(PostComposerDialog.createPostPreview(quoted));
        }
        if (post.getPoll() != null) content.getChildren().add(PollView.create(post, this::refreshProfileData));

        HBox actions = new HBox(48);
        Button reply = new Button();
        Button repost = new Button();
        Button like = new Button();
        Button bookmark = new Button();
        refreshActions(post, reply, repost, like, bookmark);
        reply.setOnAction(event -> PostInteractions.showReply(reply, post, this::refreshProfileData));
        repost.setOnAction(event -> PostInteractions.showRepostMenu(
                repost,
                post,
                () -> refreshActions(post, reply, repost, like, bookmark),
                this::refreshProfileData));
        like.setOnAction(event -> {
            boolean increasing = !post.isLiked();
            postStore.toggleLike(post);
            refreshActions(post, reply, repost, like, bookmark);
            if (increasing) PostInteractions.animateReaction(like);
            if (activeTab.equals("likes")) refreshTab();
        });
        bookmark.setOnAction(event -> { postStore.toggleBookmark(post); refreshActions(post, reply, repost, like, bookmark); });
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actions.getChildren().addAll(reply, repost, like, actionSpacer, bookmark);
        content.getChildren().add(actions);
        row.getChildren().addAll(avatar, content);
        return row;
    }

    private void refreshActions(Post post, Button reply, Button repost, Button like, Button bookmark) {
        addActionClass(reply, "reply-action");
        addActionClass(repost, "repost-action");
        addActionClass(like, "like-action");
        addActionClass(bookmark, "bookmark-action");
        actionButton(reply, "message-outline-icon.svg", post.getReplies(), post.isReplied(), "#1d9bf0");
        actionButton(repost, "repost-icon.svg", post.getRetweets(), post.isRetweeted(), "#00ba7c");
        actionButton(like, "heart-outline-icon.svg", post.getLikes(), post.isLiked(), "#f91880");
        reply.setAccessibleText((post.isReplied() ? "Replied to" : "Reply to") + " post, " + post.getReplies() + " replies");
        repost.setAccessibleText((post.isRetweeted() ? "Undo repost" : "Repost") + ", " + post.getRetweets() + " reposts");
        like.setAccessibleText((post.isLiked() ? "Unlike" : "Like") + " post, " + post.getLikes() + " likes");
        bookmark.setGraphic(AppIcons.icon("bookmark-icon.svg", 18, post.isBookmarked() ? "#1d9bf0" : "#536471"));
        bookmark.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        bookmark.setStyle(ACTION_STYLE);
        bookmark.setAccessibleText(post.isBookmarked() ? "Remove bookmark" : "Bookmark post");
    }

    private void addActionClass(Button button, String styleClass) {
        if (!button.getStyleClass().contains(styleClass)) button.getStyleClass().add(styleClass);
    }

    private void actionButton(Button button, String icon, int count, boolean active, String activeColor) {
        String color = active ? activeColor : "#536471";
        String resolvedIcon = active && icon.equals("heart-outline-icon.svg") ? "heart-filled-icon.svg" : icon;
        button.setGraphic(AppIcons.icon(resolvedIcon, 18, color));
        button.setText(formatCount(count));
        button.setGraphicTextGap(7);
        button.setStyle(ACTION_STYLE + (active ? "-fx-text-fill: " + color + ";" : ""));
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

    private Button iconOnlyButton(String icon, String color) {
        Button button = new Button();
        button.setGraphic(AppIcons.icon(icon, 18, color));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setStyle(ACTION_STYLE);
        return button;
    }

    private TextFlow createContent(String textContent) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(480);
        Matcher matcher = TOKEN_PATTERN.matcher(textContent);
        int cursor = 0;
        while (matcher.find()) {
            addText(flow, textContent.substring(cursor, matcher.start()), false);
            String value = matcher.group();
            Text token = addText(flow, value, true);
            token.setOnMouseClicked(event -> {
                if (value.startsWith("@")) {
                    openProfile(value.substring(1));
                } else {
                    postStore.requestHashtag(value);
                    postStore.requestView("explore");
                    NavigationManager.switchScene("/views/Feed.fxml");
                }
            });
            cursor = matcher.end();
        }
        addText(flow, textContent.substring(cursor), false);
        return flow;
    }

    private Text addText(TextFlow flow, String value, boolean hashtag) {
        Text text = new Text(value);
        text.setFont(AppFonts.fontFor(value, 15));
        text.setFill(hashtag ? Color.web("#1d9bf0") : Color.web("#0f1419"));
        if (hashtag) text.setStyle("-fx-cursor: hand;");
        flow.getChildren().add(text);
        return text;
    }

    @FXML private void handlePostsTab() { activeTab = "posts"; refreshTab(); }
    @FXML private void handleRepliesTab() { activeTab = "replies"; refreshTab(); }
    @FXML private void handleLikesTab() { activeTab = "likes"; refreshTab(); }
    @FXML private void handleMediaTab() { activeTab = "media"; refreshTab(); }
    @FXML private void handleEmptyTab(ActionEvent event) { activeTab = ((Button) event.getSource()).getText().toLowerCase(); refreshTab(); }
    @FXML private void handleFollowingList() { activeTab = "following"; refreshTab(); }
    @FXML private void handleFollowersList() { activeTab = "followers"; refreshTab(); }

    @FXML
    private void handleEditProfile() {
        if (!ownProfile) {
            UserSession.getInstance().toggleFollow(viewedProfile.username());
            refreshProfileData();
            return;
        }
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;
        EditProfileDialog.show(editProfileButton.getScene() == null ? null : editProfileButton.getScene().getWindow(), user)
                .ifPresent(updated -> {
            user.setDisplayName(updated.name());
            user.setBio(updated.bio());
            user.setLocation(updated.location());
            user.setWebsite(updated.website());
            user.setBirthDate(updated.birthDate());
            user.setProfessional(updated.professional());
            user.setAvatarUrl(updated.avatarUri());
            user.setBannerUrl(updated.bannerUri());
            postStore.saveProfile(user);
            postStore.updateAuthorName(user.getUsername(), user.getDisplayName());
            ProfileHoverCard.clearAvatarCache();
            viewedProfile = AccountDirectory.current();
            refreshProfileData();
        });
    }

    @FXML private void handleFollowJavaFx() { UserSession.getInstance().toggleFollow("openjfx"); refreshFollowButtons(); refreshProfileData(); }
    @FXML private void handleFollowDesign() { UserSession.getInstance().toggleFollow("designdaily"); refreshFollowButtons(); refreshProfileData(); }

    private void refreshFollowButtons() {
        followButton(followJavaFxButton, UserSession.getInstance().isFollowing("openjfx"));
        followButton(followDesignButton, UserSession.getInstance().isFollowing("designdaily"));
    }

    private void followButton(Button button, boolean following) {
        button.setText(following ? "Following" : "Follow");
        button.setStyle(following ? "-fx-background-color: white; -fx-text-fill: #0f1419; -fx-border-color: #cfd9de; -fx-border-radius: 18; -fx-background-radius: 18; -fx-font-weight: bold; -fx-padding: 7 12;" : "");
    }

    @FXML private void handleGoToHome() { goToView("home"); }
    @FXML private void handleLogoHome() { goToView("home"); }
    @FXML private void handleGoToExplore() { goToView("explore"); }
    @FXML private void handleGoToNotifications() { goToView("notifications"); }
    @FXML private void handleGoToChat() { goToView("chat"); }
    @FXML private void handleGoToBookmarks() { goToView("bookmarks"); }
    @FXML private void handleGoToMore() { goToView("more"); }
    @FXML private void handleSubscribe() { UserSession.getInstance().toggleFollow(viewedProfile.username()); refreshProfileData(); }
    @FXML private void handleGoToUnavailable() { goToView("unavailable"); }

    @FXML
    private void handleOpenPostComposer() {
        PostInteractions.showPost(sidebarPostButton, this::refreshProfileData);
    }

    private void goToView(String view) {
        postStore.requestView(view);
        NavigationManager.switchScene("/views/Feed.fxml");
    }

    private void openProfile(String username) {
        postStore.requestProfile(username);
        NavigationManager.switchScene("/views/Profile.fxml");
    }

    private void openPostDetail(long postId) {
        postStore.requestPost(postId);
        NavigationManager.switchScene("/views/Feed.fxml");
    }

    @FXML
    private void handleAccountMenu() {
        PostInteractions.showAccountMenu(accountMoreButton,
                () -> NavigationManager.switchScene("/views/Login.fxml"), this::handleLogout);
    }

    @FXML private void handleFocusSearch() { profileSearchField.requestFocus(); }

    @FXML
    private void handleSearch() {
        if (profileSearchField.getText().isBlank()) return;
        postStore.requestView("explore");
        postStore.requestSearch(profileSearchField.getText().trim());
        NavigationManager.switchScene("/views/Feed.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().clearSession();
        NavigationManager.switchScene("/views/Login.fxml");
    }

    private void openWebsite(String url) {
        if (url == null || url.isBlank()) return;
        String destination = url.trim();
        if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
            destination = "https://" + destination;
        }
        try {
            client.MainApp.getInstance().getHostServices().showDocument(destination);
        } catch (Exception e) {
            System.err.println("Could not open URL: " + destination + " (" + e.getMessage() + ")");
        }
    }
}
