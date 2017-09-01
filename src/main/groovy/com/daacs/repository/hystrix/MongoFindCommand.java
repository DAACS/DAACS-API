package com.daacs.repository.hystrix;

import com.daacs.framework.exception.NotFoundException;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoFindCommand<T> extends MongoHystrixCommand<List<T>> {
    private static final Logger log = LoggerFactory.getLogger(MongoFindCommand.class);

    private Query query;
    private Class<T> entityClass;

    public MongoFindCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, Query query, Class<T> entityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.query = query;
        this.entityClass = entityClass;
    }

    @Override
    protected Try<List<T>> run() throws Exception {
        try {
            List<T> result = mongoTemplate.find(query, entityClass);

            if(result == null){
                return failedExecutionFallback(new NotFoundException("Unable to find with query " + query.toString()));
            }

            return createSuccess(result);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
