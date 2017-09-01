package com.daacs.framework.auth.oauth2;

import com.daacs.framework.exception.NotFoundException;
import com.daacs.framework.auth.saml.repository.AuthenticationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import java.util.Map;

public class SamlTokenGranter extends AbstractTokenGranter {

    private static final String GRANT_TYPE = "saml";

    private final AuthenticationRepository samlAuthenticationRepository;

    public SamlTokenGranter(AuthenticationRepository samlAuthenticationRepository,
                            AuthorizationServerTokenServices tokenServices,
                            ClientDetailsService clientDetailsService,
                            OAuth2RequestFactory requestFactory){

        super(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
        this.samlAuthenticationRepository = samlAuthenticationRepository;
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Map<String, String> parameters = tokenRequest.getRequestParameters();
        Authentication authentication;

        String tokenParam = parameters.get("token");

        if(tokenParam == null){
            throw new InvalidGrantException("token parameter is required");
        }

        try{
            authentication = samlAuthenticationRepository.retrieveAuthentication(tokenParam);
        }
        catch(NotFoundException nfe){
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

        return new OAuth2Authentication(oAuth2Request, authentication);
    }
}
