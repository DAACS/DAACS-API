package com.daacs.service;

import com.lambdista.util.Try;
import oauth.signpost.exception.OAuthException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;


/**
 * Created by mgoldman on 2/26/19.
 */
public interface LtiService {
    Try<String> verifyLaunch(HttpServletRequest launchRequest);


    Try<Void> updateGrades(String userId);

    boolean isEnabled();
}
