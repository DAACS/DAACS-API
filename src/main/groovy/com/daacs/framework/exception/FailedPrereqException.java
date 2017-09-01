package com.daacs.framework.exception;

import com.daacs.model.ErrorContainer;
import com.daacs.model.prereqs.Prerequisite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 8/2/16.
 */
public class FailedPrereqException extends ErrorContainerException {

    public FailedPrereqException(String assessmentId, List<Prerequisite> failedPrerequisites) {
        super("prereq.failed", "Prerequisities not met", buildErrorContainers(failedPrerequisites));
    }

    private static List<ErrorContainer> buildErrorContainers(List<Prerequisite> failedPrerequisites){
        return failedPrerequisites.stream()
                .map(FailedPrereqException::buildErrorContainer)
                .collect(Collectors.toList());
    }

    private static ErrorContainer buildErrorContainer(Prerequisite prerequisite){
        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setCode(prerequisite.getPrereqType() + ".failedPrereq");
        errorContainer.setDetail(prerequisite.getReason());

        Map<String, Object> meta = new HashMap<>();
        meta.put("prereq_type", prerequisite.getPrereqType());
        meta.put("reason", prerequisite.getReason());

        errorContainer.setMeta(meta);

        return errorContainer;
    }
}
