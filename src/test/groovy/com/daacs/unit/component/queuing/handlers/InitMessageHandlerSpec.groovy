package com.daacs.unit.component.queuing.handlers

import com.daacs.component.queuing.handlers.InitMessageHandler
import com.daacs.model.queue.GradingMessage
import com.daacs.model.queue.InitMessage
import com.daacs.model.queue.QueueMessage
import com.lambdista.util.Try
import spock.lang.Specification
/**
 * Created by chostetter on 8/11/16.
 */
class InitMessageHandlerSpec extends Specification{

    InitMessageHandler messageHandler
    QueueMessage initMessage = new InitMessage();
    QueueMessage nonInitMessage = new GradingMessage();

    def setup(){
        messageHandler = new InitMessageHandler();
    }

    def "canHandle: handles InitMessages"(){
        when:
        Boolean canHandle = messageHandler.canHandle(initMessage)

        then:
        canHandle
    }

    def "canHandle: does not handle non-InitMessages"(){
        when:
        Boolean canHandle = messageHandler.canHandle(nonInitMessage)

        then:
        !canHandle
    }

    def "handle: returns success"(){
        when:
        Try<Void> maybeHandled = messageHandler.handleMessage((InitMessage) initMessage)

        then:
        maybeHandled.isSuccess()
    }
}
