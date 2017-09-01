package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoInsertByCollectionCommand<T> extends MongoHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(MongoInsertByCollectionCommand.class);

    private T entity;
    private String collectionName;

    public MongoInsertByCollectionCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, T entity, String collectionName) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.entity = entity;
        this.collectionName = collectionName;
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            mongoTemplate.insert(entity, collectionName);
            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
