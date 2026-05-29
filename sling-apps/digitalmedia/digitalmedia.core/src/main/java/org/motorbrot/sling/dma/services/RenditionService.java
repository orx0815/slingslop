package org.motorbrot.sling.dma.services;

import org.motorbrot.sling.dma.models.MediaFormat;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Service for generating image renditions at different sizes.
 */
@Component(service = RenditionService.class, immediate = true)
public class RenditionService {

    private static final Logger LOG = LoggerFactory.getLogger(RenditionService.class);

    @Reference
    private MediaFormatService mediaFormatService;

    /**
     * Generates a rendition for the given image data and format.
     *
     * @param imageData the original image input stream
     * @param format the target media format
     * @return the rendition as a byte array, or null if generation failed
     */
    public byte[] generateRendition(InputStream imageData, MediaFormat format) {
        try {
            BufferedImage originalImage = ImageIO.read(imageData);
            if (originalImage == null) {
                LOG.warn("Could not read image data for rendition generation");
                return null;
            }

            BufferedImage resizedImage = resizeImage(originalImage, format);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);

            byte[] renditionData = baos.toByteArray();
            LOG.info("Generated rendition: {} ({}x{}) - {} bytes",
                    format.getName(), resizedImage.getWidth(), resizedImage.getHeight(), renditionData.length);

            return renditionData;

        } catch (Exception e) {
            LOG.error("Failed to generate rendition for format: " + format.getName(), e);
            return null;
        }
    }

    /**
     * Resizes an image according to the media format specifications.
     */
    private BufferedImage resizeImage(BufferedImage original, MediaFormat format) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        int targetWidth = format.getWidth();
        int targetHeight = format.getHeight();

        if (format.isMaintainAspectRatio()) {
            // Calculate dimensions maintaining aspect ratio
            double aspectRatio = (double) originalWidth / originalHeight;

            if (originalWidth > originalHeight) {
                targetWidth = format.getMaxDimension();
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetHeight = format.getMaxDimension();
                targetWidth = (int) (targetHeight * aspectRatio);
            }

            // Ensure we don't exceed max dimensions
            if (targetWidth > format.getWidth()) {
                targetWidth = format.getWidth();
                targetHeight = (int) (targetWidth / aspectRatio);
            }
            if (targetHeight > format.getHeight()) {
                targetHeight = format.getHeight();
                targetWidth = (int) (targetHeight * aspectRatio);
            }
        }

        // Don't upscale images
        if (targetWidth > originalWidth || targetHeight > originalHeight) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        // Create resized image
        Image scaledImage = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Generates a preview rendition using the default preview format.
     */
    public byte[] generatePreviewRendition(InputStream imageData) {
        return generateRendition(imageData, mediaFormatService.getPreviewFormat());
    }
}
