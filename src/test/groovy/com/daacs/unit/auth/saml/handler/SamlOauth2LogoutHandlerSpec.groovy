package com.daacs.unit.auth.saml.handler

import com.daacs.framework.auth.saml.handler.SamlOauth2LogoutHandler
import com.daacs.model.SessionedUser
import com.daacs.model.User
import com.daacs.model.UserFieldConfig
import com.daacs.model.event.EventType
import com.daacs.model.event.UserEvent
import com.daacs.service.UserService
import com.lambdista.util.Try
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.TokenStore
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by chostetter on 6/23/16.
 */
class SamlOauth2LogoutHandlerSpec extends Specification{

    TokenStore tokenStore
    UserService userService

    HttpServletRequest httpServletRequest
    HttpServletResponse httpServletResponse

    OAuth2Authentication oAuth2Authentication
    Authentication authentication

    SamlOauth2LogoutHandler samlOauth2LogoutHandler

    String clientId = "web"
    String username = "curt"


    def setup(){
        tokenStore = Mock(TokenStore)
        userService = Mock(UserService)

        httpServletRequest = Mock(HttpServletRequest)
        httpServletResponse = Mock(HttpServletResponse)

        oAuth2Authentication = Mock(OAuth2Authentication)

        authentication = Mock(Authentication)
        authentication.getPrincipal() >> new SessionedUser(new User(username, [], new UserFieldConfig(null, null, null, null, null, null, null, null, false, null, null)))

        oAuth2Authentication.getUserAuthentication() >> authentication

        samlOauth2LogoutHandler = new SamlOauth2LogoutHandler(tokenStore, userService)
    }

    def "null Authentication returns 200"(){
        when:
        samlOauth2LogoutHandler.logout(httpServletRequest, httpServletResponse, null)

        then:
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_OK)
        0 * tokenStore.readAccessToken(_)
    }

    def "handler finds tokens by client id and username"(){
        when:
        samlOauth2LogoutHandler.logout(httpServletRequest, httpServletResponse, authentication)

        then:
        1 * tokenStore.findTokensByClientIdAndUserName(clientId, username) >> [ Mock(OAuth2AccessToken) ]

        then:
        1 * tokenStore.removeAccessToken(_)
        1 * userService.recordEvent(_, _) >> { args ->
            UserEvent userEvent = args[1]
            assert userEvent.eventType == EventType.LOGOUT
            assert userEvent.eventData == [:]

            return new Try.Success<Void>(null)
        }

        then:
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }
}
