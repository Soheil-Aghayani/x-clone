package client.controllers;

import client.network.serverConnection;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import shared.protocol.Response;


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
            Response response = connection.sendMessage(JsonParser.parseString("ping"));
            responseLabel.setText("Server says: " + response.getPayload());
        } catch (Exception e) {
            responseLabel.setText("Error: " + e.getMessage());
        }
    }
}