package client.controllers;

import client.UserSession;
import client.MainApp;
import client.chat.ChatStore;
import client.timeline.PostStore;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shared.models.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedNotificationChatUiTest {
    @TempDir static Path temporaryDirectory;

    @BeforeAll
    static void startJavaFx() throws Exception {
        System.setProperty("xclone.data.dir", temporaryDirectory.toString());
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            // Another UI test initialized the shared JavaFX toolkit.
        }
    }

    @Test
    void formatsDynamicWindowTitleLikeX() {
        assertEquals("(3) Home / X", MainApp.formatWindowTitle("Home", 3));
        assertEquals("Notifications / X", MainApp.formatWindowTitle("Notifications", 0));
    }

    @Test
    void rendersUnreadBadgeChatSettingsAndChangePasscode() throws Exception {
        runOnFxThread(() -> {
            String suffix = Long.toUnsignedString(System.nanoTime());
            User current = new User(20, "badge_" + suffix, "badge@example.test",
                    "Badge User", "", null, null, "2026-07-24");
            User actor = new User(21, "actor_" + suffix, "actor@example.test",
                    "Actor User", "", null, null, "2026-07-24");
            UserSession.getInstance().startSession(current, null);
            PostStore.getInstance().toggleFollow(actor, current.getUsername());
            ChatStore.getInstance().setPasscode(current.getUsername(), "1234");
            UserSession.getInstance().setChatUnlocked(true);
            PostStore.getInstance().requestView("chat");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Feed.fxml"));
            Parent root = loader.load();
            root.applyCss();
            root.layout();
            assertTrue(allText(root).contains("1"),
                    "Unread notification badge was not rendered");

            FeedController controller = loader.getController();
            setField(controller, "chatView", "settings");
            invoke(controller, "refreshTimeline");
            assertTrue(allText(root).contains("Settings"));
            assertTrue(allText(root).contains("Change passcode"));
            assertTrue(allText(root).contains("Delete all media"));

            setField(controller, "chatView", "change-passcode");
            invoke(controller, "refreshTimeline");
            assertTrue(allText(root).contains("Change Passcode"));
            UserSession.getInstance().clearSession();
            return null;
        });
    }

    private static List<String> allText(Node root) {
        List<String> text = new ArrayList<>();
        if (root instanceof Labeled labeled && labeled.getText() != null) text.add(labeled.getText());
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) text.addAll(allText(child));
        }
        if (root instanceof ScrollPane scrollPane && scrollPane.getContent() != null) {
            text.addAll(allText(scrollPane.getContent()));
        }
        if (root instanceof Labeled labeled && labeled.getGraphic() != null) {
            text.addAll(allText(labeled.getGraphic()));
        }
        return text;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
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
