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
    private static final String DEFAULT_ASSETS_PATH = "/content/motorbrot/dml/assets";

    @SlingObject
    private SlingJakartaHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private List<AssetCard> assets;
    private List<FolderCard> folders = new ArrayList<>();
    private List<Crumb> breadcrumb = new ArrayList<>();
    private boolean folderFound = true;
    private boolean includeFolders;
    private String currentPath;
    private String rootPath;
    private String parentPath;

    @PostConstruct
    protected void init() {
        assets = new ArrayList<>();

        String folderPath = request.getParameter("folder");
        if (folderPath == null || folderPath.isEmpty()) {
            folderPath = DEFAULT_ASSETS_PATH;
        }
        currentPath = folderPath;

        includeFolders = Boolean.parseBoolean(request.getParameter("includeFolders"));
        rootPath = request.getParameter("root");
        if (rootPath == null || rootPath.isEmpty()) {
            rootPath = DEFAULT_ASSETS_PATH;
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
                } else if (includeFolders && isFolder(child)) {
                    folders.add(new FolderCard(child));
                }
            }

            if (includeFolders) {
                buildBreadcrumb();
            }
        } catch (Exception e) {
            LOG.error("Error listing assets", e);
        }
    }

    private static boolean isFolder(Node node) {
        try {
            String type = node.getPrimaryNodeType().getName();
            return "sling:Folder".equals(type) || "sling:OrderedFolder".equals(type)
                    || "nt:folder".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Builds the breadcrumb from {@link #rootPath} down to {@link #currentPath},
     * and sets {@link #parentPath} when the current folder is below the root.
     */
    private void buildBreadcrumb() {
        breadcrumb.add(new Crumb("Library", rootPath));
        if (currentPath.equals(rootPath) || !currentPath.startsWith(rootPath + "/")) {
            parentPath = null;
            return;
        }
        String relative = currentPath.substring(rootPath.length() + 1);
        String[] segments = relative.split("/");
        StringBuilder accumulated = new StringBuilder(rootPath);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            accumulated.append('/').append(segment);
            String segPath = accumulated.toString();
            String title = resolveTitle(segPath, segment);
            breadcrumb.add(new Crumb(title, segPath));
        }
        int lastSlash = currentPath.lastIndexOf('/');
        parentPath = lastSlash > rootPath.length() - 1 && lastSlash > 0
                ? currentPath.substring(0, lastSlash)
                : rootPath;
        if (currentPath.equals(rootPath)) {
            parentPath = null;
        }
    }

    private String resolveTitle(String path, String fallback) {
        Resource r = resourceResolver.getResource(path);
        if (r != null) {
            String title = r.getValueMap().get("jcr:title", String.class);
            if (title != null && !title.isEmpty()) {
                return title;
            }
        }
        return fallback;
    }

    public List<AssetCard> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public List<FolderCard> getFolders() {
        return Collections.unmodifiableList(folders);
    }

    public List<Crumb> getBreadcrumb() {
        return Collections.unmodifiableList(breadcrumb);
    }

    public boolean isIncludeFolders() {
        return includeFolders;
    }

    public boolean isHasFolders() {
        return !folders.isEmpty();
    }

    public boolean isHasParent() {
        return parentPath != null;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public boolean isEmpty() {
        return assets.isEmpty();
    }

    /** True when there is nothing to show — neither assets nor subfolders. */
    public boolean isEmptyAll() {
        return assets.isEmpty() && folders.isEmpty();
    }

    public boolean isFolderFound() {
        return folderFound;
    }

    /** A single breadcrumb segment (folder title + path). */
    public static class Crumb {
        private final String name;
        private final String path;

        Crumb(String name, String path) {
            this.name = name;
            this.path = path;
        }
        public String getName() { return name; }
        public String getPath() { return path; }
    }

    /** A navigable sub-folder card in the picker grid. */
    public static class FolderCard {
        private final String path;
        private final String name;
        private final int childCount;

        FolderCard(Node node) throws Exception {
            this.path = node.getPath();
            this.name = node.hasProperty("jcr:title")
                    ? node.getProperty("jcr:title").getString()
                    : node.getName();
            int count = 0;
            NodeIterator it = node.getNodes();
            while (it.hasNext()) {
                Node child = it.nextNode();
                boolean asset = child.hasProperty("isAsset")
                        && child.getProperty("isAsset").getBoolean();
                if (asset || isFolder(child)) {
                    count++;
                }
            }
            this.childCount = count;
        }

        public String getPath() { return path; }
        public String getName() { return name; }
        public int getChildCount() { return childCount; }
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
