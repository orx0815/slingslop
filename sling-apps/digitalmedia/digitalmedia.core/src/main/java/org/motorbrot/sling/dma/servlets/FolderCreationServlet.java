package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
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
public class FolderCreationServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FolderCreationServlet.class);

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
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

            // Determine parent: use the selected folder if provided and valid
            String defaultAssetsPath = "/content/motorbrot/dma/assets";
            String parentParam = request.getParameter("folder");
            String assetsPath;
            if (parentParam != null && !parentParam.trim().isEmpty()
                    && parentParam.startsWith(defaultAssetsPath)) {
                assetsPath = parentParam.trim();
            } else {
                assetsPath = defaultAssetsPath;
            }
            Node assetsFolder = session.nodeExists(assetsPath)
                    ? session.getNode(assetsPath)
                    : session.getRootNode().addNode(assetsPath.substring(1), "sling:Folder");

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

            // Fire HX-Trigger so the folder tree container reloads via HTMX
            response.setStatus(200);
            response.setHeader("HX-Trigger", "folderCreated");
            response.getWriter().write("");

        } catch (Exception e) {
            LOG.error("Error creating folder", e);
            response.setStatus(500);
            response.getWriter().write("Failed to create folder: " + e.getMessage());
        }
    }


}
