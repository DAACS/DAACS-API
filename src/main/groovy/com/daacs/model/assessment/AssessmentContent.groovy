package com.daacs.model.assessment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 8/8/16.
 */
@JsonIgnoreProperties(["metaClass"])
class AssessmentContent {

    String assessmentId;
    AssessmentType assessmentType;
    AssessmentCategory assessmentCategory;
    String label;
    Map<String, String> content;
    Rubric overallRubric;
    List<Domain> domains;

}
