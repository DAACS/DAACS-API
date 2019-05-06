package com.daacs.model.dto

import com.daacs.model.assessment.AssessmentCategoryGroup
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ScoringType
import com.daacs.model.dto.assessmentUpdate.DomainRequest
import com.daacs.model.dto.assessmentUpdate.RubricRequest
import com.daacs.model.prereqs.Prerequisite
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty

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
        @JsonSubTypes.Type(value = CATItemGroupUpdateAssessmentRequest.class, name = "CAT"),
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
    @NotBlank
    String assessmentCategory;

    @NotNull
    AssessmentCategoryGroup assessmentCategoryGroup;

    @NotEmpty
    String label;

    @NotNull
    Boolean enabled;

    @NotNull
    @Valid
    List<Prerequisite> prerequisites;

    @NotNull
    Map<String, String> content;

    @Valid
    @NotNull
    RubricRequest overallRubric;

    @Valid
    @NotNull
    List<DomainRequest> domains;

    @NotNull
    ScoringType scoringType;
}
