package org.motorbrot.slingmatrix.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import jakarta.servlet.Servlet;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache-busting proxy servlet for the site's static client libraries (JS / CSS).
 *
 * <p>
 * The problem this solves: a reverse-proxy disk cache (Apache {@code mod_cache})
 * in front of Sling caches {@code /apps/sling-matrix/js/public/public-bundle.min.js}
 * for a long time. After a deployment the on-disk bundle changes but the cached
 * copy stays stale. Appending a {@code ?v=123} query parameter is undesirable
 * because query strings should not be part of the cache key.
 * </p>
 *
 * <p>
 * Instead the cache key is encoded into the URL <em>path</em>, Composum-style. The
 * actual resource path travels in the request <em>suffix</em>:
 * </p>
 *
 * <pre>
 * /bin/public/slingslop/clientlib.js/{CACHEKEY}/apps/sling-matrix/js/public/public-bundle.min.js
 *                                    \________/ \_________________________________________________/
 *                                     cacheKey                    resource path (suffix)
 * </pre>
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>resource cannot be resolved / not an allowed clientlib &rarr; {@code 404}</li>
 *   <li>{@code CACHEKEY} does not match the resource's current key &rarr; {@code 301}
 *       redirect to the current versioned URL (short-lived, never long-cached)</li>
 *   <li>{@code CACHEKEY} matches &rarr; {@code 200} with the correct MIME type and an
 *       immutable one-year {@code Cache-Control} header</li>
 * </ul>
 *
 * <p>
 * Because every content change produces a new key, the served {@code 200}
 * responses are safely immutable and the disk cache never goes stale: a new
 * bundle is simply requested under a new path.
 * </p>
 *
 * @see org.motorbrot.slingmatrix.slingmodels.ClientlibUrl the HTL model that renders these URLs
 */
@Component(
    service = Servlet.class,
    property = {
        "service.description=Slingslop cache-busting clientlib servlet",
        "sling.servlet.methods=GET",
        // Mounted WITHOUT an extension so Sling parses the request extension
        // (.js / .css) and routes everything after it as the request suffix,
        // i.e. /bin/public/slingslop/clientlib.js/{key}/{resourcePath}.
        "sling.servlet.paths=" + ClientlibServlet.SERVLET_PATH
    })
public class ClientlibServlet extends SlingJakartaSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    static final String SERVLET_PATH = "/bin/public/slingslop/clientlib";

    /** Only resources below this root and ending in .js/.css may ever be served. */
    private static final String ALLOWED_ROOT = "/apps/sling-matrix/";

    private static final Logger LOG = LoggerFactory.getLogger(ClientlibServlet.class);

    @Override
    protected void doGet(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws IOException {

        String suffix = request.getRequestPathInfo().getSuffix();
        if (suffix == null || suffix.length() < 2 || suffix.charAt(0) != '/') {
            response.sendError(SlingJakartaHttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // suffix = /{cacheKey}{resourcePath}, e.g. /abc123/apps/sling-matrix/js/public/public-bundle.min.js
        String rest = suffix.substring(1);
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            response.sendError(SlingJakartaHttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String requestedKey = rest.substring(0, slash);
        String resourcePath = rest.substring(slash); // starts with '/'

        // Access control: never let this servlet stream arbitrary repository content.
        if (!isAllowed(resourcePath)) {
            LOG.debug("Rejected clientlib request for disallowed path {}", resourcePath);
            response.sendError(SlingJakartaHttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Resource resource = request.getResourceResolver().getResource(resourcePath);
        if (resource == null) {
            response.sendError(SlingJakartaHttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String currentKey = cacheKey(resource);
        if (!currentKey.equals(requestedKey)) {
            // Stale key: permanently redirect to the current versioned URL. Keep the
            // redirect itself short-lived so it re-resolves after the next deployment.
            response.setHeader("Location", buildUrl(resource));
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(SlingJakartaHttpServletResponse.SC_MOVED_PERMANENTLY);
            return;
        }

        try (InputStream in = resource.adaptTo(InputStream.class)) {
            if (in == null) {
                response.sendError(SlingJakartaHttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType(contentType(resourcePath));
            response.setCharacterEncoding("UTF-8");
            // Immutable: the key changes whenever the content changes.
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
            in.transferTo(response.getOutputStream());
        }
    }

    /** Only {@code /apps/sling-matrix/**} JS/CSS files, with no path traversal, are serveable. */
    private static boolean isAllowed(String resourcePath) {
        if (!resourcePath.startsWith(ALLOWED_ROOT) || resourcePath.contains("..")) {
            return false;
        }
        String lower = resourcePath.toLowerCase();
        return lower.endsWith(".js") || lower.endsWith(".css");
    }

    private static String contentType(String resourcePath) {
        return resourcePath.toLowerCase().endsWith(".css") ? "text/css" : "text/javascript";
    }

    /**
     * Builds the versioned proxy URL for a resolved clientlib resource. Shared with
     * the HTL {@code ClientlibUrl} model so URL generation and validation stay in sync.
     *
     * @param resource a resolved JS or CSS clientlib resource
     * @return e.g. {@code /bin/public/slingslop/clientlib.js/{key}/apps/sling-matrix/js/public/public-bundle.min.js}
     */
    public static String buildUrl(Resource resource) {
        String path = resource.getPath();
        String ext = path.toLowerCase().endsWith(".css") ? "css" : "js";
        return SERVLET_PATH + "." + ext + "/" + cacheKey(resource) + path;
    }

    /**
     * Derives a short, stable cache key for a clientlib resource from its last-modified
     * timestamp (falling back to content length), encoded in base-36. The key changes
     * whenever the file is redeployed, which is exactly what busts the disk cache.
     *
     * @param resource a resolved clientlib resource
     * @return a URL-safe cache key
     */
    public static String cacheKey(Resource resource) {
        // nt:file stores metadata on its jcr:content child; fall back to the node itself.
        Resource content = resource.getChild("jcr:content");
        ValueMap vm = (content != null ? content : resource).getValueMap();

        Calendar lastModified = vm.get("jcr:lastModified", Calendar.class);
        if (lastModified != null) {
            return Long.toString(lastModified.getTimeInMillis(), 36);
        }

        long metaMod = resource.getResourceMetadata().getModificationTime();
        if (metaMod > 0) {
            return Long.toString(metaMod, 36);
        }

        long length = resource.getResourceMetadata().getContentLength();
        return "s" + Long.toString(Math.max(length, 0), 36);
    }
}
