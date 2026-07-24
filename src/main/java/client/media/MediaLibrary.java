package client.media;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Owns media selected by the user so posts do not depend on the original file
 * remaining in Desktop, Downloads, or a removable drive.
 */
public final class MediaLibrary {
    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    private MediaLibrary() {}

    public static String importFile(File source) throws IOException {
        if (source == null || !source.isFile()) {
            throw new IOException("The selected media file is unavailable.");
        }

        String extension = extensionOf(source.getName());
        if (extension.isEmpty()) {
            throw new IOException("Choose a PNG, JPG, JPEG, or GIF file.");
        }

        Path directory = dataDirectory().resolve("media");
        Files.createDirectories(directory);
        String safeName = source.getName().replaceAll("[^\\p{L}\\p{N}._-]", "_");
        Path target = directory.resolve(UUID.randomUUID() + "-" + safeName);
        Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toUri().toString();
    }

    public static Image loadImage(String uri) {
        if (!isAvailable(uri)) return null;
        try {
            Image image = new Image(uri, false);
            return image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0 ? null : image;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isAvailable(String uri) {
        if (uri == null || uri.isBlank()) return false;
        try {
            URI parsed = URI.create(uri);
            if ("file".equalsIgnoreCase(parsed.getScheme())) {
                return Files.isRegularFile(Path.of(parsed));
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String displayName(String uri) {
        if (uri == null || uri.isBlank()) return "media";
        try {
            URI parsed = URI.create(uri);
            if ("file".equalsIgnoreCase(parsed.getScheme())) {
                Path name = Path.of(parsed).getFileName();
                return name == null ? "media" : name.toString();
            }
        } catch (RuntimeException ignored) {
            // Fall through to the compact generic label.
        }
        return "media";
    }

    public static long cacheSizeBytes() {
        Path cache = dataDirectory().resolve("media-cache");
        if (!Files.isDirectory(cache)) return 0;
        try (Stream<Path> files = Files.walk(cache)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ignored) {
                    return 0;
                }
            }).sum();
        } catch (IOException ignored) {
            return 0;
        }
    }

    public static void clearCache() throws IOException {
        Path cache = dataDirectory().resolve("media-cache");
        if (!Files.isDirectory(cache)) return;
        try (Stream<Path> paths = Files.walk(cache)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(cache)) Files.deleteIfExists(path);
            }
        }
    }

    private static Path dataDirectory() {
        String custom = System.getProperty("xclone.data.dir");
        return custom == null || custom.isBlank()
                ? Path.of(System.getProperty("user.home"), ".x-clone")
                : Path.of(custom);
    }

    private static String extensionOf(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(extension)) return extension;
        }
        return "";
    }
}
