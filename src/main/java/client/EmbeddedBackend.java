package client;

import client.network.ServerEndpoint;
import server.network.server;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Starts the local backend unless a remote backend URL was configured. */
public final class EmbeddedBackend {
    private static volatile boolean startAttempted;

    private EmbeddedBackend() {}

    public static synchronized void ensureStarted() {
        if (!ServerEndpoint.isLocal()) {
            System.out.println("Using remote backend " + ServerEndpoint.baseUri());
            return;
        }
        if (isListening() || startAttempted) return;
        startAttempted = true;
        Thread backend = new Thread(() -> server.main(new String[0]), "x-clone-local-backend");
        backend.setDaemon(true);
        backend.setUncaughtExceptionHandler((thread, error) ->
                System.err.println("Local backend stopped: " + error.getMessage()));
        backend.start();

        for (int attempt = 0; attempt < 40 && !isListening(); attempt++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!isListening()) {
            System.err.println("Local backend did not become ready at " + ServerEndpoint.baseUri());
        }
    }

    private static boolean isListening() {
        try {
            HttpRequest request = HttpRequest.newBuilder(ServerEndpoint.healthUri())
                    .timeout(Duration.ofMillis(300))
                    .GET()
                    .build();
            return HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode() == 200;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
