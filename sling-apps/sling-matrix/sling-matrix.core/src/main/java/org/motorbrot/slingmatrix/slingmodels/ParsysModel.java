package org.motorbrot.slingmatrix.slingmodels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Backing model for the {@code sling-matrix/components/parsys} paragraph system.
 *
 * <p>
 * Reads the {@code allowedComponents} multi-value property from the parsys
 * container node and resolves each entry to its component definition under
 * {@code /apps/sling-matrix/components/} so the "add component" picker can show a
 * human-friendly title and description. Entries may be either a bare component
 * name (e.g. {@code feature-image}) or a full resource type
 * (e.g. {@code sling-matrix/components/feature-image}).
 * </p>
 */
@Model(adaptables = SlingJakartaHttpServletRequest.class)
public class ParsysModel {

    /** Default namespace for bare component names. */
    static final String COMPONENT_RESOURCE_TYPE_PREFIX = "sling-matrix/components/";

    private final boolean loggedIn;
    private final List<AllowedComponent> allowedComponents = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    @Inject
    public ParsysModel(@Self SlingJakartaHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        String userId = resolver.getUserID();
        this.loggedIn = userId != null && !"anonymous".equals(userId);

        Resource resource = request.getResource();
        String[] names = resource.getValueMap().get("allowedComponents", String[].class);
        if (names != null) {
            for (String entry : names) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                allowedComponents.add(resolve(resolver, entry.trim()));
            }
        }

        List<Resource> children = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            children.add(child);
        }
        for (int i = 0; i < children.size(); i++) {
            Resource child = children.get(i);
            String prevName = i > 0 ? children.get(i - 1).getName() : null;
            String nextName = i < children.size() - 1 ? children.get(i + 1).getName() : null;
            items.add(new Item(child.getName(), child.getPath(), prevName, nextName));
        }
    }

    private static AllowedComponent resolve(ResourceResolver resolver, String entry) {
        String resourceType = entry.contains("/") ? entry : COMPONENT_RESOURCE_TYPE_PREFIX + entry;
        String name = resourceType.substring(resourceType.lastIndexOf('/') + 1);

        String title = name;
        String description = null;
        Resource def = resolver.getResource("/apps/" + resourceType);
        if (def != null) {
            ValueMap defProps = def.getValueMap();
            title = defProps.get("jcr:title", name);
            description = defProps.get("jcr:description", String.class);
        }
        return new AllowedComponent(name, resourceType, title, description);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isHasAllowedComponents() {
        return !allowedComponents.isEmpty();
    }

    public List<AllowedComponent> getAllowedComponents() {
        return allowedComponents;
    }

    /** The child components in order, each enriched with move/delete metadata. */
    public List<Item> getItems() {
        return items;
    }

    /**
     * A single child component of the parsys, carrying the sibling names needed
     * to build {@code :order=before/after} reorder requests against the default
     * Sling POST servlet. {@code prevName == null} means it's the first child
     * (cannot move up); {@code nextName == null} means it's the last child
     * (cannot move down).
     */
    public static final class Item {
        private final String name;
        private final String path;
        private final String prevName;
        private final String nextName;

        Item(String name, String path, String prevName, String nextName) {
            this.name = name;
            this.path = path;
            this.prevName = prevName;
            this.nextName = nextName;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getPrevName() {
            return prevName;
        }

        public String getNextName() {
            return nextName;
        }

        public boolean isFirst() {
            return prevName == null;
        }

        public boolean isLast() {
            return nextName == null;
        }

        public boolean isMovable() {
            return prevName != null || nextName != null;
        }
    }

    /** A single entry of the parsys allow-list, enriched with display metadata. */
    public static final class AllowedComponent {
        private final String name;
        private final String resourceType;
        private final String title;
        private final String description;

        AllowedComponent(String name, String resourceType, String title, String description) {
            this.name = name;
            this.resourceType = resourceType;
            this.title = title;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }
}
