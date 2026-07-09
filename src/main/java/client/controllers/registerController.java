package client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class registerController {

    // UI elements from FXML
    @FXML
    private TextField displayNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    @FXML
    private Label errorLabel;

    /**
     * Executes client-side input integrity evaluation and prepares network payload structures.
     */
    @FXML
    private void handleRegister() {
        String displayName = displayNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        //Client-Side Structural Sanity Validation Checked locally
        if (displayName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please populate all execution parameters before registration.");
            return;
        }

        // Feedback placeholder simulating an atomic pipeline call state context
        errorLabel.setStyle("-fx-text-fill: #1d9bf0;"); // X Blue
        errorLabel.setText("Mock Pipeline: Staging packet generation for handle: " + username);
    }

    /**
     * Reroutes view state contexts backward into the Authentication (Login) UI stage tree.
     */
    @FXML
    private void handleBackToLogin() {
        System.out.println("Rewinding pipeline back to active login viewport layout.");
        // TODO: Mount scene execution framework context transition in next iteration phase
    }
}