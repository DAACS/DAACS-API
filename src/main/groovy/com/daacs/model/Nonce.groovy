package com.daacs.model

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

import java.time.Instant

@Document
class Nonce {

    String id

    @Field
    @Indexed(name="someDateFieldIndex", expireAfterSeconds=3600)
    Instant createdDate;

    Nonce(String id) {
        this.id = id
        this.createdDate = Instant.now()
    }
}
