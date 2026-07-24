package client.network;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves the backend URL without baking a deployment address into the app. */
public final class ServerEndpoint {
    public static final String PROPERTY = "xclone.server.url";
    public static final String ENVIRONMENT = "XCLONE_SERVER_URL";
    private static final String DEFAULT_URL = "http://127.0.0.1:8080";

    private ServerEndpoint() {}

    public static URI baseUri() {
        String configured = System.getProperty(PROPERTY);
        if (configured == null || configured.isBlank()) configured = System.getenv(ENVIRONMENT);
        if (configured == null || configured.isBlank()) configured = configuredFileValue();
        if (configured == null || configured.isBlank()) configured = DEFAULT_URL;
        configured = configured.trim();
        while (configured.endsWith("/")) configured = configured.substring(0, configured.length() - 1);

        URI uri = URI.create(configured);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Backend URL must begin with http:// or https://");
        }
        if (uri.getHost() == null) throw new IllegalArgumentException("Backend URL must include a host");
        return uri;
    }

    public static URI apiUri() { return URI.create(baseUri() + "/api/request"); }
    public static URI healthUri() { return URI.create(baseUri() + "/health"); }

    public static boolean isLocal() {
        String host = baseUri().getHost().toLowerCase(Locale.ROOT);
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    private static String configuredFileValue() {
        try {
            String appPath = System.getProperty("jpackage.app-path");
            Path directory = appPath == null || appPath.isBlank()
                    ? Path.of(System.getProperty("user.dir"))
                    : Path.of(appPath).toAbsolutePath().getParent();
            Path file = directory.resolve("server-url.txt");
            if (!Files.isRegularFile(file)) return null;
            return Files.readString(file).replace("\uFEFF", "").trim();
        } catch (Exception exception) {
            System.err.println("Could not read server-url.txt: " + exception.getMessage());
            return null;
        }
    }
}
