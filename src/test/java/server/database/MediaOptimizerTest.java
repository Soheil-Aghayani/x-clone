package server.database;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaOptimizerTest {
    @Test
    void shrinksOversizedJpegBeforeDatabaseStorage() throws Exception {
        BufferedImage source = new BufferedImage(3000, 2100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setPaint(new java.awt.GradientPaint(
                0, 0, Color.BLUE, 3000, 2100, Color.ORANGE));
        graphics.fillRect(0, 0, 3000, 2100);
        graphics.dispose();
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ImageIO.write(source, "jpeg", original);

        MediaOptimizer.OptimizedMedia result =
                MediaOptimizer.optimize(original.toByteArray(), "image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));

        assertTrue(decoded.getWidth() <= MediaOptimizer.MAX_DIMENSION);
        assertTrue(decoded.getHeight() <= MediaOptimizer.MAX_DIMENSION);
        assertTrue(result.bytes().length < original.size());
    }

    @Test
    void preservesGifBytesToKeepAnimationFrames() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 1, 2, 3};
        assertArrayEquals(gif, MediaOptimizer.optimize(gif, "image/gif").bytes());
    }
}
