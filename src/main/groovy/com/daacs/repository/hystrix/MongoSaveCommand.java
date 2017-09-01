package com.daacs.repository.hystrix;

import com.daacs.framework.hystrix.FailureType;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoSaveCommand<T> extends MongoHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(MongoSaveCommand.class);

    private T entity;

    public MongoSaveCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, T entity) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.entity = entity;
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            mongoTemplate.save(entity);
            return createSuccess(null);
        }
        catch (OptimisticLockingFailureException ex){
            return createFailure(ex, FailureType.NOT_RETRYABLE);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
