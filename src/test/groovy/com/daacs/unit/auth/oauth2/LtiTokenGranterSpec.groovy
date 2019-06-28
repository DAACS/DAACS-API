package com.daacs.unit.auth.oauth2

import com.daacs.framework.auth.oauth2.LtiTokenGranter
import com.daacs.framework.auth.repository.AuthenticationRepository
import com.daacs.framework.exception.NotFoundException
import com.daacs.model.SessionedUser
import com.daacs.model.User
import com.daacs.service.UserService
import com.lambdista.util.Try
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException
import org.springframework.security.oauth2.provider.*
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices
import spock.lang.Specification

/**
 * Created by mgoldman on 4/10/19.
 */
class LtiTokenGranterSpec extends Specification {
    AuthenticationRepository ltiAuthenticationRepository;
    LtiTokenGranter ltiTokenGranter
    Authentication authentication

    ClientDetails clientDetails
    TokenRequest tokenRequest
    UserService userService


    def setup(){
        clientDetails = new BaseClientDetails()
        tokenRequest = new TokenRequest(["token": "abc123"], "web", ["read", "write"], "lti")
        authentication = Mock(Authentication)
        ltiAuthenticationRepository = Mock(AuthenticationRepository)
        userService = Mock(UserService)

        ltiTokenGranter = new LtiTokenGranter(ltiAuthenticationRepository,
                Mock(AuthorizationServerTokenServices),
                Mock(ClientDetailsService),
                Mock(OAuth2RequestFactory),
                userService)
    }

    def "getOAuth2Authentication: success"(){
        when:
        OAuth2Authentication oAuth2Authentication = ltiTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * ltiAuthenticationRepository.retrieveAuthentication("abc123") >> authentication
        1 * authentication.isAuthenticated() >> true
        1 * authentication.getPrincipal() >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligratorsRcoo'))
        1 * userService.recordEvent(_,_) >> new Try.Success<Void>(null)

        then:
        notThrown(Exception)
        oAuth2Authentication != null
    }

   def "recordLogin: failure"(){
        when:
        OAuth2Authentication oAuth2Authentication = ltiTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * ltiAuthenticationRepository.retrieveAuthentication("abc123") >> authentication
        1 * authentication.isAuthenticated() >> true
        1 * authentication.getPrincipal() >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligratorsRcoo'))
        1 * userService.recordEvent(_,_) >> new Try.Failure<Void>(null)

        then:
        thrown(Exception)
    }

    def "getOAuth2Authentication: no token param"(){
        setup:
        tokenRequest = new TokenRequest([:], "web", ["read", "write"], "saml")

        when:
        ltiTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        0 * ltiAuthenticationRepository.retrieveAuthentication(_)
        0 * authentication.isAuthenticated()
        0 * authentication.getPrincipal()
        0 * userService.recordEvent(_,_)

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: token not found"(){
        when:
        ltiTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * ltiAuthenticationRepository.retrieveAuthentication("abc123") >> { throw new NotFoundException("not found") }
        0 * authentication.isAuthenticated()
        0 * authentication.getPrincipal()
        0 * userService.recordEvent(_,_)

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: not authenticated"(){
        when:
        OAuth2Authentication oAuth2Authentication = ltiTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * ltiAuthenticationRepository.retrieveAuthentication("abc123") >> authentication
        1 * authentication.isAuthenticated() >> false
        0 * authentication.getPrincipal()
        0 * userService.recordEvent(_,_)

        then:
        thrown(InvalidGrantException)
    }
}