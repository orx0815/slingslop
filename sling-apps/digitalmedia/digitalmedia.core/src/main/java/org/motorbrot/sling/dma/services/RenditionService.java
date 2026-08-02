package org.motorbrot.sling.dma.services;

import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.crop.FocusPoint;

import java.io.InputStream;

/**
 * Generates image renditions at different sizes.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link Java2DRenditionService} — pure-Java baseline, always available.</li>
 *   <li>{@link FfmRenditionService} — native ImageMagick via the Java Foreign
 *       Function &amp; Memory API. Registered with a higher service ranking so it
 *       is preferred when the native library is present and the service is
 *       enabled; otherwise it transparently delegates to the Java2D baseline.</li>
 * </ul>
 *
 * <p>Consumers simply {@code @Reference RenditionService} and get the best
 * available implementation.
 */
public interface RenditionService {

    /**
     * Generates a rendition for the given image data and format.
     *
     * @param imageData the original image input stream
     * @param format the target media format
     * @return the rendition as a byte array, or {@code null} if generation failed
     */
    byte[] generateRendition(InputStream imageData, MediaFormat format);

    /**
     * Generates a rendition with optional focus-point cropping.
     *
     * <p>When {@code format} is not aspect-preserving and the source aspect ratio
     * differs from the format, the source is cropped to the format's aspect ratio
     * around {@code focusPoint} before being scaled down to {@code format.getWidth()}
     * × {@code format.getHeight()}. When the source is smaller than the format
     * generation is refused (returns {@code null}).
     */
    byte[] generateRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint);

    /**
     * Generates a component-local <em>cropped</em> rendition, best-effort.
     *
     * <p>Unlike {@link #generateRendition(InputStream, MediaFormat, FocusPoint)}
     * this never refuses when the source is too small: it crops the largest
     * rectangle of the format's aspect ratio that fits inside the source
     * (centred on {@code focusPoint}) and scales it down to the format size,
     * never upscaling.
     *
     * @return the cropped rendition JPEG bytes, or {@code null} on failure
     */
    byte[] generateCroppedRendition(InputStream imageData, MediaFormat format, FocusPoint focusPoint);

    /**
     * Generates a preview rendition using the default preview format.
     */
    byte[] generatePreviewRendition(InputStream imageData);
}
