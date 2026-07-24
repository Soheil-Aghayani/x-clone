package server.network;

import client.network.serverConnection;
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

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpBackendIntegrationTest {
    @TempDir
    static Path temporaryData;

    private static HttpServer backend;

    @BeforeAll
    static void startBackend() throws Exception {
        System.setProperty("xclone.server.data.dir", temporaryData.toString());
        backend = server.start(0);
        System.setProperty("xclone.server.url",
                "http://127.0.0.1:" + backend.getAddress().getPort());
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) backend.stop(0);
        System.clearProperty("xclone.server.url");
        System.clearProperty("xclone.server.data.dir");
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

    private static Request request(RequestType type, JsonObject payload) {
        return new Request(UUID.randomUUID().toString(), type, payload);
    }
}
