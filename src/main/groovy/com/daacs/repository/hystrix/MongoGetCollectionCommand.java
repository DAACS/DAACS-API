package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoGetCollectionCommand extends MongoHystrixCommand<DBCollection> {
    private static final Logger log = LoggerFactory.getLogger(MongoGetCollectionCommand.class);

    private String collectionName;

    public MongoGetCollectionCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.collectionName = collectionName;
    }

    @Override
    protected Try<DBCollection> run() throws Exception {
        try {
            DBCollection dbCollection = mongoTemplate.getCollection(collectionName);
            return createSuccess(dbCollection);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
