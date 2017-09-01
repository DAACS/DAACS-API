package com.daacs.service.hystrix;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.daacs.framework.hystrix.HystrixRequestException;
import com.lambdista.util.Try;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;
import java.io.IOException;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */
public class SendMailHystrixCommand extends GavantHystrixCommand<Void>{

    protected JavaMailSender javaMailSender;
    protected MimeMessage mail;

    public SendMailHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, JavaMailSender javaMailSender, MimeMessage mail) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.javaMailSender = javaMailSender;
        this.mail = mail;
    }

    @Override
    protected String getResourceName() {
        return "javaMailSender";
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            javaMailSender.send(mail);
            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    @Override
    protected Try<Void> failedExecutionFallback(Throwable t){
        if(t instanceof MailAuthenticationException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof MailParseException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof MailPreparationException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof MailSendException){
            return createFailure(t, RETRYABLE);
        }

        if(t instanceof IOException){
            return createFailure(t, RETRYABLE);
        }

        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t){
        return new HystrixRequestException(resourceName, hystrixCommandKey, failureType, t);
    }
}