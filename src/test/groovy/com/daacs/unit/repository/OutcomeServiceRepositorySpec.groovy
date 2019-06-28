
package com.daacs.unit.repository

import com.daacs.framework.exception.NotFoundException
import com.daacs.model.lti.OutcomeParams
import com.daacs.repository.lti.OutcomeServiceRepositoryImpl
import spock.lang.Specification

/**
 * Created by mgoldman on 6/10/19.
 */
class OutcomeServiceRepositorySpec extends Specification{

    OutcomeParams params

    OutcomeServiceRepositoryImpl outcomeServiceRepository


    def setup(){
        params = Mock(OutcomeParams)
        outcomeServiceRepository = new OutcomeServiceRepositoryImpl()
        outcomeServiceRepository.init()
    }

    def "repo stores authentication and returns token"(){
        when:
        String userId = outcomeServiceRepository.storeOutcomeParams(params, "userId")

        then:
        outcomeServiceRepository.outcomeStore.containsKey(userId)
    }


    def "retrieveAuthentication with invalid token"(){
        when:
        outcomeServiceRepository.retrieveOutcomeParams("userId")

        then:
        thrown(NotFoundException)
    }

    def "retrieving Authentication removes it from store"(){
        when:
        String userId = outcomeServiceRepository.storeOutcomeParams(params, "userId")

        then:
        outcomeServiceRepository.outcomeStore.containsKey(userId)

        when:
        outcomeServiceRepository.retrieveOutcomeParams(userId)

        then:
        !outcomeServiceRepository.outcomeStore.containsKey(userId)
    }
}
