package server.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal Turso SQL-over-HTTP client for the hosted libSQL endpoint.
 *
 * <p>The authorization token is held only in memory and is never included in
 * logs or exception messages.</p>
 */
final class TursoDatabase {
    private final URI baseUri;
    private final URI pipelineUri;
    private final URI healthUri;
    private final String authToken;
    private final HttpClient httpClient;

    TursoDatabase(String databaseUrl, String authToken) {
        this(httpUri(databaseUrl), authToken, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    TursoDatabase(URI baseUri, String authToken, HttpClient httpClient) {
        this.baseUri = baseUri;
        this.pipelineUri = baseUri.resolve("/v2/pipeline");
        this.healthUri = baseUri.resolve("/health");
        this.authToken = authToken;
        this.httpClient = httpClient;
    }

    QueryResult execute(String sql, Object... arguments) {
        return executeBatch(List.of(new SqlStatement(sql, arguments))).getFirst();
    }

    List<QueryResult> executeBatch(List<SqlStatement> statements) {
        JsonObject payload = new JsonObject();
        JsonArray requests = new JsonArray();
        for (SqlStatement statement : statements) {
            JsonObject request = new JsonObject();
            request.addProperty("type", "execute");
            JsonObject body = new JsonObject();
            body.addProperty("sql", statement.sql());
            JsonArray arguments = new JsonArray();
            for (Object value : statement.arguments()) arguments.add(argument(value));
            if (!arguments.isEmpty()) body.add("args", arguments);
            request.add("stmt", body);
            requests.add(request);
        }
        JsonObject close = new JsonObject();
        close.addProperty("type", "close");
        requests.add(close);
        payload.add("requests", requests);

        HttpRequest request = HttpRequest.newBuilder(pipelineUri)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + authToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        JsonObject response = sendJson(request);
        JsonArray results = response.getAsJsonArray("results");
        if (results == null || results.size() < statements.size()) {
            throw new DatabaseException("Turso returned an incomplete pipeline response");
        }

        List<QueryResult> parsed = new ArrayList<>();
        for (int index = 0; index < statements.size(); index++) {
            JsonObject operation = results.get(index).getAsJsonObject();
            if (!"ok".equals(operation.get("type").getAsString())) {
                throw databaseError(operation);
            }
            JsonObject result = operation.getAsJsonObject("response").getAsJsonObject("result");
            parsed.add(parseResult(result));
        }
        return parsed;
    }

    void healthCheck() {
        HttpRequest request = HttpRequest.newBuilder(healthUri)
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        try {
            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DatabaseException("Turso health check returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Turso health check was interrupted", exception);
        } catch (IOException exception) {
            throw new DatabaseException("Could not reach Turso", exception);
        }
    }

    String safeHost() { return baseUri.getHost(); }

    private JsonObject sendJson(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DatabaseException("Turso returned HTTP " + response.statusCode());
            }
            JsonElement json = JsonParser.parseString(response.body());
            if (!json.isJsonObject()) throw new DatabaseException("Turso returned invalid JSON");
            return json.getAsJsonObject();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("Turso request was interrupted", exception);
        } catch (IOException exception) {
            throw new DatabaseException("Could not reach Turso", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof DatabaseException databaseException) throw databaseException;
            throw new DatabaseException("Could not decode Turso response", exception);
        }
    }

    private QueryResult parseResult(JsonObject result) {
        List<String> columns = new ArrayList<>();
        JsonArray columnArray = result.getAsJsonArray("cols");
        if (columnArray != null) {
            for (JsonElement column : columnArray) {
                columns.add(column.getAsJsonObject().get("name").getAsString());
            }
        }

        List<Map<String, String>> rows = new ArrayList<>();
        JsonArray rowArray = result.getAsJsonArray("rows");
        if (rowArray != null) {
            for (JsonElement rowElement : rowArray) {
                JsonArray values = rowElement.getAsJsonArray();
                Map<String, String> row = new LinkedHashMap<>();
                for (int index = 0; index < columns.size(); index++) {
                    row.put(columns.get(index), value(values.get(index).getAsJsonObject()));
                }
                rows.add(row);
            }
        }

        Long lastInsertRowId = null;
        JsonElement inserted = result.get("last_insert_rowid");
        if (inserted != null && !inserted.isJsonNull()) lastInsertRowId = inserted.getAsLong();
        long affected = result.has("affected_row_count")
                ? result.get("affected_row_count").getAsLong()
                : 0;
        return new QueryResult(List.copyOf(rows), affected, lastInsertRowId);
    }

    private String value(JsonObject value) {
        if ("null".equals(value.get("type").getAsString())) return null;
        JsonElement element = value.get("value");
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private JsonObject argument(Object value) {
        JsonObject argument = new JsonObject();
        if (value == null) {
            argument.addProperty("type", "null");
        } else if (value instanceof Float || value instanceof Double) {
            argument.addProperty("type", "float");
            argument.addProperty("value", value.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            argument.addProperty("type", "integer");
            argument.addProperty("value", value instanceof Boolean booleanValue
                    ? (booleanValue ? "1" : "0")
                    : value.toString());
        } else {
            argument.addProperty("type", "text");
            argument.addProperty("value", value.toString());
        }
        return argument;
    }

    private DatabaseException databaseError(JsonObject operation) {
        JsonElement error = operation.get("error");
        String details = error == null ? operation.toString() : error.toString();
        return new DatabaseException("Turso SQL error: " + details);
    }

    private static URI httpUri(String databaseUrl) {
        String normalized = databaseUrl.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("libsql://")) {
            normalized = "https://" + normalized.substring("libsql://".length());
        }
        URI uri = URI.create(normalized);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "TURSO_DATABASE_URL must use libsql:// or https://");
        }
        return URI.create("https://" + uri.getAuthority());
    }

    record SqlStatement(String sql, Object... arguments) {
        SqlStatement(String sql) { this(sql, new Object[0]); }
    }

    record QueryResult(List<Map<String, String>> rows, long affectedRows, Long lastInsertRowId) {}

    static final class DatabaseException extends IllegalStateException {
        DatabaseException(String message) { super(message); }
        DatabaseException(String message, Throwable cause) { super(message, cause); }

        boolean isUniqueViolation() {
            String message = getMessage() == null ? "" : getMessage().toLowerCase(Locale.ROOT);
            return message.contains("unique constraint")
                    || message.contains("sqlite_constraint_unique")
                    || message.contains("sqlite_constraint");
        }
    }
}
