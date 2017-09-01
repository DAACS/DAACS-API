package com.daacs.model.assessment

import com.daacs.model.assessment.user.UserAssessmentSummary
import com.daacs.model.prereqs.Prerequisite
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class AssessmentSummary {

    String assessmentId;

    AssessmentType assessmentType;

    AssessmentCategory assessmentCategory;

    String label;

    Map<String, String> content;

    Boolean enabled;

    List<Prerequisite> prerequisites;

    Boolean userPassesPrerequisites;

    UserAssessmentSummary userAssessmentSummary;

    @JsonProperty("userHasTakenAssessment")
    public Boolean userHasTakenAssessment(){
        return userAssessmentSummary != null;
    }
}
