package com.daacs.framework.auth.oauth2;

import com.daacs.framework.auth.repository.AuthenticationRepository;
import com.daacs.framework.core.ThreadLocalTrackingData;
import com.daacs.framework.exception.NotFoundException;
import com.daacs.model.SessionedUser;
import com.daacs.model.event.EventType;
import com.daacs.model.event.UserEvent;
import com.daacs.service.UserService;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.Map;

/**
 * Created by mgoldman on 4/3/19.
 */
@Configurable
public class LtiTokenGranter extends AbstractTokenGranter {
    private static final Logger log = LoggerFactory.getLogger(LtiTokenGranter.class);

    private static final String GRANT_TYPE = "lti";

    private final UserService userService;

    private final AuthenticationRepository ltiAuthenticationRepository;

    public LtiTokenGranter(
            AuthenticationRepository ltiAuthenticationRepository,
            AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService,
            OAuth2RequestFactory requestFactory,
            UserService userService) {

        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);

        this.userService = userService;
        this.ltiAuthenticationRepository = ltiAuthenticationRepository;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Map<String, String> parameters = tokenRequest.getRequestParameters();
        Authentication authentication;

        String tokenParam = parameters.get("token");

        if (tokenParam == null) {
            throw new InvalidGrantException("token parameter is required");
        }

        try {
            authentication = ltiAuthenticationRepository.retrieveAuthentication(tokenParam);
        } catch (NotFoundException nfe) {
            throw new InvalidGrantException(nfe.getMessage());
        }

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


        //record login event
        String userId = ((SessionedUser) authentication.getPrincipal()).getId();
        Try<Void> maybeEventRecorded = userService.recordEvent(userId, new UserEvent(EventType.LOGIN, ThreadLocalTrackingData.getTrackingData()));
        if (maybeEventRecorded.isFailure()) {
            Throwable t = maybeEventRecorded.failed().get();
            log.error(t.getMessage(), t);
        }

        return new OAuth2Authentication(oAuth2Request, authentication);
    }
}
