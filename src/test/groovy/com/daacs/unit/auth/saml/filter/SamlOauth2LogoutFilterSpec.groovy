package com.daacs.framework.auth.saml.filter

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.context.SAMLContextProvider
import org.springframework.security.saml.log.SAMLLogger
import org.springframework.security.saml.websso.SingleLogoutProfile
import org.springframework.security.web.authentication.logout.LogoutHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * Created by chostetter on 6/23/16.
 */
class SamlOauth2LogoutFilterSpec extends Specification{

    TokenStore tokenStore
    LogoutSuccessHandler logoutSuccessHandler
    LogoutHandler[] localHandler
    LogoutHandler[] globalHandlers

    HttpServletRequest httpServletRequest
    HttpServletResponse httpServletResponse
    FilterChain filterChain

    OAuth2Authentication oAuth2Authentication
    Authentication authentication
    SAMLCredential samlCredential
    SingleLogoutProfile singleLogoutProfile

    SamlOauth2LogoutFilter samlOauth2LogoutFilter


    def setup(){
        tokenStore = Mock(TokenStore)
        logoutSuccessHandler = Mock(LogoutSuccessHandler)
        localHandler = [ Mock(LogoutHandler) ]
        globalHandlers = [ Mock(LogoutHandler) ]

        httpServletRequest = Mock(HttpServletRequest)
        httpServletRequest.getRequestURI() >> "/saml/logout"
        httpServletRequest.getHeader("authorization") >> "Bearer 12345"

        httpServletResponse = Mock(HttpServletResponse)
        filterChain = Mock(FilterChain)

        oAuth2Authentication = Mock(OAuth2Authentication)
        tokenStore.readAuthentication(_) >> oAuth2Authentication

        authentication = Mock(Authentication)
        oAuth2Authentication.getUserAuthentication() >> authentication

        samlCredential = Mock(SAMLCredential)
        authentication.getCredentials() >> samlCredential

        singleLogoutProfile = Mock(SingleLogoutProfile)

        samlOauth2LogoutFilter = new SamlOauth2LogoutFilter(tokenStore, logoutSuccessHandler, localHandler, globalHandlers)
        samlOauth2LogoutFilter.contextProvider = Mock(SAMLContextProvider)
        samlOauth2LogoutFilter.profile = singleLogoutProfile
        samlOauth2LogoutFilter.samlLogger = Mock(SAMLLogger)
        //samlOauth2LogoutFilter.setFilterProcessesUrl("/logout");
    }

    def "URL matching processes logout"(){
        setup:
        httpServletRequest.getRequestURI() >> "/saml/logout"

        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        1 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "URL not matching processes logout"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        httpServletRequest.getRequestURI() >> "/some/other/url"
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "token must be valid"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        httpServletRequest.getHeader("authorization") >> "Bearer 12345"
        0 * httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        1 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "token must not be null"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        httpServletRequest.getHeader("authorization") >> null
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "token must start with 'Bearer'"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        httpServletRequest.getHeader("authorization") >> "curt 12345"
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "token must be two parts"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        httpServletRequest.getHeader("authorization") >> "Bearer"
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "if token doesn't exist in tokenStore, move on"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        tokenStore.readAuthentication(_) >> null
        1 * filterChain.doFilter(_, _)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "if token doesn't contain authentication, move on"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        oAuth2Authentication.getUserAuthentication() >> null
        1 * filterChain.doFilter(_, _)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }

    def "no go if auth creds aren't SAMLCredentials"(){
        when:
        samlOauth2LogoutFilter.processLogout(httpServletRequest, httpServletResponse, filterChain)

        then:
        authentication.getCredentials() >> new Object()
        1 * filterChain.doFilter(_, _)
        0 * singleLogoutProfile.sendLogoutRequest(_, _)
    }
}
