package client.ui;

import client.NavigationManager;
import client.UserSession;
import client.timeline.Post;
import client.timeline.PostStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.models.User;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadNavigationUiTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            // The suite shares one JavaFX toolkit.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void threadShowsOriginalDirectAndNestedReplies() throws Exception {
        runOnFxThread(() -> {
            User user = new User(
                    92,
                    "thread_view_test",
                    "thread-view@example.test",
                    "Thread View Test",
                    "",
                    null,
                    null,
                    "2026-07-25"
            );
            UserSession.getInstance().startSession(user, null);
            PostStore store = PostStore.getInstance();
            Post root = store.createPost(user, "Thread root");
            Post directReply = store.createReply(user, "Direct reply", null, root);
            Post nestedReply = store.createReply(user, "Nested reply", null, directReply);

            assertEquals(2, store.getThreadReplies(root.getId()).size());
            assertEquals(2, store.getConversationAncestors(nestedReply.getId()).size());

            store.requestPost(root.getId());
            Stage stage = new Stage();
            NavigationManager.setStage(stage);
            Parent feed = FXMLLoader.load(getClass().getResource("/views/Feed.fxml"));
            stage.setScene(new Scene(feed, 1280, 800));
            stage.show();
            feed.applyCss();
            feed.layout();

            Label header = (Label) feed.lookup("#headerTitleLabel");
            assertNotNull(header);
            assertEquals("Post", header.getText());
            assertNotNull(feed.lookup(".post-detail-card"));
            assertNotNull(feed.lookup(".thread-reply-composer"));
            Set<javafx.scene.Node> cards = feed.lookupAll(".post-row");
            assertEquals(3, cards.size());
            assertTrue(cards.stream().allMatch(HBox.class::isInstance));

            stage.close();
            store.deletePost(root);
            UserSession.getInstance().clearSession();
            return null;
        });
    }

    private static <T> T runOnFxThread(ThrowingSupplier<T> work) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(work.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                finished.countDown();
            }
        });
        assertTrue(finished.await(10, TimeUnit.SECONDS), "JavaFX work timed out");
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
