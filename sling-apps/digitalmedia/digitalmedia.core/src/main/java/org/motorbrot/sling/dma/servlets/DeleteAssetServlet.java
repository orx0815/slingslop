package org.motorbrot.sling.dma.servlets;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/components/asset",
    methods = "POST",
    selectors = "delete",
    extensions = "html"
)
public class DeleteAssetServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteAssetServlet.class);

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        Resource resource = request.getResource();
        String assetPath = resource.getPath();

        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null) {
                response.setStatus(500);
                response.getWriter().write("Could not get session");
                return;
            }

            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode == null) {
                response.setStatus(404);
                response.getWriter().write("Asset not found");
                return;
            }

            assetNode.remove();
            session.save();

            LOG.info("Deleted asset: {}", assetPath);

            // Tell HTMX to redirect to home page (asset panel will clear itself)
            response.setStatus(200);
            response.setHeader("HX-Redirect", "/content/motorbrot/dma/home.html");

        } catch (Exception e) {
            LOG.error("Error deleting asset: {}", assetPath, e);
            response.setStatus(500);
            response.getWriter().write("Error deleting asset: " + e.getMessage());
        }
    }
}
