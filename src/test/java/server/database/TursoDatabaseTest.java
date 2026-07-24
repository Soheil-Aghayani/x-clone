package server.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TursoDatabaseTest {
    private HttpServer server;
    private URI baseUri;
    private final AtomicReference<JsonObject> received = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> send(exchange, 200, ""));
        server.createContext("/v2/pipeline", this::pipeline);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void bindsParametersAndParsesRowsWithoutLoggingToken() {
        TursoDatabase database = new TursoDatabase(baseUri, "test-token", HttpClient.newHttpClient());
        TursoDatabase.QueryResult result =
                database.execute("SELECT username, id FROM app_users WHERE id = ?", 7);

        assertEquals("Bearer test-token", authorization.get());
        JsonObject statement = received.get().getAsJsonArray("requests")
                .get(0).getAsJsonObject().getAsJsonObject("stmt");
        assertEquals("7", statement.getAsJsonArray("args").get(0)
                .getAsJsonObject().get("value").getAsString());
        assertEquals("integer", statement.getAsJsonArray("args").get(0)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("soheil", result.rows().getFirst().get("username"));
        assertEquals("7", result.rows().getFirst().get("id"));
        assertEquals(1, result.affectedRows());
        assertEquals(7L, result.lastInsertRowId());
    }

    @Test
    void performsAuthenticatedHealthCheck() {
        TursoDatabase database = new TursoDatabase(baseUri, "health-token", HttpClient.newHttpClient());
        database.healthCheck();
        assertTrue(true);
    }

    private void pipeline(HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        received.set(JsonParser.parseString(body).getAsJsonObject());

        JsonObject result = new JsonObject();
        JsonArray columns = new JsonArray();
        columns.add(column("username", "TEXT"));
        columns.add(column("id", "INTEGER"));
        result.add("cols", columns);
        JsonArray rows = new JsonArray();
        JsonArray row = new JsonArray();
        row.add(value("text", "soheil"));
        row.add(value("integer", "7"));
        rows.add(row);
        result.add("rows", rows);
        result.addProperty("affected_row_count", 1);
        result.addProperty("last_insert_rowid", "7");

        JsonObject executeResponse = new JsonObject();
        executeResponse.addProperty("type", "execute");
        executeResponse.add("result", result);
        JsonObject operation = new JsonObject();
        operation.addProperty("type", "ok");
        operation.add("response", executeResponse);

        JsonObject closeResponse = new JsonObject();
        closeResponse.addProperty("type", "close");
        JsonObject close = new JsonObject();
        close.addProperty("type", "ok");
        close.add("response", closeResponse);

        JsonArray results = new JsonArray();
        results.add(operation);
        results.add(close);
        JsonObject response = new JsonObject();
        response.add("results", results);
        send(exchange, 200, response.toString());
    }

    private JsonObject column(String name, String type) {
        JsonObject column = new JsonObject();
        column.addProperty("name", name);
        column.addProperty("decltype", type);
        return column;
    }

    private JsonObject value(String type, String value) {
        JsonObject result = new JsonObject();
        result.addProperty("type", type);
        result.addProperty("value", value);
        return result;
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
