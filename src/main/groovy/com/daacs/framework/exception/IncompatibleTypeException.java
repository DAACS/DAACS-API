package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.assessment.AssessmentType;
import com.daacs.model.assessment.ScoringType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/25/16.
 */
public class IncompatibleTypeException extends FailureTypeException {


    public IncompatibleTypeException(String entityType, ScoringType[] expectedTypes, ScoringType actualType) {
        super(
                getCode(entityType),
                getDetail(entityType, Arrays.asList(expectedTypes), actualType),
                FailureType.NOT_RETRYABLE,
                getMeta(entityType, Arrays.asList(expectedTypes), actualType)
        );
    }

    public IncompatibleTypeException(String entityType, AssessmentType[] expectedTypes, AssessmentType actualType) {
        super(
                getCode(entityType),
                getDetail(entityType, Arrays.asList(expectedTypes), actualType),
                FailureType.NOT_RETRYABLE,
                getMeta(entityType, Arrays.asList(expectedTypes), actualType)
        );
    }

    public IncompatibleTypeException(String entityType, String[] expectedTypes, String actualType) {
        super(
                getCode(entityType),
                getDetail(entityType, Arrays.asList(expectedTypes), actualType),
                FailureType.NOT_RETRYABLE,
                getMeta(entityType, Arrays.asList(expectedTypes), actualType)
        );
    }

    private static String getCode(String entityType){
        return entityType + ".incompatibleType";
    }


    private static <T> String getDetail(String entityType, List<T> expectedTypes, T actualType){
        List<String> expectedTypesAsStrings = expectedTypes.stream()
                .map(expectedType -> expectedType.toString())
                .collect(Collectors.toList());

        return "Incompatible type for " + entityType + ", expected " + String.join(",", expectedTypesAsStrings) + " but got " + (actualType != null? actualType.toString() : "null");
    }


    private static <T> Map<String, Object> getMeta(String entityType, List<T> expectedTypes, T actualType){
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_type", entityType);
        meta.put("actual", actualType);
        meta.put("expected", expectedTypes);

        return meta;
    }
}
