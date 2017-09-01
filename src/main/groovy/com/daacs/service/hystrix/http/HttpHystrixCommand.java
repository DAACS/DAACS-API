package com.daacs.service.hystrix.http;


import com.daacs.framework.exception.UnexpectedHttpStatusException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.daacs.framework.hystrix.HystrixRequestException;
import com.lambdista.util.Try;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;


/**
 * Created by chostetter on 12/2/16.
 */
public abstract class HttpHystrixCommand<T> extends GavantHystrixCommand<T> {

    protected RestTemplate restTemplate;
    protected String url;

    protected HttpHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, String url) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.url = url;
    }

    @Override
    protected String getResourceName() {
        return "http";
    }

    @Override
    protected Try<T> failedExecutionFallback(Throwable t){
        if(t instanceof IOException){
            return createFailure(t, RETRYABLE);
        }

        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t){
        return new HystrixRequestException(resourceName, hystrixCommandKey, failureType, t);
    }

    protected Try<T> failedHttpFallback(HttpStatus httpStatus, String responseBody){
        if(httpStatus.is3xxRedirection()){
            return new Try.Failure<>(new UnexpectedHttpStatusException(getResourceName(), httpStatus, responseBody, FailureType.NOT_RETRYABLE));
        }

        if(httpStatus.is4xxClientError()){
            return new Try.Failure<>(new UnexpectedHttpStatusException(getResourceName(), httpStatus, responseBody, FailureType.NOT_RETRYABLE));
        }

        if(httpStatus.is5xxServerError()){
            return new Try.Failure<>(new UnexpectedHttpStatusException(getResourceName(), httpStatus, responseBody, FailureType.RETRYABLE));
        }

        return new Try.Failure<>(new UnexpectedHttpStatusException(getResourceName(), httpStatus, responseBody, FailureType.RETRYABLE));
    }
}