package org.motorbrot.ratml.link.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.uri.SlingUriBuilder;
import org.motorbrot.ratml.link.LinkBuilder;
import org.osgi.service.component.annotations.Component;

/**
 * Default {@link LinkBuilder} that externalizes resource paths via the resource
 * resolver's reverse mapping and {@link SlingUriBuilder}.
 *
 * @see <a href="https://sling.apache.org/documentation/the-sling-engine/mappings-for-resource-resolution.html#reverse-outgoing-mapping">Reverse (outgoing) mapping</a>
 */
@Component(service = LinkBuilder.class)
public class MappedLinkBuilder implements LinkBuilder {

    @Override
    public String toUrl(Resource resource) {
        return toUrl(resource, null);
    }

    @Override
    public String toUrl(Resource resource, String suffix) {
        if (resource == null) {
            return null;
        }
        ResourceResolver resolver = resource.getResourceResolver();

        // Map the resource path to an absolute, shortened URL (host and port
        // included where /etc/map is configured).
        String externalUrl = resolver.map(resource.getPath());

        SlingUriBuilder builder = SlingUriBuilder.parse(externalUrl, resolver)
                .setExtension("html");

        if (suffix != null && !suffix.isBlank()) {
            builder.setSuffix(suffix.startsWith("/") ? suffix : "/" + suffix);
        }
        return builder.build().toString();
    }
}
