package client.controllers;

import client.AppFonts;
import client.AppIcons;
import client.NavigationManager;
import client.UserSession;
import client.network.serverConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import shared.models.Session;
import shared.models.User;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.util.UUID;

public class LoginController {
    @FXML private Label authLogo;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button passwordVisibilityButton;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final serverConnection connection = new serverConnection();
    private boolean submitting;
    private boolean passwordVisible;

    @FXML
    public void initialize() {
        authLogo.setGraphic(AppIcons.icon("x-logo-icon.svg", 34, "#e7e9ea"));
        usernameField.textProperty().addListener((observable, oldText, newText) ->
                usernameField.setFont(AppFonts.fontFor(newText, 16)));
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordField.textProperty().addListener((observable, oldText, newText) ->
                updatePasswordFonts(newText));
        updatePasswordVisibility();
    }

    @FXML
    private void focusPassword() {
        activePasswordField().requestFocus();
    }

    @FXML
    private void togglePasswordVisibility() {
        TextField previous = activePasswordField();
        int caret = previous.getCaretPosition();
        passwordVisible = !passwordVisible;
        updatePasswordVisibility();
        TextField active = activePasswordField();
        active.requestFocus();
        active.positionCaret(Math.min(caret, active.getLength()));
    }

    @FXML
    private void handleLogin() {
        if (submitting) return;
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Enter your phone, email, or username and password.");
            return;
        }

        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);
        Request request = new Request(UUID.randomUUID().toString(), RequestType.LOGIN, credentials);

        setBusy(true, "Signing in…");
        Task<Response> task = new Task<>() {
            @Override protected Response call() throws Exception { return connection.sendMessage(request); }
        };
        task.setOnSucceeded(event -> handleResponse(task.getValue()));
        task.setOnFailed(event -> {
            setBusy(false, "Sign in");
            showError("We couldn't reach the backend. Check that the server is running and try again.");
        });
        Thread thread = new Thread(task, "x-login-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleResponse(Response response) {
        if (response == null) {
            setBusy(false, "Sign in");
            showError("The backend closed the connection. Please try again.");
            return;
        }
        if (response.getStatus() == StatusCode.OK && response.getPayload() != null) {
            try {
                JsonObject payload = response.getPayload().getAsJsonObject();
                Gson gson = new Gson();
                User user = gson.fromJson(payload.get("user"), User.class);
                Session session = gson.fromJson(payload.get("session"), Session.class);
                UserSession.getInstance().startSession(user, session);
                showSuccess("Signed in. Loading your timeline…");
                if (!NavigationManager.switchScene("/views/Feed.fxml")) {
                    UserSession.getInstance().clearSession();
                    setBusy(false, "Sign in");
                    showError("Your account was verified, but Home could not be opened.");
                }
                return;
            } catch (RuntimeException exception) {
                setBusy(false, "Sign in");
                showError("The backend returned an invalid account response.");
                return;
            }
        }

        setBusy(false, "Sign in");
        if (response.getStatus() == StatusCode.UNAUTHORIZED || response.getStatus() == StatusCode.NOT_FOUND) {
            showError("Wrong username, email, or password.");
        } else {
            showError(response.getMessage() == null ? "Sign in failed. Please try again." : response.getMessage());
        }
    }

    private void setBusy(boolean busy, String text) {
        submitting = busy;
        loginButton.setDisable(busy);
        loginButton.setText(text);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        visiblePasswordField.setDisable(busy);
        passwordVisibilityButton.setDisable(busy);
    }

    private TextField activePasswordField() {
        return passwordVisible ? visiblePasswordField : passwordField;
    }

    private void updatePasswordVisibility() {
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        passwordVisibilityButton.setGraphic(AppIcons.icon(
                passwordVisible ? "eye-hide-icon.svg" : "eye-show-icon.svg",
                20,
                "#71767b"
        ));
        passwordVisibilityButton.setAccessibleText(passwordVisible ? "Hide password" : "Show password");
    }

    private void updatePasswordFonts(String text) {
        passwordField.setFont(AppFonts.fontFor(text, 16));
        visiblePasswordField.setFont(AppFonts.fontFor(text, 16));
    }

    private void showError(String message) {
        errorLabel.getStyleClass().remove("auth-error-success");
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        if (!errorLabel.getStyleClass().contains("auth-error-success")) {
            errorLabel.getStyleClass().add("auth-error-success");
        }
        errorLabel.setText(message);
    }

    @FXML
    private void handleExternalSignIn() {
        showError("Google and Apple sign-in require provider credentials. Use your X Clone account below.");
    }

    @FXML
    private void handleForgotPassword() {
        showError("Password recovery is not connected to email yet. Create another local account if needed.");
    }

    @FXML
    private void handleGoToRegister() {
        NavigationManager.switchScene("/views/Register.fxml");
    }
}
