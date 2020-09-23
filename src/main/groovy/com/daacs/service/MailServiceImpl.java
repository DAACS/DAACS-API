package com.daacs.service;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.ErrorContainer;
import com.daacs.model.InstructorClass;
import com.daacs.model.User;
import com.daacs.model.assessment.Assessment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdista.util.Try;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.UUID;

/**
 * Created by chostetter on 8/3/16.
 */
@Service
public class MailServiceImpl implements MailService {
    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Value("${mail.help.toAddress}")
    private String helpToAddress;

    @Value("${mail.help.fromAddress}")
    private String helpFromAddress;

    @Value("${mail.help.subject}")
    private String helpSubject;

    @Value("${mail.help.preface}")
    private String helpPreface;

    @Value("${mail.forgot-password.fromAddress}")
    private String forgotPasswordFromAddress;

    @Value("${mail.forgot-password.subject}")
    private String forgotPasswordSubject;

    @Value("${mail.forgot-password.resetLink}")
    private String forgotPasswordResetLink;

    @Value("${mail.canvas-failure.toAddress}")
    private String canvasFailureToAddress;

    @Value("${mail.canvas-failure.fromAddress}")
    private String canvasFailureFromAddress;

    @Value("${mail.canvas-failure.subject}")
    private String canvasFailureSubject;

    @Value("${mail.class-invite.fromAddress}")
    private String classInviteFromAddress;

    @Value("${mail.class-invite.subject}")
    private String classInviteSubject;

    @Value("${mail.class-invite.joinLink}")
    private String classInviteJoinLink;

    @Value("${mail.daacs-invite.joinLink}")
    private String daacsInviteJoinLink;

    @Value("${mail.daacs-invite.subject}")
    private String daacsInviteSubject;

    @Override
    public Try<Void> sendForgotPasswordEmail(String username){
        Try<User> maybeUser = userService.getUserByUsername(username);
        if(maybeUser.isFailure()){
            return new Try.Failure<>(maybeUser.failed().get());
        }

        User user = maybeUser.get();
        if(user == null){
            return new Try.Failure<>(new RepoNotFoundException("User"));
        }

        String code = UUID.randomUUID().toString();
        user.setResetPasswordCode(code);

        Try<User> maybeInsertedCode = userService.saveUser(user);
        if(maybeInsertedCode.isFailure()){
            return new Try.Failure<>(maybeInsertedCode.failed().get());
        }

        StringBuilder body = new StringBuilder();
        body.append("Hello " + user.getFirstName() + " " + user.getLastName() + ",");
        body.append("\n");
        body.append("\n");
        body.append("You recently requested to reset your password for DAACS. Please click the link below or copy and paste it into your browser to reset your password.");
        body.append("\n");
        body.append("\n");
        body.append(MessageFormat.format(forgotPasswordResetLink, code, user.getId()));
        body.append("\n");
        body.append("\n");
        body.append("If you did not request a password reset, please ignore this email.");
        body.append("\n");
        body.append("\n");
        body.append("Thanks,");
        body.append("\n");
        body.append("The DAACS Team");

        MimeMessage mail = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail);

            helper.setTo(user.getUsername());
            helper.setReplyTo(forgotPasswordFromAddress);
            helper.setFrom(forgotPasswordFromAddress);
            helper.setSubject(forgotPasswordSubject);
            helper.setText(body.toString());
        }
        catch (MessagingException ex) {
            return new Try.Failure<>(new FailureTypeException(
                    "email.messageFailure",
                    ex.getMessage(),
                    FailureType.NOT_RETRYABLE,
                    ex
            ));
        }

        return hystrixCommandFactory.getSendMailHystrixCommand("MailServiceImpl-sendForgotPasswordEmail", javaMailSender, mail).execute();
    }

    @Override
    public Try<Void> sendHelpEmail(User user, String assessmentId, String enteredText, String userAgent){

        Try<Assessment> maybeAssessment = assessmentService.getAssessment(assessmentId);
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        StringBuilder body = new StringBuilder();
        body.append(helpPreface);
        body.append('\n');
        body.append('\n');
        body.append(enteredText);
        body.append('\n');
        body.append('\n');
        body.append("user: " + user.getFirstName() + " " + user.getLastName() + " (" + user.getUsername() + ", " + user.getId() + ")");
        body.append('\n');
        body.append("Browser agent: " + userAgent);
        body.append('\n');
        body.append("assessment ID: " + assessmentId);
        body.append('\n');
        body.append("assessment label: " + maybeAssessment.get().getLabel());

        MimeMessage mail = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail);

            helper.setTo(helpToAddress);
            helper.setReplyTo(helpFromAddress);
            helper.setFrom(helpFromAddress);
            helper.setSubject(helpSubject);
            helper.setText(body.toString());
        }
        catch (MessagingException ex) {
            return new Try.Failure<>(new FailureTypeException(
                    "email.messageFailure",
                    ex.getMessage(),
                    FailureType.NOT_RETRYABLE,
                    ex
            ));
        }

        return hystrixCommandFactory.getSendMailHystrixCommand("MailServiceImpl-sendHelpEmail", javaMailSender, mail).execute();
    }

    @Override
    public Try<Void> sendCanvasFailureEmail(User user, Exception exception){

        StringBuilder body = new StringBuilder();
        body.append("Unable to communicate with Canvas for User " + user.getId());
        body.append('\n');
        body.append(user.getFirstName() + " " + user.getLastName());

        if(exception instanceof FailureTypeException){
            FailureTypeException failureTypeException = (FailureTypeException) exception;
            for(ErrorContainer errorContainer : failureTypeException.getErrorContainers()){
                body.append('\n');
                body.append('\n');
                body.append("Code: " + errorContainer.getCode());
                body.append('\n');
                body.append("Detail: " + errorContainer.getDetail());
                body.append('\n');

                try {
                    body.append("MetaData: " + objectMapper.writeValueAsString(errorContainer.getMeta()));
                }
                catch(JsonProcessingException ex){
                    log.error("Unable to include MetaData for CanvasFailureEmail", ex);
                }
            }
        }
        else{
            body.append('\n');
            body.append('\n');
            body.append("Exception: " + exception.getMessage());
            body.append(ExceptionUtils.getStackTrace(exception));
        }

        MimeMessage mail = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail);

            helper.setTo(canvasFailureToAddress);
            helper.setReplyTo(canvasFailureFromAddress);
            helper.setFrom(canvasFailureFromAddress);
            helper.setSubject(canvasFailureSubject);
            helper.setText(body.toString());
        }
        catch (MessagingException ex) {
            return new Try.Failure<>(new FailureTypeException(
                    "email.messageFailure",
                    ex.getMessage(),
                    FailureType.NOT_RETRYABLE,
                    ex
            ));
        }

        return hystrixCommandFactory.getSendMailHystrixCommand("MailServiceImpl-sendCanvasFailureEmail", javaMailSender, mail).execute();
    }

    @Override
    public Try<Void> sendClassInviteEmail(User student, User instructor, InstructorClass instructorClass) {

        StringBuilder body = new StringBuilder();
        body.append("Hello " + student.getFirstName() + " " + student.getLastName() + ",");
        body.append("\n");
        body.append("\n");
        body.append(instructor.getFirstName() + " " + instructor.getLastName() + " has invited you to join their class. Click the link below to join " + instructorClass.getName() + " or copy and paste it into your browser. ");
        body.append("\n");
        body.append("\n");
        body.append(MessageFormat.format(classInviteJoinLink, instructorClass.getId(), student.getId()));
        body.append("\n");
        body.append("\n");
        body.append("Please disregard this message if it was sent to you in error.");
        body.append("\n");
        body.append("The DAACS Team");

        MimeMessage mail = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail);

            helper.setTo(student.getUsername());
            helper.setReplyTo(classInviteFromAddress);
            helper.setFrom(classInviteFromAddress);
            helper.setSubject(classInviteSubject);
            helper.setText(body.toString());
        }
        catch (MessagingException ex) {
            return new Try.Failure<>(new FailureTypeException(
                    "email.messageFailure",
                    ex.getMessage(),
                    FailureType.NOT_RETRYABLE,
                    ex
            ));
        }

        return hystrixCommandFactory.getSendMailHystrixCommand("MailServiceImpl-sendClassInviteEmail", javaMailSender, mail).execute();
    }

    @Override
    public Try<Void> sendDaacsInviteEmail(String username, String classId, User instructor) {
        StringBuilder body = new StringBuilder();
        body.append("Hello,");
        body.append("\n");
        body.append("\n");
        body.append(instructor.getFirstName() + " " + instructor.getLastName() + " has invited you to join DAACS. Follow the link below and then click the 'create account' button on the top right of the daacs site to create your account.");
        body.append("\n");
        body.append("\n");
        body.append(MessageFormat.format(daacsInviteJoinLink, classId, username));
        body.append("\n");
        body.append("\n");
        body.append("Please disregard this message if it was sent to you in error.");
        body.append("\n");
        body.append("The DAACS Team");

        MimeMessage mail = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mail);

            helper.setTo(username);
            helper.setReplyTo(classInviteFromAddress);
            helper.setFrom(classInviteFromAddress);
            helper.setSubject(daacsInviteSubject);
            helper.setText(body.toString());
        }
        catch (MessagingException ex) {
            return new Try.Failure<>(new FailureTypeException(
                    "email.messageFailure",
                    ex.getMessage(),
                    FailureType.NOT_RETRYABLE,
                    ex
            ));
        }

        return hystrixCommandFactory.getSendMailHystrixCommand("MailServiceImpl-sendDaacsInviteEmail", javaMailSender, mail).execute();
    }
}
