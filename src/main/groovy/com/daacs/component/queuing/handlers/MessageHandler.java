package com.daacs.component.queuing.handlers;

import com.daacs.model.queue.QueueMessage;
import com.lambdista.util.Try;

/**
 * Created by chostetter on 8/11/16.
 */
public interface MessageHandler<T extends QueueMessage> {
    Try<Void> handleMessage(T queueMessage) throws Exception;
    boolean canHandle(QueueMessage queueMessage);
}
