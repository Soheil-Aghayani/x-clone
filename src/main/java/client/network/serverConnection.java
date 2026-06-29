package client.network;

import com.google.gson.Gson;
import shared.protocol.message;

import java.io.*;
import java.net.Socket;

public class serverConnection {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("Connected to server.");
    }

    public message sendMessage(message message) throws IOException {
        out.println(gson.toJson(message));
        String raw = in.readLine();
        return gson.fromJson(raw, message.class);
    }

    public void disconnect() throws IOException {
        socket.close();
    }
}