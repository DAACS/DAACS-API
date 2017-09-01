package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoCreateCollectionCommand extends MongoHystrixCommand<DBCollection> {
    private static final Logger log = LoggerFactory.getLogger(MongoCreateCollectionCommand.class);

    private String collectionName;
    private CollectionOptions collectionOptions;

    public MongoCreateCollectionCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName, CollectionOptions collectionOptions) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.collectionName = collectionName;
        this.collectionOptions = collectionOptions;
    }

    @Override
    protected Try<DBCollection> run() throws Exception {
        try {
            DBCollection dbCollection = mongoTemplate.createCollection(collectionName, collectionOptions);
            return createSuccess(dbCollection);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
