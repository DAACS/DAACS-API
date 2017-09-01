package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.event.EventContainer
import com.daacs.model.event.EventType
import com.daacs.model.event.UserEvent
import com.daacs.repository.EventContainerRepository
import com.daacs.repository.EventContainerRepositoryImpl
import com.daacs.repository.hystrix.MongoUpsertCommand
import com.lambdista.util.Try
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class EventContainerRepositorySpec extends Specification {

    EventContainerRepository eventContainerRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoUpsertCommand mongoUpsertCommand

    String userId = UUID.randomUUID().toString()

    FailureTypeException failureTypeException = new FailureTypeException("failure", "failure", FailureType.RETRYABLE, new IOException());

    def setup(){

        mongoUpsertCommand = Mock(MongoUpsertCommand)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoUpsertCommand(*_) >> mongoUpsertCommand

        eventContainerRepository = new EventContainerRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "recordUserEvent: success"(){
        setup:
        UserEvent userEvent = new UserEvent(EventType.LOGIN, ["some":"value"])

        when:
        Try<Void> maybeResults = eventContainerRepository.recordUserEvent(userId, userEvent)

        then:
        1 * hystrixCommandFactory.getMongoUpsertCommand(_, _, _, _, EventContainer.class) >> { args ->
            Query query = args[2]
            Update update = args[3]

            assert query.criteria.find { Criteria criteria ->
                criteria.key == "_id" && criteria.isValue == userId
            }

            assert update.modifierOps.containsKey("\$push")
            assert update.modifierOps.get("\$push").containsKey("userEvents")
            assert update.modifierOps.get("\$push").containsValue(userEvent)

            return mongoUpsertCommand
        }

        1 * mongoUpsertCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "recordUserEvent: mongoUpsertCommand fails, i fail"(){
        setup:
        UserEvent userEvent = new UserEvent(EventType.LOGIN, ["some":"value"])

        when:
        Try<Void> maybeResults = eventContainerRepository.recordUserEvent(userId, userEvent)

        then:
        1 * hystrixCommandFactory.getMongoUpsertCommand(_, _, _, _, EventContainer.class) >> mongoUpsertCommand
        1 * mongoUpsertCommand.execute() >> new Try.Failure<Void>(failureTypeException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureTypeException
    }
}
