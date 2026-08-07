package org.motorbrot.alfvsagent.slingmodels;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

/**
 * Simple Sling Model to determine if the current user is logged in.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UserIsLoggedIn {

    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * Returns true if the current request has an authenticated user.
     */
    public boolean isLoggedIn() {
        return request.getUserPrincipal() != null;
    }
}
