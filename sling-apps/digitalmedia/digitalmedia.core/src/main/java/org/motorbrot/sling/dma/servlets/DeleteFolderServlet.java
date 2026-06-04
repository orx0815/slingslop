package org.motorbrot.sling.dma.servlets;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.io.IOException;

/**
 * Deletes a folder and all its direct asset children.
 * Refuses if the folder has subfolder children.
 * Accepts a {@code folderPath} POST parameter.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "POST",
    selectors = "delete-folder",
    extensions = "html"
)
public class DeleteFolderServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteFolderServlet.class);
    private static final String ASSETS_ROOT = "/content/motorbrot/dma/assets";

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        String folderPath = request.getParameter("folderPath");

        // Validate path: must be inside assets root but not the root itself
        if (folderPath == null
                || !folderPath.startsWith(ASSETS_ROOT + "/")
                || folderPath.equals(ASSETS_ROOT)) {
            response.setStatus(400);
            response.getWriter().write("Invalid folder path");
            return;
        }

        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null || !session.nodeExists(folderPath)) {
                response.setStatus(404);
                response.getWriter().write("Folder not found");
                return;
            }

            Node folder = session.getNode(folderPath);

            // Refuse deletion if the folder has subfolder children
            NodeIterator children = folder.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if ("sling:Folder".equals(child.getPrimaryNodeType().getName())) {
                    response.setStatus(409);
                    response.getWriter().write("Folder has subfolders; remove them first");
                    return;
                }
            }

            // Remove the folder node (all asset children are removed with it)
            folder.remove();
            session.save();

            // Redirect to home — triggers full page reload including sidebar refresh
            response.setHeader("HX-Redirect", "/content/motorbrot/dma/home.html");
            response.setStatus(200);

        } catch (Exception e) {
            LOG.error("Error deleting folder: {}", folderPath, e);
            response.setStatus(500);
            response.getWriter().write("Failed to delete folder: " + e.getMessage());
        }
    }
}
