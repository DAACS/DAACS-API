package com.daacs.repository.hystrix;

import com.lambdista.util.Try;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Bytes;
import com.mongodb.DBCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoTailableCursorCommand extends MongoHystrixCommand<DBCursor> {
    private static final Logger log = LoggerFactory.getLogger(MongoTailableCursorCommand.class);

    private String collectionName;

    public MongoTailableCursorCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, String collectionName) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.collectionName = collectionName;
    }

    @Override
    protected Try<DBCursor> run() throws Exception {
        try {
            DBCursor dbCursor = mongoTemplate.getCollection(collectionName).find()
                    .sort(BasicDBObjectBuilder.start("$natural", 1).get())
                    .addOption(Bytes.QUERYOPTION_TAILABLE | Bytes.QUERYOPTION_AWAITDATA);
            return createSuccess(dbCursor);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
