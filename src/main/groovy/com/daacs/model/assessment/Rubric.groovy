package com.daacs.model.assessment

import com.daacs.model.assessment.user.CompletionScore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.common.collect.Range

import javax.validation.Valid
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 7/5/16.
 */
@JsonIgnoreProperties(["metaClass"])
public class Rubric {
    @NotNull
    Map<CompletionScore, Range<Double>> completionScoreMap;

    @NotNull
    @Valid
    List<SupplementTableRow> supplementTable;
}
