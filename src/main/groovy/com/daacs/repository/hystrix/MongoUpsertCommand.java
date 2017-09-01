package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Created by chostetter on 12/15/16.
 */

public class MongoUpsertCommand extends MongoHystrixCommand<Void> {
    private static final Logger log = LoggerFactory.getLogger(MongoUpsertCommand.class);

    private Class<?> entityClass;
    private Query query;
    private Update update;

    public MongoUpsertCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Update update, Class<?> entityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.entityClass = entityClass;
        this.query = query;
        this.update = update;
    }

    @Override
    protected Try<Void> run() throws Exception {
        try {
            mongoTemplate.upsert(query, update, entityClass);
            return createSuccess(null);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
