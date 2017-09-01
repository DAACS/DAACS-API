package com.daacs.unit.component.queuing

import com.daacs.component.queuing.MongoListener
import com.daacs.component.queuing.QueueListener
import com.daacs.component.queuing.Retry
import com.daacs.component.queuing.handlers.MessageHandler
import com.daacs.model.queue.MessageStats
import com.daacs.model.queue.QueueMessage
import com.daacs.repository.MessageRepository
import com.lambdista.util.Try
import com.mongodb.DBCollection
import com.mongodb.DBCursor
import com.mongodb.DBObject
import com.mongodb.MongoException
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.convert.MongoConverter
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.ExecutorService
/**
 * Created by chostetter on 8/11/16.
 */
class MongoListenerSpec extends Specification{
    MessageRepository messageRepository
    MongoConverter mongoConverter

    MessageHandler messageHandler

    DBCollection dbCollection
    DBCursor dbCursor
    DBObject dbObject
    QueueMessage queueMessage

    boolean queueEnabled
    int processorThreads

    ExecutorService listenerExecutor
    ExecutorService processorExecutor

    QueueListener queueListener
    QueueListener spiedQueueListener

    Retry retry

    def setup(){

        messageRepository = Mock(MessageRepository)
        mongoConverter = Mock(MongoConverter)
        messageHandler = Mock(MessageHandler)

        dbCollection = Mock(DBCollection)
        dbCursor = Mock(DBCursor)
        dbObject = Mock(DBObject)
        queueMessage = Mock(QueueMessage)

        queueEnabled = true
        processorThreads = 1

        listenerExecutor = Mock(ExecutorService)
        processorExecutor = Mock(ExecutorService)

        retry = new Retry(0, 1);

        queueListener = new MongoListener(
                messageRepository,
                mongoConverter,
                [messageHandler],
                queueEnabled,
                processorThreads,
                listenerExecutor,
                processorExecutor,
                retry)

        spiedQueueListener = Spy(MongoListener,
                constructorArgs: [
                        messageRepository,
                        mongoConverter,
                        [messageHandler],
                        queueEnabled,
                        processorThreads,
                        listenerExecutor,
                        processorExecutor,
                        retry
                ])

    }

    def "startListening: queueEnabled = true, sets up queue, submits listen job to executor"(){
        when:
        spiedQueueListener.startListening()

        then:
        1 * spiedQueueListener.createQueue() >> {}
        1 * spiedQueueListener.setupCursor() >> {}
        1 * spiedQueueListener.getMessageStats() >> {}

        then:
        1 * listenerExecutor.submit(_)
    }

    def "startListening: queueEnabled = false"(){
        setup:
        spiedQueueListener = Spy(MongoListener,
                constructorArgs: [
                        messageRepository,
                        mongoConverter,
                        [messageHandler],
                        false,
                        processorThreads,
                        listenerExecutor,
                        processorExecutor,
                        retry
                ])

        when:
        spiedQueueListener.startListening()

        then:
        0 * spiedQueueListener.createQueue()
        0 * spiedQueueListener.setupCursor()
        0 * spiedQueueListener.getMessageStats()
        0 * listenerExecutor.submit(_)
    }

    def "createQueue: creates collections if it doesn't exist"(){
        setup:
        messageRepository.insertMessage(_) >> new Try.Success<Void>(null)

        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(false);
        1 * messageRepository.createCollection() >> new Try.Success<DBCollection>(Mock(DBCollection))
    }

    def "createQueue: does not create collection if it exists"(){
        setup:
        messageRepository.getCollection() >> new Try.Success<DBCollection>(dbCollection)
        messageRepository.insertMessage(_) >> new Try.Success<Void>(null)

        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(true);
        0 * messageRepository.createCollection()
    }

    def "createQueue: initializes collection with InitMessage if collection has no messages"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(true);

        then:
        1 * messageRepository.getCollection() >> new Try.Success<DBCollection>(dbCollection)
        1 * dbCollection.count() >> 0

        then:
        1 * messageRepository.insertMessage(_) >> new Try.Success<Void>(null)
    }

    def "createQueue: does not initialize collection with InitMessage if collection has messages"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(true);

        then:
        1 * messageRepository.getCollection() >> new Try.Success<DBCollection>(dbCollection)
        1 * dbCollection.count() >> 1

        then:
        0 * messageRepository.insertMessage(_)
    }

    def "createQueue: collectionExists fails, RuntimeException"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Failure<Boolean>(new Exception())
        0 * messageRepository.createCollection()
        0 * messageRepository.getCollection()
        0 * messageRepository.insertMessage(*_)

        then:
        thrown(RuntimeException)
    }

    def "createQueue: createCollection fails, RuntimeException"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(false)
        1 * messageRepository.createCollection() >> new Try.Failure<DBCollection>(new Exception())
        0 * messageRepository.insertMessage(*_)

        then:
        thrown(RuntimeException)
    }

    def "createQueue: getCollection fails, RuntimeException"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(true)
        1 * messageRepository.getCollection() >> new Try.Failure<DBCollection>(new Exception())
        0 * messageRepository.insertMessage(*_)

        then:
        thrown(RuntimeException)
    }

    def "createQueue: insertMessage fails, RuntimeException"(){
        when:
        spiedQueueListener.createQueue()

        then:
        1 * messageRepository.collectionExists() >> new Try.Success<Boolean>(true)
        1 * messageRepository.getCollection() >> new Try.Success<DBCollection>(dbCollection)
        1 * messageRepository.insertMessage(*_) >> new Try.Failure<Void>(new Exception())

        then:
        thrown(RuntimeException)
    }

    def "setupCursor: success"(){
        when:
        spiedQueueListener.setupCursor()

        then:
        1 * messageRepository.getCursor() >> new Try.Success<DBCursor>(dbCursor)

        then:
        spiedQueueListener.queueCursor == dbCursor
    }

    def "setupCursor: getCursor fails, runtime exception"(){
        when:
        spiedQueueListener.setupCursor()

        then:
        1 * messageRepository.getCursor() >> new Try.Failure<DBCursor>(new Exception())

        then:
        thrown(RuntimeException)
    }

    def "getMessageStats: success"(){
        setup:
        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId())

        when:
        spiedQueueListener.getMessageStats()

        then:
        1 * messageRepository.getMessageStats() >> new Try.Success<MessageStats>(messageStats)

        then:
        spiedQueueListener.messageStats == messageStats
    }

    def "getMessageStats: getMessageStats fails, runtime exception"(){
        when:
        spiedQueueListener.getMessageStats()

        then:
        1 * messageRepository.getMessageStats() >> new Try.Failure<MessageStats>(new Exception())

        then:
        thrown(RuntimeException)
    }

    def "getMessageStats: success w/null messagestats"(){
        when:
        spiedQueueListener.getMessageStats()

        then:
        1 * messageRepository.getMessageStats() >> new Try.Success<MessageStats>(null)

        then:
        spiedQueueListener.messageStats.getLastConsumedId().compareTo(new ObjectId(Date.from(Instant.EPOCH))) == -1
        //they'll be 1 off because we can never have the same ObjectId, so it increments it
    }

    def "updateLastConsumedId: success for newer ID"(){
        setup:
        ObjectId newObjectId = new ObjectId(Date.from(Instant.now()));
        spiedQueueListener.messageStats = new MessageStats(lastConsumedId: new ObjectId(Date.from(Instant.EPOCH)))

        when:
        spiedQueueListener.updateLastConsumedId(newObjectId)

        then:
        spiedQueueListener.messageStats.getLastConsumedId() == newObjectId
        1 * messageRepository.updateMessageStats(spiedQueueListener.messageStats) >> new Try.Success<Void>(null)
    }

    def "updateLastConsumedId: success for older ID"(){
        setup:
        ObjectId newObjectId = new ObjectId(Date.from(Instant.EPOCH));
        ObjectId currentObjectId = new ObjectId(Date.from(Instant.now()))
        spiedQueueListener.messageStats = new MessageStats(lastConsumedId: currentObjectId)

        when:
        spiedQueueListener.updateLastConsumedId(newObjectId)

        then:
        spiedQueueListener.messageStats.getLastConsumedId() == currentObjectId
        0 * messageRepository.updateMessageStats(_)
    }

    def "checkCursorConnection: not dropped"(){
        setup:
        spiedQueueListener.isConnected = true
        spiedQueueListener.queueCursor = dbCursor

        when:
        spiedQueueListener.checkCursorConnection();

        then:
        0 * dbCursor.close()
        0 * spiedQueueListener.stopListening()
        0 * spiedQueueListener.startListening()
    }

    def "checkCursorConnection: dropped"(){
        setup:
        spiedQueueListener.isConnected = false
        spiedQueueListener.queueCursor = dbCursor

        when:
        spiedQueueListener.checkCursorConnection();

        then:
        1 * dbCursor.close()
        1 * spiedQueueListener.stopListening() >> {}
        1 * spiedQueueListener.startListening() >> {}
    }

    def "stopListening: executor service shutdown quickly"(){
        when:
        queueListener.stopListening()

        then:
        queueListener.isListening == false
        1 * listenerExecutor.shutdown()
        1 * listenerExecutor.awaitTermination(_, _) >> true
    }

    def "stopListening: executor service shutdown slowly"(){
        when:
        queueListener.stopListening()

        then:
        queueListener.isListening == false
        1 * listenerExecutor.shutdown()
        1 * listenerExecutor.awaitTermination(_, _) >> false

        then:
        1 * listenerExecutor.shutdownNow()
        1 * listenerExecutor.awaitTermination(_, _) >> true
    }

    def "stopListening: executor service shutdown with InterruptedException"(){
        when:
        queueListener.stopListening()

        then:
        queueListener.isListening == false
        1 * listenerExecutor.shutdown()
        1 * listenerExecutor.awaitTermination(_, _) >> { throw new InterruptedException() }

        then:
        1 * listenerExecutor.shutdownNow()
    }

    def "listen: not listening!"(){
        setup:
        spiedQueueListener.isListening = false
        spiedQueueListener.queueCursor = dbCursor

        when:
        spiedQueueListener.listen()

        then:
        0 * dbCursor.hasNext()
        0 * processorExecutor.submit(_)
    }

    def "listen: listening, dbCursor has next"(){
        setup:
        dbObject.get("_id") >> new ObjectId(Date.from(Instant.now()))
        spiedQueueListener.isListening = true
        spiedQueueListener.queueCursor = dbCursor

        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId(Date.from(Instant.EPOCH)))
        spiedQueueListener.messageStats = messageStats

        when:
        spiedQueueListener.listen()

        then:
        1 * dbCursor.hasNext() >> true
        1 * dbCursor.next() >> dbObject
        1 * processorExecutor.submit(_)
        1 * spiedQueueListener.updateLastConsumedId(_) >> {}

        then:
        1 * dbCursor.hasNext() >> false
    }

    def "listen: listening, dbCursor does not have next"(){
        setup:
        spiedQueueListener.isListening = true
        spiedQueueListener.queueCursor = dbCursor

        when:
        spiedQueueListener.listen()

        then:
        1 * dbCursor.hasNext() >> false
        0 * processorExecutor.submit(_)
    }

    def "listen: listening, dbCursor throws MongoException"(){
        setup:
        spiedQueueListener.isListening = true
        spiedQueueListener.isConnected = true
        spiedQueueListener.queueCursor = dbCursor

        when:
        spiedQueueListener.listen()

        then:
        1 * dbCursor.hasNext() >> {throw new MongoException("error")}
        0 * processorExecutor.submit(_)
        !spiedQueueListener.isConnected
    }

    def "processMessage: message received is old"(){
        setup:
        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId())
        spiedQueueListener.messageStats = messageStats

        dbObject.get("_id") >> new ObjectId(Date.from(Instant.EPOCH))
        dbObject.get("_class") >> "someClass"

        when:
        spiedQueueListener.processMessage(dbObject)

        then:
        0 * messageHandler.canHandle(*_)
        0 * messageHandler.handleMessage(*_)
    }

    def "processMessage: message received is new, handled"(){
        setup:
        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId(Date.from(Instant.EPOCH)))
        spiedQueueListener.messageStats = messageStats

        dbObject.get("_id") >> new ObjectId()
        dbObject.get("_class") >> QueueMessage.getClass().getName()

        when:
        spiedQueueListener.processMessage(dbObject)

        then:
        1 * mongoConverter.read(_, dbObject) >> queueMessage

        then:
        1 * messageHandler.canHandle(queueMessage) >> true
        1 * messageHandler.handleMessage(queueMessage) >> new Try.Success<Void>(null)
    }

    def "processMessage: message received is new, not handled"(){
        setup:
        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId(Date.from(Instant.EPOCH)))
        spiedQueueListener.messageStats = messageStats

        dbObject.get("_id") >> new ObjectId()
        dbObject.get("_class") >> QueueMessage.getClass().getName()

        when:
        spiedQueueListener.processMessage(dbObject)

        then:
        1 * mongoConverter.read(_, dbObject) >> queueMessage

        then:
        1 * messageHandler.canHandle(queueMessage) >> false
        0 * messageHandler.handleMessage(*_)
    }

    def "processMessage: message received is new, handled w/exception"(){
        setup:
        MessageStats messageStats = new MessageStats()
        messageStats.setLastConsumedId(new ObjectId(Date.from(Instant.EPOCH)))
        spiedQueueListener.messageStats = messageStats

        dbObject.get("_id") >> new ObjectId()
        dbObject.get("_class") >> QueueMessage.getClass().getName()

        when:
        spiedQueueListener.processMessage(dbObject)

        then:
        1 * mongoConverter.read(_, dbObject) >> queueMessage

        then:
        1 * messageHandler.canHandle(queueMessage) >> true
        1 * messageHandler.handleMessage(queueMessage) >> { throw new Exception() }
    }
}
