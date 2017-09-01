package com.daacs.model.dto

import com.daacs.framework.validation.annotations.ValidUpdateUserAssessmentRequest
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.DomainScore

import javax.validation.Valid
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/14/16.
 */
@ValidUpdateUserAssessmentRequest
class UpdateUserAssessmentRequest {
    @NotNull
    String id;

    @NotNull
    CompletionStatus status;

    String userId;

    @Valid
    List<DomainScore> domainScores = [];

    CompletionScore overallScore;
}
