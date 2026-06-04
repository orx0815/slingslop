package org.motorbrot.sling.dma.slingmodels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model backing the {@code assets} selector on the dashboard page.
 * Lists all asset nodes inside the requested folder.
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class AssetListModel {

    private static final Logger LOG = LoggerFactory.getLogger(AssetListModel.class);
    private static final String DEFAULT_ASSETS_PATH = "/content/motorbrot/dma/assets";

    @SlingObject
    private SlingJakartaHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<AssetCard> assets;
    private boolean folderFound = true;

    @PostConstruct
    protected void init() {
        assets = new ArrayList<>();

        String folderPath = request.getParameter("folder");
        if (folderPath == null || folderPath.isEmpty()) {
            folderPath = DEFAULT_ASSETS_PATH;
        }

        try {
            Resource folderResource = resourceResolver.getResource(folderPath);
            if (folderResource == null) {
                folderFound = false;
                return;
            }

            Node folderNode = folderResource.adaptTo(Node.class);
            if (folderNode == null) {
                folderFound = false;
                return;
            }

            NodeIterator children = folderNode.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child.hasProperty("isAsset") && child.getProperty("isAsset").getBoolean()) {
                    assets.add(new AssetCard(child));
                }
            }
        } catch (Exception e) {
            LOG.error("Error listing assets", e);
        }
    }

    public List<AssetCard> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public boolean isEmpty() {
        return assets.isEmpty();
    }

    public boolean isFolderFound() {
        return folderFound;
    }

    /**
     * Value object representing a single asset card in the grid.
     */
    public static class AssetCard {
        private final String path;
        private final String name;
        private final String fileType;
        private final String formattedSize;
        private final boolean hasPreview;
        private final boolean isSvg;
        private final String iconEmoji;

        AssetCard(Node assetNode) throws Exception {
            this.path = assetNode.getPath();
            this.name = assetNode.getName();

            Node metaNode = assetNode.hasNode("metadata") ? assetNode.getNode("metadata") : null;
            this.fileType = (metaNode != null && metaNode.hasProperty("fileType"))
                    ? metaNode.getProperty("fileType").getString()
                    : "unknown";

            long fileSize = (metaNode != null && metaNode.hasProperty("fileSize"))
                    ? metaNode.getProperty("fileSize").getLong()
                    : 0;
            this.formattedSize = formatFileSize(fileSize);

            this.hasPreview = assetNode.hasNode("renditions/preview");
            this.isSvg = "svg".equals(fileType);
            this.iconEmoji = getIconForFileType(fileType);
        }

        public String getPath() { return path; }
        public String getName() { return name; }
        public String getFileType() { return fileType; }
        public String getFormattedSize() { return formattedSize; }
        public boolean isHasPreview() { return hasPreview; }
        public boolean isSvg() { return isSvg; }
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

        private static String formatFileSize(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            }
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }
    }
}
