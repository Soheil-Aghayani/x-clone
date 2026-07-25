package client.network;

import client.UserSession;
import client.media.MediaLibrary;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import shared.models.SharedSocialState;
import shared.models.SharedTrend;
import shared.models.SocialSettings;
import shared.models.User;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.io.IOException;
import java.util.UUID;
import java.util.List;

/** Authenticated client for social state shared by local and hosted backends. */
public final class SharedSocialClient {
    private final serverConnection connection = new serverConnection();
    private final Gson gson = new Gson();

    public SharedSocialState sync() throws IOException {
        return send(RequestType.SYNC_SOCIAL, payload());
    }

    public SharedSocialState createPost(
            String content,
            String mediaUri,
            Long replyToId,
            Long quotedPostId) throws IOException {
        JsonObject payload = payload();
        payload.addProperty("content", content == null ? "" : content);
        String durableMedia = ensureDurableMedia(mediaUri);
        if (durableMedia != null) payload.addProperty("mediaUri", durableMedia);
        RequestType type = replyToId != null
                ? RequestType.CREATE_REPLY
                : quotedPostId != null ? RequestType.CREATE_QUOTE : RequestType.CREATE_POST;
        if (replyToId != null) payload.addProperty("postId", replyToId);
        if (quotedPostId != null) payload.addProperty("postId", quotedPostId);
        return send(type, payload);
    }

    public SharedSocialState toggleLike(long postId) throws IOException {
        return postAction(RequestType.TOGGLE_LIKE, postId);
    }

    public SharedSocialState toggleRepost(long postId) throws IOException {
        return postAction(RequestType.TOGGLE_REPOST, postId);
    }

    public SharedSocialState toggleBookmark(long postId) throws IOException {
        return postAction(RequestType.TOGGLE_BOOKMARK, postId);
    }

    public SharedSocialState deletePost(long postId) throws IOException {
        return postAction(RequestType.DELETE_TWEET, postId);
    }

    public SharedSocialState toggleFollow(String username) throws IOException {
        JsonObject payload = payload();
        payload.addProperty("username", username);
        return send(RequestType.TOGGLE_FOLLOW, payload);
    }

    public SharedSocialState markNotificationsRead() throws IOException {
        return send(RequestType.MARK_NOTIFICATIONS_READ, payload());
    }

    public SharedSocialState updateProfile(User profile) throws IOException {
        profile.setAvatarUrl(ensureDurableMedia(profile.getAvatarUrl()));
        profile.setBannerUrl(ensureDurableMedia(profile.getBannerUrl()));
        JsonObject payload = payload();
        payload.add("profile", gson.toJsonTree(profile));
        return send(RequestType.UPDATE_PROFILE, payload);
    }

    public SharedSocialState feed(Long beforeId, int limit, boolean followingOnly) throws IOException {
        JsonObject payload = payload();
        if (beforeId != null) payload.addProperty("beforeId", beforeId);
        payload.addProperty("limit", limit);
        payload.addProperty("followingOnly", followingOnly);
        return send(RequestType.GET_FEED, payload);
    }

    public SharedSocialState search(
            String query, String tab, Long beforeId, int limit) throws IOException {
        JsonObject payload = payload();
        payload.addProperty("query", query == null ? "" : query);
        payload.addProperty("tab", tab == null ? "top" : tab);
        if (beforeId != null) payload.addProperty("beforeId", beforeId);
        payload.addProperty("limit", limit);
        return send(RequestType.SEARCH_SOCIAL, payload);
    }

    public List<SharedTrend> trends() throws IOException {
        Response response = sendRaw(RequestType.GET_TRENDS, payload());
        try {
            return List.of(gson.fromJson(response.getPayload(), SharedTrend[].class));
        } catch (RuntimeException exception) {
            throw new IOException("The backend returned invalid trends.", exception);
        }
    }

    public SocialSettings settings() throws IOException {
        Response response = sendRaw(RequestType.GET_SETTINGS, payload());
        return gson.fromJson(response.getPayload(), SocialSettings.class);
    }

    public SocialSettings updateFakeContent(boolean enabled) throws IOException {
        JsonObject payload = payload();
        payload.addProperty("fakeContentEnabled", enabled);
        Response response = sendRaw(RequestType.UPDATE_SETTINGS, payload);
        return gson.fromJson(response.getPayload(), SocialSettings.class);
    }

    private SharedSocialState postAction(RequestType type, long postId) throws IOException {
        JsonObject payload = payload();
        payload.addProperty("postId", postId);
        return send(type, payload);
    }

    private JsonObject payload() throws IOException {
        String token = UserSession.getInstance().getToken();
        if (token == null || token.isBlank()) throw new IOException("Sign in is required.");
        JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        return payload;
    }

    private SharedSocialState send(RequestType type, JsonObject payload) throws IOException {
        Response response = sendRaw(type, payload);
        try {
            return gson.fromJson(response.getPayload(), SharedSocialState.class);
        } catch (RuntimeException exception) {
            throw new IOException("The shared backend returned invalid social data.", exception);
        }
    }

    private Response sendRaw(RequestType type, JsonObject payload) throws IOException {
        Response response = connection.sendMessage(
                new Request(UUID.randomUUID().toString(), type, payload));
        if (response.getStatus() != StatusCode.OK || response.getPayload() == null) {
            String message = response.getMessage();
            throw new IOException(message == null || message.isBlank()
                    ? "The shared backend rejected the request."
                    : message);
        }
        return response;
    }

    private String ensureDurableMedia(String mediaUri) throws IOException {
        if (mediaUri == null || mediaUri.isBlank()) return null;
        MediaLibrary.UploadPayload upload = MediaLibrary.uploadPayload(mediaUri);
        if (upload == null) return mediaUri;
        JsonObject payload = payload();
        payload.addProperty("mimeType", upload.mimeType());
        payload.addProperty("originalName", upload.originalName());
        payload.addProperty("data", upload.base64Data());
        Response response = sendRaw(RequestType.UPLOAD_MEDIA, payload);
        JsonObject result = response.getPayload().getAsJsonObject();
        if (!result.has("mediaUri")) throw new IOException("The backend did not save the media.");
        return result.get("mediaUri").getAsString();
    }
}
