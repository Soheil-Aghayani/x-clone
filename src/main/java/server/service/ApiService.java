package server.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import server.database.AppDatabase;
import shared.models.Session;
import shared.models.User;
import shared.protocol.Request;
import shared.protocol.Response;
import shared.protocol.StatusCode;

/** Transport-independent request handling used by the public HTTP API. */
public final class ApiService {
    private final Gson gson = new Gson();

    public Response handle(Request request) {
        if (request == null || request.getType() == null) {
            return Response.error(null, StatusCode.BAD_REQUEST, "Invalid request.");
        }
        try {
            return switch (request.getType()) {
                case PING -> Response.ok(request.getRequestId(), gson.toJsonTree("Hi"));
                case REGISTER -> register(request);
                case LOGIN -> login(request);
                default -> Response.error(request.getRequestId(), StatusCode.BAD_REQUEST,
                        "This request is not supported yet.");
            };
        } catch (IllegalArgumentException exception) {
            return Response.error(request.getRequestId(), StatusCode.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace();
            return Response.error(request.getRequestId(), StatusCode.SERVER_ERROR, "Server error");
        }
    }

    private Response register(Request request) {
        JsonObject payload = payload(request);
        String displayName = string(payload, "displayName");
        String username = string(payload, "username");
        String email = string(payload, "email");
        String password = string(payload, "password");
        if (displayName.isBlank() || username.isBlank() || email.isBlank() || password.length() < 6) {
            return Response.error(request.getRequestId(), StatusCode.BAD_REQUEST,
                    "Complete every field and use a password with at least 6 characters.");
        }
        User user = AppDatabase.getInstance().register(displayName, username, email, password);
        if (user == null) {
            return Response.error(request.getRequestId(), StatusCode.CONFLICT,
                    "Username or email already exists.");
        }
        return Response.ok(request.getRequestId(), sessionPayload(user));
    }

    private Response login(Request request) {
        JsonObject payload = payload(request);
        String username = string(payload, "username");
        String password = string(payload, "password");
        User user = AppDatabase.getInstance().authenticate(username, password);
        if (user == null) {
            return Response.error(request.getRequestId(), StatusCode.UNAUTHORIZED,
                    "Invalid username or password.");
        }
        return Response.ok(request.getRequestId(), sessionPayload(user));
    }

    private JsonObject payload(Request request) {
        if (request.getPayload() == null || !request.getPayload().isJsonObject()) {
            throw new IllegalArgumentException("The request payload must be a JSON object.");
        }
        return request.getPayload().getAsJsonObject();
    }

    private JsonElement sessionPayload(User user) {
        Session session = AppDatabase.getInstance().createSession(user);
        JsonObject payload = new JsonObject();
        payload.add("user", gson.toJsonTree(user));
        payload.add("session", gson.toJsonTree(session));
        return payload;
    }

    private String string(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString().trim()
                : "";
    }
}
