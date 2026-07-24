package client.ui;

import client.AppIcons;
import client.UserSession;
import client.timeline.Post;
import client.timeline.PostStore;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Duration;
import shared.models.User;

/** Shared menus and modal actions for timeline cards. */
public final class PostInteractions {
    private PostInteractions() {}

    public static void showAccountMenu(Button anchor, Runnable addAccount, Runnable logout) {
        User user = UserSession.getInstance().getCurrentUser();
        String username = user == null ? "user" : user.getUsername();
        MenuItem add = new MenuItem("Add an existing account");
        MenuItem logOut = new MenuItem("Log out @" + username);
        add.setOnAction(event -> addAccount.run());
        logOut.setOnAction(event -> logout.run());
        ContextMenu menu = menu(add, new SeparatorMenuItem(), logOut);
        menu.show(anchor, Side.TOP, -225, 0);
    }

    public static void showReply(Button anchor, Post original, Runnable refreshView) {
        Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
        PostComposerDialog.show(owner, original, PostComposerDialog.Mode.REPLY).ifPresent(composition -> {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;
            Post created = PostStore.getInstance().createReply(
                    user, composition.text(), composition.mediaUri(), original);
            if (created != null) refreshView.run();
            else XDialog.info(owner, "Reply not sent", "The shared server could not publish your reply.");
        });
    }

    public static void showPost(Button anchor, Runnable refreshView) {
        Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
        PostComposerDialog.showPost(owner).ifPresent(composition -> {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;
            Post created = PostStore.getInstance().createPost(
                    user, composition.text(), composition.mediaUri());
            if (created != null) refreshView.run();
            else XDialog.info(owner, "Post not sent", "The shared server could not publish your post.");
        });
    }

    public static void showRepostMenu(Button anchor, Post original, Runnable refreshActions, Runnable refreshView) {
        MenuItem repost = new MenuItem(original.isRetweeted() ? "Undo Repost" : "Repost");
        repost.setGraphic(AppIcons.icon("repost-icon.svg", 18, "#0f1419"));
        MenuItem quote = new MenuItem("Quote");
        Text quoteIcon = new Text("✎");
        quoteIcon.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        quote.setGraphic(quoteIcon);

        ContextMenu menu = menu(repost, quote);
        repost.setOnAction(event -> {
            boolean increasing = !original.isRetweeted();
            PostStore.getInstance().toggleRepost(original);
            refreshActions.run();
            if (increasing) animateIncrease(anchor);
        });
        quote.setOnAction(event -> {
            Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
            PostComposerDialog.show(owner, original, PostComposerDialog.Mode.QUOTE).ifPresent(composition -> {
                User user = UserSession.getInstance().getCurrentUser();
                if (user == null) return;
                Post created = PostStore.getInstance().createQuote(
                        user, composition.text(), composition.mediaUri(), original);
                if (created != null) refreshView.run();
                else XDialog.info(owner, "Quote not sent", "The shared server could not publish your quote.");
            });
        });
        menu.show(anchor, Side.BOTTOM, -12, 2);
    }

    public static void showPostMenu(Button anchor, Post post, Runnable refreshView) {
        User active = UserSession.getInstance().getCurrentUser();
        boolean ownPost = active != null && active.getUsername().equalsIgnoreCase(post.getAuthorUsername());
        ContextMenu menu;
        if (ownPost) {
            MenuItem pin = new MenuItem(post.isPinned() ? "Unpin from profile" : "Pin to your profile");
            MenuItem delete = new MenuItem("Delete post");
            pin.setOnAction(event -> { PostStore.getInstance().togglePinned(post); refreshView.run(); });
            delete.setOnAction(event -> {
                Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
                if (XDialog.confirm(owner, "Delete post?", "This post will be permanently deleted.",
                        "Delete", true)) {
                    PostStore.getInstance().deletePost(post);
                    refreshView.run();
                }
            });
            menu = menu(pin, new SeparatorMenuItem(), delete);
        } else {
            boolean following = active != null && UserSession.getInstance().isFollowing(post.getAuthorUsername());
            MenuItem follow = new MenuItem((following ? "Unfollow @" : "Follow @") + post.getAuthorUsername());
            MenuItem notInterested = new MenuItem("Not interested in this post");
            MenuItem mute = new MenuItem((PostStore.getInstance().isMuted(post.getAuthorUsername()) ? "Unmute @" : "Mute @") + post.getAuthorUsername());
            MenuItem report = new MenuItem("Report post");
            follow.setOnAction(event -> { UserSession.getInstance().toggleFollow(post.getAuthorUsername()); refreshView.run(); });
            notInterested.setOnAction(event -> { PostStore.getInstance().hidePost(post); refreshView.run(); });
            mute.setOnAction(event -> { PostStore.getInstance().toggleMute(post.getAuthorUsername()); refreshView.run(); });
            report.setOnAction(event -> {
                Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
                XDialog.info(owner, "Report received", "Thanks. We’ll review this post.");
            });
            menu = menu(follow, notInterested, mute, new SeparatorMenuItem(), report);
        }
        menu.show(anchor, Side.BOTTOM, -190, 2);
    }

    private static ContextMenu menu(MenuItem... items) {
        ContextMenu menu = new ContextMenu(items);
        menu.getStyleClass().add("x-context-menu");
        return menu;
    }

    private static void animateIncrease(Node node) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(240), node);
        slide.setFromY(10);
        slide.setToY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(240), node);
        fade.setFromValue(0.25);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(240), node);
        scale.setFromX(0.88);
        scale.setFromY(0.88);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(slide, fade, scale).play();
    }

    public static void animateReaction(Node node) {
        animateIncrease(node);
    }
}
