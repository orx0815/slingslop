package org.motorbrot.sling.dma.servlets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.motorbrot.sling.dma.client.crop.SimpleFocusPoint;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.services.RenditionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates <em>component-local, focus-cropped</em> renditions.
 *
 * <p>Registered against the {@code sling/servlet/default} resource type so it
 * applies to any consuming component (e.g. a {@code feature-image} node),
 * regardless of its own resource type.
 *
 * <p>Endpoint: {@code POST {componentPath}.crop-rendition.html} with form
 * parameters:
 * <ul>
 *   <li>{@code format} — one or more {@link MediaFormat} names (multi-value)</li>
 *   <li>{@code focusX}, {@code focusY} — focus point in percent (0..100)</li>
 * </ul>
 *
 * <p>Unlike {@link RenditionGenerationServlet} (which stores renditions on the
 * shared DML asset), this servlet reads the source image from the component's
 * {@code fileReference} asset but writes each cropped rendition under the
 * component itself at {@code {componentPath}/renditions/{formatName}_cropped}
 * (an {@code nt:file}). The focus point is likewise persisted on the component
 * node ({@code dmaFocusX} / {@code dmaFocusY}) — not on the DML asset — so the
 * crop is specific to this placement.
 *
 * <p>This is the path used when {@code RenditionValidator} reports a format as
 * invalid (source too small) for the shared asset, yet an editor still wants a
 * best-effort focus-cropped rendition for this component.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "sling/servlet/default",
    methods = "POST",
    selectors = "crop-rendition",
    extensions = "html"
)
public class CropRenditionServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CropRenditionServlet.class);
    private static final String CROPPED_SUFFIX = "_cropped";

    @Reference
    private RenditionService renditionService;

    @Reference
    private MediaFormatRegistry mediaFormatRegistry;

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        String[] formatNames = request.getParameterValues("format");
        if (formatNames == null || formatNames.length == 0) {
            response.setStatus(400);
            response.getWriter().write("At least one 'format' parameter is required");
            return;
        }

        Resource component = request.getResource();
        // The editor may crop a freshly-picked image that hasn't been saved onto
        // the component yet, so an explicit 'fileReference' param wins over the
        // stored property.
        String fileReference = request.getParameter("fileReference");
        if (fileReference == null || fileReference.isEmpty()) {
            fileReference = component.getValueMap().get("fileReference", String.class);
        }
        if (fileReference == null || fileReference.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("Component has no fileReference");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        Resource asset = resolver.getResource(fileReference);
        if (asset == null) {
            response.setStatus(404);
            response.getWriter().write("Referenced asset not found: " + fileReference);
            return;
        }

        FocusPoint focus = resolveFocusPoint(request);

        try {
            Node assetNode = asset.adaptTo(Node.class);
            if (assetNode == null || !assetNode.hasNode("jcr:content")) {
                response.setStatus(404);
                response.getWriter().write("Referenced asset has no content");
                return;
            }

            Node componentNode = component.adaptTo(Node.class);
            if (componentNode == null) {
                response.setStatus(500);
                response.getWriter().write("Component is not writable");
                return;
            }

            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                response.setStatus(500);
                response.getWriter().write("Could not get session");
                return;
            }

            // Persist the focus point on the component node itself.
            componentNode.setProperty("dmaFocusX", focus.getXPercent());
            componentNode.setProperty("dmaFocusY", focus.getYPercent());

            int generated = 0;
            for (String formatName : formatNames) {
                if (formatName == null || formatName.isEmpty()) {
                    continue;
                }
                Optional<MediaFormat> formatOpt = mediaFormatRegistry.getByName(formatName);
                if (!formatOpt.isPresent()) {
                    LOG.warn("crop-rendition: unknown format {}", formatName);
                    continue;
                }
                // Fresh stream per format — the source binary is consumed each time.
                InputStream originalData = assetNode.getNode("jcr:content")
                        .getProperty("jcr:data").getBinary().getStream();
                byte[] renditionData = renditionService.generateCroppedRendition(
                        originalData, formatOpt.get(), focus);
                if (renditionData == null) {
                    LOG.warn("crop-rendition: generation failed for {}", formatName);
                    continue;
                }
                storeRendition(session, componentNode, formatName + CROPPED_SUFFIX, renditionData);
                generated++;
            }

            session.save();
            LOG.info("Generated {} cropped rendition(s) under {} focus=({}%, {}%)",
                    generated, componentNode.getPath(), focus.getXPercent(), focus.getYPercent());

            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().write(
                    "<div class=\"dma-crop-result\" data-count=\"" + generated + "\">"
                            + generated + " cropped rendition(s) generated</div>");

        } catch (Exception e) {
            LOG.error("Error generating cropped rendition(s)", e);
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    private static void storeRendition(Session session, Node componentNode, String nodeName, byte[] data)
            throws Exception {
        Node renditionsNode = componentNode.hasNode("renditions")
                ? componentNode.getNode("renditions")
                : componentNode.addNode("renditions", "nt:unstructured");

        Node renditionNode = renditionsNode.hasNode(nodeName)
                ? renditionsNode.getNode(nodeName)
                : renditionsNode.addNode(nodeName, "nt:file");

        Node content = renditionNode.hasNode("jcr:content")
                ? renditionNode.getNode("jcr:content")
                : renditionNode.addNode("jcr:content", "nt:resource");

        content.setProperty("jcr:data", session.getValueFactory().createBinary(
                new ByteArrayInputStream(data)));
        content.setProperty("jcr:mimeType", "image/jpeg");
        content.setProperty("jcr:lastModified", java.util.Calendar.getInstance());
    }

    private static FocusPoint resolveFocusPoint(SlingJakartaHttpServletRequest request) {
        Double fx = parseDouble(request.getParameter("focusX"));
        Double fy = parseDouble(request.getParameter("focusY"));
        if (fx == null || fy == null) {
            return FocusPoint.CENTER;
        }
        return new SimpleFocusPoint(fx, fy);
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
