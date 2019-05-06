package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.AlreadyExistsException
import com.daacs.framework.exception.NotFoundException
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.User
import com.daacs.model.UserSearchResult
import com.daacs.repository.UserRepository
import com.daacs.repository.UserRepositoryImpl
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import spock.lang.Specification
/**
 * Created by chostetter on 6/22/16.
 */
class UserRepositorySpec extends Specification {

    UserRepository userRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoFindOneCommand mongoFindOneCommand
    MongoFindByIdCommand mongoFindByIdCommand
    MongoSaveCommand mongoSaveCommand
    MongoInsertCommand mongoInsertCommand
    MongoAggregateCommand mongoAggregateCommand
    MongoFindCommand mongoFindCommand

    User dummyUser = new User("dummyuser", "dummypass", "Mr", "Dummy", true, [], "dummysecondaryid", "canvassisid")

    String userId = UUID.randomUUID().toString()

    FailureTypeException notFoundFailureTypeException = new FailureTypeException("failure", "failure", FailureType.NOT_RETRYABLE, new NotFoundException("not found"));
    FailureTypeException ioFailureTypeException = new FailureTypeException("failure", "failure", FailureType.RETRYABLE, new IOException());

    def setup(){
        dummyUser.setId(userId)

        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoFindOneCommand = Mock(MongoFindOneCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)
        mongoInsertCommand = Mock(MongoInsertCommand)
        mongoAggregateCommand = Mock(MongoAggregateCommand)
        mongoFindCommand = Mock(MongoFindCommand)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoFindOneCommand(*_) >> mongoFindOneCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand

        userRepository = new UserRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory)
    }

    def "getUser: success"(){
        when:
        Try<User> maybeUser = userRepository.getUser(userId)

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<User>(dummyUser)
        maybeUser.isSuccess()
        maybeUser.get().getId() == userId
    }

    def "getUser: IO failure"(){
        when:
        Try<User> maybeUser = userRepository.getUser(userId)

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<User>(ioFailureTypeException)
        maybeUser.isFailure()
    }

    def "getUser: not found failure"(){
        when:
        Try<User> maybeUser = userRepository.getUser(userId)

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<User>(notFoundFailureTypeException)
        maybeUser.isFailure()
    }

    def "getByUsername: success"(){
        when:
        Try<User> maybeUser = userRepository.getUserByUsername(dummyUser.getUsername())

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<User>(dummyUser)
        maybeUser.isSuccess()
        maybeUser.get().getUsername() == dummyUser.getUsername()
    }

    def "getUserBySecondaryId: success"(){
        when:
        Try<User> maybeUser = userRepository.getUserBySecondaryId(dummyUser.getSecondaryId())

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<User>(dummyUser)
        maybeUser.isSuccess()
        maybeUser.get().getSecondaryId() == dummyUser.getSecondaryId()
    }

    def "saveUser: success"(){
        when:
        Try<Void> maybeResults = userRepository.saveUser(dummyUser)

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertUser: success"(){
        when:
        Try<Void> maybeResults = userRepository.insertUser(dummyUser)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<User>(null)

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertUser: already exists"(){
        when:
        Try<Void> maybeResults = userRepository.insertUser(dummyUser)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<User>(dummyUser)

        then:
        0 * mongoInsertCommand.execute()
        maybeResults.isFailure()
        maybeResults.failed().get() instanceof AlreadyExistsException
    }

    def "insertUser: io exception #1"(){
        when:
        Try<Void> maybeResults = userRepository.insertUser(dummyUser)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Failure<User>(ioFailureTypeException)

        then:
        0 * mongoInsertCommand.execute()
        maybeResults.isFailure()
    }

    def "insertUser: io exception #2"(){
        when:
        Try<Void> maybeResults = userRepository.insertUser(dummyUser)

        then:
        1 * mongoFindOneCommand.execute() >> new Try.Success<User>(null)

        then:
        1 * mongoInsertCommand.execute() >> new Try.Failure<Void>(ioFailureTypeException)
        maybeResults.isFailure()
    }

    def "searchUsers: success"(){
        when:
        Try<List<UserSearchResult>> maybeSearchResults = userRepository.searchUsers(["curt", "hostetter"], 10);

        then:
        1 * hystrixCommandFactory.getMongoAggregateCommand(*_) >> { arguments ->
            Aggregation agg = arguments[2]

            assert agg.operations.get(0) instanceof GroupOperation

            assert agg.operations.get(1) instanceof ProjectionOperation
            assert agg.operations.get(1).projections.find { it.field.name == "firstName" } != null
            assert agg.operations.get(1).projections.find { it.field.name == "lastName" } != null
            assert agg.operations.get(1).projections.find { it.field.name == "username" } != null
            assert agg.operations.get(1).projections.find { it.field.name == "_id" } != null

            //one per search term
            assert agg.operations.get(2) instanceof MatchOperation
            assert agg.operations.get(2).criteriaDefinition.key == "searchField"
            assert agg.operations.get(2).criteriaDefinition.isValue.pattern == "curt"

            assert agg.operations.get(3) instanceof MatchOperation
            assert agg.operations.get(3).criteriaDefinition.key == "searchField"
            assert agg.operations.get(3).criteriaDefinition.isValue.pattern == "hostetter"

            assert agg.operations.get(4) instanceof SortOperation

            assert agg.operations.get(5) instanceof LimitOperation
            assert agg.operations.get(5).maxElements == 10

            return mongoAggregateCommand
        }

        then:
        1 * mongoAggregateCommand.execute() >> new Try.Success<List<User>>([new User(firstName: "curt", lastName: "hostetter")])

        then:
        maybeSearchResults.isSuccess()
        maybeSearchResults.get().size() == 1
        maybeSearchResults.get().get(0).getFirstName() == "curt"
        maybeSearchResults.get().get(0).getLastName() == "hostetter"
    }

    def "searchUsers: mongoAggregateCommand fails, i fail"(){
        when:
        Try<List<UserSearchResult>> maybeSearchResults = userRepository.searchUsers(["curt", "hostetter"], 10);

        then:
        1 * hystrixCommandFactory.getMongoAggregateCommand(*_) >> mongoAggregateCommand

        then:
        1 * mongoAggregateCommand.execute() >> new Try.Failure<List<User>>(new Exception())

        then:
        maybeSearchResults.isFailure()
    }

    def "getUsers: success"(){
        when:
        Try<List<User>> maybeResults = userRepository.getUsers(["ROLE_STUDENT"])

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(_, _, _, User.class) >> { args ->
            Query query = args[2]
            assert query.criteria.find { Criteria criteria ->
                criteria.key == "roles" && criteria.criteria.containsKey("\$in") && criteria.criteria.get("\$in").containsAll(["ROLE_STUDENT"])
            } != null

            return mongoFindCommand
        }
        1 * mongoFindCommand.execute() >> new Try.Success<List<User>>([dummyUser])

        then:
        maybeResults.isSuccess()
        maybeResults.get().size() == 1
        maybeResults.get().contains(dummyUser)
    }

    def "getUsers: mongoFindCommand fails, i fail"(){
        when:
        Try<List<User>> maybeResults = userRepository.getUsers(["ROLE_STUDENT"])

        then:
        1 * hystrixCommandFactory.getMongoFindCommand(_, _, _, User.class) >> mongoFindCommand
        1 * mongoFindCommand.execute() >> new Try.Failure<List<User>>(new Exception())

        then:
        maybeResults.isFailure()
    }
}
