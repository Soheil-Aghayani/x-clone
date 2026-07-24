package client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordVisibilityUiTest {
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
    void loginPasswordCanBeShownAndHiddenWithoutLosingText() throws Exception {
        verifyToggle("/views/Login.fxml");
    }

    @Test
    void signupPasswordCanBeShownAndHiddenWithoutLosingText() throws Exception {
        verifyToggle("/views/Register.fxml");
    }

    private void verifyToggle(String view) throws Exception {
        runOnFxThread(() -> {
            Parent root = FXMLLoader.load(getClass().getResource(view));
            PasswordField hidden = (PasswordField) root.lookup("#passwordField");
            TextField visible = (TextField) root.lookup("#visiblePasswordField");
            Button toggle = (Button) root.lookup("#passwordVisibilityButton");

            hidden.setText("correct horse battery staple");
            assertTrue(hidden.isVisible());
            assertTrue(hidden.isManaged());
            assertFalse(visible.isVisible());
            assertEquals("Show password", toggle.getAccessibleText());

            toggle.fire();
            assertFalse(hidden.isVisible());
            assertFalse(hidden.isManaged());
            assertTrue(visible.isVisible());
            assertTrue(visible.isManaged());
            assertEquals(hidden.getText(), visible.getText());
            assertEquals("Hide password", toggle.getAccessibleText());

            visible.appendText("!");
            toggle.fire();
            assertTrue(hidden.isVisible());
            assertFalse(visible.isVisible());
            assertEquals("correct horse battery staple!", hidden.getText());
            assertEquals("Show password", toggle.getAccessibleText());
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
