package com.daacs.framework.serializer.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant
/**
 * Created by chostetter on 7/7/16.
 */

public class InstantDeserializer extends JsonDeserializer<Instant> {
    protected static final Logger log = LoggerFactory.getLogger(InstantDeserializer.class);

    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        String instant = node.asText();

        return Instant.parse(instant);
    }
}