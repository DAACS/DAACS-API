package com.daacs.framework.serializer.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.google.common.collect.BoundType
import com.google.common.collect.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Created by chostetter on 7/7/16.
 */

public class RangeDeserializer extends JsonDeserializer<Range<Comparable>> {
    protected static final Logger log = LoggerFactory.getLogger(RangeDeserializer.class);

    @Override
    public Range<Comparable> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        String expression = node.asText();

        List<String> validUpperBoundChars = [ ")", "]" ]
        List<String> validLowerBoundChars = [ "(", "[" ]

        String lowerBoundChar = expression.substring(0, 1);
        String upperBoundChar = expression.substring(expression.length() - 1, expression.length());

        if(!validUpperBoundChars.contains(upperBoundChar)){
            throw new IllegalArgumentException("Invalid upper bound character");
        }

        if(!validLowerBoundChars.contains(lowerBoundChar)){
            throw new IllegalArgumentException("Invalid lower bound character");
        }

        String[] numbers = expression.substring(1, expression.length() - 1).split(",")
        if(numbers.length != 2){
            throw new IllegalArgumentException("Invalid range");
        }

        if(numbers[0] == "-INF" && numbers[1] == "INF"){
            return Range.all()
        }

        if(numbers[0] == "-INF"){
            return Range.atMost(parseNumber(numbers[1]))
        }

        if(numbers[1] == "INF"){
            return Range.atLeast(parseNumber(numbers[0]))
        }

        BoundType lowerType = lowerBoundChar.equals("(") ? BoundType.OPEN : BoundType.CLOSED;
        BoundType upperType = upperBoundChar.equals(")") ? BoundType.OPEN : BoundType.CLOSED;

        if(lowerType == BoundType.OPEN && upperType == BoundType.OPEN){
            return Range.open(parseNumber(numbers[0]), parseNumber(numbers[1]))
        }

        if(lowerType == BoundType.CLOSED && upperType == BoundType.CLOSED){
            return Range.closed(parseNumber(numbers[0]), parseNumber(numbers[1]))
        }

        if(lowerType == BoundType.OPEN && upperType == BoundType.CLOSED){
            return Range.openClosed(parseNumber(numbers[0]), parseNumber(numbers[1]))
        }

        if(lowerType == BoundType.CLOSED && upperType == BoundType.OPEN){
            return Range.closedOpen(parseNumber(numbers[0]), parseNumber(numbers[1]))
        }
    }

    private static Comparable parseNumber(String number){
        if(number.contains(".")){
            try{
                return Double.parseDouble(number);
            }
            catch(NumberFormatException ex){
                log.debug(ex.getMessage(), ex);
            }
        }

        try{
            return Integer.parseInt(number);
        }
        catch(NumberFormatException ex){
            log.debug(ex.getMessage(), ex);
        }

        throw new NumberFormatException("Unable to parse number " + number + " during Range marshalling");
    }
}