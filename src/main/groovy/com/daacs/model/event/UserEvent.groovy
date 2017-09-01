package com.daacs.model.event

import javax.validation.constraints.NotNull
import java.time.Instant

/**
 * Created by chostetter on 12/14/16.
 */
class UserEvent {

    String id = UUID.randomUUID().toString();

    @NotNull
    EventType eventType;

    Instant timestamp = Instant.now();

    Map<String, Object> eventData = [:];

    UserEvent() {}

    UserEvent(EventType eventType, Map<String, Object> eventData) {
        this.eventType = eventType
        this.eventData = eventData
    }
}
