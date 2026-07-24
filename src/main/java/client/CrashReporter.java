package client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Persists otherwise invisible JavaFX failures when running the packaged application without a console. */
public final class CrashReporter {
    private CrashReporter() {}

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                Path directory = Path.of(System.getProperty("user.home"), ".x-clone");
                Files.createDirectories(directory);
                StringWriter trace = new StringWriter();
                error.printStackTrace(new PrintWriter(trace));
                String entry = System.lineSeparator() + "=== " + Instant.now() + " | "
                        + thread.getName() + " ===" + System.lineSeparator() + trace;
                Files.writeString(directory.resolve("client-error.log"), entry, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
                error.printStackTrace();
            }
        });
    }
}
