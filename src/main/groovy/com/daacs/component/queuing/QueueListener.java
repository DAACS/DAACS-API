package com.daacs.component.queuing;

/**
 * Created by chostetter on 8/9/16.
 */
public interface QueueListener<T> {
    void startListening();
    void stopListening();
    void listen();
    void processMessage(T message);
}
