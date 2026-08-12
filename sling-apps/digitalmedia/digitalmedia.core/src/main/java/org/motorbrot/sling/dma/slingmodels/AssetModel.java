package org.motorbrot.sling.dma.slingmodels;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.motorbrot.sling.dma.internal.AssetBinary;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sling Model for a media asset.
 * Provides access to asset metadata and renditions.
 */
@Model(adaptables = {SlingJakartaHttpServletRequest.class, Resource.class})
public class AssetModel {

    @SlingObject
    private Resource resource;

    private String filename;
    private String fileType;
    private long fileSize;
    private String mimeType;
    private String assetPath;
    private List<String> renditionNames;
    private String createdDate;
    private String modifiedDate;
    private String uploadedBy;

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
                Node contentNode = AssetBinary.originalResource(assetNode);
                Node metaNode = assetNode.hasNode("metadata") ? assetNode.getNode("metadata") : null;

                this.assetPath = assetNode.getPath();
                this.filename = (metaNode != null && metaNode.hasProperty("filename"))
                        ? metaNode.getProperty("filename").getString()
                        : assetNode.getName();

                this.fileType = (metaNode != null && metaNode.hasProperty("fileType"))
                        ? metaNode.getProperty("fileType").getString()
                        : "unknown";

                this.fileSize = (metaNode != null && metaNode.hasProperty("fileSize"))
                        ? metaNode.getProperty("fileSize").getLong()
                        : 0;

                this.mimeType = contentNode.hasProperty("jcr:mimeType")
                        ? contentNode.getProperty("jcr:mimeType").getString()
                        : "application/octet-stream";

                // Load created and modified dates
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                this.createdDate = assetNode.hasProperty("jcr:created")
                        ? sdf.format(assetNode.getProperty("jcr:created").getDate().getTime())
                        : "";

                this.modifiedDate = contentNode.hasProperty("jcr:lastModified")
                        ? sdf.format(contentNode.getProperty("jcr:lastModified").getDate().getTime())
                        : "";

                this.uploadedBy = assetNode.hasProperty("uploadedBy")
                        ? assetNode.getProperty("uploadedBy").getString()
                        : "";

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

    /** Returns a map of format name → true so HTL can do asset.renditionExists['thumbnail']. */
    public Map<String, Boolean> getRenditionExists() {
        Map<String, Boolean> map = new HashMap<>();
        if (renditionNames != null) {
            for (String name : renditionNames) {
                map.put(name, Boolean.TRUE);
            }
        }
        return map;
    }

    /** True when at least one of the known generate formats is not yet present. */
    public boolean isMissingRenditions() {
        List<String> known = Arrays.asList("thumbnail", "web", "large");
        if (renditionNames == null) return true;
        for (String fmt : known) {
            if (!renditionNames.contains(fmt)) return true;
        }
        return false;
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

    public String getPath() {
        return assetPath;
    }

    public String getName() {
        if (assetPath != null) {
            int lastSlash = assetPath.lastIndexOf('/');
            return lastSlash >= 0 ? assetPath.substring(lastSlash + 1) : assetPath;
        }
        return "";
    }

    public String getPreviewUrl() {
        return assetPath + "/renditions/preview/jcr:content";
    }

    public String getFormattedSize() {
        return getFormattedFileSize();
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public boolean getSupportsImageRenditions() {
        return "image".equals(fileType);
    }
}
