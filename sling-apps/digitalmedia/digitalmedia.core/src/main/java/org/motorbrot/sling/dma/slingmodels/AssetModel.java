package org.motorbrot.sling.dma.slingmodels;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Sling Model for a media asset.
 * Provides access to asset metadata and renditions.
 */
@Model(adaptables = {SlingJakartaHttpServletRequest.class, Resource.class})
public class AssetModel {

    @Self
    private Resource resource;

    @Inject
    private SlingJakartaHttpServletRequest request;

    private String filename;
    private String fileType;
    private long fileSize;
    private String mimeType;
    private String assetPath;
    private List<String> renditionNames;

    public AssetModel() {
        // Default constructor required by Sling Models
    }

    /**
     * Initializes the model by reading asset metadata from JCR.
     */
    @javax.annotation.PostConstruct
    protected void init() {
        try {
            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode != null && assetNode.hasNode("jcr:content")) {
                Node contentNode = assetNode.getNode("jcr:content");

                this.assetPath = assetNode.getPath();
                this.filename = contentNode.hasProperty("dma:filename")
                        ? contentNode.getProperty("dma:filename").getString()
                        : assetNode.getName();

                this.fileType = contentNode.hasProperty("dma:fileType")
                        ? contentNode.getProperty("dma:fileType").getString()
                        : "unknown";

                this.fileSize = contentNode.hasProperty("dma:fileSize")
                        ? contentNode.getProperty("dma:fileSize").getLong()
                        : 0;

                this.mimeType = contentNode.hasProperty("jcr:mimeType")
                        ? contentNode.getProperty("jcr:mimeType").getString()
                        : "application/octet-stream";

                // Load rendition names
                this.renditionNames = new ArrayList<>();
                if (assetNode.hasNode("renditions")) {
                    Node renditionsNode = assetNode.getNode("renditions");
                    NodeIterator iterator = renditionsNode.getNodes();
                    while (iterator.hasNext()) {
                        renditionNames.add(iterator.nextNode().getName());
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail
            this.filename = "Error loading asset";
            this.fileType = "error";
        }
    }

    public String getFilename() {
        return filename;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getDownloadUrl() {
        return assetPath + "/jcr:content";
    }

    public List<String> getRenditionNames() {
        return renditionNames;
    }

    public boolean hasRenditions() {
        return renditionNames != null && !renditionNames.isEmpty();
    }

    public String getRenditionUrl(String renditionName) {
        return assetPath + "/renditions/" + renditionName + "/jcr:content";
    }

    public boolean isImage() {
        return "image".equals(fileType);
    }

    public String getIconEmoji() {
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
