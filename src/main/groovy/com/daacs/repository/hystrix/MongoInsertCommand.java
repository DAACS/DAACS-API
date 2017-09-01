package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoInsertCommand<T> extends MongoHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(MongoInsertCommand.class);

    private T entity;

    public MongoInsertCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, T entity) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.entity = entity;
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            mongoTemplate.insert(entity);
            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
