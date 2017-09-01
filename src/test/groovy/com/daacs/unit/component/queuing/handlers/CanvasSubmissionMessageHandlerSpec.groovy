package com.daacs.unit.component.queuing.handlers

import com.daacs.component.queuing.Retry
import com.daacs.component.queuing.handlers.CanvasSubmissionMessageHandler
import com.daacs.framework.exception.InvalidObjectException
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.User
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.CompletionSummary
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.queue.CanvasSubmissionMessage
import com.daacs.model.queue.InitMessage
import com.daacs.model.queue.QueueMessage
import com.daacs.service.CanvasService
import com.daacs.service.MailService
import com.daacs.service.UserAssessmentService
import com.daacs.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdista.util.Try
import spock.lang.Specification
/**
 * Created by chostetter on 4/10/17.
 */
class CanvasSubmissionMessageHandlerSpec extends Specification{

    CanvasSubmissionMessageHandler messageHandler
    CanvasSubmissionMessage canvasSubmissionMessage = new CanvasSubmissionMessage(userId: "abc123")

    CanvasService canvasService
    UserAssessmentService userAssessmentService
    UserService userService
    ObjectMapper objectMapper
    MailService mailService

    UserAssessment userAssessment
    UserAssessment gradedUserAssessment

    User dummyUser;

    def setup(){
        dummyUser = new User("username", "", "Mr", "Dummy", true, ["ROLE_STUDENT"], "secondaryId", "canvasSisId")
        dummyUser.setId("abc123");

        userAssessment = new CATUserAssessment(status: CompletionStatus.COMPLETED)
        gradedUserAssessment = new CATUserAssessment(status: CompletionStatus.GRADED)

        canvasService = Mock(CanvasService)
        userAssessmentService = Mock(UserAssessmentService)
        userService = Mock(UserService)
        mailService = Mock(MailService)

        objectMapper = new ObjectMapperConfig().objectMapper()
        messageHandler = new CanvasSubmissionMessageHandler(
                objectMapper: objectMapper,
                canvasService: canvasService,
                userService: userService,
                mailService: mailService,
                userAssessmentService: userAssessmentService,
                retry: new Retry(0, 1))
    }

    def "canHandle: handles CanvasSubmissionMessage"(){
        setup:
        QueueMessage canvasSubmissionMessage = new CanvasSubmissionMessage()

        when:
        Boolean canHandle = messageHandler.canHandle(canvasSubmissionMessage)

        then:
        canHandle
    }

    def "canHandle: does not handle non-CanvasSubmissionMessage"(){
        setup:
        QueueMessage nonCanvasSubmissionMessage = new InitMessage()

        when:
        Boolean canHandle = messageHandler.canHandle(nonCanvasSubmissionMessage)

        then:
        !canHandle
    }

    def "handle: success"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userAssessmentService.getCompletionSummary(canvasSubmissionMessage.getUserId()) >> new Try.Success<CompletionSummary>(new CompletionSummary(hasCompletedAllCategories: true))
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Success<User>(dummyUser)
        1 * canvasService.markAssignmentCompleted(dummyUser.getCanvasSisId()) >> new Try.Success<Void>(null)
        1 * userService.saveUser(_) >> { args ->
            User savedUser = args[0]
            assert savedUser.reportedCompletionToCanvas

            return new Try.Success<User>(savedUser)
        }

        then:
        maybeHandled.isSuccess()
    }

    def "handle: canvas not enabled"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> false
        0 * userAssessmentService.getCompletionSummary(*_)
        0 * userService.getUser(*_)
        0 * canvasService.markAssignmentCompleted(*_)

        then:
        maybeHandled.isSuccess()
    }

    def "handle: markAssignmentCompleted fails, email sent"(){
        setup:
        Exception failureException = new Exception()

        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userAssessmentService.getCompletionSummary(canvasSubmissionMessage.getUserId()) >> new Try.Success<CompletionSummary>(new CompletionSummary(hasCompletedAllCategories: true))
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Success<User>(dummyUser)
        1 * canvasService.markAssignmentCompleted(dummyUser.getCanvasSisId()) >> new Try.Failure<Void>(failureException)
        1 * mailService.sendCanvasFailureEmail(dummyUser, _) >> new Try.Success<Void>(null)

        then:
        maybeHandled.isSuccess()
    }

    def "handle: getUser fails, i fail"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Failure<User>(new Exception())
        0 * userAssessmentService.getCompletionSummary(*_)
        0 * canvasService.markAssignmentCompleted(*_)

        then:
        maybeHandled.isFailure()
    }

    def "handle: getCompletionSummary fails, i fail"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Success<User>(dummyUser)
        1 * userAssessmentService.getCompletionSummary(canvasSubmissionMessage.getUserId()) >> new Try.Failure<CompletionSummary>(new Exception())
        0 * canvasService.markAssignmentCompleted(*_)

        then:
        maybeHandled.isFailure()
    }

    def "handle: success when hasCompletedAllCategories is false"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Success<User>(dummyUser)
        1 * userAssessmentService.getCompletionSummary(canvasSubmissionMessage.getUserId()) >> new Try.Success<CompletionSummary>(new CompletionSummary(hasCompletedAllCategories: false))
        0 * canvasService.markAssignmentCompleted(*_)

        then:
        maybeHandled.isSuccess()
    }

    def "handle: canvasSisId is null"(){
        setup:
        dummyUser.setCanvasSisId(null)

        when:
        Try<Void> maybeHandled = messageHandler.handleMessage(canvasSubmissionMessage)

        then:
        1 * canvasService.isEnabled() >> true
        1 * userAssessmentService.getCompletionSummary(canvasSubmissionMessage.getUserId()) >> new Try.Success<CompletionSummary>(new CompletionSummary(hasCompletedAllCategories: true))
        1 * userService.getUser(canvasSubmissionMessage.getUserId()) >> new Try.Success<User>(dummyUser)
        0 * canvasService.markAssignmentCompleted(*_)

        then:
        maybeHandled.isFailure()
        maybeHandled.failed().get() instanceof InvalidObjectException
    }
}
