package org.motorbrot.sling.dma.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Aggregates every {@link MediaFormat} registered in the OSGi service registry.
 *
 * <p>Consuming sling-apps don't need to call this directly: they just publish their
 * own formats as services. The DML dashboard and the rendition servlet use this
 * registry to discover what's available.
 */
@ProviderType
public interface MediaFormatRegistry {

    /** All currently-registered formats, ordered by provider then name. */
    Collection<MediaFormat> getAll();

    /** Lookup by {@link MediaFormat#getName()}. */
    Optional<MediaFormat> getByName(String name);

    /**
     * Formats grouped by the human-friendly provider label (typically the bundle's
     * {@code Bundle-Name} header, fallback symbolic name). Stable iteration order.
     */
    Map<String, List<MediaFormat>> getByProvider();
}
