package org.motorbrot.sling.dma.servlets;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns an HTML fragment with folder info for display in the metadata panel.
 * Accepts a {@code folderPath} request parameter.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "GET",
    selectors = "folder-info",
    extensions = "html"
)
public class FolderInfoServlet extends SlingJakartaSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FolderInfoServlet.class);
    private static final String ASSETS_ROOT = "/content/motorbrot/dma/assets";

    @Override
    protected void doGet(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String folderPath = request.getParameter("folderPath");
        if (folderPath == null || !folderPath.startsWith(ASSETS_ROOT)) {
            response.getWriter().write("<div class=\"dml-metadata-empty\"><p>Invalid folder path.</p></div>");
            return;
        }

        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if (session == null || !session.nodeExists(folderPath)) {
                response.getWriter().write("<div class=\"dml-metadata-empty\"><p>Folder not found.</p></div>");
                return;
            }

            Node folder = session.getNode(folderPath);
            boolean isRoot = ASSETS_ROOT.equals(folderPath);

            String displayName = isRoot ? "Library"
                    : (folder.hasProperty("jcr:title")
                            ? folder.getProperty("jcr:title").getString()
                            : folder.getName());

            // Collect assets and count subfolders
            List<String> assetNames = new ArrayList<>();
            int subfolderCount = 0;
            NodeIterator children = folder.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if ("sling:Folder".equals(child.getPrimaryNodeType().getName())) {
                    subfolderCount++;
                } else if (child.hasProperty("isAsset")) {
                    String filename = child.hasNode("metadata")
                            ? child.getNode("metadata").getProperty("filename").getString()
                            : child.getName();
                    assetNames.add(filename);
                }
            }

            String dashboardPath = request.getResource().getPath();
            String encodedPath = URLEncoder.encode(folderPath, StandardCharsets.UTF_8);
            String deleteUrl = dashboardPath + ".delete-folder.html";

            PrintWriter w = response.getWriter();
            w.write("<div class=\"dml-folder-info\">");

            // Header
            w.write("<div class=\"dml-metadata-header\">");
            w.write("  <div class=\"dml-folder-info-icon\">📁</div>");
            w.write("  <h2 class=\"dml-metadata-title\">" + escapeHtml(displayName) + "</h2>");
            w.write("</div>");

            // Summary section
            w.write("<div class=\"dml-metadata-section\">");
            w.write("  <h3>Contents</h3>");
            w.write("  <div class=\"dml-metadata-row\">");
            w.write("    <span class=\"dml-metadata-label\">Assets</span>");
            w.write("    <span class=\"dml-metadata-value\">" + assetNames.size() + "</span>");
            w.write("  </div>");
            if (subfolderCount > 0) {
                w.write("  <div class=\"dml-metadata-row\">");
                w.write("    <span class=\"dml-metadata-label\">Subfolders</span>");
                w.write("    <span class=\"dml-metadata-value\">" + subfolderCount + "</span>");
                w.write("  </div>");
            }
            w.write("</div>");

            // File list
            if (!assetNames.isEmpty()) {
                w.write("<div class=\"dml-metadata-section\">");
                w.write("  <h3>Files</h3>");
                w.write("  <ul class=\"dml-folder-file-list\">");
                for (String name : assetNames) {
                    w.write("    <li>" + escapeHtml(name) + "</li>");
                }
                w.write("  </ul>");
                w.write("</div>");
            }

            // Delete action (not shown for root Library)
            if (!isRoot) {
                w.write("<div class=\"dml-metadata-actions\">");
                w.write("  <button class=\"dml-btn dml-btn-danger dml-btn-download\"");
                w.write("          onclick=\"document.getElementById('dml-folder-delete-modal').showModal()\">");
                w.write("    🗑 Delete Folder");
                w.write("  </button>");
                w.write("</div>");

                // Delete modal — content differs based on whether subfolders exist
                w.write("<dialog id=\"dml-folder-delete-modal\" class=\"dml-modal\">");
                w.write("  <div class=\"dml-modal-content\">");
                w.write("    <div class=\"dml-modal-icon\">🗑</div>");
                if (subfolderCount > 0) {
                    // Refusal state
                    w.write("    <h3 class=\"dml-modal-title\">Cannot Delete Folder</h3>");
                    w.write("    <p class=\"dml-modal-body\">");
                    w.write("      <strong>" + escapeHtml(displayName) + "</strong> contains ");
                    w.write(subfolderCount == 1 ? "1 subfolder" : subfolderCount + " subfolders");
                    w.write(". Remove all subfolders first before deleting this folder.");
                    w.write("    </p>");
                    w.write("    <div class=\"dml-modal-actions\">");
                    w.write("      <button class=\"dml-btn dml-btn-primary\"");
                    w.write("              onclick=\"document.getElementById('dml-folder-delete-modal').close()\">");
                    w.write("        OK");
                    w.write("      </button>");
                    w.write("    </div>");
                } else {
                    // Confirmation state
                    int assetCount = assetNames.size();
                    String assetLabel = assetCount == 1 ? "1 asset" : assetCount + " assets";
                    w.write("    <h3 class=\"dml-modal-title\">Delete Folder?</h3>");
                    w.write("    <p class=\"dml-modal-body\">");
                    w.write("      Sure you want to delete <strong>" + escapeHtml(displayName) + "</strong>");
                    if (assetCount > 0) {
                        w.write(" and its " + assetLabel + "?");
                    } else {
                        w.write("? The folder is empty.");
                    }
                    w.write(" This cannot be undone.");
                    w.write("    </p>");
                    w.write("    <div class=\"dml-modal-actions\">");
                    w.write("      <button class=\"dml-btn dml-btn-secondary\"");
                    w.write("              onclick=\"document.getElementById('dml-folder-delete-modal').close()\">");
                    w.write("        Cancel");
                    w.write("      </button>");
                    w.write("      <button class=\"dml-btn dml-btn-danger\"");
                    w.write("              hx-post=\"" + deleteUrl + "\"");
                    w.write("              hx-vals='{\"folderPath\": \"" + escapeHtml(folderPath) + "\"}'");
                    w.write("              hx-target=\"body\"");
                    w.write("              onclick=\"document.getElementById('dml-folder-delete-modal').close()\">");
                    w.write("        Yes, Delete");
                    w.write("      </button>");
                    w.write("    </div>");
                }
                w.write("  </div>");
                w.write("</dialog>");
            }

            w.write("</div>"); // .dml-folder-info

        } catch (Exception e) {
            LOG.error("Error loading folder info for path: {}", folderPath, e);
            response.getWriter().write("<div class=\"dml-metadata-empty\"><p>Error loading folder info.</p></div>");
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
