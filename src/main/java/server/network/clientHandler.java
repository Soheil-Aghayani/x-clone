package server.network;

import server.service.ApiService;
import shared.protocol.MessageCodec;
import shared.protocol.Request;
import shared.protocol.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/** Legacy socket adapter retained for source compatibility. New clients use HTTPS. */
public class clientHandler implements Runnable {
    private final Socket socket;
    private final ApiService api = new ApiService();

    public clientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String raw;
            while ((raw = in.readLine()) != null) {
                Request message = MessageCodec.decodeRequest(raw);
                Response response = api.handle(message);
                out.println(MessageCodec.encodeResponse(response));
            }
        } catch (IOException exception) {
            System.out.println("Legacy socket client disconnected.");
        }
    }
}
