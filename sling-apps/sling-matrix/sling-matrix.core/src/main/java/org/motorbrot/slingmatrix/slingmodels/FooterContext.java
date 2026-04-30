package org.motorbrot.slingmatrix.slingmodels;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Provides context for the footer component, including homepage detection
 * and access to the shared footer resource.
 *
 * <p>
 * This model traverses up the resource tree to find the homepage resource
 * (identified by {@code sling:resourceType="sling-matrix/pages/homepage"}),
 * determines if the current page is the homepage, and provides access to
 * the homepage's footer resource for shared footer content.
 * </p>
 *
 * <p>
 * Usage example in HTL:
 * </p>
 *
 * <pre>
 * {@code
 * <footer data-sly-use.footerCtx="org.motorbrot.slingmatrix.slingmodels.FooterContext"
 *         data-zen-editable="${footerCtx.homepage}">
 *   <p>${footerCtx.footerData.copyrightText}</p>
 * </footer>
 * }
 * </pre>
 */
@Model(adaptables = { Resource.class })
public class FooterContext {

  @Self
  private Resource resource;

  private Resource homepageResource;
  private boolean isHomepage;
  private Resource homeFooterResource;

  /**
   * Initializes the model by finding the homepage resource and determining
   * the current page context.
   */
  @PostConstruct
  protected void init() {
    // Traverse up to find the page's jcr:content node
    Resource currentPageContent = findPageContent(resource);

    if (currentPageContent != null) {
      // Find the homepage by traversing up from the current page content
      homepageResource = findHomepage(currentPageContent);

      // Check if we are on the homepage
      isHomepage = homepageResource != null &&
                   currentPageContent.getPath().equals(homepageResource.getPath());

      // Get the homepage footer resource if homepage exists
      if (homepageResource != null) {
        Resource homePage = homepageResource.getParent(); // Get the page node
        if (homePage != null) {
          homeFooterResource = homePage.getChild("jcr:content/footer");
        }
      }
    }
  }

  /**
   * Traverses up the resource tree to find the containing page's jcr:content node.
   *
   * @param res the starting resource
   * @return the jcr:content resource of the containing page, or null if not found
   */
  private Resource findPageContent(Resource res) {
    Resource current = res;
    while (current != null) {
      if ("jcr:content".equals(current.getName())) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Traverses up the resource tree to find the homepage resource.
   * The homepage is identified by having sling:resourceType="sling-matrix/pages/homepage".
   *
   * @param pageContent the current page's jcr:content resource
   * @return the homepage's jcr:content resource, or null if not found
   */
  private Resource findHomepage(Resource pageContent) {
    Resource current = pageContent;

    while (current != null) {
      // Check if this jcr:content has the homepage resource type
      if (isHomepageResourceType(current)) {
        return current;
      }

      // Move up to parent page's jcr:content
      Resource parent = current.getParent(); // Get the page node
      if (parent != null) {
        parent = parent.getParent(); // Get the parent page node
        if (parent != null) {
          current = parent.getChild("jcr:content");
        } else {
          current = null;
        }
      } else {
        current = null;
      }
    }

    return null;
  }

  /**
   * Checks if a resource has the homepage resource type.
   *
   * @param res the resource to check
   * @return true if the resource type is "sling-matrix/pages/homepage"
   */
  private boolean isHomepageResourceType(Resource res) {
    if (res == null) {
      return false;
    }
    String resourceType = res.getResourceType();
    return "sling-matrix/pages/homepage".equals(resourceType);
  }

  /**
   * Checks if the current page is the homepage.
   *
   * @return true if the current page is the homepage
   */
  public boolean isHomepage() {
    return isHomepage;
  }

  /**
   * Gets the homepage's jcr:content resource.
   *
   * @return the homepage resource, or null if not found
   */
  public Resource getHomepageResource() {
    return homepageResource;
  }

  /**
   * Gets the homepage's footer resource.
   *
   * @return the footer resource from the homepage, or null if not found
   */
  public Resource getHomeFooterResource() {
    return homeFooterResource;
  }

  /**
   * Gets the footer data to display.
   * If on the homepage, returns the current resource's properties.
   * Otherwise, returns the homepage footer's properties (or current if not available).
   *
   * @return the ValueMap containing footer properties
   */
  public ValueMap getFooterData() {
    if (isHomepage) {
      // On homepage, use local properties
      return resource.getValueMap();
    } else {
      // On other pages, use homepage footer if available, otherwise use local properties
      if (homeFooterResource != null) {
        return homeFooterResource.getValueMap();
      } else {
        return resource.getValueMap();
      }
    }
  }
}
