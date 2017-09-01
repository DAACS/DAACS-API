package com.daacs.unit.service

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.User
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.CATAssessment
import com.daacs.service.AssessmentService
import com.daacs.service.MailService
import com.daacs.service.MailServiceImpl
import com.daacs.service.UserService
import com.daacs.service.hystrix.SendMailHystrixCommand
import com.lambdista.util.Try
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

import javax.mail.Address
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
/**
 * Created by chostetter on 8/5/16.
 */
class MailServiceSpec extends Specification {

    MailService mailService;
    UserService userService;

    JavaMailSender javaMailSender;
    AssessmentService assessmentService;
    HystrixCommandFactory hystrixCommandFactory;
    SendMailHystrixCommand sendMailHystrixCommand;

    MimeMessage mimeMessage;

    String helpToAddress = "help@toaddress.com";
    String helpFromAddress = "help@fromaddress.com";
    String helpSubject = "help subject";
    String helpPreface = "help preface";

    String forgotPasswordFromAddress = "fp@fromaddress.com";
    String forgotPasswordSubject = "forgot password subject";
    String forgotPasswordResetLink = "http://resetlink?code={0}&userId={1}";

    String canvasFailureFromAddress = "cf@fromaddress.com";
    String canvasFailureToAddress = "cf@toaddress.com";
    String canvasFailureSubject = "Canvas Failure";

    User dummyUser

    Assessment dummyAssessment = new CATAssessment(id: "assessment-1", label: "Dummy Assessment")

    FailureTypeException failureTypeException = new FailureTypeException("failure", "failure", FailureType.NOT_RETRYABLE, new NotFoundException(""))

    def setup(){
        dummyUser = new User("username", "Mr", "Dummy");
        dummyUser.setId("123")

        javaMailSender = Mock(JavaMailSender)
        assessmentService = Mock(AssessmentService)
        userService = Mock(UserService)
        hystrixCommandFactory = Mock(HystrixCommandFactory)
        sendMailHystrixCommand = Mock(SendMailHystrixCommand)

        mimeMessage = Mock(MimeMessage)

        mailService = new MailServiceImpl(
                objectMapper: new ObjectMapperConfig().objectMapper(),
                helpToAddress: helpToAddress,
                helpFromAddress: helpFromAddress,
                helpSubject: helpSubject,
                helpPreface: helpPreface,
                javaMailSender: javaMailSender,
                assessmentService: assessmentService,
                hystrixCommandFactory: hystrixCommandFactory,
                userService: userService,
                forgotPasswordFromAddress: forgotPasswordFromAddress,
                forgotPasswordSubject: forgotPasswordSubject,
                forgotPasswordResetLink: forgotPasswordResetLink,
                canvasFailureFromAddress: canvasFailureFromAddress,
                canvasFailureToAddress: canvasFailureToAddress,
                canvasFailureSubject: canvasFailureSubject)

        javaMailSender.createMimeMessage() >> mimeMessage
    }

    def "sendHelpEmail: sends help message"(){
        setup:
        String enteredText = "help me obi wan kenobi you're my only hope"
        String userAgent = "Chrome 25"

        when:
        Try<Void> maybeResults = mailService.sendHelpEmail(dummyUser, dummyAssessment.getId(), enteredText, userAgent)

        then:
        1 * assessmentService.getAssessment(dummyAssessment.getId()) >> new Try.Success<Assessment>(dummyAssessment)
        1 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(helpToAddress))
        1 * mimeMessage.setReplyTo([new InternetAddress(helpFromAddress)] as Address[])
        1 * mimeMessage.setFrom(new InternetAddress(helpFromAddress))
        1 * mimeMessage.setSubject(helpSubject)
        1 * mimeMessage.setText(_) >> { arguments ->
            String bodyText = arguments[0]
            assert bodyText.contains(enteredText)
        }

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "sendHelpEmail: getAssessment fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendHelpEmail(dummyUser, dummyAssessment.getId(), "", "")

        then:
        1 * assessmentService.getAssessment(dummyAssessment.getId()) >> new Try.Failure<Assessment>(new Exception())
        0 * mimeMessage.setText(_)
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
    }

    def "sendHelpEmail: MessagingException, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendHelpEmail(dummyUser, dummyAssessment.getId(), "", "")

        then:
        1 * assessmentService.getAssessment(dummyAssessment.getId()) >> new Try.Success<Assessment>(dummyAssessment)
        1 * mimeMessage.setText(_) >> { throw new MessagingException() }
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
    }

    def "sendHelpEmail: hystrix command fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendHelpEmail(dummyUser, dummyAssessment.getId(), "", "")

        then:
        1 * assessmentService.getAssessment(dummyAssessment.getId()) >> new Try.Success<Assessment>(dummyAssessment)
        1 * mimeMessage.setText(_)

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }


    def "sendForgotPasswordEmail: sends forgot password message"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(dummyUser)
        1 * userService.saveUser(_) >> { args ->
            User user = args[0]
            assert user.getResetPasswordCode() != null
            return new Try.Success<User>(user)
        }

        then:
        1 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(dummyUser.getUsername()))
        1 * mimeMessage.setReplyTo([new InternetAddress(forgotPasswordFromAddress)] as Address[])
        1 * mimeMessage.setFrom(new InternetAddress(forgotPasswordFromAddress))
        1 * mimeMessage.setSubject(forgotPasswordSubject)
        1 * mimeMessage.setText(_) >> { arguments ->
            String bodyText = arguments[0]
            assert bodyText.contains("code=")
            assert bodyText.contains("userId=123")
        }

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "sendForgotPasswordEmail: getUserByUsername fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Failure<User>(failureTypeException)
        0 * mimeMessage.setText(_)
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "sendForgotPasswordEmail: getUserByUsername returns null user"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(null)
        0 * mimeMessage.setText(_)
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof RepoNotFoundException
    }

    def "sendForgotPasswordEmail: saveUser fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(dummyUser)
        1 * userService.saveUser(_) >> new Try.Failure<User>(failureTypeException)
        0 * mimeMessage.setText(_)
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "sendForgotPasswordEmail: MessagingException, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(dummyUser)
        1 * userService.saveUser(_) >> new Try.Success<User>(dummyUser)
        1 * mimeMessage.setText(_) >> { throw new MessagingException() }
        0 * hystrixCommandFactory.getSendMailHystrixCommand(*_)

        then:
        maybeResults.isFailure()
    }

    def "sendForgotPasswordEmail: hystrix command fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendForgotPasswordEmail(dummyUser.getUsername())

        then:
        1 * userService.getUserByUsername(dummyUser.getUsername()) >> new Try.Success<User>(dummyUser)
        1 * userService.saveUser(_) >> new Try.Success<User>(dummyUser)

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Failure<Void>(failureTypeException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }

    def "sendCanvasFailureEmail: sends canvas failure message"(){
        setup:
        Exception exception = new FailureTypeException("some.code", "some description", FailureType.RETRYABLE, ["field":"value"])

        when:
        Try<Void> maybeResults = mailService.sendCanvasFailureEmail(dummyUser, exception)

        then:
        1 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(canvasFailureToAddress))
        1 * mimeMessage.setReplyTo([new InternetAddress(canvasFailureFromAddress)] as Address[])
        1 * mimeMessage.setFrom(new InternetAddress(canvasFailureFromAddress))
        1 * mimeMessage.setSubject(canvasFailureSubject)
        1 * mimeMessage.setText(_) >> { arguments ->
            String bodyText = arguments[0]
            assert bodyText.contains("some.code")
            assert bodyText.contains("some description")
            assert bodyText.contains('"field":"value"')
        }

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "sendCanvasFailureEmail: sendMailHystrixCommand fails, i fail"(){
        when:
        Try<Void> maybeResults = mailService.sendCanvasFailureEmail(dummyUser, new Exception())

        then:
        1 * hystrixCommandFactory.getSendMailHystrixCommand(_, javaMailSender, mimeMessage) >> sendMailHystrixCommand
        1 * sendMailHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }
}
