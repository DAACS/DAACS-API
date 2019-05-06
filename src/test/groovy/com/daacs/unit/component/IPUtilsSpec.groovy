package com.daacs.unit.component

import com.daacs.component.utils.IPUtils
import com.daacs.component.utils.IPUtilsImpl
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Created by adistasio on 5/5/17.
 */
class IPUtilsSpec extends Specification {

    IPUtils ipUtils

    def setup() {
        ipUtils = new IPUtilsImpl()
    }

    def "getIpAddress succeeds from header"() {
        given:
        HttpServletRequest request = Mock(HttpServletRequest)

        when:
        String ipAddress = ipUtils.getIPAddress(request)

        then:
        1 * request.getHeader("X-Real-IP") >> "1.2.3.4"
        0 * request.getRemoteAddr()
        ipAddress == "1.2.3.4"
    }

    def "getIpAddress falls back to remoteAddr"() {
        given:
        HttpServletRequest request = Mock(HttpServletRequest)

        when:
        String ipAddress = ipUtils.getIPAddress(request)

        then:
        1 * request.getHeader("X-Real-IP") >> null
        1 * request.getRemoteAddr() >> "5.6.7.8"
        ipAddress == "5.6.7.8"
    }
}
