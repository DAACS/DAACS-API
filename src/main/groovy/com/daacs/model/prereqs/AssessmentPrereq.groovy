package com.daacs.model.prereqs

import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.user.CompletionStatus
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class AssessmentPrereq extends Prerequisite {

    @NotNull
    AssessmentCategory assessmentCategory;

    @NotNull
    @Size(min = 1)
    List<CompletionStatus> statuses;
}
