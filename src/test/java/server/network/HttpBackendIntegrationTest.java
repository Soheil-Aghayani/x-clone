package server.network;

import client.network.serverConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;
import shared.models.SharedSocialState;

import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpBackendIntegrationTest {
    @TempDir
    static Path temporaryData;

    private static HttpServer backend;

    @BeforeAll
    static void startBackend() throws Exception {
        System.setProperty("xclone.server.data.dir", temporaryData.toString());
        System.setProperty("xclone.npc.interval.seconds", "1");
        System.setProperty("xclone.admin.username", "admin_integration");
        backend = server.start(0);
        System.setProperty("xclone.server.url",
                "http://127.0.0.1:" + backend.getAddress().getPort());
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) backend.stop(0);
        System.clearProperty("xclone.server.url");
        System.clearProperty("xclone.server.data.dir");
        System.clearProperty("xclone.npc.interval.seconds");
        System.clearProperty("xclone.admin.username");
    }

    @Test
    void registersAndLogsInThroughHttp() throws Exception {
        serverConnection connection = new serverConnection();
        connection.connect();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = "user_" + suffix;
        String email = username + "@example.test";
        String password = "correct-horse";

        JsonObject registration = new JsonObject();
        registration.addProperty("displayName", "HTTP User");
        registration.addProperty("username", username);
        registration.addProperty("email", email);
        registration.addProperty("password", password);
        Response registered = connection.sendMessage(request(RequestType.REGISTER, registration));

        assertEquals(StatusCode.OK, registered.getStatus());
        assertNotNull(registered.getPayload().getAsJsonObject().get("session"));

        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", email);
        credentials.addProperty("password", password);
        Response loggedIn = connection.sendMessage(request(RequestType.LOGIN, credentials));

        assertEquals(StatusCode.OK, loggedIn.getStatus());
        assertEquals(username, loggedIn.getPayload().getAsJsonObject()
                .getAsJsonObject("user").get("username").getAsString());
    }

    @Test
    void sharesPostsFollowsInteractionsAndNotificationsBetweenUsers() throws Exception {
        serverConnection connection = new serverConnection();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonObject alice = register(connection, "Alice " + suffix, "alice_" + suffix);
        JsonObject bob = register(connection, "Bob " + suffix, "bob_" + suffix);
        String aliceToken = alice.getAsJsonObject("session").get("token").getAsString();
        String bobToken = bob.getAsJsonObject("session").get("token").getAsString();
        String aliceUsername = alice.getAsJsonObject("user").get("username").getAsString();

        JsonObject create = authenticated(aliceToken);
        create.addProperty("content", "A shared post from Alice");
        SharedSocialState afterCreate = state(connection.sendMessage(
                request(RequestType.CREATE_POST, create)));
        assertEquals(0, afterCreate.profiles().stream()
                .filter(profile -> profile.username().startsWith("xclone_"))
                .count());
        assertTrue(afterCreate.posts().stream()
                .noneMatch(post -> post.authorUsername().startsWith("xclone_")));

        long postId = afterCreate.posts().stream()
                .filter(post -> post.content().equals("A shared post from Alice"))
                .findFirst().orElseThrow().id();

        JsonObject search = authenticated(bobToken);
        search.addProperty("query", "shared post from alice");
        search.addProperty("tab", "top");
        search.addProperty("limit", 10);
        SharedSocialState searchResults = state(connection.sendMessage(
                request(RequestType.SEARCH_SOCIAL, search)));
        assertTrue(searchResults.posts().stream()
                .anyMatch(post -> post.id() == postId));

        JsonObject firstPage = authenticated(bobToken);
        firstPage.addProperty("limit", 1);
        SharedSocialState page = state(connection.sendMessage(
                request(RequestType.GET_FEED, firstPage)));
        assertEquals(1, page.posts().size());

        JsonObject like = authenticated(bobToken);
        like.addProperty("postId", postId);
        connection.sendMessage(request(RequestType.TOGGLE_LIKE, like));

        JsonObject follow = authenticated(bobToken);
        follow.addProperty("username", aliceUsername);
        connection.sendMessage(request(RequestType.TOGGLE_FOLLOW, follow));

        JsonObject reply = authenticated(bobToken);
        reply.addProperty("postId", postId);
        reply.addProperty("content", "Bob can see and reply");
        connection.sendMessage(request(RequestType.CREATE_REPLY, reply));

        JsonObject repost = authenticated(bobToken);
        repost.addProperty("postId", postId);
        connection.sendMessage(request(RequestType.TOGGLE_REPOST, repost));

        JsonObject bookmark = authenticated(bobToken);
        bookmark.addProperty("postId", postId);
        connection.sendMessage(request(RequestType.TOGGLE_BOOKMARK, bookmark));

        JsonObject quote = authenticated(bobToken);
        quote.addProperty("postId", postId);
        quote.addProperty("content", "Bob quotes Alice");
        SharedSocialState afterQuote = state(connection.sendMessage(
                request(RequestType.CREATE_QUOTE, quote)));
        long quoteId = afterQuote.posts().stream()
                .filter(post -> post.content().equals("Bob quotes Alice"))
                .findFirst().orElseThrow().id();

        JsonObject profile = new JsonObject();
        profile.addProperty("displayName", "Alice Updated");
        profile.addProperty("bio", "Shared profile details");
        JsonObject profileUpdate = authenticated(aliceToken);
        profileUpdate.add("profile", profile);
        connection.sendMessage(request(RequestType.UPDATE_PROFILE, profileUpdate));

        SharedSocialState aliceState = state(connection.sendMessage(
                request(RequestType.SYNC_SOCIAL, authenticated(aliceToken))));
        var sharedPost = aliceState.posts().stream()
                .filter(post -> post.id() == postId)
                .findFirst().orElseThrow();

        assertEquals(1, sharedPost.likes());
        assertEquals(1, sharedPost.replies());
        assertEquals(2, sharedPost.reposts());
        assertTrue(aliceState.follows().stream().anyMatch(item ->
                item.followerUsername().equalsIgnoreCase("bob_" + suffix)
                        && item.followedUsername().equalsIgnoreCase(aliceUsername)));
        assertTrue(aliceState.profiles().stream().anyMatch(profileItem ->
                profileItem.username().equalsIgnoreCase(aliceUsername)
                        && profileItem.displayName().equals("Alice Updated")
                        && profileItem.bio().equals("Shared profile details")));
        assertTrue(aliceState.notifications().stream().anyMatch(item -> item.type().equals("LIKE")));
        assertTrue(aliceState.notifications().stream().anyMatch(item -> item.type().equals("FOLLOW")));
        assertTrue(aliceState.notifications().stream().anyMatch(item -> item.type().equals("REPLY")));
        assertTrue(aliceState.notifications().stream().anyMatch(item -> item.type().equals("REPOST")));

        SharedSocialState bobState = state(connection.sendMessage(
                request(RequestType.SYNC_SOCIAL, authenticated(bobToken))));
        var bobView = bobState.posts().stream()
                .filter(post -> post.id() == postId)
                .findFirst().orElseThrow();
        assertTrue(bobView.likedByViewer());
        assertTrue(bobView.repostedByViewer());
        assertTrue(bobView.bookmarkedByViewer());

        SharedSocialState readState = state(connection.sendMessage(
                request(RequestType.MARK_NOTIFICATIONS_READ, authenticated(aliceToken))));
        assertTrue(readState.notifications().stream().allMatch(item -> item.read()));

        JsonObject deleteQuote = authenticated(bobToken);
        deleteQuote.addProperty("postId", quoteId);
        SharedSocialState afterDelete = state(connection.sendMessage(
                request(RequestType.DELETE_TWEET, deleteQuote)));
        assertTrue(afterDelete.posts().stream().noneMatch(post -> post.id() == quoteId));
    }

    @Test
    void rejectsSocialRequestsWithoutAValidSession() throws Exception {
        serverConnection connection = new serverConnection();
        Response response = connection.sendMessage(
                request(RequestType.SYNC_SOCIAL, authenticated("not-a-real-session")));
        assertEquals(StatusCode.UNAUTHORIZED, response.getStatus());
    }

    @Test
    void validatesRevokesAndRejectsRevokedSessions() throws Exception {
        serverConnection connection = new serverConnection();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonObject account = register(connection, "Session User", "session_" + suffix);
        String token = account.getAsJsonObject("session").get("token").getAsString();

        Response validated = connection.sendMessage(
                request(RequestType.VALIDATE_SESSION, authenticated(token)));
        assertEquals(StatusCode.OK, validated.getStatus());
        assertEquals("session_" + suffix, validated.getPayload().getAsJsonObject()
                .getAsJsonObject("user").get("username").getAsString());

        Response loggedOut = connection.sendMessage(
                request(RequestType.LOGOUT, authenticated(token)));
        assertEquals(StatusCode.OK, loggedOut.getStatus());
        Response rejected = connection.sendMessage(
                request(RequestType.VALIDATE_SESSION, authenticated(token)));
        assertEquals(StatusCode.UNAUTHORIZED, rejected.getStatus());
    }

    @Test
    void storesUploadedMediaOnTheBackendAndServesItToOtherClients() throws Exception {
        serverConnection connection = new serverConnection();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonObject account = register(connection, "Media User", "media_" + suffix);
        String token = account.getAsJsonObject("session").get("token").getAsString();
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2n3sAAAAASUVORK5CYII=");

        JsonObject upload = authenticated(token);
        upload.addProperty("mimeType", "image/png");
        upload.addProperty("originalName", "pixel.png");
        upload.addProperty("data", Base64.getEncoder().encodeToString(png));
        Response uploaded = connection.sendMessage(request(RequestType.UPLOAD_MEDIA, upload));
        assertEquals(StatusCode.OK, uploaded.getStatus(), uploaded.getMessage());
        String mediaUri = uploaded.getPayload().getAsJsonObject().get("mediaUri").getAsString();
        assertTrue(mediaUri.startsWith("xclone-media:"));

        JsonObject create = authenticated(token);
        create.addProperty("content", "Persistent media");
        create.addProperty("mediaUri", mediaUri);
        SharedSocialState state = state(connection.sendMessage(
                request(RequestType.CREATE_POST, create)));
        assertTrue(state.posts().stream().anyMatch(post -> mediaUri.equals(post.mediaUri())));

        long mediaId = Long.parseLong(mediaUri.substring("xclone-media:".length()));
        URI uri = URI.create(System.getProperty("xclone.server.url") + "/api/media/" + mediaId);
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(java.util.Arrays.equals(png, response.body()));
    }

    @Test
    void onlyDatabaseAdminsCanEnableSharedFakeContent() throws Exception {
        serverConnection connection = new serverConnection();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonObject user = register(connection, "Normal User", "normal_" + suffix);
        JsonObject admin = register(connection, "Administrator", "admin_integration");
        String userToken = user.getAsJsonObject("session").get("token").getAsString();
        String adminToken = admin.getAsJsonObject("session").get("token").getAsString();
        assertEquals("admin", admin.getAsJsonObject("user").get("role").getAsString());

        JsonObject deniedUpdate = authenticated(userToken);
        deniedUpdate.addProperty("fakeContentEnabled", true);
        Response denied = connection.sendMessage(
                request(RequestType.UPDATE_SETTINGS, deniedUpdate));
        assertEquals(StatusCode.UNAUTHORIZED, denied.getStatus());

        JsonObject enable = authenticated(adminToken);
        enable.addProperty("fakeContentEnabled", true);
        Response enabled = connection.sendMessage(request(RequestType.UPDATE_SETTINGS, enable));
        assertEquals(StatusCode.OK, enabled.getStatus(), enabled.getMessage());
        assertTrue(enabled.getPayload().getAsJsonObject().get("fakeContentEnabled").getAsBoolean());
        SharedSocialState visible = state(connection.sendMessage(
                request(RequestType.SYNC_SOCIAL, authenticated(userToken))));
        assertEquals(10, visible.profiles().stream()
                .filter(profile -> profile.username().startsWith("xclone_"))
                .count());

        JsonObject disable = authenticated(adminToken);
        disable.addProperty("fakeContentEnabled", false);
        Response disabled = connection.sendMessage(request(RequestType.UPDATE_SETTINGS, disable));
        assertEquals(StatusCode.OK, disabled.getStatus());
        assertFalse(disabled.getPayload().getAsJsonObject()
                .get("fakeContentEnabled").getAsBoolean());
    }

    private JsonObject register(serverConnection connection, String displayName, String username)
            throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("displayName", displayName);
        payload.addProperty("username", username);
        payload.addProperty("email", username + "@example.test");
        payload.addProperty("password", "correct-horse");
        Response response = connection.sendMessage(request(RequestType.REGISTER, payload));
        assertEquals(StatusCode.OK, response.getStatus());
        return response.getPayload().getAsJsonObject();
    }

    private static JsonObject authenticated(String token) {
        JsonObject payload = new JsonObject();
        payload.addProperty("token", token);
        return payload;
    }

    private static SharedSocialState state(Response response) {
        assertEquals(StatusCode.OK, response.getStatus(), response.getMessage());
        return new Gson().fromJson(response.getPayload(), SharedSocialState.class);
    }

    private static Request request(RequestType type, JsonObject payload) {
        return new Request(UUID.randomUUID().toString(), type, payload);
    }

    private static Set<Long> npcPostIds(SharedSocialState state) {
        return state.posts().stream()
                .filter(post -> post.authorUsername().startsWith("xclone_"))
                .map(post -> post.id())
                .collect(Collectors.toSet());
    }

    private static long npcActivityScore(SharedSocialState state) {
        long posts = state.posts().stream()
                .filter(post -> post.authorUsername().startsWith("xclone_"))
                .count();
        long engagement = state.posts().stream()
                .filter(post -> post.authorUsername().startsWith("xclone_"))
                .mapToLong(post -> post.likes() + post.replies() + post.reposts() + post.views())
                .sum();
        long follows = state.follows().stream()
                .filter(follow -> follow.followerUsername().startsWith("xclone_"))
                .count();
        return posts * 1_000_000L + follows * 10_000L + engagement;
    }
}
