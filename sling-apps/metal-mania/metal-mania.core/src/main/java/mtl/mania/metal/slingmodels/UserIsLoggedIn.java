package mtl.mania.metal.slingmodels;

import javax.inject.Inject;

import org.apache.sling.api.SlingJakartaHttpServletRequest;
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
 * Usage in HTL:
 * </p>
 *
 * <pre>
 * {@code
 * <div data-sly-use.auth="mtl.mania.metal.slingmodels.UserIsLoggedIn"
 *      data-sly-set.editPath="${resource.path}.edit-form.html"
 *      data-zen-editable="true"
 *      data-sly-attribute.hx-get="${auth.loggedIn ? editPath : false}"
 *      data-sly-attribute.hx-trigger="${auth.loggedIn ? 'click' : false}"
 *      data-sly-attribute.hx-swap="${auth.loggedIn ? 'outerHTML' : false}">
 * }
 * </pre>
 */
@Model(adaptables = { SlingJakartaHttpServletRequest.class })
public class UserIsLoggedIn {

  private final boolean loggedIn;

  /**
   * Creates a new instance checking authentication status from the request.
   *
   * @param request the current Sling request
   */
  @Inject
  public UserIsLoggedIn(@Self SlingJakartaHttpServletRequest request) {
    String userId = request.getResourceResolver().getUserID();
    this.loggedIn = userId != null && !"anonymous".equals(userId);
  }

  /**
   * @return {@code true} if the current user is authenticated
   */
  public boolean isLoggedIn() {
    return loggedIn;
  }
}
