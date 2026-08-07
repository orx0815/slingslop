package org.motorbrot.cyberpunkalpaca.slingmodels;

import javax.inject.Inject;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/**
 * Exposes whether the current request is made by an authenticated (non-anonymous)
 * user. Used by HTL templates to gate the inline-editing UI so that only logged-in
 * authors receive the editor bundle and the zen-editable hooks.
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
