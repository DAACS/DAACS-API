package com.daacs.framework.auth.oauth2;

import com.daacs.framework.core.ThreadLocalTrackingData;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.SessionedUser;
import com.daacs.model.event.EventType;
import com.daacs.model.event.UserEvent;
import com.daacs.service.UserService;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class UserTokenGranter extends AbstractTokenGranter {
    private static final Logger log = LoggerFactory.getLogger(UserTokenGranter.class);

    private static final String GRANT_TYPE = "password";

    private final UserService userService;
    private final TokenStore tokenStore;
    private final ShaPasswordEncoder encoder = new ShaPasswordEncoder();

    public UserTokenGranter(AuthorizationServerTokenServices tokenServices,
                            ClientDetailsService clientDetailsService,
                            OAuth2RequestFactory requestFactory,
                            UserService userService,
                            TokenStore tokenStore){

        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);

        this.userService = userService;
        this.tokenStore = tokenStore;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Map<String, String> parameters = tokenRequest.getRequestParameters();
        Authentication authentication;

        String usernameParam = parameters.get("username");
        String passwordParam = parameters.get("password");

        if(usernameParam == null || passwordParam == null){
            throw new InvalidGrantException("username and password parameters are required");
        }

        //Get the user.
        UserDetails userDetails;
        try{
            userDetails = userService.loadUserByUsername(usernameParam);
        }
        catch(FailureTypeException e){
            logger.error(e);
            throw new InvalidGrantException(e.getMessage());
        }

        //auth against db password.
        if (!userDetails.getPassword().equals(encoder.encodePassword(passwordParam, null))) {
            throw new InvalidGrantException("Incorrect password");
        }

        authentication = new AnonymousAuthenticationToken(GRANT_TYPE, userDetails, userDetails.getAuthorities());

        if (!authentication.isAuthenticated()) {
            throw new InvalidGrantException("Could not authenticate user");
        }

        OAuth2Request oAuth2Request = new OAuth2Request(
                tokenRequest.getRequestParameters(),
                tokenRequest.getClientId(),
                client.getAuthorities(),
                true,
                tokenRequest.getScope(),
                client.getResourceIds(),
                null, null, null);

        //record login event if this is indeed a login
        Collection<OAuth2AccessToken> existingTokens = tokenStore.findTokensByClientIdAndUserName(tokenRequest.getClientId(), userDetails.getUsername());
        int numExpiredTokens = existingTokens.stream().filter(OAuth2AccessToken::isExpired).collect(Collectors.toList()).size();

        if(numExpiredTokens == existingTokens.size()){
            String userId = ((SessionedUser) userDetails).getId();

            Try<Void> maybeEventRecorded = userService.recordEvent(userId, new UserEvent(EventType.LOGIN, ThreadLocalTrackingData.getTrackingData()));
            if(maybeEventRecorded.isFailure()){
                Throwable t = maybeEventRecorded.failed().get();
                log.error(t.getMessage(), t);
            }
        }

        return new OAuth2Authentication(oAuth2Request, authentication);
    }
}
