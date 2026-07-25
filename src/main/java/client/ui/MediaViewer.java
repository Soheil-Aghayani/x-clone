package client.ui;

import client.AppFonts;
import client.AppIcons;
import client.media.MediaLibrary;
import client.profile.AccountDirectory;
import client.timeline.Post;
import client.timeline.PostStore;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * X-style media lightbox. Media remains the focus while the selected post and
 * its conversation stay visible in a fixed panel on the right.
 */
public final class MediaViewer {
    private MediaViewer() {}

    public static void show(Window owner, Post initial, List<Post> timelinePosts) {
        if (owner == null || initial == null || initial.getMediaUri() == null) return;
        List<Post> mediaPosts = new ArrayList<>();
        if (timelinePosts != null) {
            timelinePosts.stream()
                    .filter(post -> post != null && post.getMediaUri() != null)
                    .forEach(mediaPosts::add);
        }
        if (mediaPosts.stream().noneMatch(post -> post.getId() == initial.getId())) {
            mediaPosts.addFirst(initial);
        }
        int initialIndex = 0;
        for (int index = 0; index < mediaPosts.size(); index++) {
            if (mediaPosts.get(index).getId() == initial.getId()) {
                initialIndex = index;
                break;
            }
        }

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Media / X");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("media-viewer");
        StackPane canvas = new StackPane();
        canvas.getStyleClass().add("media-viewer-canvas");
        VBox detail = new VBox();
        detail.setPrefWidth(430);
        detail.setMinWidth(360);
        detail.setMaxWidth(480);
        detail.getStyleClass().add("media-viewer-detail");
        root.setCenter(canvas);
        root.setRight(detail);

        Button close = new Button("×");
        close.setAccessibleText("Close media");
        close.getStyleClass().add("media-viewer-close");
        close.setOnAction(event -> stage.close());
        StackPane.setAlignment(close, Pos.TOP_LEFT);
        StackPane.setMargin(close, new Insets(18));

        Button previous = iconButton("arrow-left-icon.svg", "Previous media", 22);
        previous.getStyleClass().add("media-viewer-navigation");
        StackPane.setAlignment(previous, Pos.CENTER_LEFT);
        StackPane.setMargin(previous, new Insets(0, 0, 0, 18));

        Button next = iconButton("arrow-left-icon.svg", "Next media", 22);
        next.getGraphic().setRotate(180);
        next.getStyleClass().add("media-viewer-navigation");
        StackPane.setAlignment(next, Pos.CENTER_RIGHT);
        StackPane.setMargin(next, new Insets(0, 18, 0, 0));

        int[] selected = {initialIndex};
        Runnable render = () -> {
            Post post = mediaPosts.get(selected[0]);
            renderImage(canvas, post, close, previous, next);
            renderDetails(detail, post);
            previous.setVisible(mediaPosts.size() > 1);
            previous.setManaged(mediaPosts.size() > 1);
            next.setVisible(mediaPosts.size() > 1);
            next.setManaged(mediaPosts.size() > 1);
        };
        previous.setOnAction(event -> {
            selected[0] = (selected[0] - 1 + mediaPosts.size()) % mediaPosts.size();
            render.run();
        });
        next.setOnAction(event -> {
            selected[0] = (selected[0] + 1) % mediaPosts.size();
            render.run();
        });

        Scene scene = new Scene(root,
                Math.max(900, owner.getWidth()),
                Math.max(600, owner.getHeight()));
        var css = MediaViewer.class.getResource("/styles/twitter.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.setX(owner.getX());
        stage.setY(owner.getY());
        render.run();
        stage.show();
    }

    private static void renderImage(
            StackPane canvas,
            Post post,
            Button close,
            Button previous,
            Button next) {
        canvas.getChildren().clear();
        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(42, 42);
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.fitWidthProperty().bind(canvas.widthProperty().subtract(72));
        imageView.fitHeightProperty().bind(canvas.heightProperty().subtract(44));

        VBox error = new VBox(12);
        error.setAlignment(Pos.CENTER);
        Label errorTitle = new Label("This media couldn’t be loaded");
        errorTitle.getStyleClass().add("media-viewer-error-title");
        Button retry = new Button("Retry");
        retry.getStyleClass().add("media-viewer-retry");
        error.getChildren().addAll(errorTitle, retry);
        error.setVisible(false);
        error.setManaged(false);

        Runnable load = () -> {
            error.setVisible(false);
            error.setManaged(false);
            loading.setVisible(true);
            try {
                String url = MediaLibrary.resolveForDisplay(post.getMediaUri());
                Image image = new Image(url, true);
                imageView.setImage(image);
                loading.progressProperty().unbind();
                loading.progressProperty().bind(image.progressProperty());
                image.errorProperty().addListener((observable, oldValue, failed) -> {
                    if (failed) {
                        loading.setVisible(false);
                        error.setVisible(true);
                        error.setManaged(true);
                    }
                });
                image.progressProperty().addListener((observable, oldValue, progress) -> {
                    if (progress.doubleValue() >= 1 && !image.isError()) loading.setVisible(false);
                });
            } catch (RuntimeException exception) {
                loading.setVisible(false);
                error.setVisible(true);
                error.setManaged(true);
            }
        };
        retry.setOnAction(event -> load.run());
        canvas.getChildren().addAll(imageView, loading, error, close, previous, next);
        load.run();
    }

    private static void renderDetails(VBox detail, Post post) {
        detail.getChildren().clear();
        Node avatar = ProfileHoverCard.avatarNode(
                AccountDirectory.find(post.getAuthorUsername()), 44);
        VBox identity = new VBox(1);
        Label name = new Label(post.getAuthorName());
        name.setFont(AppFonts.fontFor(name.getText(), 15, FontWeight.BOLD));
        Label handle = new Label("@" + post.getAuthorUsername());
        handle.setTextFill(Color.web("#536471"));
        identity.getChildren().addAll(name, handle);
        Region identitySpacer = new Region();
        HBox.setHgrow(identitySpacer, Priority.ALWAYS);
        Button more = iconButton("more-horizontal-filled-icon.svg", "More", 18);
        HBox author = new HBox(10, avatar, identity, identitySpacer, more);
        author.setAlignment(Pos.CENTER_LEFT);
        author.getStyleClass().add("media-viewer-author");

        Label body = new Label(post.getContent());
        body.setWrapText(true);
        body.setFont(AppFonts.fontFor(post.getContent(), 17));
        body.getStyleClass().add("media-viewer-body");

        Label metadata = new Label(PostStore.getInstance().relativeTime(post)
                + "  ·  " + compact(post.getViews()) + " Views");
        metadata.setTextFill(Color.web("#536471"));
        metadata.getStyleClass().add("media-viewer-metadata");

        Button reply = action("message-outline-icon.svg", post.getReplies());
        Button repost = action("repost-icon.svg", post.getRetweets());
        Button like = action(
                post.isLiked() ? "heart-filled-icon.svg" : "heart-outline-icon.svg",
                post.getLikes());
        Button bookmark = action("bookmark-icon.svg", post.getBookmarkedBy().size());
        HBox actions = new HBox(28, reply, repost, like, bookmark);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("media-viewer-actions");

        reply.setOnAction(event -> PostInteractions.showReply(
                reply, post, () -> renderDetails(detail, post)));
        repost.setOnAction(event -> PostInteractions.showRepostMenu(
                repost, post, () -> renderDetails(detail, post), () -> renderDetails(detail, post)));
        like.setOnAction(event -> {
            PostStore.getInstance().toggleLike(post);
            renderDetails(detail, post);
        });
        bookmark.setOnAction(event -> {
            PostStore.getInstance().toggleBookmark(post);
            renderDetails(detail, post);
        });
        more.setOnAction(event -> PostInteractions.showPostMenu(
                more, post, () -> renderDetails(detail, post)));

        VBox conversation = new VBox();
        for (Post response : PostStore.getInstance().getThreadReplies(post.getId())) {
            Label replyAuthor = new Label(
                    response.getAuthorName() + "  @" + response.getAuthorUsername());
            replyAuthor.setFont(AppFonts.fontFor(replyAuthor.getText(), 14, FontWeight.BOLD));
            Label replyBody = new Label(response.getContent());
            replyBody.setWrapText(true);
            replyBody.setFont(AppFonts.fontFor(replyBody.getText(), 15));
            VBox card = new VBox(5, replyAuthor, replyBody);
            card.getStyleClass().add("media-viewer-reply");
            conversation.getChildren().add(card);
        }
        if (conversation.getChildren().isEmpty()) {
            Label empty = new Label("No replies yet");
            empty.setTextFill(Color.web("#536471"));
            empty.getStyleClass().add("media-viewer-empty");
            conversation.getChildren().add(empty);
        }
        ScrollPane replies = new ScrollPane(conversation);
        replies.setFitToWidth(true);
        replies.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        replies.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(replies, Priority.ALWAYS);
        detail.getChildren().addAll(author, body, metadata, actions, replies);
    }

    private static Button iconButton(String icon, String accessibleText, double size) {
        Button button = new Button();
        button.setGraphic(AppIcons.icon(icon, size, "#e7e9ea"));
        button.setAccessibleText(accessibleText);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        return button;
    }

    private static Button action(String icon, int count) {
        Button button = new Button(count == 0 ? "" : compact(count));
        button.setGraphic(AppIcons.icon(icon, 19, "#536471"));
        button.getStyleClass().add("media-viewer-action");
        return button;
    }

    private static String compact(int count) {
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0).replace(".0M", "M");
        if (count >= 1_000) return String.format("%.1fK", count / 1_000.0).replace(".0K", "K");
        return Integer.toString(count);
    }
}
