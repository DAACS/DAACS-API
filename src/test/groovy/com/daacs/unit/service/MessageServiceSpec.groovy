package com.daacs.unit.service

import com.daacs.model.User
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.queue.CanvasSubmissionMessage
import com.daacs.model.queue.GradingMessage
import com.daacs.repository.MessageRepository
import com.daacs.service.MessageService
import com.daacs.service.MessageServiceImpl
import com.daacs.service.UserService
import com.lambdista.util.Try
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class MessageServiceSpec extends Specification {

    MessageRepository messageRepository

    MessageService messageService

    UserAssessment userAssessment

    UserService userService

    def setup(){
        userAssessment = new CATUserAssessment(id: "123", userId: "abc")
        messageRepository = Mock(MessageRepository)
        userService = Mock(UserService)
        messageService = new MessageServiceImpl(messageRepository: messageRepository, userService: userService)
    }

    def "queueUserAssessmentForGrading: success"(){
        when:
        Try<Void> maybeResults = messageService.queueUserAssessmentForGrading(userAssessment)

        then:
        1 * messageRepository.insertMessage(_) >> { args ->
            GradingMessage queueMessage = args[0]
            assert queueMessage.userAssessmentId == userAssessment.id
            assert queueMessage.userId == userAssessment.userId

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isSuccess()
    }

    def "queueUserAssessmentForGrading: insertMessage fails, i fail"(){
        when:
        Try<Void> maybeResults = messageService.queueUserAssessmentForGrading(userAssessment)

        then:
        1 * messageRepository.insertMessage(_) >> new Try.Failure<Void>(null)

        then:
        maybeResults.isFailure()
    }

    def "queueCanvasSubmissionUpdate: success"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdate("123")

        then:
        1 * messageRepository.insertMessage(_) >> { args ->
            CanvasSubmissionMessage queueMessage = args[0]
            assert queueMessage.userId == "123"

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isSuccess()
    }

    def "queueCanvasSubmissionUpdate: insertMessage fails, i fail"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdate("123")

        then:
        1 * messageRepository.insertMessage(_) >> new Try.Failure<Void>(null)

        then:
        maybeResults.isFailure()
    }

    def "queueCanvasSubmissionUpdateForAllStudents: success"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdateForAllStudents(true)

        then:
        1 * userService.getUsers(Arrays.asList("ROLE_STUDENT")) >> new Try.Success<List<User>>([new User(id: "123")])
        1 * userService.saveUser(_) >> { args ->
            User savedUser = args[0]
            assert !savedUser.reportedCompletionToCanvas
            return new Try.Success<User>(savedUser)
        }
        1 * messageRepository.insertMessage(_) >> { args ->
            CanvasSubmissionMessage queueMessage = args[0]
            assert queueMessage.userId == "123"

            return new Try.Success<Void>(null)
        }

        then:
        maybeResults.isSuccess()
    }

    def "queueCanvasSubmissionUpdateForAllStudents: insertMessage fails, i fail"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdateForAllStudents(true)

        then:
        1 * userService.getUsers(Arrays.asList("ROLE_STUDENT")) >> new Try.Success<List<User>>([new User(id: "123")])
        1 * userService.saveUser(_) >> { args -> new Try.Success<User>(args[0]) }
        1 * messageRepository.insertMessage(_) >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "queueCanvasSubmissionUpdateForAllStudents: saveUser fails, i fail"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdateForAllStudents(true)

        then:
        1 * userService.getUsers(Arrays.asList("ROLE_STUDENT")) >> new Try.Success<List<User>>([new User(id: "123")])
        1 * userService.saveUser(_) >> new Try.Failure<Void>(new Exception())
        0 * messageRepository.insertMessage(_)

        then:
        maybeResults.isFailure()
    }

    def "queueCanvasSubmissionUpdateForAllStudents: getUsers fails, i fail"(){
        when:
        Try<Void> maybeResults = messageService.queueCanvasSubmissionUpdateForAllStudents(true)

        then:
        1 * userService.getUsers(Arrays.asList("ROLE_STUDENT")) >> new Try.Failure<List<User>>(new Exception())
        0 * messageRepository.insertMessage(_)

        then:
        maybeResults.isFailure()
    }
}
