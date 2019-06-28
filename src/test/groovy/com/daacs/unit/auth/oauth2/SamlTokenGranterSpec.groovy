package com.daacs.unit.auth.oauth2

import com.daacs.framework.auth.repository.AuthenticationRepository
import com.daacs.framework.auth.oauth2.SamlTokenGranter
import com.daacs.framework.exception.NotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException
import org.springframework.security.oauth2.provider.*
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices
import spock.lang.Specification

/**
 * Created by chostetter on 7/18/16.
 */
class SamlTokenGranterSpec extends Specification {
    AuthenticationRepository samlAuthenticationRepository;
    SamlTokenGranter samlTokenGranter;
    Authentication authentication;

    ClientDetails clientDetails
    TokenRequest tokenRequest

    def setup(){
        clientDetails = new BaseClientDetails()
        tokenRequest = new TokenRequest(["token": "abc123"], "web", ["read", "write"], "saml")
        authentication = Mock(Authentication)
        samlAuthenticationRepository = Mock(AuthenticationRepository)

        samlTokenGranter = new SamlTokenGranter(samlAuthenticationRepository,
                Mock(AuthorizationServerTokenServices),
                Mock(ClientDetailsService),
                Mock(OAuth2RequestFactory))
    }

    def "getOAuth2Authentication: success"(){
        when:
        OAuth2Authentication oAuth2Authentication = samlTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * samlAuthenticationRepository.retrieveAuthentication("abc123") >> authentication
        1 * authentication.isAuthenticated() >> true

        then:
        notThrown(Exception)
        oAuth2Authentication != null
    }

    def "getOAuth2Authentication: no token param"(){
        setup:
        tokenRequest = new TokenRequest([:], "web", ["read", "write"], "saml")

        when:
        samlTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        0 * samlAuthenticationRepository.retrieveAuthentication(_)
        0 * authentication.isAuthenticated()

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: token not found"(){
        when:
        samlTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * samlAuthenticationRepository.retrieveAuthentication("abc123") >> { throw new NotFoundException("not found") }
        0 * authentication.isAuthenticated()

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: not authenticated"(){
        when:
        OAuth2Authentication oAuth2Authentication = samlTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * samlAuthenticationRepository.retrieveAuthentication("abc123") >> authentication
        1 * authentication.isAuthenticated() >> false

        then:
        thrown(InvalidGrantException)
    }
}