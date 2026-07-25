package client.media;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLibraryTest {
    @TempDir Path temporaryDirectory;

    @AfterEach
    void clearDataDirectoryOverride() {
        System.clearProperty("xclone.data.dir");
    }

    @Test
    void importedMediaSurvivesRemovalOfOriginalFile() throws Exception {
        System.setProperty("xclone.data.dir", temporaryDirectory.resolve("app-data").toString());
        byte[] content = {71, 73, 70, 56, 57, 97};
        Path original = temporaryDirectory.resolve("my animation.gif");
        Files.write(original, content);

        String storedUri = MediaLibrary.importFile(original.toFile());
        Path stored = Path.of(java.net.URI.create(storedUri));

        assertNotEquals(original, stored);
        assertTrue(stored.startsWith(temporaryDirectory.resolve("app-data").resolve("media")));
        Files.delete(original);
        assertTrue(MediaLibrary.isAvailable(storedUri));
        assertArrayEquals(content, Files.readAllBytes(stored));
    }

    @Test
    void missingFileUriIsRejectedBeforeJavaFxCreatesABlankImage() {
        String missing = temporaryDirectory.resolve("gone.gif").toUri().toString();
        assertFalse(MediaLibrary.isAvailable(missing));
    }

    @Test
    void bundledNpcMediaLoadsFromThePortableApplicationClasspath() {
        String bundled = "/images/npc/media/maya-1.jpg";

        assertTrue(MediaLibrary.isAvailable(bundled));
        assertNotNull(MediaLibrary.loadImage(bundled));
    }
}
