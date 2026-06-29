package server.network;

import com.google.gson.Gson;
import shared.protocol.message;

import java.io.*;
import java.net.Socket;

public class clientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();

    public clientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
        ) {
            String raw;
            int count = 1;
            while ((raw = in.readLine()) != null) {
                message message = gson.fromJson(raw, message.class);
                System.out.println("Received: " + message.getType());

                if (message.getType().equals("ping")) {
                    message response = new message("ping", "Hello from server! (" + count++ + ")");
                    out.println(gson.toJson(response));
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}