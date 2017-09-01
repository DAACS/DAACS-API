package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.assessment.user.CompletionScore

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class SupplementTableRowRequest {
    @NotNull
    CompletionScore completionScore;

    @NotNull
    String content;

    String contentSummary;
}
