package com.daacs.model.queue

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Created by chostetter on 8/10/16.
 */
@Document(collection = "message_stats")
public class MessageStats {
    @Id
    final String id = "message_stats";

    ObjectId lastConsumedId;
}
