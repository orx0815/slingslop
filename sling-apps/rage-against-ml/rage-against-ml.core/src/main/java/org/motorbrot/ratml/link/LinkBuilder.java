package org.motorbrot.ratml.link;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Builds externalized, resolver-mapped URLs for content resources.
 *
 * <p>
 * This is the Rage Against the Machine Learning counterpart to wcm.io's URL handling
 * (<a href="https://github.com/wcm-io/io.wcm.handler.url">io.wcm.handler.url</a>)
 * — deliberately <em>not</em> named a "handler". It is a small builder that turns
 * a JCR resource path into a browser-ready link by running it through
 * {@link org.apache.sling.api.resource.ResourceResolver#map(String) reverse
 * mapping} and {@link org.apache.sling.api.uri.SlingUriBuilder}, so links honour
 * any {@code /etc/map} configuration (shortened paths, host and scheme).
 * </p>
 */
@ProviderType
public interface LinkBuilder {

    /**
     * Builds the externalized {@code .html} URL for an addressable resource
     * (typically a page).
     *
     * @param resource the resource to link to; may be {@code null}
     * @return the mapped, externalized URL ending in {@code .html}, or {@code null}
     *         when {@code resource} is {@code null}
     */
    String toUrl(Resource resource);

    /**
     * Builds the externalized {@code .html} URL for an addressable resource,
     * appending the given suffix.
     *
     * @param resource the resource to link to; may be {@code null}
     * @param suffix   suffix path segment (a leading slash is added when missing);
     *                 may be {@code null} or blank to omit
     * @return the mapped, externalized URL, or {@code null} when {@code resource}
     *         is {@code null}
     */
    String toUrl(Resource resource, String suffix);
}
