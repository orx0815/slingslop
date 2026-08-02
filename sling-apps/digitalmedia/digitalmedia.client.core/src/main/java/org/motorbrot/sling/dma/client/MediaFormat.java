package org.motorbrot.sling.dma.client;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A named target size for a rendition.
 *
 * <p>Consuming sling-apps publish their own formats as OSGi services
 * ({@code @Component(service = MediaFormat.class)}). The {@link MediaFormatRegistry}
 * collects them via the whiteboard pattern, so the DML dashboard's "Generate Renditions"
 * panel automatically picks them up.
 *
 * <p>Convenience implementation: {@link SimpleMediaFormat}.
 */
@ConsumerType
public interface MediaFormat {

    /** Unique identifier of this format (used as the rendition node name in JCR). */
    String getName();

    /** Target width in pixels. */
    int getWidth();

    /** Target height in pixels. */
    int getHeight();

    /** Human-readable label shown in the picker / dashboard. */
    default String getLabel() {
        return getName();
    }

    /** Optional longer description. */
    default String getDescription() {
        return "";
    }

    /**
     * When {@code true} the rendition is scaled inside the width×height box keeping
     * the source aspect ratio (no crop, no squish). When {@code false} the rendition
     * has exactly width×height and is cropped to the format's aspect ratio.
     */
    default boolean isMaintainAspectRatio() {
        return false;
    }

    /** Width / Height ratio of the requested format. */
    default double getAspectRatio() {
        if (getHeight() <= 0) {
            return 0d;
        }
        return (double) getWidth() / (double) getHeight();
    }
}
