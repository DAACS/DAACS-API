package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoFindByIdCommand<T> extends MongoHystrixCommand<T> {
    private static final Logger log = LoggerFactory.getLogger(MongoFindByIdCommand.class);

    private String id;
    private Class<T> entityClass;

    public MongoFindByIdCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, String id, Class<T> entityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.id = id;
        this.entityClass = entityClass;
    }

    @Override
    protected Try<T> run() throws Exception {
        try {
            T result = mongoTemplate.findById(id, entityClass);

            return createSuccess(result);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
