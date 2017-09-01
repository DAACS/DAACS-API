package com.daacs.model.assessment

import com.daacs.model.assessment.user.UserAssessmentSummary
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class AssessmentCategorySummary {

    AssessmentCategory assessmentCategory;

    AssessmentSummary enabledAssessmentSummary;

    UserAssessmentSummary latestUserAssessmentSummary;

    Boolean userHasTakenCategory;

}
