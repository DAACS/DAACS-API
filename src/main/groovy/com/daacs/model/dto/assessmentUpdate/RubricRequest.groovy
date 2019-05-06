package com.daacs.model.dto.assessmentUpdate

import com.daacs.model.assessment.user.CompletionScore
import com.google.common.collect.Range

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/20/16.
 */
class RubricRequest {

    @NotNull
    Map<CompletionScore, Range<Double>> completionScoreMap;

    @Valid
    @NotNull
    List<SupplementTableRowRequest> supplementTable;
}
