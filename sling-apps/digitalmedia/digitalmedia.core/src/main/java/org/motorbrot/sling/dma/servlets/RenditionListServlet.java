package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.models.MediaFormat;
import org.motorbrot.sling.dma.services.MediaFormatService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet for listing existing renditions of an asset.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "nt:file",
    methods = "GET",
    selectors = "renditions",
    extensions = "html"
)
public class RenditionListServlet extends SlingJakartaSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RenditionListServlet.class);

    @Reference
    private MediaFormatService mediaFormatService;

    @Override
    protected void doGet(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Resource resource = request.getResource();
            Node assetNode = resource.adaptTo(Node.class);

            if (assetNode == null) {
                out.write("<div class=\"dml-rendition-item\">Asset not found</div>");
                return;
            }

            // Check if renditions folder exists
            if (!assetNode.hasNode("renditions")) {
                out.write("<div class=\"dml-rendition-item\">No renditions generated yet</div>");
                return;
            }

            Node renditionsNode = assetNode.getNode("renditions");
            List<String> renditionNames = new ArrayList<>();

            renditionsNode.getNodes().forEachRemaining(obj -> {
                Node renditionNode = (Node) obj;
                try {
                    renditionNames.add(renditionNode.getName());
                } catch (Exception e) {
                    LOG.warn("Could not read rendition node", e);
                }
            });

            if (renditionNames.isEmpty()) {
                out.write("<div class=\"dml-rendition-item\">No renditions generated yet</div>");
                return;
            }

            // Generate HTML for each rendition
            for (String renditionName : renditionNames) {
                MediaFormat format = mediaFormatService.getFormat(renditionName).orElse(null);

                String dimensions = "";
                if (format != null) {
                    dimensions = String.format("(%dx%d)", format.getWidth(), format.getHeight());
                } else {
                    dimensions = "(custom)";
                }

                String renditionPath = assetNode.getPath() + "/renditions/" + renditionName;

                out.write(String.format(
                    "<div class=\"dml-rendition-item\">" +
                    "  <div class=\"dml-rendition-info\">" +
                    "    <span class=\"dml-rendition-name\">%s</span>" +
                    "    <span class=\"dml-rendition-dimensions\">%s</span>" +
                    "  </div>" +
                    "  <a href=\"%s\" download class=\"dml-rendition-download\">Download</a>" +
                    "</div>",
                    renditionName, dimensions, renditionPath
                ));
            }

        } catch (Exception e) {
            LOG.error("Error listing renditions", e);
            out.write("<div class=\"dml-rendition-item\">Error loading renditions</div>");
        }
    }
}
