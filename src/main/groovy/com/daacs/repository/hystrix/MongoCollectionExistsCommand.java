package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoCollectionExistsCommand extends MongoHystrixCommand<Boolean> {
    private static final Logger log = LoggerFactory.getLogger(MongoCollectionExistsCommand.class);

    private String collectionName;

    public MongoCollectionExistsCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.collectionName = collectionName;
    }

    @Override
    protected Try<Boolean> run() throws Exception {
        try {
            return createSuccess(mongoTemplate.collectionExists(collectionName));
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
