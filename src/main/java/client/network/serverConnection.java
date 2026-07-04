package client.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import shared.protocol.*;

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

    public Response sendMessage(JsonElement message) throws IOException {
        Request req = new Request("1", RequestType.PING, message);
        out.println(MessageCodec.encodeRequest(req));
        String raw = in.readLine();
        return MessageCodec.decodeResponse(raw);
    }

    public void disconnect() throws IOException {
        socket.close();
    }
}