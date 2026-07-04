package server.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import shared.protocol.MessageCodec;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

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
            while ((raw = in.readLine()) != null) {
                Request message = MessageCodec.decodeRequest(raw);
                System.out.println("Received: " + message.getType());

                if (message.getType() == RequestType.PING) {
                    JsonElement res = JsonParser.parseString("Hi ");
                    out.println(MessageCodec.encodeResponse(Response.ok("1",res)));
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}