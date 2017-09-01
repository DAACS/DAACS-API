package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.hystrix.FailureType
import com.daacs.framework.hystrix.FailureTypeException
import com.daacs.model.queue.MessageStats
import com.daacs.model.queue.QueueMessage
import com.daacs.repository.MessageRepository
import com.daacs.repository.MessageRepositoryImpl
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import com.mongodb.DBCollection
import com.mongodb.DBCursor
import spock.lang.Specification

/**
 * Created by chostetter on 6/22/16.
 */
class MessageRepositorySpec extends Specification {

    MessageRepository messageRepository

    HystrixCommandFactory hystrixCommandFactory
    MongoTailableCursorCommand mongoTailableCursorCommand
    MongoCollectionExistsCommand mongoCollectionExistsCommand
    MongoCreateCollectionCommand mongoCreateCollectionCommand
    MongoGetCollectionCommand mongoGetCollectionCommand
    MongoFindByIdCommand mongoFindByIdCommand
    MongoSaveCommand mongoSaveCommand
    MongoInsertByCollectionCommand mongoInsertByCollectionCommand

    DBCursor dbCursor
    DBCollection dbCollection

    FailureTypeException ioFailureTypeException = new FailureTypeException("failure", "failure", FailureType.RETRYABLE, new IOException());

    def setup(){

        dbCursor = Mock(DBCursor)
        dbCollection = Mock(DBCollection)

        hystrixCommandFactory = Mock(HystrixCommandFactory)
        mongoTailableCursorCommand = Mock(MongoTailableCursorCommand)
        mongoCollectionExistsCommand = Mock(MongoCollectionExistsCommand)
        mongoCreateCollectionCommand = Mock(MongoCreateCollectionCommand)
        mongoGetCollectionCommand = Mock(MongoGetCollectionCommand)
        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)
        mongoInsertByCollectionCommand = Mock(MongoInsertByCollectionCommand)

        hystrixCommandFactory.getMongoTailableCursorCommand(*_) >> mongoTailableCursorCommand
        hystrixCommandFactory.getMongoCollectionExistsCommand(*_) >> mongoCollectionExistsCommand
        hystrixCommandFactory.getMongoCreateCollectionCommand(*_) >> mongoCreateCollectionCommand
        hystrixCommandFactory.getMongoGetCollectionCommand(*_) >> mongoGetCollectionCommand
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand
        hystrixCommandFactory.getMongoInsertByCollectionCommand(*_) >> mongoInsertByCollectionCommand

        messageRepository = new MessageRepositoryImpl(
                hystrixCommandFactory: hystrixCommandFactory,
                messageCollectionName: "collection",
                statsCollectionName: "stats",
                maxDocuments: 10,
                size: 1000)
    }


    def "getCursor: success"(){
        when:
        Try<DBCursor> maybeDBCursor = messageRepository.getCursor()

        then:
        1 * mongoTailableCursorCommand.execute() >> new Try.Success<DBCursor>(Mock(DBCursor))
        maybeDBCursor.isSuccess()
    }

    def "getCursor: IO failure"(){
        when:
        Try<DBCursor> maybeDBCursor = messageRepository.getCursor()

        then:
        1 * mongoTailableCursorCommand.execute() >> new Try.Failure<DBCursor>(ioFailureTypeException)
        maybeDBCursor.isFailure()
    }


    def "collectionExists: success"(){
        when:
        Try<Boolean> maybeCollectionExists = messageRepository.collectionExists()

        then:
        1 * mongoCollectionExistsCommand.execute() >> new Try.Success<Boolean>(true)
        maybeCollectionExists.isSuccess()
    }

    def "collectionExists: IO failure"(){
        when:
        Try<Boolean> maybeCollectionExists = messageRepository.collectionExists()

        then:
        1 * mongoCollectionExistsCommand.execute() >> new Try.Failure<Boolean>(ioFailureTypeException)
        maybeCollectionExists.isFailure()
    }


    def "createCollection: success"(){
        when:
        Try<DBCollection> maybeCollection = messageRepository.createCollection()

        then:
        1 * mongoCreateCollectionCommand.execute() >> new Try.Success<DBCollection>(Mock(DBCollection))
        maybeCollection.isSuccess()
    }

    def "createCollection: IO failure"(){
        when:
        Try<DBCollection> maybeCollection = messageRepository.createCollection()

        then:
        1 * mongoCreateCollectionCommand.execute() >> new Try.Failure<DBCollection>(ioFailureTypeException)
        maybeCollection.isFailure()
    }


    def "getCollection: success"(){
        when:
        Try<DBCollection> maybeCollection = messageRepository.getCollection()

        then:
        1 * mongoGetCollectionCommand.execute() >> new Try.Success<DBCollection>(Mock(DBCollection))
        maybeCollection.isSuccess()
    }

    def "getCollection: IO failure"(){
        when:
        Try<DBCollection> maybeCollection = messageRepository.getCollection()

        then:
        1 * mongoGetCollectionCommand.execute() >> new Try.Failure<DBCollection>(ioFailureTypeException)
        maybeCollection.isFailure()
    }


    def "getMessageStats: success"(){
        when:
        Try<MessageStats> maybeMessageStats = messageRepository.getMessageStats()

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<MessageStats>(Mock(MessageStats))
        maybeMessageStats.isSuccess()
    }

    def "getMessageStats: IO failure"(){
        when:
        Try<MessageStats> maybeMessageStats = messageRepository.getMessageStats()

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<MessageStats>(ioFailureTypeException)
        maybeMessageStats.isFailure()
    }


    def "updateMessageStats: success"(){
        when:
        Try<Void> maybeResults = messageRepository.updateMessageStats(Mock(MessageStats))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "updateMessageStats: IO failure"(){
        when:
        Try<Void> maybeResults = messageRepository.updateMessageStats(Mock(MessageStats))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Failure<Void>(ioFailureTypeException)
        maybeResults.isFailure()
    }


    def "insertMessage: success"(){
        when:
        Try<Void> maybeResults = messageRepository.insertMessage(Mock(QueueMessage))

        then:
        1 * mongoInsertByCollectionCommand.execute() >> new Try.Success<Void>(null)
        maybeResults.isSuccess()
    }

    def "insertMessage: IO failure"(){
        when:
        Try<Void> maybeResults = messageRepository.insertMessage(Mock(QueueMessage))

        then:
        1 * mongoInsertByCollectionCommand.execute() >> new Try.Failure<Void>(ioFailureTypeException)
        maybeResults.isFailure()
    }
    
}
