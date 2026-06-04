package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servlet for listing folders in the Digital Media Library folder tree.
 * Returns HTML fragments for the folder tree sidebar, including recursive subfolders.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "GET",
    selectors = "folder-tree",
    extensions = "html"
)
public class FolderListServlet extends SlingJakartaSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FolderListServlet.class);

    private static final String ASSETS_PATH = "/content/motorbrot/dma/assets";

    @Override
    protected void doGet(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String dashboardPath = request.getResource().getPath();
        StringBuilder html = new StringBuilder();

        // Library root entry
        html.append("<div class=\"dml-folder-tree-item\">");
        html.append(folderButton(ASSETS_PATH, "Library", dashboardPath, true, "🗄️"));
        html.append("</div>");

        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session != null && session.nodeExists(ASSETS_PATH)) {
                Node assetsNode = session.getNode(ASSETS_PATH);
                appendSubfolders(assetsNode, html, dashboardPath);
            }
        } catch (Exception e) {
            LOG.error("Error loading folder list", e);
        }

        response.getWriter().write(html.toString());
    }

    private void appendSubfolders(Node parent, StringBuilder html, String dashboardPath) throws RepositoryException {
        NodeIterator children = parent.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (!"sling:Folder".equals(child.getPrimaryNodeType().getName())) {
                continue;
            }
            String folderPath = child.getPath();
            String folderName = child.hasProperty("jcr:title")
                    ? child.getProperty("jcr:title").getString()
                    : child.getName();

            boolean hasSubfolders = hasSubfolderChildren(child);
            html.append("<div class=\"dml-folder-tree-item\">");
            html.append(folderButton(folderPath, folderName, dashboardPath, false, "📁"));
            if (hasSubfolders) {
                html.append("<div class=\"dml-folder-children\">");
                appendSubfolders(child, html, dashboardPath);
                html.append("</div>");
            }
            html.append("</div>");
        }
    }

    private boolean hasSubfolderChildren(Node node) throws RepositoryException {
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            if ("sling:Folder".equals(it.nextNode().getPrimaryNodeType().getName())) {
                return true;
            }
        }
        return false;
    }

    private String folderButton(String folderPath, String folderName, String dashboardPath, boolean isActive, String icon) {
        String encodedPath = URLEncoder.encode(folderPath, StandardCharsets.UTF_8);
        String hxUrl = dashboardPath + ".folder-info.html?folderPath=" + encodedPath;
        String activeClass = isActive ? " active" : "";
        String onclick = "document.querySelectorAll('.dml-folder-tree-button').forEach(function(b){b.classList.remove('active');}); " +
               "this.classList.add('active'); " +
               "document.getElementById('dml-current-folder').value='" + folderPath.replace("'", "\\'") + "'; " +
               "htmx.trigger(document.body,'folderSelected');";
        return "<button class=\"dml-folder-tree-button" + activeClass + "\" data-folder-path=\"" + folderPath + "\"" +
               " hx-get=\"" + hxUrl + "\"" +
               " hx-target=\".dml-metadata-panel\"" +
               " hx-swap=\"innerHTML\"" +
               " onclick=\"" + onclick + "\">" +
               "<span class=\"dml-folder-icon\">" + icon + "</span>" +
               "<span class=\"dml-folder-name\">" + escapeHtml(folderName) + "</span>" +
               "</button>";
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
