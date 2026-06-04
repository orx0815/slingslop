package org.motorbrot.sling.dma.servlets;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.services.MetadataExtractionService;
import org.motorbrot.sling.dma.services.RenditionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Servlet for uploading assets to the Digital Media Library.
 * Handles file upload, metadata extraction, and preview generation.
 * Renders the response HTML via HTL using the GET-wrapper dispatcher pattern.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/pages/dashboard",
    methods = "POST",
    selectors = "upload",
    extensions = "html"
)
public class AssetUploadServlet extends SlingJakartaAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AssetUploadServlet.class);

    @Reference
    private MetadataExtractionService metadataService;

    @Reference
    private RenditionService renditionService;

    @Override
    protected void doPost(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();

        try {
            // Get all uploaded files (multi-file support)
            org.apache.sling.api.request.RequestParameter[] fileParams = request.getRequestParameters("asset");
            if (fileParams == null || fileParams.length == 0
                    || (fileParams.length == 1 && fileParams[0].getFileName().isEmpty())) {
                response.setStatus(400);
                response.getWriter().write("No file uploaded");
                return;
            }

            String parentPath = request.getParameter("folder");
            List<String> createdPaths = new ArrayList<>();

            for (org.apache.sling.api.request.RequestParameter fileParam : fileParams) {
                if (fileParam.getFileName() == null || fileParam.getFileName().isEmpty()) {
                    continue;
                }

                String filename = fileParam.getFileName();
                byte[] fileBytes = fileParam.getInputStream().readAllBytes();

                Map<String, Object> metadata = metadataService.extractMetadata(
                        new ByteArrayInputStream(fileBytes), filename);

                String assetPath = createAssetNode(resolver, filename, fileBytes, metadata, parentPath);
                if (assetPath != null) {
                    createdPaths.add(assetPath);
                } else {
                    LOG.warn("Failed to create asset node for {}", filename);
                }
            }

            // Render each created asset via its HTL asset-card script (GET-wrapper pattern)
            response.setStatus(200);
            response.setContentType("text/html");

            for (String assetPath : createdPaths) {
                Resource assetResource = resolver.getResource(assetPath);
                if (assetResource != null) {
                    RequestDispatcher dispatcher = request.getRequestDispatcher(
                            assetPath + ".asset-card.html");
                    if (dispatcher != null) {
                        dispatcher.include(new HttpServletRequestWrapper(request) {
                            @Override
                            public String getMethod() { return "GET"; }
                        }, response);
                    }
                }
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
                                     Map<String, Object> metadata, String parentPath) {
        try {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                return null;
            }

            // Resolve target folder — use selected parentPath when valid
            String defaultAssetsPath = "/content/motorbrot/dma/assets";
            String assetsPath;
            if (parentPath != null && !parentPath.trim().isEmpty()
                    && parentPath.startsWith(defaultAssetsPath)) {
                assetsPath = parentPath.trim();
            } else {
                assetsPath = defaultAssetsPath;
            }
            Node assetsFolder = session.nodeExists(assetsPath)
                    ? session.getNode(assetsPath)
                    : session.getRootNode().addNode(assetsPath.substring(1), "sling:Folder");

            // Create unique asset name
            String assetName = generateUniqueAssetName(assetsFolder, filename);

            // Create asset node as nt:unstructured so it can hold jcr:content,
            // metadata, and renditions as siblings. nt:file only permits jcr:content.
            Node assetNode = assetsFolder.addNode(assetName, "nt:unstructured");
            assetNode.setProperty("sling:resourceType", "motorbrot/dma/components/asset");
            assetNode.setProperty("isAsset", true);
            assetNode.setProperty("jcr:created", Calendar.getInstance());
            assetNode.setProperty("uploadedBy", session.getUserID());

            // Create jcr:content node with file data
            Node contentNode = assetNode.addNode("jcr:content", "nt:resource");
            contentNode.setProperty("jcr:data", session.getValueFactory().createBinary(
                    new ByteArrayInputStream(fileBytes)));
            contentNode.setProperty("jcr:mimeType", (String) metadata.get("mimeType"));
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

            // Add metadata on a dedicated nt:unstructured child node.
            // nt:resource does not allow custom properties, and dma: namespace is not registered.
            Node metaNode = assetNode.addNode("metadata", "nt:unstructured");
            metaNode.setProperty("filename", filename);
            metaNode.setProperty("fileType", (String) metadata.get("fileType"));
            metaNode.setProperty("fileSize", (long) fileBytes.length);

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
}
