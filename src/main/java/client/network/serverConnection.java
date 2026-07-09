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
    //private final Gson gson = new Gson();

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("Connected to server.");
    }

    public Response sendMessage(Request req) throws IOException {
        //Encode the Request object
        String jsonPayload = MessageCodec.encodeRequest(req);
        out.println(jsonPayload);

        //read the raw JSON response from the server input stream
        String rawResponse = in.readLine();

        //Decode the raw string back
        return MessageCodec.decodeResponse(rawResponse);
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}