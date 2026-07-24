package client.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XDialogTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            // The test suite shares one JavaFX toolkit.
        }
    }

    @Test
    void destructiveConfirmationUsesAppStyledControls() throws Exception {
        runOnFxThread(() -> {
            StackPane content = XDialog.createContent(
                    "Delete post?",
                    "This post will be permanently deleted.",
                    "Delete",
                    true,
                    null,
                    false,
                    "Cancel",
                    ignored -> { }
            );
            List<Node> nodes = flatten(content);
            assertTrue(content.getStyleClass().contains("x-dialog-shell"));
            assertTrue(nodes.stream().anyMatch(node -> node.getStyleClass().contains("x-dialog-card")));
            assertTrue(nodes.stream().filter(Button.class::isInstance)
                    .anyMatch(node -> node.getStyleClass().contains("x-dialog-danger")));
            assertTrue(nodes.stream().filter(Labeled.class::isInstance)
                    .map(Labeled.class::cast).anyMatch(label -> "Delete post?".equals(label.getText())));
            assertTrue(nodes.stream().filter(Labeled.class::isInstance)
                    .map(Labeled.class::cast).anyMatch(label -> "Cancel".equals(label.getText())));
            return null;
        });
    }

    private static List<Node> flatten(Node node) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                nodes.addAll(flatten(child));
            }
        }
        if (node instanceof Labeled labeled && labeled.getGraphic() != null) {
            nodes.addAll(flatten(labeled.getGraphic()));
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
