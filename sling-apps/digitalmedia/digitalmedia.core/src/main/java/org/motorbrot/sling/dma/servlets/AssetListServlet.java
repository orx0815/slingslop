package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet for listing assets in a folder.
 * Returns HTML fragment with asset cards.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "GET",
    selectors = "assets",
    extensions = "html"
)
public class AssetListServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AssetListServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();
        String folderPath = request.getParameter("folder");

        if (folderPath == null || folderPath.isEmpty()) {
            folderPath = "/content/motorbrot/dma/assets";
        }

        try {
            Resource folderResource = resolver.getResource(folderPath);
            if (folderResource == null) {
                response.setStatus(404);
                response.getWriter().write("<div class=\"dml-empty-state\" style=\"grid-column: 1 / -1;\">" +
                        "<p>Folder not found</p></div>");
                return;
            }

            Node folderNode = folderResource.adaptTo(Node.class);
            if (folderNode == null) {
                response.setStatus(500);
                return;
            }

            StringBuilder html = new StringBuilder();
            boolean hasAssets = false;

            NodeIterator children = folderNode.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();

                // Only show files (assets), not folders
                if ("nt:file".equals(child.getPrimaryNodeType().getName())) {
                    hasAssets = true;
                    html.append(generateAssetCard(child));
                }
            }

            if (!hasAssets) {
                html.append("<div class=\"dml-empty-state\" style=\"grid-column: 1 / -1;\">")
                    .append("<div class=\"dml-empty-state-icon\">📂</div>")
                    .append("<h3>No assets yet</h3>")
                    .append("<p>Upload your first asset to get started</p>")
                    .append("</div>");
            }

            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().write(html.toString());

        } catch (Exception e) {
            LOG.error("Error listing assets", e);
            response.setStatus(500);
            response.getWriter().write("Error loading assets: " + e.getMessage());
        }
    }

    /**
     * Generates HTML for an asset card.
     */
    private String generateAssetCard(Node assetNode) throws Exception {
        String assetPath = assetNode.getPath();
        String assetName = assetNode.getName();

        Node contentNode = assetNode.getNode("jcr:content");
        String fileType = contentNode.hasProperty("dma:fileType")
                ? contentNode.getProperty("dma:fileType").getString()
                : "unknown";

        long fileSize = contentNode.hasProperty("dma:fileSize")
                ? contentNode.getProperty("dma:fileSize").getLong()
                : 0;

        String iconEmoji = getIconForFileType(fileType);
        String formattedSize = formatFileSize(fileSize);

        // Check if preview rendition exists
        boolean hasPreview = assetNode.hasNode("renditions/preview");
        String previewHtml = hasPreview
                ? String.format("<img src=\"%s/renditions/preview/jcr:content\" alt=\"%s\" />",
                        assetPath, assetName)
                : String.format("<span class=\"dml-asset-preview-icon\">%s</span>", iconEmoji);

        return String.format(
            "<div class=\"dml-asset-item\" data-asset-id=\"%s\" data-asset-path=\"%s\">" +
            "  <div class=\"dml-asset-preview\">%s" +
            "    <span class=\"dml-asset-type-badge\">%s</span>" +
            "  </div>" +
            "  <div class=\"dml-asset-info\">" +
            "    <div class=\"dml-asset-name\" title=\"%s\">%s</div>" +
            "    <div class=\"dml-asset-meta\">" +
            "      <span class=\"dml-asset-size\">%s</span>" +
            "    </div>" +
            "  </div>" +
            "</div>",
            assetPath, assetPath, previewHtml, fileType, assetName, assetName, formattedSize
        );
    }

    /**
     * Returns an appropriate icon emoji for the file type.
     */
    private String getIconForFileType(String fileType) {
        switch (fileType) {
            case "image": return "🖼️";
            case "video": return "🎬";
            case "audio": return "🎵";
            case "pdf": return "📄";
            case "archive": return "📦";
            case "text": return "📝";
            default: return "📁";
        }
    }

    /**
     * Formats file size in human-readable format.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
