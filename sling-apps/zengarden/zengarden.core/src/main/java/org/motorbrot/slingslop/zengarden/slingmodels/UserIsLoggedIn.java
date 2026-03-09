package org.motorbrot.slingslop.zengarden.slingmodels;

import javax.inject.Inject;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Checks whether the current user is authenticated (i.e. not anonymous).
 *
 * <p>
 * Used in HTL templates to guard editor CSS/JS includes and {@code hx-*}
 * attributes so that anonymous visitors never receive editor markup.
 * </p>
 *
 * <p>
 * Usage example in HTL:
 * </p>
 * 
 * <pre>
 * {@code
 * <div data-sly-use.auth="org.motorbrot.slingslop.zengarden.slingmodels.UserIsLoggedIn"
 *      data-sly-set.editPath="${resource.path}.edit-form.html"
 *      data-sly-attribute.hx-get="${auth.loggedIn ? editPath : false}"
 *      data-sly-attribute.hx-trigger="${auth.loggedIn ? 'click' : false}"
 *      data-sly-attribute.hx-swap="${auth.loggedIn ? 'outerHTML' : false}">
 * }
 * </pre>
 * 
 */
@Model(adaptables = { SlingJakartaHttpServletRequest.class })
public class UserIsLoggedIn {

  private final boolean loggedIn;

  /**
   * Creates a new instance with the provided request.
   *
   * @param request the Sling request to check authentication status
   */
  @Inject
  public UserIsLoggedIn(@Self SlingJakartaHttpServletRequest request) {
    String userId = request.getResourceResolver().getUserID();
    boolean isAuthenticated = userId != null && !"anonymous".equals(userId);
    
    // Silly hack to show editmode also for anon 
    String pageResourceType = getPageResourceType(request.getResource());
    boolean isHomepage = "slingslop/zengarden/pages/homepage".equals(pageResourceType);
    
    this.loggedIn = isAuthenticated || isHomepage;
  }

  /**
   * Walks up the resource tree to find the enclosing {@code jcr:content} node
   * and returns its resource type (which holds the page template type).
   *
   * @param resource the current resource to start from
   * @return the resource type of the enclosing {@code jcr:content}, or {@code null} if not found
   */
  private static String getPageResourceType(Resource resource) {
    Resource current = resource;
    while (current != null) {
      if ("jcr:content".equals(current.getName())) {
        return current.getResourceType();
      }
      current = current.getParent();
    }
    return null;
  }

  /**
   * Checks if the current user is authenticated.
   *
   * @return {@code true} when the current user is authenticated
   */
  public boolean isLoggedIn() {
    return loggedIn;
  }
}
