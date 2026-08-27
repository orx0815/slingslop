package org.motorbrot.sling.dma.slingmodels;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Model backing the {@code folder-info} selector on the dashboard page.
 * Provides folder metadata for the metadata panel.
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class FolderInfoModel {

    private static final Logger LOG = LoggerFactory.getLogger(FolderInfoModel.class);
    private static final String ASSETS_ROOT = "/content/motorbrot/dml/assets";

    @SlingObject
    private SlingJakartaHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private boolean valid;
    private boolean root;
    private String displayName;
    private int assetCount;
    private int subfolderCount;
    private List<String> assetNames;
    private String folderPath;
    private String deleteUrl;
    private boolean canDelete;

    @PostConstruct
    protected void init() {
        assetNames = new ArrayList<>();
        folderPath = request.getParameter("folderPath");

        if (folderPath == null || !folderPath.startsWith(ASSETS_ROOT)) {
            valid = false;
            return;
        }

        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null || !session.nodeExists(folderPath)) {
                valid = false;
                return;
            }

            valid = true;
            root = ASSETS_ROOT.equals(folderPath);

            Node folder = session.getNode(folderPath);
            displayName = root ? "Library"
                    : (folder.hasProperty("jcr:title")
                            ? folder.getProperty("jcr:title").getString()
                            : folder.getName());

            NodeIterator children = folder.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if ("sling:Folder".equals(child.getPrimaryNodeType().getName())) {
                    subfolderCount++;
                } else if (child.hasProperty("isAsset")) {
                    String filename = child.hasNode("metadata")
                            ? child.getNode("metadata").getProperty("filename").getString()
                            : child.getName();
                    assetNames.add(filename);
                }
            }
            assetCount = assetNames.size();

            String dashboardPath = request.getResource().getPath();
            deleteUrl = dashboardPath + ".delete-folder.html";
            canDelete = !root && subfolderCount == 0;

        } catch (Exception e) {
            LOG.error("Error loading folder info for path: {}", folderPath, e);
            valid = false;
        }
    }

    public boolean isValid() { return valid; }
    public boolean isRoot() { return root; }
    public String getDisplayName() { return displayName; }
    public int getAssetCount() { return assetCount; }
    public int getSubfolderCount() { return subfolderCount; }
    public List<String> getAssetNames() { return Collections.unmodifiableList(assetNames); }
    public boolean isHasAssets() { return !assetNames.isEmpty(); }
    public boolean isHasSubfolders() { return subfolderCount > 0; }
    public String getFolderPath() { return folderPath; }
    public String getDeleteUrl() { return deleteUrl; }
    public boolean isCanDelete() { return canDelete; }

    /** Pre-built hx-vals JSON for the delete button. */
    public String getDeleteHxVals() {
        return "{\"folderPath\": \"" + folderPath.replace("\"", "\\\"") + "\"}";
    }

    /** Label like "1 asset" or "3 assets" for use in the delete confirmation. */
    public String getAssetLabel() {
        return assetCount == 1 ? "1 asset" : assetCount + " assets";
    }

    /** Label like "1 subfolder" or "3 subfolders" for use in the refusal message. */
    public String getSubfolderLabel() {
        return subfolderCount == 1 ? "1 subfolder" : subfolderCount + " subfolders";
    }
}
