package client.controllers;

import client.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import client.network.serverConnection;
import com.google.gson.JsonObject;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.util.UUID;

import client.UserSession;

public class LoginController {

    // UI elements from FXML
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    // Network connection instance
    private final serverConnection connection = new serverConnection();

    @FXML
    public void initialize() {
        // Establish initial connection
        try {
            connection.connect();
        }
        catch (Exception e) {
            errorLabel.setText("Network Error: Could not connect to backend server.");
        }
    }

    /**
     * Extracts credentials, builds JSON payload, and sends login request via network.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // ----------------------------------------------------------------------
        // DEVELOPMENT MOCK BYPASS: Active for offline visual compilation tasks
        // ----------------------------------------------------------------------
        System.out.println("Authentication bypass: Staging session context tracking...");

        // Generating official model frames populated with mock properties
        shared.models.User mockUser = new shared.models.User(1, username, username + "@example.com", "Sample User", "Bio Details", null, null, "2026-01-01");
        shared.models.Session mockSession = new shared.models.Session(101, 1, "MOCK_JWT_TOKEN_12345", "2026-12-31");

        // Passing the unified models directly into the client UI session pipeline
        UserSession.getInstance().startSession(mockUser, mockSession);

        NavigationManager.switchScene("/views/Feed.fxml");
        if (true) return;
        // ----------------------------------------------------------------------

        // Step 1: Client-side validation
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            // Step 2: Build JSON payload matching server expectations
            JsonObject loginCredentials = new JsonObject();
            loginCredentials.addProperty("username", username);
            loginCredentials.addProperty("password", password);

            // Step 3: Create a Request instance
            String uniqueId = UUID.randomUUID().toString();
            Request loginRequest = new Request(uniqueId, RequestType.LOGIN, loginCredentials);

            // Step 4: Transmit request using serverConnection context
            Response response = sendCustomRequest(loginRequest);

            // Step 5: Process Server Response
            if (response != null) {
                if (response.getStatus() == StatusCode.OK) {
                    errorLabel.setStyle("-fx-text-fill: #00ba7c;");
                    errorLabel.setText("Login successful! Redirecting...");

                    NavigationManager.switchScene("/views/Feed.fxml");
                }
                else if (response.getStatus() == StatusCode.UNAUTHORIZED) {
                    errorLabel.setText("Invalid username or password.");
                }
                else if (response.getStatus() == StatusCode.NOT_FOUND) {
                    errorLabel.setText("Account not found.");
                }
                else {
                    errorLabel.setText("Server error: " + response.getStatus());
                }
            }

        }
        catch (Exception e) {
            errorLabel.setText("Transmission failed: " + e.getMessage());
        }
    }

    /**
     * Temporary bridge method to execute custom Requests over current socket pipeline.
     */
    private Response sendCustomRequest(Request req) throws Exception {
       return null; // Will hook into server connection once backend handler is synchronized
    }

    /**
     * Triggered when user clicks 'Create account'. Routes the UI stage to register view.
     */
    @FXML
    private void handleGoToRegister() {
        NavigationManager.switchScene("/views/Register.fxml");
    }
}
