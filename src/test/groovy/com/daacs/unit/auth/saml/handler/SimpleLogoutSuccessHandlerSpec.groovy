package com.daacs.unit.auth.saml.handler

import com.daacs.framework.auth.saml.handler.SimpleLogoutSuccessHandler
import org.springframework.security.core.Authentication
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by chostetter on 6/23/16.
 */
class SimpleLogoutSuccessHandlerSpec extends Specification{

    HttpServletRequest httpServletRequest
    HttpServletResponse httpServletResponse

    Authentication authentication

    SimpleLogoutSuccessHandler simpleLogoutSuccessHandler

    def setup(){
        httpServletRequest = Mock(HttpServletRequest)
        httpServletResponse = Mock(HttpServletResponse)
        authentication = Mock(Authentication)

        simpleLogoutSuccessHandler = new SimpleLogoutSuccessHandler()
    }

    def "returns a 200"(){
        when:
        simpleLogoutSuccessHandler.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication)

        then:
        1 * httpServletResponse.setStatus(HttpServletResponse.SC_OK)
    }
}
