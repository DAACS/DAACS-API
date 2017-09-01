package com.daacs.repository.hystrix;

import com.daacs.framework.exception.NotFoundException;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;

/**
 * Created by chostetter on 6/23/16.
 */

public class MongoAggregateCommand<T> extends MongoHystrixCommand<List<T>> {
    private static final Logger log = LoggerFactory.getLogger(MongoAggregateCommand.class);

    private Aggregation aggregation;
    private Class<?> inputEntityClass;
    private Class<T> outputEntityClass;

    public MongoAggregateCommand(String hystrixGroupKey, String hystrixCommandKey, MongoTemplate mongoTemplate, Aggregation aggregation, Class<?> inputEntityClass, Class<T> outputEntityClass) {
        super(hystrixGroupKey, hystrixCommandKey, mongoTemplate);
        this.aggregation = aggregation;
        this.inputEntityClass = inputEntityClass;
        this.outputEntityClass = outputEntityClass;
    }

    @Override
    protected Try<List<T>> run() throws Exception {
        try {

            AggregationResults<T> groupResults = mongoTemplate.aggregate(aggregation, inputEntityClass, outputEntityClass);
            List<T> result = groupResults.getMappedResults();

            if(result == null){
                return failedExecutionFallback(new NotFoundException("Unable to find with aggregation " + aggregation.toString()));
            }

            return createSuccess(result);
        }
        catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

}
