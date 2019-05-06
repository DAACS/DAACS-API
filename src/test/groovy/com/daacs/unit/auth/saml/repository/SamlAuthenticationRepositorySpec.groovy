package com.daacs.unit.auth.saml.repository

import com.daacs.framework.auth.saml.repository.SamlAuthenticationRepository
import com.daacs.framework.exception.NotFoundException
import org.springframework.security.core.Authentication
import spock.lang.Specification
/**
 * Created by chostetter on 6/23/16.
 */
class SamlAuthenticationRepositorySpec extends Specification{

    Authentication authentication

    SamlAuthenticationRepository samlAuthenticationRepository

    int TOKEN_TTL = 200

    def setup(){
        authentication = Mock(Authentication)
        samlAuthenticationRepository = new SamlAuthenticationRepository(TOKEN_TTL: TOKEN_TTL)
        samlAuthenticationRepository.init()
    }

    def "repo stores authentication and returns token"(){
        when:
        String token = samlAuthenticationRepository.storeAuthenitcation(authentication)

        then:
        samlAuthenticationRepository.authStore.containsKey(token)
    }

    def "repo only stores auth until TTL expires"(){
        when:
        String token = samlAuthenticationRepository.storeAuthenitcation(authentication)

        then:
        sleep(TOKEN_TTL)

        then:
        !samlAuthenticationRepository.authStore.containsKey(token)
    }

    def "retrieveAuthentication with invalid token"(){
        when:
        samlAuthenticationRepository.retrieveAuthentication("12345")

        then:
        thrown(NotFoundException)
    }

    def "retrieving Authentication removes it from store"(){
        when:
        String token = samlAuthenticationRepository.storeAuthenitcation(authentication)

        then:
        samlAuthenticationRepository.authStore.containsKey(token)

        when:
        samlAuthenticationRepository.retrieveAuthentication(token)

        then:
        !samlAuthenticationRepository.authStore.containsKey(token)
    }
}
