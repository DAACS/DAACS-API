package com.daacs.model.event

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 12/14/16.
 */

@Document(collection = "event_containers")
class EventContainer {
    @Id
    String userId;

    @NotNull
    List<UserEvent> userEvents = [];

    @NotNull
    @Version
    Long version;
}
