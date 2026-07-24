package server.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import server.database.AppDatabase;
import server.database.SocialDatabase;
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
                case SYNC_SOCIAL -> socialState(request);
                case CREATE_POST, CREATE_TWEET -> createPost(request, null, null);
                case CREATE_REPLY -> createPost(
                        request, longValue(payload(request), "postId"), null);
                case CREATE_QUOTE -> createPost(
                        request, null, longValue(payload(request), "postId"));
                case TOGGLE_LIKE -> toggleInteraction(request, SocialDatabase.Interaction.LIKE);
                case TOGGLE_REPOST -> toggleInteraction(request, SocialDatabase.Interaction.REPOST);
                case TOGGLE_BOOKMARK -> toggleInteraction(request, SocialDatabase.Interaction.BOOKMARK);
                case TOGGLE_FOLLOW -> toggleFollow(request);
                case DELETE_TWEET -> deletePost(request);
                case MARK_NOTIFICATIONS_READ -> markNotificationsRead(request);
                case UPDATE_PROFILE -> updateProfile(request);
                default -> Response.error(request.getRequestId(), StatusCode.BAD_REQUEST,
                        "This request is not supported yet.");
            };
        } catch (SecurityException exception) {
            return Response.error(request.getRequestId(), StatusCode.UNAUTHORIZED, exception.getMessage());
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
        if (username.toLowerCase(java.util.Locale.ROOT).startsWith("xclone_")) {
            return Response.error(request.getRequestId(), StatusCode.BAD_REQUEST,
                    "Usernames beginning with xclone_ are reserved for automated demo accounts.");
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

    private Response socialState(Request request) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().state(string(payload, "token"))));
    }

    private Response createPost(Request request, Long replyToId, Long quotedPostId) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().createPost(
                        string(payload, "token"),
                        string(payload, "content"),
                        nullableString(payload, "mediaUri"),
                        replyToId,
                        quotedPostId)));
    }

    private Response toggleInteraction(Request request, SocialDatabase.Interaction interaction) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().togglePostInteraction(
                        string(payload, "token"),
                        longValue(payload, "postId"),
                        interaction)));
    }

    private Response toggleFollow(Request request) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().toggleFollow(
                        string(payload, "token"),
                        string(payload, "username"))));
    }

    private Response deletePost(Request request) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().deletePost(
                        string(payload, "token"),
                        longValue(payload, "postId"))));
    }

    private Response markNotificationsRead(Request request) {
        JsonObject payload = payload(request);
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().markNotificationsRead(string(payload, "token"))));
    }

    private Response updateProfile(Request request) {
        JsonObject payload = payload(request);
        User profile = payload.has("profile")
                ? gson.fromJson(payload.get("profile"), User.class)
                : null;
        return Response.ok(request.getRequestId(), gson.toJsonTree(
                SocialDatabase.getInstance().updateProfile(
                        string(payload, "token"),
                        profile)));
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

    private String nullableString(JsonObject object, String name) {
        String value = string(object, name);
        return value.isBlank() ? null : value;
    }

    private long longValue(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        try {
            long value = object.get(name).getAsLong();
            if (value < 1) throw new NumberFormatException();
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(name + " must be a positive number.");
        }
    }
}
