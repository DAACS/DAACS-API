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

public class RangeDeserializer extends JsonDeserializer<Range<Double>> {
    protected static final Logger log = LoggerFactory.getLogger(RangeDeserializer.class);

    @Override
    public Range<Double> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        String expression = node.asText();
        return deserializeHelper(expression);
    }

    public Range<Double> deserializeHelper(String expression) {


        List<String> validUpperBoundChars = [")", "]"]
        List<String> validLowerBoundChars = ["(", "["]

        if(expression.size() < 3){
            return null
        }

        String lowerBoundChar = expression.substring(0, 1);
        String upperBoundChar = expression.substring(expression.length() - 1, expression.length());

        if (!validUpperBoundChars.contains(upperBoundChar)) {
            throw new IllegalArgumentException("Invalid upper bound character");
        }

        if (!validLowerBoundChars.contains(lowerBoundChar)) {
            throw new IllegalArgumentException("Invalid lower bound character");
        }

        ArrayList<String> numbers = expression.substring(1, expression.length() - 1).split(",")
        numbers.removeIf { s -> s.isEmpty() }

        if (numbers.size() == 0) {
            return null
        } else if (numbers.size() == 1) {
            if (expression.equals('(-INF,]')) {
                numbers.add('0')
            } else if (expression.equals('[,INF)')) {
                numbers.add(0, '0')
            } else {
                return null;
            }
        } else if (numbers.size() != 2) {
            throw new IllegalArgumentException("Invalid range");
        }

        if (numbers[0] == "-INF" && numbers[1] == "INF") {
            return Range.all()
        }

        if (numbers[0] == "-INF") {
            if (upperBoundChar.equals(")")) {
                return Range.lessThan(parseNumbers(new ArrayList<>(Arrays.asList(numbers[1], "0")))[0])
            }
            return Range.atMost(parseNumbers(new ArrayList<>(Arrays.asList(numbers[1], "0")))[0])
        }

        if (numbers[1] == "INF") {
            if (lowerBoundChar.equals("(")) {
                return Range.greaterThan(parseNumbers(new ArrayList<>(Arrays.asList(numbers[0], "0")))[0])
            }
            return Range.atLeast(parseNumbers(new ArrayList<>(Arrays.asList(numbers[0], "0")))[0])
        }

        BoundType lowerType = lowerBoundChar.equals("(") ? BoundType.OPEN : BoundType.CLOSED;
        BoundType upperType = upperBoundChar.equals(")") ? BoundType.OPEN : BoundType.CLOSED;


        ArrayList<Comparable> parsedNumbers = parseNumbers(numbers)
        if (parsedNumbers[0] > (parsedNumbers[1])) {
            return null
        }

        if (lowerType == BoundType.OPEN && upperType == BoundType.OPEN) {
            if (parsedNumbers[0].equals(parsedNumbers[1])) {
                return null
            }
            return Range.open(parsedNumbers[0], parsedNumbers[1])
        }

        if (lowerType == BoundType.CLOSED && upperType == BoundType.CLOSED) {
            return Range.closed(parsedNumbers[0], parsedNumbers[1])
        }

        if (lowerType == BoundType.OPEN && upperType == BoundType.CLOSED) {
            return Range.openClosed(parsedNumbers[0], parsedNumbers[1])
        }

        if (lowerType == BoundType.CLOSED && upperType == BoundType.OPEN) {
            return Range.closedOpen(parsedNumbers[0], parsedNumbers[1])
        }
    }

    private static ArrayList<Comparable> parseNumbers(ArrayList<String> numbers) {

        try {
            return new ArrayList<>(Arrays.asList(Double.parseDouble(numbers[0]), Double.parseDouble(numbers[1])))
        }
        catch (NumberFormatException ex) {
            log.debug(ex.getMessage(), ex);
        }

        throw new NumberFormatException("Unable to parse numbers (" + numbers[0] + "," + numbers[1] + ") during Range marshalling");
    }
}