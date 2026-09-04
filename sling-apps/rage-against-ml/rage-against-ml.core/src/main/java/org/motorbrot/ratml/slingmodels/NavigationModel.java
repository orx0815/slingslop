package org.motorbrot.ratml.slingmodels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.motorbrot.ratml.link.LinkBuilder;

/**
 * Backing model for the {@code ratml/components/navigation} component.
 *
 * <p>
 * Builds the site navigation dynamically from the content tree instead of a
 * hand-maintained list of links: starting from the homepage it lists its child
 * sections (level 1) and their child pages (level 2). Titles come from each
 * page's {@code jcr:content/jcr:title} (or a section folder's {@code jcr:title}),
 * and every link is externalized through the {@link LinkBuilder} service so it
 * honours resource-resolver mapping.
 * </p>
 *
 * <p>
 * The homepage is located by walking up from the current page to the ancestor
 * whose {@code jcr:content} carries a {@code .../homepage} resource type.
 * </p>
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class NavigationModel {

    private static final String NN_CONTENT = "jcr:content";
    private static final String PN_TITLE = "jcr:title";
    private static final String PN_RESOURCE_TYPE = "sling:resourceType";
    private static final String RT_PAGE = "ratml/pages/page";
    private static final String RT_HOMEPAGE_SUFFIX = "/homepage";

    private final List<NavItem> items = new ArrayList<>();
    private String homeUrl;
    private String homeTitle;
    private boolean homeActive;

    @Inject
    public NavigationModel(@Self SlingJakartaHttpServletRequest request,
                           @OSGiService LinkBuilder linkBuilder) {
        Resource start = request.getResource();
        Resource currentPage = findEnclosingPage(start);
        Resource home = findHome(currentPage != null ? currentPage : start);
        if (home == null) {
            return;
        }
        String currentPath = currentPage != null ? currentPage.getPath() : null;

        this.homeUrl = linkBuilder.toUrl(home);
        this.homeTitle = title(home);
        this.homeActive = home.getPath().equals(currentPath);

        for (Resource child : home.getChildren()) {
            if (isNavigable(child)) {
                items.add(buildItem(child, currentPath, linkBuilder));
            }
        }
    }

    private static NavItem buildItem(Resource resource, String currentPath, LinkBuilder linkBuilder) {
        List<NavItem> children = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            if (isNavigable(child)) {
                children.add(buildItem(child, currentPath, linkBuilder));
            }
        }

        String url;
        if (isPage(resource)) {
            url = linkBuilder.toUrl(resource);
        } else if (!children.isEmpty()) {
            // Section folder without its own content: link to its first page.
            url = children.get(0).getUrl();
        } else {
            url = null;
        }

        boolean active = resource.getPath().equals(currentPath);
        return new NavItem(title(resource), url, active, children);
    }

    /** A resource is a page when it has a {@code jcr:content} child node. */
    private static boolean isPage(Resource resource) {
        return resource.getChild(NN_CONTENT) != null;
    }

    /** A resource belongs in the navigation when it is (or contains) a page. */
    private static boolean isNavigable(Resource resource) {
        String name = resource.getName();
        if (name.startsWith("jcr:") || name.startsWith("rep:") || name.startsWith("sling:")) {
            return false;
        }
        if (isPage(resource)) {
            return true;
        }
        for (Resource child : resource.getChildren()) {
            if (isNavigable(child)) {
                return true;
            }
        }
        return false;
    }

    private static String title(Resource resource) {
        Resource content = resource.getChild(NN_CONTENT);
        if (content != null) {
            String contentTitle = content.getValueMap().get(PN_TITLE, String.class);
            if (contentTitle != null && !contentTitle.isBlank()) {
                return contentTitle;
            }
        }
        String ownTitle = resource.getValueMap().get(PN_TITLE, String.class);
        if (ownTitle != null && !ownTitle.isBlank()) {
            return ownTitle;
        }
        return resource.getName();
    }

    /** Walks up from the given resource to the page node that encloses it. */
    private static Resource findEnclosingPage(Resource resource) {
        Resource r = resource;
        while (r != null) {
            if (isPage(r) || RT_PAGE.equals(r.getResourceType())) {
                return r;
            }
            r = r.getParent();
        }
        return null;
    }

    /**
     * Walks up from the current page to the site homepage — the ancestor whose
     * {@code jcr:content} carries a {@code .../homepage} resource type. Falls back
     * to the highest page ancestor when no explicit homepage marker is found.
     */
    private static Resource findHome(Resource page) {
        Resource r = page;
        Resource highestPage = null;
        while (r != null) {
            Resource content = r.getChild(NN_CONTENT);
            if (content != null) {
                String rt = content.getValueMap().get(PN_RESOURCE_TYPE, "");
                if (rt.endsWith(RT_HOMEPAGE_SUFFIX)) {
                    return r;
                }
                highestPage = r;
            }
            r = r.getParent();
        }
        return highestPage;
    }

    /** The top-level navigation entries (site sections). */
    public List<NavItem> getItems() {
        return items;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public String getHomeTitle() {
        return homeTitle;
    }

    public boolean isHomeActive() {
        return homeActive;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** A navigation entry with an optional list of child entries. */
    public static final class NavItem {
        private final String title;
        private final String url;
        private final boolean active;
        private final List<NavItem> children;

        NavItem(String title, String url, boolean active, List<NavItem> children) {
            this.title = title;
            this.url = url;
            this.active = active;
            this.children = children;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public boolean isActive() {
            return active;
        }

        public List<NavItem> getChildren() {
            return children;
        }

        public boolean isHasChildren() {
            return !children.isEmpty();
        }
    }
}
