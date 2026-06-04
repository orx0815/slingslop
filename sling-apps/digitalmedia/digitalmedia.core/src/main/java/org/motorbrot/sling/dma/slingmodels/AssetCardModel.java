package org.motorbrot.sling.dma.slingmodels;

import javax.annotation.PostConstruct;
import javax.jcr.Node;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight Sling Model for rendering a single asset card.
 * Used by the asset-card.html script when included via RequestDispatcher
 * after upload.
 */
@Model(adaptables = Resource.class)
public class AssetCardModel {

    private static final Logger LOG = LoggerFactory.getLogger(AssetCardModel.class);

    @SlingObject
    private Resource resource;

    private String path;
    private String name;
    private String fileType;
    private boolean hasPreview;
    private boolean svg;
    private String iconEmoji;

    @PostConstruct
    protected void init() {
        try {
            Node assetNode = resource.adaptTo(Node.class);
            if (assetNode == null) {
                return;
            }
            path = assetNode.getPath();
            name = assetNode.getName();

            Node metaNode = assetNode.hasNode("metadata") ? assetNode.getNode("metadata") : null;
            fileType = (metaNode != null && metaNode.hasProperty("fileType"))
                    ? metaNode.getProperty("fileType").getString()
                    : "unknown";

            hasPreview = assetNode.hasNode("renditions/preview");
            svg = "svg".equals(fileType);
            iconEmoji = getIconForFileType(fileType);
        } catch (Exception e) {
            LOG.warn("Error initializing AssetCardModel", e);
        }
    }

    public String getPath() { return path; }
    public String getName() { return name; }
    public String getFileType() { return fileType; }
    public boolean isHasPreview() { return hasPreview; }
    public boolean isSvg() { return svg; }
    public String getIconEmoji() { return iconEmoji; }
    public String getPreviewUrl() { return path + "/renditions/preview/jcr:content"; }
    public String getSvgUrl() { return path + "/jcr:content"; }

    private static String getIconForFileType(String fileType) {
        switch (fileType) {
            case "image":        return "\uD83D\uDDBC\uFE0F";
            case "svg":          return "\uD83C\uDFA8";
            case "video":        return "\uD83C\uDFAC";
            case "audio":        return "\uD83C\uDFB5";
            case "pdf":          return "\uD83D\uDCD5";
            case "spreadsheet":  return "\uD83D\uDCCA";
            case "presentation": return "\uD83D\uDCCB";
            case "document":     return "\uD83D\uDCDD";
            case "archive":      return "\uD83D\uDCE6";
            case "text":         return "\uD83D\uDCC4";
            default:             return "\uD83D\uDCC1";
        }
    }
}
