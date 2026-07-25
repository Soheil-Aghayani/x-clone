package client.ui;

import client.NavigationManager;
import client.UserSession;
import client.profile.AccountDirectory;
import client.profile.AccountProfile;
import client.timeline.PostStore;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shared.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileIdentityNavigationTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            // The test suite shares one JavaFX toolkit.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void avatarNameAndHandleShareTheSameProfileAction() throws Exception {
        runOnFxThread(() -> {
            Label avatar = new Label("A");
            Label name = new Label("JavaFX");
            Label handle = new Label("@openjfx");
            AtomicInteger opens = new AtomicInteger();

            ProfileHoverCard.attachIdentity(
                    avatar,
                    name,
                    handle,
                    "openjfx",
                    opens::incrementAndGet
            );

            for (Node node : List.of(avatar, name, handle)) {
                assertNotNull(node.getOnMouseClicked());
                assertEquals("Open @openjfx profile", node.getAccessibleText());
                Event.fireEvent(node, click());
            }
            assertEquals(3, opens.get());
            return null;
        });
    }

    @Test
    void hoverCardAvatarNameAndHandleAreProfileLinks() throws Exception {
        runOnFxThread(() -> {
            AccountProfile account = AccountDirectory.find("openjfx");
            AtomicInteger opens = new AtomicInteger();
            VBox card = ProfileHoverCard.buildCard(account, opens::incrementAndGet);

            List<Node> profileLinks = flatten(card).stream()
                    .filter(node -> "Open @openjfx profile".equals(node.getAccessibleText()))
                    .toList();
            assertEquals(3, profileLinks.size());
            profileLinks.forEach(node -> Event.fireEvent(node, click()));
            assertEquals(3, opens.get());
            return null;
        });
    }

    @Test
    void clickingTimelineAvatarOpensThatAuthorsProfilePage() throws Exception {
        runOnFxThread(() -> {
            User current = new User(
                    90,
                    "profile_nav_test",
                    "profile-nav@example.test",
                    "Profile Navigation Test",
                    "",
                    null,
                    null,
                    "2026-07-24"
            );
            UserSession.getInstance().startSession(current, null);
            PostStore.getInstance().createPost(
                    current, "A real test post used for profile navigation.");
            PostStore.getInstance().requestView("home");

            Stage stage = new Stage();
            NavigationManager.setStage(stage);
            Parent feed = FXMLLoader.load(getClass().getResource("/views/Feed.fxml"));
            stage.setScene(new Scene(feed, 1280, 800));
            stage.show();
            feed.applyCss();
            feed.layout();

            HBox post = (HBox) feed.lookupAll(".post-row").stream().findFirst().orElseThrow();
            Node avatar = post.getChildren().getFirst();
            String expectedHandle = avatar.getAccessibleText()
                    .replace("Open ", "")
                    .replace(" profile", "");

            Event.fireEvent(avatar, click());

            Label viewedHandle = (Label) stage.getScene().getRoot().lookup("#usernameLabel");
            assertNotNull(viewedHandle);
            assertEquals(expectedHandle, viewedHandle.getText());

            stage.close();
            UserSession.getInstance().clearSession();
            return null;
        });
    }

    private static MouseEvent click() {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                MouseButton.PRIMARY,
                1,
                false, false, false, false,
                true, false, false,
                true, false, false,
                null
        );
    }

    private static List<Node> flatten(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                nodes.addAll(flatten(child));
            }
        }
        return nodes;
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
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
