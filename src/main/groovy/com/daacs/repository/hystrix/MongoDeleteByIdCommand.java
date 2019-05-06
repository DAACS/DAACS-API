package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import com.mongodb.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Created by mgoldman on 2/28/19.
 */

public class MongoDeleteByIdCommand<T> extends MongoHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(MongoDeleteByIdCommand.class);

    private Query query;
    private Class<T> entityClass;

    public MongoDeleteByIdCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.query = query;
        this.entityClass = entityClass;
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            WriteResult result = mongoTemplate.remove(query, entityClass);

            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
