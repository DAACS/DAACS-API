package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.model.event.EventContainer;
import com.daacs.model.event.UserEvent;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;


@Repository
public class EventContainerRepositoryImpl implements EventContainerRepository {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<Void> recordUserEvent(String userId, UserEvent userEvent){
        return hystrixCommandFactory.getMongoUpsertCommand(
                "EventContainerRepositoryImpl-recordUserEvent",
                mongoTemplate,
                Query.query(Criteria.where("_id").is(userId)),
                new Update().push("userEvents", userEvent),
                EventContainer.class).execute();
    }
}
