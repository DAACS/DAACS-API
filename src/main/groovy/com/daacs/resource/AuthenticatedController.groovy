package com.daacs.resource

import com.daacs.framework.auth.service.SessionService
import com.daacs.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException
/**
 * Created by chostetter on 7/5/16.
 */

public abstract class AuthenticatedController {
    @Autowired
    private SessionService sessionService;

    protected final String ROLE_ADMIN = "ROLE_ADMIN";
    protected final String ROLE_ADVISOR = "ROLE_ADVISOR";
    protected final String ROLE_STUDENT = "ROLE_STUDENT";
    protected final String ROLE_INSTRUCTOR = "ROLE_INSTRUCTOR";
    protected final String ROLE_SYSTEM = "ROLE_SYSTEM";

    protected String determineUserId(String passedInUserId){
        if(passedInUserId != null){
            if(hasRole(getLoggedInUser(), [ROLE_ADMIN, ROLE_ADVISOR])){
                return passedInUserId;
            }

            throw new UnauthorizedUserException("You do not have permissions for this user");
        }

        return getLoggedInUser().getId();
    }

    protected User getLoggedInUser(){
        return new User(sessionService.getRequiredSessionedUser());
    }

    protected static boolean hasRole(User user, List<String> roles){
        return user.getRoles().find{ roles.contains(it) } != null
    }

    protected void checkPermissions(List<String> roles){
        if(!hasRole(getLoggedInUser(), roles)){
            throw new UnauthorizedUserException("You do not have permissions for this endpoint");
        }
    }

    protected static void checkPermissions(User user, List<String> roles){
        if(!hasRole(user, roles)){
            throw new UnauthorizedUserException("You do not have permissions for this endpoint");
        }
    }
}
