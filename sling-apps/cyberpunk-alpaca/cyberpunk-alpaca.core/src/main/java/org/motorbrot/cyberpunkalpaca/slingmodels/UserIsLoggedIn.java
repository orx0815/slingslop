package org.motorbrot.cyberpunkalpaca.slingmodels;

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
 */
@Model(adaptables = { SlingJakartaHttpServletRequest.class })
public class UserIsLoggedIn {

  private final boolean loggedIn;

  @Inject
  public UserIsLoggedIn(@Self SlingJakartaHttpServletRequest request) {
    String userId = request.getResourceResolver().getUserID();
    this.loggedIn = userId != null && !"anonymous".equals(userId);
  }

  public boolean isLoggedIn() {
    return loggedIn;
  }
}
