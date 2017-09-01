package com.daacs.framework.auth.service;


import com.daacs.model.SessionedUser;

import java.util.Optional;

/**
 * Created by alandistasio on 3/1/15.
 */
public interface SessionService {
    Optional<SessionedUser> getSessionedUser();
    SessionedUser getRequiredSessionedUser();
    String getUserId();
}
