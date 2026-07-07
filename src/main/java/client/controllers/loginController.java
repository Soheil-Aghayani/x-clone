package client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

    /**
     * Handles the login action
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Step 1: Client-side validation (Immediate feedback without network delay)
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        errorLabel.setStyle("-fx-text-fill: #1d9bf0;"); // Twitter blue for status
        errorLabel.setText("Connecting to server...");

        // TODO: Step 2: Package data into Request protocol and send via network pipeline
    }
}
