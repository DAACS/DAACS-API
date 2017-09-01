package com.daacs.component.queuing;

import com.daacs.component.queuing.handlers.MessageHandler;
import com.daacs.model.queue.InitMessage;
import com.daacs.model.queue.MessageStats;
import com.daacs.model.queue.QueueMessage;
import com.daacs.repository.MessageRepository;
import com.lambdista.util.Try;
import com.mongodb.*;
import groovy.transform.Synchronized;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by chostetter on 8/9/16.
 */

@Scope("singleton")
@Component
public class MongoListener implements QueueListener<DBObject> {
    private static final Logger log = LoggerFactory.getLogger(MongoListener.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private List<MessageHandler> messageHandlers;

    @Value("${queue.enabled:true}")
    private boolean queueEnabled;

    @Value("${queue.processor.threads:1}")
    private int processorThreads;

    protected DBCursor queueCursor;
    private Retry retry;

    private ExecutorService listenerExecutor;
    private ExecutorService processorExecutor;

    protected boolean isListening = false;
    protected MessageStats messageStats;
    protected boolean isConnected = false;


    public MongoListener(){}
    public MongoListener(
            MessageRepository messageRepository,
            MongoConverter mongoConverter,
            List<MessageHandler> messageHandlers,
            boolean queueEnabled,
            int processorThreads,
            ExecutorService listenerExecutor,
            ExecutorService processorExecutor,
            Retry retry) {

        this.messageRepository = messageRepository;
        this.mongoConverter = mongoConverter;
        this.messageHandlers = messageHandlers;
        this.queueEnabled = queueEnabled;
        this.processorThreads = processorThreads;
        this.listenerExecutor = listenerExecutor;
        this.processorExecutor = processorExecutor;
        this.retry = retry;
    }

    @PostConstruct
    private void init(){
        retry = new Retry(5, 5);

        listenerExecutor = Executors.newSingleThreadExecutor();
        processorExecutor = Executors.newFixedThreadPool(processorThreads);
    }

    @Override
    public void startListening() {
        if(!queueEnabled){
            log.info("Message queue is not enabled!");
            return;
        }

        createQueue();
        setupCursor();
        getMessageStats();

        isListening = true;
        isConnected = true;
        listenerExecutor.submit(this::listen);
    }

    @PreDestroy
    @Override
    public void stopListening() {

        isListening = false;
        listenerExecutor.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if(!listenerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                listenerExecutor.shutdownNow(); // Cancel currently executing tasks

                if(!listenerExecutor.awaitTermination(10, TimeUnit.SECONDS)){
                    log.error("listenerExecutor did not terminate");
                }

            }
        }
        catch (InterruptedException ie) {
            listenerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }

    public void listen() {
        if(!isListening) return;

        log.info("Started listening on queue");

        try{
            while (queueCursor.hasNext()) {
                DBObject dbObject = queueCursor.next();
                ObjectId messageId = (ObjectId) dbObject.get("_id");

                if (messageId.compareTo(messageStats.getLastConsumedId()) <= 0) {
                    log.debug("Skipping message w/id {}", messageId.toString());
                    continue;
                }

                try{
                    processorExecutor.submit(() -> processMessage(dbObject));
                    updateLastConsumedId(messageId);
                }
                catch(Exception ex){
                    log.error("Error while processing or consuming messages", ex);
                }
            }
        }
        catch(MongoException ex){
            log.error("Error while listening for new messages", ex);
            isConnected = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void processMessage(DBObject dbObject) {
        ObjectId messageId = (ObjectId) dbObject.get("_id");
        String messageClass = (String) dbObject.get("_class");

        try {
            Instant start = Instant.now();
            log.info("Queue message received: {}, id: {}", messageClass, messageId.toString());

            Object message = mongoConverter.read(Class.forName(messageClass), dbObject);
            QueueMessage queueMessage = (QueueMessage) message;

            boolean handledMessage = false;
            for (MessageHandler messageHandler : messageHandlers) {
                if(!messageHandler.canHandle(queueMessage)) continue;

                retry.execute(() -> messageHandler.handleMessage(queueMessage));
                handledMessage = true;

                Instant end = Instant.now();
                log.info("Queue message completed: {}, id: {} in {} seconds", messageClass, messageId.toString(), ChronoUnit.SECONDS.between(start, end));

                break;
            }

            if (!handledMessage) {
                log.warn("Unable to process queue message of type {}", queueMessage.getClass());
            }

        }
        catch(Exception e) {
            log.error("Failure while processing message", e);
        }
    }

    protected void createQueue(){

        Try<Boolean> maybeCollectionExists = messageRepository.collectionExists();
        if(maybeCollectionExists.isFailure()){
            log.error("Unable to setup collection for MongoListener");
            throw new RuntimeException(maybeCollectionExists.failed().get());
        }


        DBCollection dbCollection;
        if(!maybeCollectionExists.get()){
            Try<DBCollection> maybeCreateCollection = messageRepository.createCollection();
            if(maybeCreateCollection.isFailure()){
                log.error("Unable to setup collection for MongoListener");
                throw new RuntimeException(maybeCreateCollection.failed().get());
            }

            dbCollection = maybeCreateCollection.get();
        }
        else{
            Try<DBCollection> maybeCollection = messageRepository.getCollection();
            if(maybeCollection.isFailure()){
                log.error("Unable to setup collection for MongoListener");
                throw new RuntimeException(maybeCollection.failed().get());
            }

            dbCollection = maybeCollection.get();
        }


        if(dbCollection.count() == 0){
            //no messages? initialize queue with a starter message
            Try<Void> maybeInsertInitMessage = messageRepository.insertMessage(new InitMessage());
            if(maybeInsertInitMessage.isFailure()){
                log.error("Unable to setup collection for MongoListener");
                throw new RuntimeException(maybeInsertInitMessage.failed().get());
            }
        }

    }

    protected void setupCursor(){
        Try<DBCursor> maybeDbCursor = messageRepository.getCursor();
        if(maybeDbCursor.isFailure()){
            log.error("Unable to setup cursor for MongoListener");
            throw new RuntimeException(maybeDbCursor.failed().get());
        }

        queueCursor = maybeDbCursor.get();
    }

    protected void getMessageStats(){
        Try<MessageStats> maybeMessageStats = messageRepository.getMessageStats();
        if(maybeMessageStats.isFailure()){
            log.error("Unable to get message stats for MongoListener");
            throw new RuntimeException(maybeMessageStats.failed().get());
        }

        messageStats = maybeMessageStats.get();

        if(messageStats == null){
            messageStats = new MessageStats();
            messageStats.setLastConsumedId(new ObjectId(Date.from(Instant.EPOCH)));
        }
    }

    @Synchronized
    protected void updateLastConsumedId(ObjectId messageId) throws Exception{
        //don't update if messageId is before the last one.
        if (messageId.compareTo(messageStats.getLastConsumedId()) <= 0) {
            return;
        }

        messageStats.setLastConsumedId(messageId);
        retry.execute(() -> messageRepository.updateMessageStats(messageStats));
    }

    @Scheduled(cron = "*/10 * * * * *")
    protected void checkCursorConnection() {
        if(!queueEnabled){
            return;
        }

        if(!isConnected){
            log.error("Connection lost for MongoListener's DBCursor, restarting...");

            //kill the cursor, we lost connection.
            try {
                queueCursor.close();
            }
            catch(IllegalStateException ex){
                log.debug("IllegalStateException while closing cursor, ignoring");
            }

            stopListening();
            init();
            startListening();

            return;
        }

        log.debug("MongoListener's DBCursor connection is healthy");
    }
}