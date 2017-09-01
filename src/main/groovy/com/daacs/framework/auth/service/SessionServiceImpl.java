package com.daacs.framework.auth.service;

import com.daacs.model.SessionedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Created by alandistasio on 3/1/15.
 */
@Component
public class SessionServiceImpl implements SessionService {

    @Override
    public Optional<SessionedUser> getSessionedUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof SessionedUser){
                return Optional.of((SessionedUser) principal);
            }
        }

        return Optional.empty();
    }

    @Override
    public SessionedUser getRequiredSessionedUser() {
        Optional<SessionedUser> sessionedUserOptional = getSessionedUser();

        return sessionedUserOptional.orElseThrow(IllegalStateException::new);
    }

    @Override
    public String getUserId() {
        Optional<SessionedUser> user = getSessionedUser();
        if (user.isPresent()) {
           return user.get().getId();
        }

        return null;
    }
}
