package org.motorbrot.sling.dma.client.crop;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Normalised focus point on an image: {@code 0..100} percent of width / height
 * from the top-left corner. {@code (50, 50)} = dead centre.
 *
 * <p>Renditions that need to be cropped to a target aspect ratio keep the
 * focus point inside the crop window (and as close to the centre as the source
 * allows).
 */
@ProviderType
public interface FocusPoint {

    /** Default focus point (image centre). */
    FocusPoint CENTER = new SimpleFocusPoint(50d, 50d);

    /** Horizontal position in percent of the source width, range {@code [0, 100]}. */
    double getXPercent();

    /** Vertical position in percent of the source height, range {@code [0, 100]}. */
    double getYPercent();

    /** Convert to absolute pixel X for a source of the given width. */
    default int xPixel(int sourceWidth) {
        return clamp((int) Math.round(getXPercent() / 100d * sourceWidth), 0, Math.max(0, sourceWidth - 1));
    }

    /** Convert to absolute pixel Y for a source of the given height. */
    default int yPixel(int sourceHeight) {
        return clamp((int) Math.round(getYPercent() / 100d * sourceHeight), 0, Math.max(0, sourceHeight - 1));
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
