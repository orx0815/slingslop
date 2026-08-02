package org.motorbrot.sling.dma.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.client.MediaFormat;
import org.motorbrot.sling.dma.client.MediaFormatRegistry;
import org.motorbrot.sling.dma.client.crop.FocusPoint;
import org.motorbrot.sling.dma.client.crop.SimpleFocusPoint;
import org.motorbrot.sling.dma.services.RenditionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Servlet for generating renditions on demand.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/components/asset",
    methods = "POST",
    selectors = "generate-rendition",
    extensions = "html"
)
public class RenditionGenerationServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RenditionGenerationServlet.class);

    @Reference
    private RenditionService renditionService;

    @Reference
    private MediaFormatRegistry mediaFormatRegistry;

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        String formatName = request.getParameter("format");
        if (formatName == null || formatName.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("Format parameter is required");
            return;
        }

        Optional<MediaFormat> formatOpt = mediaFormatRegistry.getByName(formatName);
        if (!formatOpt.isPresent()) {
            response.setStatus(404);
            response.getWriter().write("Format not found: " + formatName);
            return;
        }

        MediaFormat format = formatOpt.get();

        try {
            Resource resource = request.getResource();
            Node assetNode = resource.adaptTo(Node.class);

            if (assetNode == null || !assetNode.hasNode("jcr:content")) {
                response.setStatus(404);
                response.getWriter().write("Asset not found");
                return;
            }

            // Get original image data
            Node contentNode = assetNode.getNode("jcr:content");
            InputStream originalData = contentNode.getProperty("jcr:data").getBinary().getStream();

            // Resolve focus point: request params override asset metadata
            FocusPoint focus = resolveFocusPoint(request, assetNode);

            // Generate rendition
            byte[] renditionData = renditionService.generateRendition(originalData, format, focus);

            if (renditionData == null) {
                response.setStatus(500);
                response.getWriter().write("Failed to generate rendition");
                return;
            }

            // Store rendition
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null) {
                response.setStatus(500);
                response.getWriter().write("Could not get session");
                return;
            }

            // Create renditions folder if it doesn't exist
            Node renditionsNode;
            if (assetNode.hasNode("renditions")) {
                renditionsNode = assetNode.getNode("renditions");
            } else {
                renditionsNode = assetNode.addNode("renditions", "nt:unstructured");
            }

            // Create or replace rendition
            Node renditionNode;
            if (renditionsNode.hasNode(formatName)) {
                renditionNode = renditionsNode.getNode(formatName);
            } else {
                renditionNode = renditionsNode.addNode(formatName, "nt:file");
            }

            Node renditionContent;
            if (renditionNode.hasNode("jcr:content")) {
                renditionContent = renditionNode.getNode("jcr:content");
            } else {
                renditionContent = renditionNode.addNode("jcr:content", "nt:resource");
            }

            renditionContent.setProperty("jcr:data", session.getValueFactory().createBinary(
                    new ByteArrayInputStream(renditionData)));
            renditionContent.setProperty("jcr:mimeType", "image/jpeg");

            session.save();

            LOG.info("Generated rendition {} for asset {}", formatName, assetNode.getPath());

            response.setStatus(200);
            response.setContentType("text/html");
            request.setAttribute("renditionFormat", formatName);
            RequestDispatcher dispatcher = request.getRequestDispatcher(
                resource.getPath() + ".rendition-item.html");
            if (dispatcher != null) {
                // Use a GET wrapper so Sling resolves the HTL script (not the POST servlet)
                dispatcher.include(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getMethod() { return "GET"; }
                }, response);
            } else {
                response.getWriter().write("<div class=\"dml-rendition-item\">" + formatName + " - Generated</div>");
            }
            request.removeAttribute("renditionFormat");

        } catch (Exception e) {
            LOG.error("Error generating rendition", e);
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    private static FocusPoint resolveFocusPoint(SlingJakartaHttpServletRequest request, Node assetNode) {
        Double fx = parseDouble(request.getParameter("focusX"));
        Double fy = parseDouble(request.getParameter("focusY"));
        if (fx == null || fy == null) {
            try {
                if (assetNode != null && assetNode.hasNode("metadata")) {
                    Node m = assetNode.getNode("metadata");
                    if (fx == null && m.hasProperty("dmaFocusX")) {
                        fx = m.getProperty("dmaFocusX").getDouble();
                    }
                    if (fy == null && m.hasProperty("dmaFocusY")) {
                        fy = m.getProperty("dmaFocusY").getDouble();
                    }
                }
            } catch (Exception ignored) {
                // fall back to centre
            }
        }
        if (fx == null || fy == null) {
            return FocusPoint.CENTER;
        }
        return new SimpleFocusPoint(fx, fy);
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }
}
