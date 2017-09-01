package com.daacs.model.assessment

import com.daacs.model.assessment.user.CompletionScore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/7/16.
 */

@JsonIgnoreProperties(["metaClass"])
class SupplementTableRow {

    @NotNull
    CompletionScore completionScore;

    String content;

    String contentSummary;
}
