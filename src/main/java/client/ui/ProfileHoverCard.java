package client.ui;

import client.AppFonts;
import client.AppIcons;
import client.UserSession;
import client.profile.AccountDirectory;
import client.profile.AccountProfile;
import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Attaches the delayed X-style account summary shown when hovering a name or handle. */
public final class ProfileHoverCard {
    private static final Map<String, Image> AVATAR_CACHE = new ConcurrentHashMap<>();

    private ProfileHoverCard() {}

    public static void attach(Node anchor, String username, Runnable openProfile) {
        attachAnchors(List.of(anchor), username, openProfile);
    }

    private static void attachAnchors(
            List<Node> anchors, String username, Runnable openProfile) {
        AccountProfile account = AccountDirectory.find(username);
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        Runnable navigate = () -> {
            popup.hide();
            openProfile.run();
        };
        VBox card = buildCard(account, navigate);
        popup.getContent().add(card);

        PauseTransition showDelay = new PauseTransition(Duration.millis(350));
        PauseTransition hideDelay = new PauseTransition(Duration.millis(280));
        Node[] activeAnchor = new Node[1];
        showDelay.setOnFinished(event -> {
            if (popup.isShowing()) return;
            Node anchor = activeAnchor[0];
            if (anchor == null) return;
            if (anchor.getScene() == null || anchor.getScene().getWindow() == null) return;
            Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds == null) return;
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 2);
        });
        hideDelay.setOnFinished(event -> popup.hide());
        for (Node anchor : anchors) {
            if (anchor == null) continue;
            anchor.setOnMouseEntered(event -> {
                activeAnchor[0] = anchor;
                hideDelay.stop();
                if (!popup.isShowing()) showDelay.playFromStart();
            });
            anchor.setOnMouseExited(event -> {
                showDelay.stop();
                hideDelay.playFromStart();
            });
            anchor.setOnMouseClicked(event -> {
                navigate.run();
                event.consume();
            });
            if (!anchor.getStyle().contains("-fx-cursor: hand")) {
                anchor.setStyle(anchor.getStyle() + "-fx-cursor: hand;");
            }
            anchor.setAccessibleText("Open @" + account.username() + " profile");
        }
        card.setOnMouseEntered(event -> hideDelay.stop());
        card.setOnMouseExited(event -> hideDelay.playFromStart());
    }

    /**
     * Gives an avatar, display name, and handle the same profile destination.
     * Null nodes are accepted for compact identity layouts.
     */
    public static void attachIdentity(
            Node avatar,
            Node displayName,
            Node handle,
            String username,
            Runnable openProfile
    ) {
        attachAnchors(
                java.util.stream.Stream.of(avatar, displayName, handle)
                        .filter(java.util.Objects::nonNull)
                        .toList(),
                username,
                openProfile);
    }

    static VBox buildCard(AccountProfile account, Runnable openProfile) {
        VBox card = new VBox(8);
        card.getStyleClass().add("profile-hover-card");
        var stylesheet = ProfileHoverCard.class.getResource("/styles/twitter.css");
        if (stylesheet != null) card.getStylesheets().add(stylesheet.toExternalForm());

        Node avatar = avatarNode(account, 58);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button follow = new Button();
        follow.getStyleClass().add("follow-button");
        boolean own = account.username().equalsIgnoreCase(UserSession.getInstance().getUsername());
        follow.setManaged(!own);
        follow.setVisible(!own);
        updateFollow(follow, account.username());
        follow.setOnAction(event -> {
            UserSession.getInstance().toggleFollow(account.username());
            updateFollow(follow, account.username());
            event.consume();
        });
        HBox top = new HBox(avatar, spacer, follow);
        top.setAlignment(Pos.TOP_LEFT);

        HBox nameRow = new HBox(5);
        Label name = new Label(account.displayName());
        name.setFont(AppFonts.fontFor(account.displayName(), 17, javafx.scene.text.FontWeight.BOLD));
        nameRow.getChildren().add(name);
        if (account.verified()) nameRow.getChildren().add(AppIcons.icon("verified-check-icon.svg", 17, "#1d9bf0"));
        Label handle = new Label("@" + account.username());
        handle.setStyle("-fx-text-fill: #536471;");
        makeProfileLink(avatar, account.username(), openProfile);
        makeProfileLink(name, account.username(), openProfile);
        makeProfileLink(handle, account.username(), openProfile);
        Label bio = new Label(account.bio());
        bio.setWrapText(true);
        bio.setMaxWidth(260);
        bio.setFont(AppFonts.fontFor(account.bio(), 14));
        HBox stats = new HBox(18,
                stat(account.following(), "Following"),
                stat(account.followers(), "Followers"));
        Button summary = new Button("Profile Summary");
        summary.setGraphic(AppIcons.icon("logo-icon.svg", 18, "#0f1419"));
        summary.setMaxWidth(Double.MAX_VALUE);
        summary.getStyleClass().add("profile-summary-button");
        summary.setOnAction(event -> openProfile.run());
        card.getChildren().addAll(top, nameRow, handle, bio, stats, summary);
        return card;
    }

    private static void makeProfileLink(Node node, String username, Runnable openProfile) {
        node.setStyle(node.getStyle() + "-fx-cursor: hand;");
        node.setAccessibleText("Open @" + username + " profile");
        node.setOnMouseClicked(event -> {
            openProfile.run();
            event.consume();
        });
    }

    private static HBox stat(String value, String label) {
        Label number = new Label(value);
        number.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f1419;");
        Label caption = new Label(label);
        caption.setStyle("-fx-text-fill: #536471;");
        return new HBox(4, number, caption);
    }

    public static Node avatarNode(AccountProfile account, double size) {
        String avatar = account == null ? null : account.avatarResource();
        if (avatar != null && !avatar.isBlank()) {
            Image image = loadImage(avatar, size);
            if (image != null && !image.isError()) {
                ImageView view = new ImageView(image);
                view.setFitWidth(size);
                view.setFitHeight(size);
                view.setPreserveRatio(false);
                view.setClip(new Circle(size / 2, size / 2, size / 2));
                return view;
            }
        }
        String name = account == null || account.displayName() == null || account.displayName().isBlank()
                ? "U" : account.displayName();
        Label fallback = new Label(name.substring(0, 1).toUpperCase());
        fallback.getStyleClass().add("avatar-small");
        fallback.setAlignment(Pos.CENTER);
        fallback.setMinSize(size, size);
        fallback.setPrefSize(size, size);
        return fallback;
    }

    /** Drop cached avatars after a profile photo change so the new image is shown. */
    public static void clearAvatarCache() {
        AVATAR_CACHE.clear();
    }

    private static Image loadImage(String source, double size) {
        if (source == null || source.isBlank()) return null;
        try {
            String key = source + "@" + Math.round(size);
            Image cached = AVATAR_CACHE.get(key);
            if (cached != null) return cached;
            String uri;
            if (source.startsWith("file:") || source.startsWith("http:") || source.startsWith("https:") || source.startsWith("data:")) {
                uri = source;
            } else {
                var resource = ProfileHoverCard.class.getResource(source);
                if (resource == null) return null;
                uri = resource.toExternalForm();
            }
            Image loaded = new Image(uri, size, size, false, true);
            if (loaded.isError() || loaded.getWidth() <= 0 || loaded.getHeight() <= 0) return null;
            AVATAR_CACHE.put(key, loaded);
            return loaded;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void updateFollow(Button button, String username) {
        boolean following = UserSession.getInstance().isFollowing(username);
        button.setText(following ? "Following" : "Follow");
        if (following) button.setStyle("-fx-background-color: white; -fx-text-fill: #0f1419; -fx-border-color: #cfd9de; -fx-border-radius: 18; -fx-background-radius: 18;");
        else button.setStyle("");
    }
}
