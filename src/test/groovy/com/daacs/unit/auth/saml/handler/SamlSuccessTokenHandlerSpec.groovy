package com.daacs.unit.auth.saml.handler

import com.daacs.framework.auth.repository.AuthenticationRepository
import com.daacs.framework.auth.saml.handler.SamlSuccessTokenHandler
import org.springframework.security.core.Authentication
import org.springframework.security.web.RedirectStrategy
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * Created by chostetter on 6/23/16.
 */
class SamlSuccessTokenHandlerSpec extends Specification{

    HttpServletRequest httpServletRequest
    HttpServletResponse httpServletResponse
    Authentication authentication
    AuthenticationRepository samlAuthenticationRepository
    SamlSuccessTokenHandler samlSuccessTokenHandler
    RedirectStrategy redirectStrategy

    String defaultTargetUrl = "http://default/target/url"
    String token = "12345"

    def setup(){
        httpServletRequest = Mock(HttpServletRequest)
        httpServletResponse = Mock(HttpServletResponse)
        authentication = Mock(Authentication)

        samlAuthenticationRepository = Mock(AuthenticationRepository)

        redirectStrategy = Mock(RedirectStrategy)

        samlSuccessTokenHandler = new SamlSuccessTokenHandler(samlAuthenticationRepository: samlAuthenticationRepository)
        samlSuccessTokenHandler.setRedirectStrategy(redirectStrategy)
        samlSuccessTokenHandler.setDefaultTargetUrl(defaultTargetUrl)
    }

    def "authentication gets stored in repo, user gets forwarded with token as param"(){
        when:
        samlSuccessTokenHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication)

        then:
        1 * samlAuthenticationRepository.storeAuthenitcation(authentication) >> token

        then:
        1 * redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, _) >> { arguments ->
            String targetUrl = arguments[2]
            assert targetUrl == defaultTargetUrl + "?token=" + token
        }
    }

    def "token gets appended as first param"(){
        when:
        String targetUrl = samlSuccessTokenHandler.getTokenTargetUrl(token)

        then:
        targetUrl == defaultTargetUrl + "?token=" + token
    }

    def "token gets appended as nth param"(){
        setup:
        String newTargetUrl = defaultTargetUrl + "?anotherparam=value"
        samlSuccessTokenHandler.setDefaultTargetUrl(newTargetUrl)

        when:
        String targetUrl = samlSuccessTokenHandler.getTokenTargetUrl(token)

        then:
        targetUrl == newTargetUrl + "&token=" + token
    }
}
