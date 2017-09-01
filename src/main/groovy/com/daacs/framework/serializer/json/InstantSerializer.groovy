package com.daacs.framework.serializer.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

import java.time.Instant
/**
 * Created by chostetter on 7/12/16.
 */
public class InstantSerializer extends JsonSerializer<Instant> {
    public void serialize(Instant instant, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(instant.toString());
    }
}