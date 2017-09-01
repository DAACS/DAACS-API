package com.daacs.model.dto

import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/14/16.
 */
class CreateUserAssessmentRequest {
    @NotNull
    String assessmentId;
}
