package com.daacs.model.assessment.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel

import java.time.Instant

/**
 * Created by chostetter on 4/3/17.
 */
@JsonIgnoreProperties(["metaClass"])
@ApiModel
class UserAssessmentTakenDate {
    String userId;
    String assessmentId;
    Instant takenDate;
}
