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
            //Create a valid Request object for PING instead of raw JsonElement
            java.util.UUID.randomUUID().toString();
            shared.protocol.Request pingRequest = new shared.protocol.Request(
                    java.util.UUID.randomUUID().toString(),
                    shared.protocol.RequestType.PING,
                    com.google.gson.JsonParser.parseString("\"ping\"")
            );

            //Pass the unified Request object to the updated network pipeline
            Response response = connection.sendMessage(pingRequest);
            responseLabel.setText("Server says: " + response.getPayload());
        }
        catch (Exception e) {
            responseLabel.setText("Error: " + e.getMessage());
        }
    }
}