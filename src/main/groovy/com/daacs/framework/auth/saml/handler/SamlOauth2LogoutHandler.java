package com.daacs.framework.auth.saml.handler;

import com.daacs.model.SessionedUser;
import com.daacs.model.event.EventType;
import com.daacs.model.event.UserEvent;
import com.daacs.service.UserService;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by chostetter on 6/29/16.
 */
public class SamlOauth2LogoutHandler implements LogoutHandler {
    private static final Logger log = LoggerFactory.getLogger(SamlOauth2LogoutHandler.class);

    private static final String WEB_CLIENT_ID = "web";

    private TokenStore tokenStore;
    private UserService userService;

    public SamlOauth2LogoutHandler(TokenStore tokenStore, UserService userService) {
        this.tokenStore = tokenStore;
        this.userService = userService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if(authentication != null){
            SessionedUser sessionedUser = (SessionedUser) authentication.getPrincipal();
            String username = sessionedUser.getUsername();

            Collection<OAuth2AccessToken> oAuth2AccessTokens = tokenStore.findTokensByClientIdAndUserName(WEB_CLIENT_ID, username);

            for(OAuth2AccessToken oAuth2AccessToken : oAuth2AccessTokens){
                tokenStore.removeAccessToken(oAuth2AccessToken);
            }

            Try<Void> maybeEventRecorded = userService.recordEvent(sessionedUser.getId(), new UserEvent(EventType.LOGOUT, new HashMap<>()));
            if(maybeEventRecorded.isFailure()){
                Throwable t = maybeEventRecorded.failed().get();
                log.error(t.getMessage(), t);
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}