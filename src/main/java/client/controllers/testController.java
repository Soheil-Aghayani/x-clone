package client.controllers;

import client.network.serverConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import shared.protocol.message;

public class testController {
    @FXML
    private Label responseLabel;

    private final serverConnection connection = new serverConnection();

    @FXML
    public void initialize() {
        try {
            connection.connect();
        } catch (Exception e) {
            responseLabel.setText("Could not connect to server.");
        }
    }

    @FXML
    private void handlePing() {
        try {
            message response = connection.sendMessage(
                    new message("ping", ""));
            responseLabel.setText("Server says: "
                    + response.getType() + " — " + response.getPayload());
        } catch (Exception e) {
            responseLabel.setText("Error: " + e.getMessage());
        }
    }
}