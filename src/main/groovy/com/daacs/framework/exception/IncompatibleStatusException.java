package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.assessment.user.CompletionStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/25/16.
 */
public class IncompatibleStatusException extends FailureTypeException {

    public IncompatibleStatusException(String entityType, CompletionStatus[] expectedStatuses, CompletionStatus actualStatus, String userAssessmentId) {
        super(
                getCode(entityType),
                getDetail(entityType, Arrays.asList(expectedStatuses), actualStatus, userAssessmentId),
                FailureType.NOT_RETRYABLE,
                getMeta(entityType, Arrays.asList(expectedStatuses), actualStatus)
        );
    }

    private static String getCode(String entityType){
        return entityType + ".incompatibleStatus";
    }

    private static String getDetail(String entityType, List<CompletionStatus> expectedStatuses, CompletionStatus actualStatus, String userAssessmentId){

        List<String> expectedStatusesStrings = expectedStatuses.stream()
                .map(expectedStatus -> expectedStatus.toString())
                .collect(Collectors.toList());

        return "Incompatible status for " + entityType + " (" + userAssessmentId + "), expected " + String.join(",", expectedStatusesStrings) + " but got " + (actualStatus != null ? actualStatus.toString() : "null");
    }

    private static Map<String, Object> getMeta(String entityType, List<CompletionStatus> expectedStatuses, CompletionStatus actualStatus){
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_type", entityType);
        meta.put("expected", expectedStatuses);
        meta.put("actual", actualStatus);

        return meta;
    }
}
