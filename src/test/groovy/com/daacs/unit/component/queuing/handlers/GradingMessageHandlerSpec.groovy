package com.daacs.unit.component.queuing.handlers

import com.daacs.component.queuing.Retry
import com.daacs.component.queuing.handlers.GradingMessageHandler
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.queue.GradingMessage
import com.daacs.model.queue.InitMessage
import com.daacs.model.queue.QueueMessage
import com.daacs.service.ScoringService
import com.daacs.service.UserAssessmentService
import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdista.util.Try
import spock.lang.Specification
/**
 * Created by chostetter on 8/11/16.
 */
class GradingMessageHandlerSpec extends Specification{

    GradingMessageHandler messageHandler
    GradingMessage gradingMessage = new GradingMessage(userId: "abc123", userAssessmentId: "123abc")

    ScoringService scoringService
    UserAssessmentService userAssessmentService
    ObjectMapper objectMapper

    UserAssessment userAssessment
    UserAssessment gradedUserAssessment

    def setup(){
        userAssessment = new CATUserAssessment(status: CompletionStatus.COMPLETED)
        gradedUserAssessment = new CATUserAssessment(status: CompletionStatus.GRADED)

        scoringService = Mock(ScoringService)
        userAssessmentService = Mock(UserAssessmentService)

        objectMapper = new ObjectMapperConfig().objectMapper()
        messageHandler = new GradingMessageHandler(objectMapper: objectMapper, scoringService: scoringService, userAssessmentService: userAssessmentService, retry: new Retry(0, 1))
    }

    def "canHandle: handles GradingMessage"(){
        setup:
        QueueMessage gradingMessage = new GradingMessage()

        when:
        Boolean canHandle = messageHandler.canHandle(gradingMessage)

        then:
        canHandle
    }

    def "canHandle: does not handle non-GradingMessage"(){
        setup:
        QueueMessage nonGradingMessage = new InitMessage()

        when:
        Boolean canHandle = messageHandler.canHandle(nonGradingMessage)

        then:
        !canHandle
    }

    def "handle: success"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(gradingMessage)

        then:
        1 * userAssessmentService.getUserAssesment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * scoringService.autoGradeUserAssessment(userAssessment) >> new Try.Success<UserAssessment>(gradedUserAssessment)
        1 * userAssessmentService.saveUserAssessment(gradedUserAssessment) >> new Try.Success<Void>(null)

        then:
        maybeHandled.isSuccess()
    }

    def "handle: getUserAssessment fails, i fail"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(gradingMessage)

        then:
        1 * userAssessmentService.getUserAssesment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId()) >> new Try.Failure<UserAssessment>(new Exception())
        0 * scoringService.autoGradeUserAssessment(*_)
        0 * userAssessmentService.saveUserAssessment(*_)

        then:
        maybeHandled.isFailure()
    }

    def "handle: saveUserAssessment fails, i fail"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(gradingMessage)

        then:
        1 * userAssessmentService.getUserAssesment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * scoringService.autoGradeUserAssessment(userAssessment) >> new Try.Success<UserAssessment>(gradedUserAssessment)
        1 * userAssessmentService.saveUserAssessment(gradedUserAssessment) >> new Try.Failure<Void>(null)

        then:
        maybeHandled.isFailure()
    }

    def "handle: grading fails, save as grade error successful"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(gradingMessage)

        then:
        1 * userAssessmentService.getUserAssesment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * scoringService.autoGradeUserAssessment(userAssessment) >> new Try.Failure<UserAssessment>(new Exception())
        1 * userAssessmentService.saveUserAssessment(_) >> { args ->
            UserAssessment savingUserAssessment = args[0]
            assert savingUserAssessment.getStatus() == CompletionStatus.GRADING_FAILURE
            return new Try.Success<Void>(null)
        }

        then:
        maybeHandled.isSuccess()
    }

    def "handle: grading fails, save as grade error fail"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(gradingMessage)

        then:
        1 * userAssessmentService.getUserAssesment(gradingMessage.getUserId(), gradingMessage.getUserAssessmentId()) >> new Try.Success<UserAssessment>(userAssessment)
        1 * scoringService.autoGradeUserAssessment(userAssessment) >> new Try.Failure<UserAssessment>(new Exception())
        1 * userAssessmentService.saveUserAssessment(_) >> { args ->
            UserAssessment savingUserAssessment = args[0]
            assert savingUserAssessment.getStatus() == CompletionStatus.GRADING_FAILURE
            return new Try.Failure<Void>(null)
        }

        then:
        maybeHandled.isFailure()
    }
}
