package com.daacs.model.dto

import com.daacs.model.dto.assessmentUpdate.WritingPromptRequest

import javax.validation.constraints.NotNull

/**
 * Created by alandistasio on 10/25/16.
 */
class WritingUpdateAssessmentRequest extends UpdateAssessmentRequest {

    @NotNull
    WritingPromptRequest writingPrompt
}
