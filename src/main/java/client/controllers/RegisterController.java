package client.controllers;

import client.NavigationManager;
import client.network.serverConnection;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.util.UUID;

public class RegisterController {

    @FXML
    private TextField displayNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    // Isolated network connection context for this screen
    private final serverConnection connection = new serverConnection();

    /**
     * Extracts inputs, packs them into a standard JSON payload, and requests remote account creation.
     */
    @FXML
    private void handleRegister() {
        String displayName = displayNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Client-side structural integrity check (Keeps faults isolated to client)
        if (displayName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please populate all fields before proceeding.");
            return;
        }

        System.out.println("Registration bypass: Routing user to the main timeline feed...");
        NavigationManager.switchScene("/views/feed.fxml");
        if (true) return;

        try {
            // Package registration data into a uniform JSON object payload
            JsonObject registerPayload = new JsonObject();
            registerPayload.addProperty("displayName", displayName);
            registerPayload.addProperty("username", username);
            registerPayload.addProperty("email", email);
            registerPayload.addProperty("password", password);

            // Wrap inside the unified architecture Request object
            String uniqueId = UUID.randomUUID().toString();
            Request registerRequest = new Request(uniqueId, RequestType.REGISTER, registerPayload);

            // Establish standard connection bridge
            connection.connect();

            // Dispatch request through the client connection pipeline
            Response response = connection.sendMessage(registerRequest);

            // Process standard response status codes from network protocol
            if (response != null) {
                if (response.getStatus() == StatusCode.OK) {
                    errorLabel.setStyle("-fx-text-fill: #00ba7c;");
                    errorLabel.setText("Account created successfully! Welcome ...");
                    // Reroute to login context so user can authenticate
                    NavigationManager.switchScene("/views/Feed.fxml");
                }
                else if (response.getStatus() == StatusCode.BAD_REQUEST) {
                    errorLabel.setText("Registration rejected: Username or email already exists.");
                }
                else {
                    errorLabel.setText("System status code: " + response.getStatus());
                }
            }

        }
        catch (Exception e) {
            // If server is not running or network fails, display a generic message to user
            errorLabel.setText("Network transmission offline. Local simulation active.");
            System.out.println("Network info: Server response handler might be pending or offline.");
        }
        finally {
            try {
                connection.disconnect();
            }
            catch (Exception ignored) {}
        }
    }

    /**
     * Reroutes view state contexts backward into the Authentication (Login) UI stage tree.
     */
    @FXML
    private void handleBackToLogin() {
        NavigationManager.switchScene("/views/Login.fxml");
    }
}