package org.motorbrot.sling.dma.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingJakartaSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.motorbrot.sling.dma.internal.AssetBinary;
import org.motorbrot.sling.dma.slingmodels.AssetModel;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for returning asset metadata as JSON.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "motorbrot/dma/components/asset",
    methods = "GET",
    selectors = "metadata",
    extensions = "json"
)
public class AssetMetadataServlet extends SlingJakartaSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AssetMetadataServlet.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(SlingJakartaHttpServletRequest request, SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {

        Resource resource = request.getResource();
        AssetModel asset = resource.adaptTo(AssetModel.class);

        if (asset == null) {
            response.setStatus(404);
            response.getWriter().write("{\"error\": \"Asset not found\"}");
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", asset.getFilename());
        metadata.put("fileType", asset.getFileType());
        metadata.put("fileSize", asset.getFileSize());
        metadata.put("formattedFileSize", asset.getFormattedFileSize());
        metadata.put("mimeType", asset.getMimeType());
        metadata.put("assetPath", asset.getAssetPath());
        metadata.put("downloadUrl", asset.getDownloadUrl());
        metadata.put("renditions", asset.getRenditionNames());
        metadata.put("hasRenditions", asset.hasRenditions());
        metadata.put("isImage", asset.isImage());

        // Add rendition URLs
        if (asset.hasRenditions()) {
            Map<String, String> renditionUrls = new HashMap<>();
            for (String renditionName : asset.getRenditionNames()) {
                renditionUrls.put(renditionName, asset.getRenditionUrl(renditionName));
            }
            metadata.put("renditionUrls", renditionUrls);
        }

        // Add created/modified dates if available
        try {
            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode != null && assetNode.hasNode("jcr:content")) {
                Node contentNode = AssetBinary.originalResource(assetNode);
                if (contentNode.hasProperty("jcr:lastModified")) {
                    metadata.put("lastModified", contentNode.getProperty("jcr:lastModified").getString());
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read dates from asset", e);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(metadata));
    }
}
