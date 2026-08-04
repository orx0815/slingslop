package org.motorbrot.slingmatrix.slingmodels;

import javax.inject.Inject;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.motorbrot.slingmatrix.servlets.ClientlibServlet;

/**
 * Renders a cache-busting client library URL for use in HTL templates.
 *
 * <p>
 * Given the repository path of a JS or CSS clientlib, it produces the versioned
 * proxy URL served by {@link ClientlibServlet}, embedding a content-derived cache
 * key in the URL path (not a query parameter) so the reverse-proxy disk cache
 * never serves a stale bundle after a deployment.
 * </p>
 *
 * <p>Usage in HTL (the {@code path} parameter is the real clientlib resource path):</p>
 *
 * <pre>
 * {@code
 * <sly data-sly-use.pubJs="${'org.motorbrot.slingmatrix.slingmodels.ClientlibUrl'
 *          @ path='/apps/sling-matrix/js/public/public-bundle.min.js'}">
 *   <script src="${pubJs.href}"></script>
 * </sly>
 * }
 * </pre>
 *
 * <p>
 * If the resource cannot be resolved (e.g. during local development before the
 * bundle has been built) the raw {@code path} is returned unchanged so the page
 * still renders.
 * </p>
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class ClientlibUrl {

    private final String href;

    /**
     * Creates the model from the current request and the requested clientlib path.
     *
     * @param request the current Sling request
     * @param path    the repository path of the JS/CSS clientlib to version
     */
    @Inject
    public ClientlibUrl(@Self SlingJakartaHttpServletRequest request, @RequestAttribute(name = "path") String path) {
        Resource resource = request.getResourceResolver().getResource(path);
        this.href = resource != null ? ClientlibServlet.buildUrl(resource) : path;
    }

    /**
     * @return the versioned proxy URL, or the raw path if the resource is unavailable
     */
    public String getHref() {
        return href;
    }
}
