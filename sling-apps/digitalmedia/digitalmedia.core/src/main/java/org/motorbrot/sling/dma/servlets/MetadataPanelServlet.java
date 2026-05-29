package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet for rendering the metadata panel for an asset.
 * Delegates to the metadata-panel component.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "nt:file",
    methods = "GET",
    selectors = "metadata-panel",
    extensions = "html"
)
public class MetadataPanelServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        // Include the metadata-panel component
        Resource componentResource = request.getResourceResolver()
                .getResource("/apps/motorbrot/dma/components/metadata-panel");

        if (componentResource != null) {
            request.getRequestDispatcher(componentResource, "metadata-panel.html")
                    .include(request, response);
        } else {
            response.getWriter().write("<div class=\"dml-metadata-empty\">" +
                    "<p>Metadata panel component not found</p></div>");
        }
    }
}
