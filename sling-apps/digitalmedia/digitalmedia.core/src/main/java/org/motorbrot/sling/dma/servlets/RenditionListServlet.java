package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import jakarta.servlet.RequestDispatcher;
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
    resourceTypes = "motorbrot/dma/components/asset",
    methods = "GET",
    selectors = "renditions",
    extensions = "html"
)
public class RenditionListServlet extends SlingJakartaSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(RenditionListServlet.class);

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

            // Render each rendition via shared HTL script
            for (String renditionName : renditionNames) {
                request.setAttribute("renditionFormat", renditionName);
                RequestDispatcher dispatcher = request.getRequestDispatcher(
                    resource.getPath() + ".rendition-item.html");
                if (dispatcher != null) {
                    dispatcher.include(request, response);
                }
            }
            request.removeAttribute("renditionFormat");

        } catch (Exception e) {
            LOG.error("Error listing renditions", e);
            out.write("<div class=\"dml-rendition-item\">Error loading renditions</div>");
        }
    }
}
