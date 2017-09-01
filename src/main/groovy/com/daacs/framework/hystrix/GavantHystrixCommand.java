package com.daacs.framework.hystrix;

import com.lambdista.util.Try;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.time.Instant;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */

public abstract class GavantHystrixCommand<T> extends HystrixCommand<Try<T>> {
    protected static final Logger log = LoggerFactory.getLogger(GavantHystrixCommand.class);

    private static final String ERROR_TEMPLATE = "[{0}] required fallback at {1}. [Failed = {2} ({3}), Timeout = {4}, Circuit Open = {5}, Events = {6}].";

    protected final String hystrixGroupKey;
    protected final String hystrixCommandKey;

    public GavantHystrixCommand(String hystrixGroupKey, String hystrixCommandKey) {
        super(
                Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(hystrixGroupKey))
                        .andCommandKey(HystrixCommandKey.Factory.asKey(hystrixCommandKey))
        );
        this.hystrixGroupKey = hystrixGroupKey;
        this.hystrixCommandKey = hystrixCommandKey;
    }

    protected abstract String getResourceName();

    protected void logFallback() {
        String errorMessage = MessageFormat.format(ERROR_TEMPLATE, hystrixCommandKey,
                Instant.now(), this.isFailedExecution(), this.getFailedExecutionException(),
                this.isResponseTimedOut(), this.isCircuitBreakerOpen(), this.getExecutionEvents().size());

        log.warn(errorMessage, this.getFailedExecutionException());
    }

    protected abstract Try<T> failedExecutionFallback(Throwable t);

    protected Try<T> responseTimedOutFallback(Throwable t) {
        if (t == null) {
            t = new Throwable("Response timed out.");
        }

        return createFailure(t, RETRYABLE);
    }

    protected Try<T> responseShortCircuitedFallback(Throwable t) {
        if (t == null) {
            t = new Throwable("Response short circuited.");
        }

        return createFailure(t, RETRYABLE);
    }

    @Override
    protected Try<T> getFallback() {
        logFallback();

        Throwable t = this.getFailedExecutionException();

        if (this.isFailedExecution()) {
            return failedExecutionFallback(t);
        }

        // Response time outs seem to occur sometimes when load balancer up, but services behind it down
        if (this.isResponseTimedOut()) {
            return responseTimedOutFallback(t);
        }

        if (this.isResponseShortCircuited()) {
            return responseShortCircuitedFallback(t);
        }

        // default to non retryable
        return createFailure(t, NOT_RETRYABLE);
    }

    protected Try<T> createFailure(Throwable t, FailureType failureType) {
        return new Try.Failure<>(buildFailureRequestException(getResourceName(), hystrixCommandKey, failureType, t));
    }

    protected abstract FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t);

    protected Try<T> createSuccess(T obj) {
        return new Try.Success<>(obj);
    }
}