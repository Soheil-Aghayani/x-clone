package server.database;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * Keeps uploaded images practical for a shared database. Animated GIFs are
 * intentionally passed through unchanged because ImageIO would only preserve
 * their first frame.
 */
final class MediaOptimizer {
    static final int MAX_DIMENSION = 1920;
    private static final float JPEG_QUALITY = 0.84f;

    private MediaOptimizer() {}

    static OptimizedMedia optimize(byte[] original, String mimeType) {
        if (original == null || original.length == 0 || "image/gif".equals(mimeType)) {
            return new OptimizedMedia(mimeType, original);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) return new OptimizedMedia(mimeType, original);

            double scale = Math.min(1.0,
                    Math.min((double) MAX_DIMENSION / source.getWidth(),
                            (double) MAX_DIMENSION / source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            boolean jpeg = "image/jpeg".equals(mimeType);
            BufferedImage rendered = new BufferedImage(
                    width,
                    height,
                    jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = rendered.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }

            byte[] optimized = jpeg ? writeJpeg(rendered) : writePng(rendered);
            if (optimized.length == 0) return new OptimizedMedia(mimeType, original);
            if (scale == 1.0 && optimized.length >= original.length) {
                return new OptimizedMedia(mimeType, original);
            }
            return new OptimizedMedia(mimeType, optimized);
        } catch (Exception ignored) {
            return new OptimizedMedia(mimeType, original);
        }
    }

    private static byte[] writePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static byte[] writeJpeg(BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) return new byte[0];
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    record OptimizedMedia(String mimeType, byte[] bytes) {}
}
