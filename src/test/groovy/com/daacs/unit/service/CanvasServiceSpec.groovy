package com.daacs.unit.service

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.NotEnabledException
import com.daacs.service.CanvasService
import com.daacs.service.CanvasServiceImpl
import com.daacs.service.hystrix.http.CanvasUpdateSubmissionHystrixCommand
import com.lambdista.util.Try
import org.apache.http.message.BasicNameValuePair
import spock.lang.Specification
/**
 * Created by chostetter on 4/10/17.
 */
class CanvasServiceSpec extends Specification {
    CanvasService canvasService
    HystrixCommandFactory hystrixCommandFactory
    CanvasUpdateSubmissionHystrixCommand canvasUpdateSubmissionHystrixCommand

    def setup(){
        hystrixCommandFactory = Mock(HystrixCommandFactory)
        canvasUpdateSubmissionHystrixCommand = Mock(CanvasUpdateSubmissionHystrixCommand)
        canvasService = new CanvasServiceImpl(
                enabled: true,
                courseId: 123,
                assignmentId: 123,
                hystrixCommandFactory: hystrixCommandFactory)
    }

    def "isEnabled: true"(){
        setup:
        canvasService = new CanvasServiceImpl(enabled: true)

        when:
        boolean isEnabled = canvasService.isEnabled()

        then:
        isEnabled
    }

    def "isEnabled: false"(){
        setup:
        canvasService = new CanvasServiceImpl(enabled: false)

        when:
        boolean isEnabled = canvasService.isEnabled()

        then:
        !isEnabled
    }

    def "markAssignmentCompleted: success"(){
        when:
        Try<String> maybeResults = canvasService.markAssignmentCompleted("abc123")

        then:
        1 * hystrixCommandFactory.getCanvasUpdateSubmissionHystrixCommand(_, 123, 123, "abc123", [new BasicNameValuePair("submission[posted_grade]", "complete")]) >> canvasUpdateSubmissionHystrixCommand
        1 * canvasUpdateSubmissionHystrixCommand.execute() >> new Try.Success<String>("valid response")

        then:
        maybeResults.isSuccess()
        maybeResults.get() == "valid response"
    }

    def "markAssignmentCompleted: canvasUpdateSubmissionHystrixCommand fails, i fail"(){
        when:
        Try<String> maybeResults = canvasService.markAssignmentCompleted("abc123")

        then:
        1 * hystrixCommandFactory.getCanvasUpdateSubmissionHystrixCommand(_, 123, 123, "abc123", [new BasicNameValuePair("submission[posted_grade]", "complete")]) >> canvasUpdateSubmissionHystrixCommand
        1 * canvasUpdateSubmissionHystrixCommand.execute() >> new Try.Failure<String>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "markAssignmentCompleted: not enabled"(){
        setup:
        canvasService = new CanvasServiceImpl(enabled: false)

        when:
        Try<String> maybeResults = canvasService.markAssignmentCompleted("abc123")

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof NotEnabledException
    }
}
