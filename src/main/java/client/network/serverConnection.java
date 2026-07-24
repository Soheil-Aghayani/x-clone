package client.network;

import shared.protocol.MessageCodec;
import shared.protocol.Request;
import shared.protocol.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class serverConnection {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public synchronized void connect() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(ServerEndpoint.healthUri())
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new ConnectException("Backend health check returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Backend health check was interrupted", exception);
        }
    }

    public synchronized Response sendMessage(Request requestMessage) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(ServerEndpoint.apiUri())
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(MessageCodec.encodeRequest(requestMessage)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.body() == null || response.body().isBlank()) {
                throw new IOException("Backend returned an empty response (HTTP " + response.statusCode() + ")");
            }
            Response decoded = MessageCodec.decodeResponse(response.body());
            if (decoded == null) throw new IOException("Backend returned invalid JSON");
            return decoded;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Backend request was interrupted", exception);
        }
    }

    public synchronized void disconnect() {
        // HttpClient manages pooled HTTPS connections and needs no explicit close.
    }
}
