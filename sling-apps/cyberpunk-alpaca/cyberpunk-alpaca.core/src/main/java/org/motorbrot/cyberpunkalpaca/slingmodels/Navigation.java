package org.motorbrot.cyberpunkalpaca.slingmodels;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Builds the site navigation dynamically from the sibling pages of the current
 * page. Each direct child of the content root (e.g. {@code /content/cyberpunk-alpaca})
 * that carries a {@code jcr:content} node becomes a navigation entry, using the
 * child's {@code jcr:content/jcr:title} for the label and {@code &lt;name&gt;.html}
 * for the link. This keeps the nav in sync with authored content — add a page in
 * sample-content and it shows up automatically.
 */
@Model(adaptables = { SlingJakartaHttpServletRequest.class })
public class Navigation {

  private final SlingJakartaHttpServletRequest request;
  private final List<NavItem> items = new ArrayList<>();

  @Inject
  public Navigation(@Self SlingJakartaHttpServletRequest request) {
    this.request = request;
  }

  @PostConstruct
  protected void init() {
    Resource current = request.getResource();
    // Walk up from jcr:content (or a component) to the page node, then to its parent (content root).
    Resource pageNode = findPageNode(current);
    if (pageNode == null) {
      return;
    }
    Resource contentRoot = pageNode.getParent();
    if (contentRoot == null) {
      return;
    }
    String currentPath = pageNode.getPath();
    for (Resource child : contentRoot.getChildren()) {
      Resource jcrContent = child.getChild("jcr:content");
      if (jcrContent == null) {
        continue;
      }
      ValueMap vm = jcrContent.getValueMap();
      String title = vm.get("jcr:title", child.getName());
      String link = child.getPath() + ".html";
      boolean active = child.getPath().equals(currentPath);
      items.add(new NavItem(title, link, active));
    }
  }

  private Resource findPageNode(Resource resource) {
    Resource r = resource;
    while (r != null) {
      if (r.getChild("jcr:content") != null && r.getParent() != null) {
        return r;
      }
      r = r.getParent();
    }
    return null;
  }

  public List<NavItem> getItems() {
    return items;
  }

  /** A single navigation entry. */
  public static final class NavItem {
    private final String title;
    private final String link;
    private final boolean active;

    NavItem(String title, String link, boolean active) {
      this.title = title;
      this.link = link;
      this.active = active;
    }

    public String getTitle() {
      return title;
    }

    public String getLink() {
      return link;
    }

    public boolean isActive() {
      return active;
    }
  }
}
