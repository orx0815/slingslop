package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet for creating folders in the Digital Media Library.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "POST",
    selectors = "create-folder",
    extensions = "html"
)
public class FolderCreationServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FolderCreationServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {
            String folderName = request.getParameter("folderName");
            if (folderName == null || folderName.trim().isEmpty()) {
                response.setStatus(400);
                response.getWriter().write("Folder name is required");
                return;
            }

            // Sanitize folder name
            folderName = folderName.trim().replaceAll("[^a-zA-Z0-9-_ ]", "").replace(" ", "-");

            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null) {
                response.setStatus(500);
                response.getWriter().write("Could not get session");
                return;
            }

            // Create folder under assets
            String assetsPath = "/content/motorbrot/dma/assets";
            Node assetsFolder = session.nodeExists(assetsPath)
                    ? session.getNode(assetsPath)
                    : session.getRootNode().addNode("content/motorbrot/dma/assets", "sling:Folder");

            // Check if folder already exists
            if (assetsFolder.hasNode(folderName)) {
                response.setStatus(409);
                response.getWriter().write("Folder already exists");
                return;
            }

            // Create new folder
            Node newFolder = assetsFolder.addNode(folderName, "sling:Folder");
            newFolder.setProperty("jcr:title", folderName);
            session.save();

            String folderPath = newFolder.getPath();
            LOG.info("Created folder: {}", folderPath);

            // Return folder tree item HTML
            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().write(generateFolderTreeItem(folderPath, folderName));

        } catch (Exception e) {
            LOG.error("Error creating folder", e);
            response.setStatus(500);
            response.getWriter().write("Failed to create folder: " + e.getMessage());
        }
    }

    /**
     * Generates HTML for a folder tree item.
     */
    private String generateFolderTreeItem(String folderPath, String folderName) {
        return String.format(
            "<div class=\"dml-folder-tree-item dml-fade-in\">" +
            "  <button class=\"dml-folder-tree-button\" data-folder-path=\"%s\">" +
            "    <span class=\"dml-folder-icon\">📁</span>" +
            "    <span class=\"dml-folder-name\">%s</span>" +
            "  </button>" +
            "</div>",
            folderPath, folderName
        );
    }
}
