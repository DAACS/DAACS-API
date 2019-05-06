package com.daacs.service.hystrix.s3;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.lambdista.util.Try;
import com.daacs.framework.exception.S3RequestException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;


/**
 * Created by chostetter on 12/22/16.
 */
public abstract class S3HystrixCommand<T> extends GavantHystrixCommand<T> {

    protected AmazonS3 s3;

    protected S3HystrixCommand(String hystrixGroupKey, String hystrixCommandKey, AmazonS3 s3) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.s3 = s3;
    }

    @Override
    protected String getResourceName() {
        return "S3";
    }

    @Override
    protected Try<T> failedExecutionFallback(Throwable t) {
        if (t instanceof AmazonServiceException) {
            switch (((AmazonServiceException) t).getErrorType()) {
                case Client:
                    return createFailure(t, NOT_RETRYABLE);

                case Unknown:
                case Service:
                    if (((AmazonServiceException) t).isRetryable()) {
                        return createFailure(t, RETRYABLE);
                    }

                    return createFailure(t, NOT_RETRYABLE);
            }
        }

        if (t instanceof AmazonClientException) {
            return createFailure(t, RETRYABLE);
        }

        return createFailure(t, NOT_RETRYABLE);
    }

    @Override
    protected FailureTypeException buildFailureRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable t) {
        return new S3RequestException(hystrixCommandKey, failureType, t);
    }
}