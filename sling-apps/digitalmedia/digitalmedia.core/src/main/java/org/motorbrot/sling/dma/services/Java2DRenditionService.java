package org.motorbrot.sling.dma.services;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.RenditionValidator;
import org.motorbrot.sling.dma.client.crop.CropBox;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Pure-Java ({@code java.awt} + {@link ImageIO}) implementation of
 * {@link RenditionService}. This is the always-available baseline; it is used
 * (via delegation from {@link FfmRenditionService}) whenever the native path is
 * unavailable or disabled.
 *
 * <p>Deliberately published <strong>only</strong> under its own concrete type,
 * not under {@link RenditionService}. {@link FfmRenditionService} is the single
 * {@code RenditionService} in the container and delegates here as needed. This
 * avoids a two-service ranking race where a reluctant consumer reference could
 * pin the baseline before the native service registers.
 */
@Component(service = Java2DRenditionService.class, immediate = true)
public class Java2DRenditionService implements RenditionService {

    private static final Logger LOG = LoggerFactory.getLogger(Java2DRenditionService.class);

    @Reference
    private MediaFormatRegistry mediaFormatRegistry;

    @Override
    public byte[] generateRendition(InputStream imageData, MediaFormat format) {
        return generateRendition(imageData, format, null);
    }

    @Override
    public byte[] generateRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint) {
        try {
            BufferedImage originalImage = ImageIO.read(imageData);
            if (originalImage == null) {
                LOG.warn("Could not read image data for rendition generation");
                return null;
            }

            RenditionValidator.Result vr = RenditionValidator.validate(
                    originalImage.getWidth(), originalImage.getHeight(), format);
            if (vr.isTooSmall()) {
                LOG.warn("Refusing to generate {}: {}", format.getName(), vr.getMessage());
                return null;
            }

            BufferedImage prepared = originalImage;
            if (!format.isMaintainAspectRatio()) {
                CropBox crop = RenditionValidator.cropFor(
                        originalImage.getWidth(), originalImage.getHeight(), format,
                        focusPoint == null ? FocusPoint.CENTER : focusPoint);
                if (crop != null) {
                    prepared = originalImage.getSubimage(
                            crop.getX(), crop.getY(), crop.getWidth(), crop.getHeight());
                }
            }

            BufferedImage resizedImage = resizeImage(prepared, format);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);

            byte[] renditionData = baos.toByteArray();
            LOG.info("Generated rendition: {} ({}x{}) - {} bytes focus={}",
                    format.getName(), resizedImage.getWidth(), resizedImage.getHeight(),
                    renditionData.length, focusPoint);

            return renditionData;

        } catch (Exception e) {
            LOG.error("Failed to generate rendition for format: " + format.getName(), e);
            return null;
        }
    }

    @Override
    public byte[] generateCroppedRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint) {
        try {
            BufferedImage originalImage = ImageIO.read(imageData);
            if (originalImage == null) {
                LOG.warn("Could not read image data for cropped rendition generation");
                return null;
            }

            BufferedImage prepared = originalImage;
            if (!format.isMaintainAspectRatio() && format.getAspectRatio() > 0d) {
                CropBox crop = CropBox.fromFocus(
                        originalImage.getWidth(), originalImage.getHeight(),
                        format.getAspectRatio(),
                        focusPoint == null ? FocusPoint.CENTER : focusPoint);
                if (crop != null) {
                    prepared = originalImage.getSubimage(
                            crop.getX(), crop.getY(), crop.getWidth(), crop.getHeight());
                }
            }

            BufferedImage resizedImage = resizeImage(prepared, format);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            byte[] renditionData = baos.toByteArray();
            LOG.info("Generated cropped rendition: {} ({}x{}) - {} bytes focus={}",
                    format.getName(), resizedImage.getWidth(), resizedImage.getHeight(),
                    renditionData.length, focusPoint);
            return renditionData;

        } catch (Exception e) {
            LOG.error("Failed to generate cropped rendition for format: " + format.getName(), e);
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
        int maxDimension = Math.max(targetWidth, targetHeight);

        if (format.isMaintainAspectRatio()) {
            // Calculate dimensions maintaining aspect ratio
            double aspectRatio = (double) originalWidth / originalHeight;

            if (originalWidth > originalHeight) {
                targetWidth = maxDimension;
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetHeight = maxDimension;
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

    @Override
    public byte[] generatePreviewRendition(InputStream imageData) {
        return generateRendition(imageData, previewFormat());
    }

    private MediaFormat previewFormat() {
        return mediaFormatRegistry.getByName("preview")
                .orElseThrow(() -> new IllegalStateException(
                        "Preview format not found \u2014 is digitalmedia.core's PreviewMediaFormat installed?"));
    }
}
