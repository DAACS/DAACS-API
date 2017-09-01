package com.daacs.model.dto

import com.daacs.model.assessment.AssessmentType
import com.daacs.model.dto.assessmentUpdate.DomainRequest
import com.daacs.model.dto.assessmentUpdate.RubricRequest
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

import javax.validation.Valid
import javax.validation.constraints.NotNull
/**
 * Created by chostetter on 8/30/16.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "assessmentType",
        visible = true)
@JsonSubTypes([
        @JsonSubTypes.Type(value = ItemGroupUpdateAssessmentRequest.class, name = "CAT"),
        @JsonSubTypes.Type(value = ItemGroupUpdateAssessmentRequest.class, name = "MULTIPLE_CHOICE"),
        @JsonSubTypes.Type(value = ItemGroupUpdateAssessmentRequest.class, name = "LIKERT"),
        @JsonSubTypes.Type(value = WritingUpdateAssessmentRequest.class, name = "WRITING_PROMPT")
])
abstract class UpdateAssessmentRequest {
    @NotNull
    String id;

    @NotNull
    AssessmentType assessmentType;

    @NotNull
    Boolean enabled;

    @NotNull
    Map<String, String> content;

    @Valid
    @NotNull
    RubricRequest overallRubric;

    @Valid
    @NotNull
    List<DomainRequest> domains;
}
