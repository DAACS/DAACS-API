package com.daacs.model.queue

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
/**
 * Created by chostetter on 8/9/16.
 */

@JsonIgnoreProperties(["metaClass"])
@Document(collection = "messages")
public abstract class QueueMessage {
    @Id
    ObjectId id;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant dateCreated = Instant.now();
}
