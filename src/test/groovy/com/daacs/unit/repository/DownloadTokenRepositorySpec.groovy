package com.daacs.unit.repository

import com.daacs.model.User
import com.daacs.repository.DownloadTokenRepository
import com.daacs.repository.DownloadTokenRepositoryImpl
import com.lambdista.util.Try
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class DownloadTokenRepositorySpec extends Specification {

    DownloadTokenRepository downloadTokenRepository
    int tokenTTL = 100
    User dummyUser = new User("username", "mr", "dummy")

    def setup(){
        downloadTokenRepository = new DownloadTokenRepositoryImpl(TOKEN_TTL: tokenTTL)
        downloadTokenRepository.init()
    }

    def "storeUser: success"(){
        when:
        String token = downloadTokenRepository.storeUser(dummyUser)

        then:
        downloadTokenRepository.tokenStore.get(token) == dummyUser
    }

    def "storeUser: token expires after tokenTTL"(){
        when:
        String token = downloadTokenRepository.storeUser(dummyUser)
        sleep(tokenTTL)

        then:
        downloadTokenRepository.tokenStore.get(token) == null
    }

    def "retrieveUser: success"(){
        setup:
        String token = downloadTokenRepository.storeUser(dummyUser)

        when:
        Try<User> maybeUser = downloadTokenRepository.retrieveUser(token)

        then:
        maybeUser.isSuccess()
        maybeUser.get() == dummyUser
    }

    def "retrieveUser: not found"(){
        when:
        Try<User> maybeUser = downloadTokenRepository.retrieveUser("not a real token")

        then:
        maybeUser.isFailure()
    }

    def "retrieveUser: expires"(){
        setup:
        String token = downloadTokenRepository.storeUser(dummyUser)
        sleep(tokenTTL)

        when:
        Try<User> maybeUser = downloadTokenRepository.retrieveUser(token)

        then:
        maybeUser.isFailure()
    }
}
