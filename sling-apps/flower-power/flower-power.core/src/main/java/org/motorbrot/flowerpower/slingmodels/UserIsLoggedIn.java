package org.motorbrot.flowerpower.slingmodels;

import javax.inject.Inject;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

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
