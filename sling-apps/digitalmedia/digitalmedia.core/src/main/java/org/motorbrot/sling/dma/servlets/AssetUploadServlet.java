package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.services.MetadataExtractionService;
import org.motorbrot.sling.dma.services.RenditionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

/**
 * Servlet for uploading assets to the Digital Media Library.
 * Handles file upload, metadata extraction, and preview generation.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "POST",
    selectors = "upload",
    extensions = "html"
)
public class AssetUploadServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AssetUploadServlet.class);

    @Reference
    private MetadataExtractionService metadataService;

    @Reference
    private RenditionService renditionService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();

        try {
            // Get the uploaded file
            org.apache.sling.api.request.RequestParameter fileParam = request.getRequestParameter("asset");
            if (fileParam == null) {
                response.setStatus(400);
                response.getWriter().write("No file uploaded");
                return;
            }

            String filename = fileParam.getFileName();
            InputStream fileData = fileParam.getInputStream();

            // Read file data once (we need it for metadata and rendition)
            byte[] fileBytes = fileData.readAllBytes();

            // Extract metadata
            Map<String, Object> metadata = metadataService.extractMetadata(
                    new ByteArrayInputStream(fileBytes), filename);

            // Create asset node
            String assetPath = createAssetNode(resolver, filename, fileBytes, metadata);

            if (assetPath != null) {
                response.setStatus(200);
                response.setContentType("text/html");
                response.getWriter().write(generateAssetCard(assetPath, filename, metadata));
            } else {
                response.setStatus(500);
                response.getWriter().write("Failed to create asset");
            }

        } catch (Exception e) {
            LOG.error("Error uploading asset", e);
            response.setStatus(500);
            response.getWriter().write("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Creates an asset node in JCR with file data and metadata.
     */
    private String createAssetNode(ResourceResolver resolver, String filename, byte[] fileBytes,
                                     Map<String, Object> metadata) {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return null;
            }

            // Create assets folder if it doesn't exist
            String assetsPath = "/content/motorbrot/dma/assets";
            Node assetsFolder = session.nodeExists(assetsPath)
                    ? session.getNode(assetsPath)
                    : session.getRootNode().addNode("content/motorbrot/dma/assets", "sling:Folder");

            // Create unique asset name
            String assetName = generateUniqueAssetName(assetsFolder, filename);

            // Create asset node
            Node assetNode = assetsFolder.addNode(assetName, "nt:file");

            // Create jcr:content node with file data
            Node contentNode = assetNode.addNode("jcr:content", "nt:resource");
            contentNode.setProperty("jcr:data", session.getValueFactory().createBinary(
                    new ByteArrayInputStream(fileBytes)));
            contentNode.setProperty("jcr:mimeType", (String) metadata.get("mimeType"));
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

            // Add metadata
            contentNode.setProperty("dma:filename", filename);
            contentNode.setProperty("dma:fileType", (String) metadata.get("fileType"));
            contentNode.setProperty("dma:fileSize", fileBytes.length);

            // Generate preview rendition for images
            if (metadataService.supportsImageRenditions((String) metadata.get("mimeType"))) {
                byte[] preview = renditionService.generatePreviewRendition(new ByteArrayInputStream(fileBytes));
                if (preview != null) {
                    Node renditionsNode = assetNode.addNode("renditions", "nt:unstructured");
                    Node previewNode = renditionsNode.addNode("preview", "nt:file");
                    Node previewContent = previewNode.addNode("jcr:content", "nt:resource");
                    previewContent.setProperty("jcr:data", session.getValueFactory().createBinary(
                            new ByteArrayInputStream(preview)));
                    previewContent.setProperty("jcr:mimeType", "image/jpeg");
                }
            }

            session.save();
            return assetNode.getPath();

        } catch (Exception e) {
            LOG.error("Failed to create asset node", e);
            return null;
        }
    }

    /**
     * Generates a unique asset name based on filename.
     */
    private String generateUniqueAssetName(Node parent, String filename) throws Exception {
        String baseName = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String assetName = baseName;
        int counter = 1;

        while (parent.hasNode(assetName)) {
            String[] parts = baseName.split("\\.");
            if (parts.length > 1) {
                assetName = parts[0] + "_" + counter + "." + parts[parts.length - 1];
            } else {
                assetName = baseName + "_" + counter;
            }
            counter++;
        }

        return assetName;
    }

    /**
     * Generates HTML for an asset card to be inserted into the grid.
     */
    private String generateAssetCard(String assetPath, String filename, Map<String, Object> metadata) {
        String fileType = (String) metadata.get("fileType");
        String iconEmoji = getIconForFileType(fileType);

        return String.format(
            "<div class=\"dml-asset-item dml-fade-in\" data-asset-id=\"%s\">" +
            "  <div class=\"dml-asset-preview\">" +
            "    <span class=\"dml-asset-preview-icon\">%s</span>" +
            "    <span class=\"dml-asset-type-badge\">%s</span>" +
            "  </div>" +
            "  <div class=\"dml-asset-info\">" +
            "    <div class=\"dml-asset-name\">%s</div>" +
            "    <div class=\"dml-asset-meta\">" +
            "      <span class=\"dml-asset-size\">Just uploaded</span>" +
            "    </div>" +
            "  </div>" +
            "</div>",
            assetPath, iconEmoji, fileType, filename
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
}
