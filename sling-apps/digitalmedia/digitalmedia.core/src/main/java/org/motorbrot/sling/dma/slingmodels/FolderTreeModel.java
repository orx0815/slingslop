package org.motorbrot.sling.dma.slingmodels;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model backing the {@code folder-tree} selector on the dashboard page.
 * Builds a recursive folder tree starting from the assets root.
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class FolderTreeModel {

    private static final Logger LOG = LoggerFactory.getLogger(FolderTreeModel.class);
    private static final String ASSETS_PATH = "/content/motorbrot/dma/assets";

    @SlingObject
    private SlingJakartaHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private String dashboardPath;
    private List<FolderItem> rootFolders;

    @PostConstruct
    protected void init() {
        rootFolders = new ArrayList<>();
        dashboardPath = request.getResource().getPath();

        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null && session.nodeExists(ASSETS_PATH)) {
                Node assetsNode = session.getNode(ASSETS_PATH);
                rootFolders = buildSubfolders(assetsNode);
            }
        } catch (Exception e) {
            LOG.error("Error loading folder tree", e);
        }
    }

    public String getAssetsPath() {
        return ASSETS_PATH;
    }

    public String getDashboardPath() {
        return dashboardPath;
    }

    public String getLibraryInfoUrl() {
        return dashboardPath + ".folder-info.html?folderPath="
                + URLEncoder.encode(ASSETS_PATH, StandardCharsets.UTF_8);
    }

    public List<FolderItem> getRootFolders() {
        return Collections.unmodifiableList(rootFolders);
    }

    private List<FolderItem> buildSubfolders(Node parent) throws RepositoryException {
        List<FolderItem> items = new ArrayList<>();
        NodeIterator children = parent.getNodes();
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (!"sling:Folder".equals(child.getPrimaryNodeType().getName())) {
                continue;
            }
            items.add(new FolderItem(child, dashboardPath));
        }
        return items;
    }

    /**
     * Represents a folder node in the tree with recursive children.
     */
    public static class FolderItem {
        private final String path;
        private final String name;
        private final String infoUrl;
        private final List<FolderItem> children;

        FolderItem(Node node, String dashboardPath) throws RepositoryException {
            this.path = node.getPath();
            this.name = node.hasProperty("jcr:title")
                    ? node.getProperty("jcr:title").getString()
                    : node.getName();
            this.infoUrl = dashboardPath + ".folder-info.html?folderPath="
                    + URLEncoder.encode(path, StandardCharsets.UTF_8);

            this.children = new ArrayList<>();
            NodeIterator kids = node.getNodes();
            while (kids.hasNext()) {
                Node kid = kids.nextNode();
                if ("sling:Folder".equals(kid.getPrimaryNodeType().getName())) {
                    children.add(new FolderItem(kid, dashboardPath));
                }
            }
        }

        public String getPath() { return path; }
        public String getName() { return name; }
        public String getInfoUrl() { return infoUrl; }
        public List<FolderItem> getChildren() { return children; }
        public boolean isHasChildren() { return !children.isEmpty(); }
    }
}
