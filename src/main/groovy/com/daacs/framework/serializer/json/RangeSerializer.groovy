package com.daacs.framework.serializer.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.google.common.collect.BoundType
import com.google.common.collect.Range
/**
 * Created by chostetter on 7/7/16.
 */

public class RangeSerializer extends JsonSerializer<Range<Comparable>> {
    public void serialize(Range<Comparable> range, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {

        String lowerBoundChar = range.hasLowerBound()? (range.lowerBoundType() == BoundType.OPEN ? "(" : "[") : "(";
        String upperBoundChar = range.hasUpperBound()? (range.upperBoundType() == BoundType.OPEN ? ")" : "]") : ")";

        String lowerEndpoint = range.hasLowerBound()? range.lowerEndpoint().toString() : "-INF";
        String upperEndpoint = range.hasUpperBound()? range.upperEndpoint().toString() : "INF";

        jgen.writeString(String.format("%s%s,%s%s",
                lowerBoundChar,
                lowerEndpoint,
                upperEndpoint,
                upperBoundChar
        ));
    }
}