package com.daacs.repository;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.model.queue.MessageStats;
import com.daacs.model.queue.QueueMessage;
import com.lambdista.util.Try;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * Created by chostetter on 8/10/16.
 */

@Repository
public class MessageRepositoryImpl implements MessageRepository {

    @Value("${queue.messageCollectionName}")
    private String messageCollectionName;

    @Value("${queue.statsCollectionName}")
    private String statsCollectionName;

    @Value("${queue.maxDocuments}")
    private int maxDocuments;

    @Value("${queue.size}")
    private int size;

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Try<DBCursor> getCursor() {
        return hystrixCommandFactory.getMongoTailableCursorCommand("MessageRepositoryImpl-getCursor", mongoTemplate, messageCollectionName).execute();
    }

    @Override
    public Try<Boolean> collectionExists(){
        return hystrixCommandFactory.getMongoCollectionExistsCommand("MessageRepositoryImpl-collectionExists", mongoTemplate, messageCollectionName).execute();
    }

    @Override
    public Try<DBCollection> createCollection(){
        CollectionOptions collectionOptions = new CollectionOptions(size, maxDocuments, true);
        return hystrixCommandFactory.getMongoCreateCollectionCommand("MessageRepositoryImpl-createCollection", mongoTemplate, messageCollectionName, collectionOptions).execute();
    }

    @Override
    public Try<DBCollection> getCollection(){
        return hystrixCommandFactory.getMongoGetCollectionCommand("MessageRepositoryImpl-getCollection", mongoTemplate, messageCollectionName).execute();
    }

    @Override
    public Try<MessageStats> getMessageStats(){
        return hystrixCommandFactory.getMongoFindByIdCommand("MessageRepositoryImpl-getMessageStats", mongoTemplate, "message_stats", MessageStats.class).execute();
    }

    @Override
    public Try<Void> updateMessageStats(MessageStats messageStats){
       return hystrixCommandFactory.getMongoSaveCommand("MessageRepositoryImpl-updateMessageStats", mongoTemplate, messageStats).execute();
    }

    @Override
    public Try<Void> insertMessage(QueueMessage queueMessage){
        return hystrixCommandFactory.getMongoInsertByCollectionCommand("MessageRepositoryImpl-insertMessage", mongoTemplate, queueMessage, messageCollectionName).execute();
    }
}
