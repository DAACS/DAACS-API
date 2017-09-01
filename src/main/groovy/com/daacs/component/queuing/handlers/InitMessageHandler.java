package com.daacs.component.queuing.handlers;

import com.daacs.model.queue.InitMessage;
import com.daacs.model.queue.QueueMessage;
import com.lambdista.util.Try;
import org.springframework.stereotype.Component;

/**
 * Created by chostetter on 8/11/16.
 */

@Component
public class InitMessageHandler implements  MessageHandler<InitMessage> {
    @Override
    public Try<Void> handleMessage(InitMessage queueMessage) throws Exception {
        //do nothing, eat it.
        return new Try.Success<>(null);
    }

    @Override
    public boolean canHandle(QueueMessage queueMessage) {
        return queueMessage instanceof InitMessage;
    }
}
