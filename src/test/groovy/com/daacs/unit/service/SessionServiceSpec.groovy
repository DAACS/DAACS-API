package com.daacs.service

import com.daacs.model.SessionedUser
import com.daacs.framework.auth.service.SessionService
import com.daacs.framework.auth.service.SessionServiceImpl
import com.daacs.model.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Created by chostetter on 6/23/16.
 */
class SessionServiceSpec extends Specification {

    SecurityContext securityContext
    Authentication authentication
    SessionService sessionService
    User dummyUser = new User("dummyuser", "Mr", "Dummy");
    SessionedUser dummySessionedUser = new SessionedUser(dummyUser)

    def setup(){
        authentication = Mock(Authentication)
        securityContext = Mock(SecurityContext)
        securityContext.getAuthentication() >> authentication
        SecurityContextHolder.setContext(securityContext)

        sessionService = new SessionServiceImpl()
    }

    def "getSessionedUser: returns sessioned user"(){
        when:
        Optional<SessionedUser> optionalSessionedUser = sessionService.getSessionedUser()

        then:
        1 * authentication.getPrincipal() >> dummySessionedUser
        optionalSessionedUser.isPresent()
        optionalSessionedUser.get() == dummySessionedUser
    }

    def "getSessionedUser: empty if authentication doesn't exist"(){
        when:
        Optional<SessionedUser> optionalSessionedUser = sessionService.getSessionedUser()

        then:
        1 * securityContext.getAuthentication() >> null
        !optionalSessionedUser.isPresent()
    }

    def "getSessionedUser: empty if authentication isn't a SessionedUser"(){
        when:
        Optional<SessionedUser> optionalSessionedUser = sessionService.getSessionedUser()

        then:
        1 * authentication.getPrincipal() >> dummyUser
        !optionalSessionedUser.isPresent()
    }

    def "getRequiredSessionedUser: returns sessioned user"(){
        when:
        SessionedUser sessionedUser = sessionService.getRequiredSessionedUser()

        then:
        1 * authentication.getPrincipal() >> dummySessionedUser
        notThrown(IllegalStateException)
        sessionedUser == dummySessionedUser
    }

    def "getRequiredSessionedUser: throws exception if no sessioned user"(){
        when:
        SessionedUser sessionedUser = sessionService.getRequiredSessionedUser()

        then:
        1 * authentication.getPrincipal() >> null
        thrown(IllegalStateException)
    }

    def "getUserId: returns userId of sessioned user"(){
        when:
        String userId = sessionService.getUserId()

        then:
        1 * authentication.getPrincipal() >> dummySessionedUser
        userId == dummySessionedUser.getId()
    }

    def "getUserId: returns null for if no sessioned user"(){
        when:
        String userId = sessionService.getUserId()

        then:
        1 * authentication.getPrincipal() >> null
        userId == null
    }
}
