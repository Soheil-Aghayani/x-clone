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

public class RegisterController {
    @FXML private Label authLogo;
    @FXML private Button backButton;
    @FXML private TextField displayNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button passwordVisibilityButton;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    private final serverConnection connection = new serverConnection();
    private boolean submitting;
    private boolean passwordVisible;

    @FXML
    public void initialize() {
        authLogo.setGraphic(AppIcons.icon("x-logo-icon.svg", 31, "#e7e9ea"));
        backButton.setText("×");
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        for (TextField field : new TextField[]{displayNameField, usernameField, emailField, passwordField, visiblePasswordField}) {
            field.textProperty().addListener((observable, oldText, newText) -> field.setFont(AppFonts.fontFor(newText, 16)));
        }
        updatePasswordVisibility();
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
    private void handleRegister() {
        if (submitting) return;
        String displayName = displayNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (displayName.isEmpty() || username.isEmpty() || email.isEmpty()) {
            showError("Complete every field to create your account.");
            return;
        }
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            showError("Enter a valid email address.");
            return;
        }
        if (!username.matches("[A-Za-z0-9_]{1,15}")) {
            showError("Username must be 1–15 letters, numbers, or underscores.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must contain at least 6 characters.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("displayName", displayName);
        payload.addProperty("username", username);
        payload.addProperty("email", email);
        payload.addProperty("password", password);
        Request request = new Request(UUID.randomUUID().toString(), RequestType.REGISTER, payload);

        setBusy(true, "Creating account…");
        Task<Response> task = new Task<>() {
            @Override protected Response call() throws Exception { return connection.sendMessage(request); }
        };
        task.setOnSucceeded(event -> handleResponse(task.getValue()));
        task.setOnFailed(event -> {
            setBusy(false, "Create account");
            showError("We couldn't reach the backend. Check that the server is running and try again.");
        });
        Thread thread = new Thread(task, "x-register-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleResponse(Response response) {
        if (response == null) {
            setBusy(false, "Create account");
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
                showSuccess("Account created. Loading X…");
                if (!NavigationManager.switchScene("/views/Feed.fxml")) {
                    UserSession.getInstance().clearSession();
                    setBusy(false, "Create account");
                    showError("Your account was created, but Home could not be opened.");
                }
                return;
            } catch (RuntimeException exception) {
                setBusy(false, "Create account");
                showError("The backend returned an invalid account response.");
                return;
            }
        }

        setBusy(false, "Create account");
        if (response.getStatus() == StatusCode.CONFLICT) {
            showError("That username or email is already in use.");
        } else {
            showError(response.getMessage() == null ? "Account creation failed." : response.getMessage());
        }
    }

    private void setBusy(boolean busy, String text) {
        submitting = busy;
        registerButton.setDisable(busy);
        registerButton.setText(text);
        displayNameField.setDisable(busy);
        usernameField.setDisable(busy);
        emailField.setDisable(busy);
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
    private void handleBackToLogin() {
        NavigationManager.switchScene("/views/Login.fxml");
    }
}
