package com.daacs.unit.auth.oauth2

import com.daacs.framework.auth.oauth2.UserTokenGranter
import com.daacs.framework.core.ThreadLocalTrackingData
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.SessionedUser
import com.daacs.model.event.EventType
import com.daacs.model.event.UserEvent
import com.daacs.service.UserService
import com.lambdista.util.Try
import org.springframework.security.authentication.encoding.ShaPasswordEncoder
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException
import org.springframework.security.oauth2.provider.*
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices
import org.springframework.security.oauth2.provider.token.TokenStore
import spock.lang.Specification
/**
 * Created by chostetter on 7/18/16.
 */
class UserTokenGranterSpec extends Specification {
    UserTokenGranter userTokenGranter;
    UserService userService;
    Authentication authentication;
    TokenStore tokenStore;

    ClientDetails clientDetails
    TokenRequest tokenRequest

    ShaPasswordEncoder encoder = new ShaPasswordEncoder()

    def setup(){
        clientDetails = new BaseClientDetails()
        tokenRequest = new TokenRequest(["username": "abc123", "password": "123abc"], "web", ["read", "write"], "password")
        authentication = Mock(Authentication)
        userService = Mock(UserService)
        tokenStore = Mock(TokenStore)

        userTokenGranter = new UserTokenGranter(
                Mock(AuthorizationServerTokenServices),
                Mock(ClientDetailsService),
                Mock(OAuth2RequestFactory),
                userService,
                tokenStore)
    }

    def "getOAuth2Authentication: success"(){
        setup:
        ThreadLocalTrackingData.addTrackingData("key", "value")

        when:
        OAuth2Authentication oAuth2Authentication = userTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * userService.loadUserByUsername("abc123") >> new SessionedUser(new com.daacs.model.User("abc123", "123abc", "user", "name", true, ["STUDENT"], "abc123", "abc123"));
        1 * tokenStore.findTokensByClientIdAndUserName("web", "abc123") >> []

        then:
        1 * userService.recordEvent(_, _) >> { args ->
            UserEvent userEvent = args[1]
            assert userEvent.eventType == EventType.LOGIN
            assert userEvent.eventData == ["key":"value"]

            return new Try.Success<Void>(null)
        }

        then:
        notThrown(Exception)
        oAuth2Authentication != null
    }

    def "getOAuth2Authentication: no username param"(){
        setup:
        tokenRequest = new TokenRequest(["password": "123abc"], "web", ["read", "write"], "password")

        when:
        userTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        0 * userService.loadUserByUsername(_)

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: no password param"(){
        setup:
        tokenRequest = new TokenRequest(["username": "abc123"], "web", ["read", "write"], "password")

        when:
        userTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        0 * userService.loadUserByUsername(_)

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: user not found"(){
        setup:
        tokenRequest = new TokenRequest(["username": "abc123", "password": "123abc"], "web", ["read", "write"], "password")

        when:
        userTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * userService.loadUserByUsername(_) >> { throw new FailureTypeException("code", "detail", FailureType.NOT_RETRYABLE) }

        then:
        thrown(InvalidGrantException)
    }

    def "getOAuth2Authentication: bad password"(){
        when:
        userTokenGranter.getOAuth2Authentication(clientDetails, tokenRequest)

        then:
        1 * userService.loadUserByUsername("abc123") >> new User("abc123", encoder.encodePassword("aaaa", null), [new SimpleGrantedAuthority("STUDENT")]);

        then:
        thrown(InvalidGrantException)
    }
}