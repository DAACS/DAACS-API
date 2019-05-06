package com.daacs.model.assessment

import com.daacs.framework.serializer.Views
import com.daacs.framework.validation.annotations.ValidWritingAssessment
import com.daacs.framework.validation.annotations.group.UpdateGroup
import com.daacs.model.item.WritingPrompt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonView

import javax.validation.Valid
import javax.validation.constraints.NotNull

/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
@ValidWritingAssessment(groups = UpdateGroup.class)
class WritingAssessment extends Assessment {

    @NotNull
    @Valid
    WritingPrompt writingPrompt = new WritingPrompt()

    @Valid
    @JsonView([Views.NotExport])
    LightSideConfig lightSideConfig;
}
