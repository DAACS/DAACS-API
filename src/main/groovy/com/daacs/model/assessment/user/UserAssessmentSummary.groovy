package com.daacs.model.assessment.user

import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ScoringType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

import java.time.Instant
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
@ApiModel
public class UserAssessmentSummary {

    String userAssessmentId;

    String userId;

    String username;

    String firstName;

    String lastName;

    String assessmentId;

    AssessmentType assessmentType;

    AssessmentCategory assessmentCategory;

    String assessmentLabel;

    @ApiModelProperty(dataType = "java.lang.String")
    Instant takenDate;

    CompletionStatus status;

    Double progressPercentage;

    @ApiModelProperty(dataType = "java.lang.String")
    Instant completionDate;

    List<DomainScore> domainScores;

    CompletionScore overallScore;

    ScoringType scoringType;

}
