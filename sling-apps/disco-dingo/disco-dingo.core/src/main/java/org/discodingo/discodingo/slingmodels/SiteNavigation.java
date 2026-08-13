package org.discodingo.discodingo.slingmodels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Builds the top navigation bar dynamically from the sibling pages of the
 * current resource's content root (i.e. every page directly under
 * {@code /content/disco-dingo}).
 *
 * <p>
 * Only pages that carry a {@code jcr:title} are considered "real" pages;
 * technical child nodes without a title are skipped. Order follows JCR child
 * order, which authors can control in the Composum node browser.
 * </p>
 */
@Model(adaptables = { Resource.class })
public class SiteNavigation {

  private final List<NavItem> items = new ArrayList<>();

  /**
   * Builds the navigation entries from the resource's grandparent (the
   * content root), walking every child page and reading its {@code jcr:content}.
   *
   * @param resource the current resource (typically the page's jcr:content node)
   */
  @Inject
  public SiteNavigation(@Self Resource resource) {
    Resource contentRoot = findContentRoot(resource);
    if (contentRoot == null) {
      return;
    }
    Iterator<Resource> children = contentRoot.listChildren();
    while (children.hasNext()) {
      Resource page = children.next();
      Resource jcrContent = page.getChild("jcr:content");
      if (jcrContent == null) {
        continue;
      }
      ValueMap props = jcrContent.getValueMap();
      String title = props.get("jcr:title", String.class);
      if (title == null || title.isBlank()) {
        continue;
      }
      items.add(new NavItem(title, "/" + page.getPath().replaceFirst("^/", "") + ".html"));
    }
  }

  /**
   * Walks up from the current resource to the {@code /content/disco-dingo}
   * root so navigation works no matter which page it is rendered on.
   *
   * @param resource the starting resource
   * @return the content root resource, or {@code null} if not found
   */
  private static Resource findContentRoot(Resource resource) {
    Resource current = resource;
    while (current != null) {
      if ("disco-dingo".equals(current.getName())
          && current.getParent() != null
          && "content".equals(current.getParent().getName())) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Returns the navigation items in JCR child order.
   *
   * @return the list of navigation items
   */
  public List<NavItem> getItems() {
    return items;
  }

  /** A single navigation link: a display title and its target URL. */
  public static class NavItem {
    private final String title;
    private final String href;

    NavItem(String title, String href) {
      this.title = title;
      this.href = href;
    }

    public String getTitle() {
      return title;
    }

    public String getHref() {
      return href;
    }
  }
}
