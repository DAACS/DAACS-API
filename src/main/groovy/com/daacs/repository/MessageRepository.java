package com.daacs.repository;

import com.daacs.model.queue.MessageStats;
import com.daacs.model.queue.QueueMessage;
import com.lambdista.util.Try;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

/**
 * Created by chostetter on 8/10/16.
 */
public interface MessageRepository {
    Try<DBCursor> getCursor();
    Try<Boolean> collectionExists();
    Try<DBCollection> createCollection();
    Try<Void> insertMessage(QueueMessage queueMessage);
    Try<MessageStats> getMessageStats();
    Try<Void> updateMessageStats(MessageStats messageStats);
    Try<DBCollection> getCollection();
}
