package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoFindOneCommand<T> extends MongoHystrixCommand<T> {
    private static final Logger log = LoggerFactory.getLogger(MongoFindOneCommand.class);

    private Query query;
    private Class<T> entityClass;

    public MongoFindOneCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.query = query;
        this.entityClass = entityClass;
    }

    @Override
    protected Try<T> run() throws Exception {
        try {
            T result = mongoTemplate.findOne(query, entityClass);

            return createSuccess(result);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
