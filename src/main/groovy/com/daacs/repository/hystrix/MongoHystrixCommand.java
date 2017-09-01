package com.daacs.repository.hystrix;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.framework.hystrix.GavantHystrixCommand;
import com.daacs.framework.hystrix.HystrixRequestException;
import com.lambdista.util.Try;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;

import static com.daacs.framework.hystrix.FailureType.NOT_RETRYABLE;
import static com.daacs.framework.hystrix.FailureType.RETRYABLE;

/**
 * Created by chostetter on 6/23/16.
 */
public abstract class MongoHystrixCommand<T> extends GavantHystrixCommand<T>{

    protected MongoTemplate mongoTemplate;

    public MongoHystrixCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate) {
        super(hystrixGroupKey, hystrixCommandKey);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected String getResourceName() {
        return "MongoDB";
    }

    @Override
    protected Try<T> failedExecutionFallback(Throwable t){
        if(t instanceof DuplicateKeyException){
            return createFailure(t, NOT_RETRYABLE);
        }

        if(t instanceof OptimisticLockingFailureException){
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