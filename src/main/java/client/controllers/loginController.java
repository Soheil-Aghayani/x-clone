package client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import client.network.serverConnection;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;
import shared.protocol.MessageCodec;
import java.util.UUID;

public class loginController {

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
            if (response.getStatus() == StatusCode.OK) {
                errorLabel.setStyle("-fx-text-fill: #00ba7c;"); // X green for success
                errorLabel.setText("Login successful! Redirecting...");
                // TODO: Load dashboard/timeline view
            }
            else if (response.getStatus() == StatusCode.UNAUTHORIZED) {
                errorLabel.setText("Invalid username or password.");
            }
            else {
                errorLabel.setText("Server error: " + response.getStatus());
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
}
