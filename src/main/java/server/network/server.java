package server.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import server.database.AppDatabase;
import server.service.ApiService;
import shared.protocol.MessageCodec;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class server {
    private static final int DEFAULT_PORT = 8080;
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private static final ApiService API = new ApiService();

    public static void main(String[] args) {
        try {
            AppDatabase.getInstance().verifyReady();
            HttpServer httpServer = start(resolvePort());
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> httpServer.stop(1), "x-clone-server-shutdown"));
            new CountDownLatch(1).await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            System.err.println("Backend failed to start: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public static HttpServer start(int port) throws IOException {
        AppDatabase.getInstance().verifyReady();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        httpServer.createContext("/health", server::health);
        httpServer.createContext("/api/request", server::request);
        httpServer.setExecutor(Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                runnable -> {
                    Thread thread = new Thread(runnable, "x-clone-http");
                    thread.setDaemon(true);
                    return thread;
                }));
        httpServer.start();
        System.out.println("X Clone backend listening on 0.0.0.0:" + httpServer.getAddress().getPort());
        return httpServer;
    }

    private static int resolvePort() {
        String configured = System.getenv("PORT");
        if (configured == null || configured.isBlank()) configured = System.getProperty("xclone.server.port");
        if (configured == null || configured.isBlank()) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(configured);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be between 1 and 65535");
        }
    }

    private static void health(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 405, "{\"status\":\"method_not_allowed\"}");
            return;
        }
        send(exchange, 200, "{\"status\":\"ok\"}");
    }

    private static void request(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, 405, MessageCodec.encodeResponse(
                    Response.error(null, StatusCode.BAD_REQUEST, "POST is required.")));
            return;
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            send(exchange, 413, MessageCodec.encodeResponse(
                    Response.error(null, StatusCode.BAD_REQUEST, "Request is too large.")));
            return;
        }
        Response response;
        try {
            response = API.handle(MessageCodec.decodeRequest(new String(body, StandardCharsets.UTF_8)));
        } catch (RuntimeException exception) {
            response = Response.error(null, StatusCode.BAD_REQUEST, "Malformed JSON request.");
        }
        send(exchange, httpStatus(response.getStatus()), MessageCodec.encodeResponse(response));
    }

    private static int httpStatus(StatusCode status) {
        if (status == null) return 500;
        return switch (status) {
            case OK -> 200;
            case BAD_REQUEST -> 400;
            case UNAUTHORIZED -> 401;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case SERVER_ERROR -> 500;
        };
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
