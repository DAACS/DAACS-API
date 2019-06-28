package com.daacs.unit.service

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.auth.repository.UniversalAuthenticationRepository
import com.daacs.framework.exception.BadInputException
import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.Nonce
import com.daacs.model.SessionedUser
import com.daacs.model.User
import com.daacs.model.lti.OutcomeParams
import com.daacs.repository.hystrix.LtiReplaceResultCommand
import com.daacs.repository.lti.LtiNonceRepository
import com.daacs.repository.lti.OutcomeServiceRepositoryImpl
import com.daacs.service.LtiServiceImpl
import com.daacs.service.UserService
import com.lambdista.util.Try
import org.imsglobal.lti.launch.LtiVerificationResult
import org.imsglobal.lti.launch.LtiVerifier
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Created by mgoldman on 4/10/19.
 */
class LtiServiceSpec extends Specification {

    LtiServiceImpl ltiService
    LtiNonceRepository ltiNonceRepository
    LtiVerifier ltiVerifier
    UniversalAuthenticationRepository authenticationRepository
    OutcomeServiceRepositoryImpl outcomeServiceRepository
    HystrixCommandFactory hystrixCommandFactory
    LtiReplaceResultCommand ltiReplaceResultCommand
    UserService userService

    HttpServletRequest httpServletRequest

    LtiVerificationResult ltiResults

    String oauthVersion = "1.0"
    String signatureMethod = "HMAC-SHA1"
    String secretKey = "shhh"
    boolean canvasEnabled


    def setup() {

        authenticationRepository = Mock(UniversalAuthenticationRepository)
        outcomeServiceRepository = Mock(OutcomeServiceRepositoryImpl)
        hystrixCommandFactory = Mock(HystrixCommandFactory)
        ltiReplaceResultCommand = Mock(LtiReplaceResultCommand)
        ltiVerifier = Mock(LtiVerifier)
        ltiNonceRepository = Mock(LtiNonceRepository)
        userService = Mock(UserService)
        ltiResults = Mock(LtiVerificationResult)


        ltiService = new LtiServiceImpl(
                ltiNonceRepository: ltiNonceRepository,
                ltiVerifier: ltiVerifier,
                authenticationRepository: authenticationRepository,
                userService: userService,
                oauthVersion: oauthVersion,
                signatureMethod: signatureMethod,
                hystrixCommandFactory: hystrixCommandFactory,
                outcomeServiceRepository: outcomeServiceRepository,
                canvasEnabled: canvasEnabled,
                secretKey: secretKey
        )


        httpServletRequest = Mock(HttpServletRequest)
        httpServletRequest.getParameter("username") >> "toddrick"

    }


    def "verifyLaunch: success"() {
        when:
        Try<String> maybeToken = ltiService.verifyLaunch(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        1 * httpServletRequest.getParameter('oauth_nonce')
        1 * httpServletRequest.getParameter('custom_username') >> "name"

        1 * ltiVerifier.verify(_, _) >> ltiResults
        1 * ltiResults.getSuccess() >> true

        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        1 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isSuccess()
    }

    def "verifyLaunch: failure"() {
        when:
        Try<String> maybeToken = ltiService.verifyLaunch(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> "garbage"
        0 * httpServletRequest.getParameter("oauth_version")
        0 * httpServletRequest.getParameter("oauth_timestamp")
        0 * ltiNonceRepository.insertNonce(_)
        0 * ltiNonceRepository.getNonce(_)
        0 * ltiVerifier.verify(_, _)
        0 * ltiResults.getSuccess()
        0 * userService.loadUserByUsername(_)
        0 * authenticationRepository.storeAuthenitcation(_)

        then:
        maybeToken.isFailure()
    }


    def "verifyOauth: success"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        1 * httpServletRequest.getParameter('oauth_nonce')
        1 * httpServletRequest.getParameter('custom_username') >> "name"

        1 * ltiVerifier.verify(_, _) >> ltiResults
        1 * ltiResults.getSuccess() >> true

        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        1 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isSuccess()
    }

    def "verifyOauth: failure, oauth_signature_method"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> "something else"
        0 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        0 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        0 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        0 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        0 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"

        0 * ltiVerifier.verify(_, _) >> ltiResults
        0 * ltiResults.getSuccess() >> true

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, oauth_version"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> "something else"
        0 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        0 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        0 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        0 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"

        0 * ltiVerifier.verify(_, _) >> ltiResults
        0 * ltiResults.getSuccess() >> true

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, expired timestamp"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> ((System.currentTimeMillis() / 1000L).intValue() - 400).toString()

        0 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        0 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        0 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"

        0 * ltiVerifier.verify(_, _) >> ltiResults
        0 * ltiResults.getSuccess() >> true

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, insertNonce"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Failure<Void>(null)
        0 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        0 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"

        0 * ltiVerifier.verify(_, _) >> ltiResults
        0 * ltiResults.getSuccess() >> true

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, verifyLti throws exception"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        1 * ltiVerifier.verify(_, _) >> { throw Mock(Exception) }
        0 * ltiResults.getSuccess() >> true

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, lti not verified"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        1 * ltiVerifier.verify(_, _) >> ltiResults
        1 * ltiResults.getSuccess() >> false

        0 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        maybeToken.isFailure()
    }

    def "verifyOauth: failure, loadUserByUsername"() {
        when:
        Try<String> maybeToken = ltiService.verifyOauth(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("oauth_signature_method") >> signatureMethod
        1 * httpServletRequest.getParameter("oauth_version") >> oauthVersion
        1 * httpServletRequest.getParameter("oauth_timestamp") >> System.currentTimeMillis().toString()

        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        1 * httpServletRequest.getParameter('oauth_nonce')
        1 * httpServletRequest.getParameter('custom_username') >> "name"

        1 * ltiVerifier.verify(_, _) >> ltiResults
        1 * ltiResults.getSuccess() >> true

        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        1 * authenticationRepository.storeAuthenitcation(_) >> { throw Mock(InvalidGrantException) }

        then:
        maybeToken.isFailure()
    }

    def "verifyNonce: success"() {
        when:
        Try<Boolean> maybeVerified = ltiService.verifyNonce("nonceId")

        then:
        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        then:
        maybeVerified.isSuccess()
    }

    def "verifyNonce: failure on insert"() {
        when:
        Try<Boolean> maybeVerified = ltiService.verifyNonce("nonceId")

        then:
        1 * ltiNonceRepository.insertNonce(_) >> new Try.Failure<Void>(null)
        0 * ltiNonceRepository.getNonce(_) >> new Try.Success<Nonce>(new Nonce("id"))

        then:
        maybeVerified.isFailure()
    }

    def "verifyNonce: failure on get"() {
        when:
        Try<Boolean> maybeVerified = ltiService.verifyNonce("nonceId")

        then:
        1 * ltiNonceRepository.insertNonce(_) >> new Try.Success<Void>(null)
        1 * ltiNonceRepository.getNonce(_) >> new Try.Failure<Nonce>(null)

        then:
        maybeVerified.isFailure()
    }

    def "getAuthentication: success"() {
        when:
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        1 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        noExceptionThrown()
        token == "tokenKey"
    }

    def "getAuthentication: success with canvas enabled"() {
        when:
        ltiService.canvasEnabled = true
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        1 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        1 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"
        1 * httpServletRequest.getParameter("lis_outcome_service_url") >> "url"
        1 * httpServletRequest.getParameter("lis_result_sourcedid") >> "123"

        1 * userService.getUserByUsername("username") >> new Try.Success<User>(new User(id: "userid"))
        1 * outcomeServiceRepository.storeOutcomeParams(_, "userid") >> "key"


        then:
        noExceptionThrown()
        token == "tokenKey"
    }

    def "getAuthentication: failure missing lti params: oauth_consumer_key"() {
        when:
        ltiService.canvasEnabled = true
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        1 * httpServletRequest.getParameter("oauth_consumer_key") >> ""
        0 * httpServletRequest.getParameter("lis_outcome_service_url") >> "url"
        0 * httpServletRequest.getParameter("lis_result_sourcedid") >> "123"

        0 * userService.getUserByUsername("username") >> new Try.Success<User>(new User(id: "userid"))
        0 * outcomeServiceRepository.storeOutcomeParams(_, "userid") >> "key"


        then:
        thrown(BadInputException)
    }

    def "getAuthentication: failure missing lti params: lis_outcome_service_url"() {
        when:
        ltiService.canvasEnabled = true
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        1 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"
        1 * httpServletRequest.getParameter("lis_outcome_service_url") >> ""
        0 * httpServletRequest.getParameter("lis_result_sourcedid") >> "123"

        0 * userService.getUserByUsername("username") >> new Try.Success<User>(new User(id: "userid"))
        0 * outcomeServiceRepository.storeOutcomeParams(_, "userid") >> "key"


        then:
        thrown(BadInputException)
    }

    def "getAuthentication: failure missing lti params: lis_result_sourcedid"() {
        when:
        ltiService.canvasEnabled = true
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators'))
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        1 * httpServletRequest.getParameter("oauth_consumer_key") >> "biglongkey"
        1 * httpServletRequest.getParameter("lis_outcome_service_url") >> "url"
        1 * httpServletRequest.getParameter("lis_result_sourcedid") >> ""

        0 * userService.getUserByUsername("username") >> new Try.Success<User>(new User(id: "userid"))
        0 * outcomeServiceRepository.storeOutcomeParams(_, "userid") >> "key"


        then:
        thrown(BadInputException)
    }

    def "getAuthentication: failure missing username"() {
        when:
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> ""
        0 * userService.loadUserByUsername(_) >> { throw Mock(FailureTypeException) }
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        thrown(InvalidGrantException)
    }

    def "getAuthentication: failure on loadUserByUsername"() {
        when:
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> { throw Mock(FailureTypeException) }
        0 * authenticationRepository.storeAuthenitcation(_) >> "tokenKey"

        then:
        thrown(InvalidGrantException)
    }

    def "getAuthentication: failure on storeAuthenitcation"() {
        when:
        String token = ltiService.getAuthentication(httpServletRequest)

        then:
        1 * httpServletRequest.getParameter("custom_username") >> "username"
        1 * userService.loadUserByUsername(_) >> new SessionedUser(new User(id: '1', roles: ['admin'], username: 'Bichael', password: 'alligrators7'))
        1 * authenticationRepository.storeAuthenitcation(_) >> { throw Mock(InvalidGrantException) }

        then:
        thrown(InvalidGrantException)
    }

    def "updateGrades: success"() {
        when:
        Try<Void> result = ltiService.updateGrades("id")

        then:
        1 * outcomeServiceRepository.retrieveOutcomeParams("id") >> new OutcomeParams("consumerKey", "url", "sourcedid")
        1 * hystrixCommandFactory.getLTIReplaceResultCommand("LtiServiceImpl-updateGrades", _, _, _, _, "1") >> ltiReplaceResultCommand
        1 * ltiReplaceResultCommand.execute() >> new Try.Success(null)

        then:
        result.isSuccess()
    }

    def "updateGrades: failure, missing lti params"() {
        when:
        Try<Void> result = ltiService.updateGrades("id")

        then:
        1 * outcomeServiceRepository.retrieveOutcomeParams("id") >> {throw new NotFoundException()}
        0 * hystrixCommandFactory.getLTIReplaceResultCommand("LtiServiceImpl-updateGrades", _, _, _, _, "1") >> ltiReplaceResultCommand
        0 * ltiReplaceResultCommand.execute() >> new Try.Success(null)

        then:
        result.isFailure()
    }

    def "updateGrades: failure, getLTIReplaceResultCommand"() {
        when:
        Try<Void> result = ltiService.updateGrades("id")

        then:
        1 * outcomeServiceRepository.retrieveOutcomeParams("id") >> new OutcomeParams("consumerKey", "url", "sourcedid")
        1 * hystrixCommandFactory.getLTIReplaceResultCommand("LtiServiceImpl-updateGrades", _, _, _, _, "1") >> ltiReplaceResultCommand
        1 * ltiReplaceResultCommand.execute() >> new Try.Failure(null)

        then:
        result.isFailure()
    }


}
